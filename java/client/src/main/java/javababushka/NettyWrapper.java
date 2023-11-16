package javababushka;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.UnixChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import response.ResponseOuterClass;

class NettyWrapper implements AutoCloseable {
  private final String unixSocket = getSocket();

  private static String getSocket() {
    try {
      return RustWrapper.startSocketListenerExternal();
    } catch (Exception | UnsatisfiedLinkError e) {
      System.err.printf("Failed to get UDS from babushka and dedushka: %s%n%n", e);
      throw new RuntimeException(e);
    }
  }

  @Getter private Channel channel = null;
  private EventLoopGroup group = null;

  // Futures to handle responses. Index is callback id, starting from 1 (0 index is for connection
  // request always).
  // Is it not a concurrent nor sync collection, but it is synced on adding. No removes.
  @Getter
  private final Map<Integer, CompletableFuture<ResponseOuterClass.Response>> responses =
      new HashMap<>();

  // We support MacOS and Linux only, because Babushka does not support Windows, because tokio does
  // not support it.
  // Probably we should use NIO (NioEventLoopGroup) for Windows.
  private static final boolean isMacOs = isMacOs();

  private static boolean isMacOs() {
    try {
      Class.forName("io.netty.channel.kqueue.KQueue");
      return KQueue.isAvailable();
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  static {
    // TODO fix: netty still doesn't use slf4j nor log4j
    InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
  }

  public static NettyWrapper INSTANCE = new NettyWrapper();

  private NettyWrapper() {
    // TODO maybe move to constructor or to static?
    try {
      channel =
          new Bootstrap()
              .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024, 4096))
              .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
              .group(group = isMacOs ? new KQueueEventLoopGroup() : new EpollEventLoopGroup())
              .channel(isMacOs ? KQueueDomainSocketChannel.class : EpollDomainSocketChannel.class)
              .handler(
                  new ChannelInitializer<UnixChannel>() {
                    @Override
                    public void initChannel(UnixChannel ch) throws Exception {
                      ch.pipeline()
                          .addLast("logger", new LoggingHandler(LogLevel.DEBUG))
                          // https://netty.io/4.1/api/io/netty/handler/codec/protobuf/ProtobufEncoder.html
                          .addLast("protobufDecoder", new ProtobufVarint32FrameDecoder())
                          .addLast("protobufEncoder", new ProtobufVarint32LengthFieldPrepender())
                          .addLast(
                              new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg)
                                    throws Exception {
                                  // System.out.printf("=== channelRead %s %s %n", ctx, msg);
                                  var buf = (ByteBuf) msg;
                                  var bytes = new byte[buf.readableBytes()];
                                  buf.readBytes(bytes);
                                  // TODO surround parsing with try-catch, set error to future if
                                  // parsing failed.
                                  var response = ResponseOuterClass.Response.parseFrom(bytes);
                                  int callbackId = response.getCallbackIdx();
                                  // System.out.printf("== Received response with callback %d%n",
                                  responses.get(callbackId).complete(response);
                                  if (callbackId != 0) {
                                    responses.remove(callbackId);
                                  }
                                  super.channelRead(ctx, bytes);
                                }

                                @Override
                                public void exceptionCaught(
                                    ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                  System.out.printf("=== exceptionCaught %s %s %n", ctx, cause);
                                  cause.printStackTrace();
                                  super.exceptionCaught(ctx, cause);
                                }
                              })
                          .addLast(
                              new ChannelOutboundHandlerAdapter() {
                                @Override
                                public void write(
                                    ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
                                    throws Exception {
                                  // System.out.printf("=== write %s %s %s %n", ctx, msg, promise);
                                  var bytes = (byte[]) msg;

                                  super.write(ctx, Unpooled.copiedBuffer(bytes), promise);
                                }
                              });
                    }
                  })
              .connect(new DomainSocketAddress(unixSocket))
              .sync()
              .channel();

    } catch (Exception e) {
      System.err.printf(
          "Failed to create a channel %s: %s%n", e.getClass().getSimpleName(), e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  public void registerRequest(
      Integer callbackId, CompletableFuture<ResponseOuterClass.Response> future) {
    responses.put(callbackId, future);
  }

  @Override
  public void close() throws Exception {
    // channel.closeFuture().sync()
    group.shutdownGracefully();
  }
}
