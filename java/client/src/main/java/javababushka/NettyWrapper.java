package javababushka;

import static response.ResponseOuterClass.Response;

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
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
class NettyWrapper {
  private final String unixSocket = getSocket();

  private static String getSocket() {
    try {
      return BabushkaCoreNativeDefinitions.startSocketListenerExternal();
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
  private final Map<Integer, CompletableFuture<Response>> responses = new ConcurrentHashMap<>();

  // We support MacOS and Linux only, because Babushka does not support Windows, because tokio does
  // not support it.
  // Probably we should use NIO (NioEventLoopGroup) for Windows.
  private static final boolean isMacOs = isMacOs();

  // TODO support IO-Uring and NIO
  private static boolean isMacOs() {
    try {
      Class.forName("io.netty.channel.kqueue.KQueue");
      return KQueue.isAvailable();
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  public static NettyWrapper INSTANCE = null;

  private NettyWrapper() {
    try {
      int cpuCount = Runtime.getRuntime().availableProcessors();
      group =
          isMacOs
              ? new KQueueEventLoopGroup(
                  cpuCount, new DefaultThreadFactory("NettyWrapper-kqueue-elg", true))
              : new EpollEventLoopGroup(
                  cpuCount, new DefaultThreadFactory("NettyWrapper-epoll-elg", true));
      channel =
          new Bootstrap()
              .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024, 4096))
              .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
              .group(group)
              .channel(isMacOs ? KQueueDomainSocketChannel.class : EpollDomainSocketChannel.class)
              .handler(
                  new ChannelInitializer<UnixChannel>() {
                    @Override
                    public void initChannel(@NonNull UnixChannel ch) throws Exception {
                      ch.pipeline()
                          .addLast("logger", new LoggingHandler(LogLevel.DEBUG))
                          // https://netty.io/4.1/api/io/netty/handler/codec/protobuf/ProtobufEncoder.html
                          .addLast("protobufDecoder", new ProtobufVarint32FrameDecoder())
                          .addLast("protobufEncoder", new ProtobufVarint32LengthFieldPrepender())
                          .addLast(
                              new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRead(
                                    @NonNull ChannelHandlerContext ctx, @NonNull Object msg)
                                    throws Exception {
                                  // System.out.printf("=== channelRead %s %s %n", ctx, msg);
                                  var buf = (ByteBuf) msg;
                                  var bytes = new byte[buf.readableBytes()];
                                  buf.readBytes(bytes);
                                  // TODO surround parsing with try-catch, set error to future if
                                  // parsing failed.
                                  var response = Response.parseFrom(bytes);
                                  int callbackId = response.getCallbackIdx();
                                  // System.out.printf("== Received response with callback %d%n",
                                  responses.get(callbackId).complete(response);
                                  if (callbackId != 0) {
                                    responses.remove(callbackId);
                                  }
                                  buf.release();
                                }

                                @Override
                                public void exceptionCaught(
                                    ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                  System.out.printf("=== exceptionCaught %s %s %n", ctx, cause);
                                  cause.printStackTrace(System.err);
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

  public void registerRequest(Integer callbackId, CompletableFuture<Response> future) {
    responses.put(callbackId, future);
  }

  public void close() {
    channel.close();
    group.shutdownGracefully();
  }

  private static class ShutdownHook implements Runnable {
    @Override
    public void run() {
      if (INSTANCE != null) {
        INSTANCE.close();
      }
    }
  }

  static {
    INSTANCE = new NettyWrapper();
    Runtime.getRuntime()
        .addShutdownHook(new Thread(new ShutdownHook(), "NettyWrapper-shutdown-hook"));
  }
}
