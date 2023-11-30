package babushka.connection;

import static response.ResponseOuterClass.Response;

import babushka.BabushkaCoreNativeDefinitions;
import com.google.protobuf.GeneratedMessageV3;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketManager {
  private final String unixSocket = getSocket();

  private static String getSocket() {
    try {
      return BabushkaCoreNativeDefinitions.startSocketListenerExternal();
    } catch (Exception | UnsatisfiedLinkError e) {
      System.err.printf("Failed to create a UDS connection: %s%n%n", e);
      throw new RuntimeException(e);
    }
  }

  private final AtomicInteger requestId = new AtomicInteger(0);

  // At the moment, Windows is not supported
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

  private Channel channel = null;
  private EventLoopGroup group = null;

  /**
   * Returns the singleton instance of the NettyWrapper
   *
   * @return NettyWrapper instance
   */
  public static synchronized SocketManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new SocketManager();
      Runtime.getRuntime()
          .addShutdownHook(new Thread(new ShutdownHook(), "NettyWrapper-shutdown-hook"));
    }
    return INSTANCE;
  }

  private static SocketManager INSTANCE = null;

  /** Constructor class for the single instance */
  private SocketManager() {
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
              .handler(new ChannelHandler())
              .connect(new DomainSocketAddress(unixSocket))
              .sync()
              .channel();

    } catch (Exception e) {
      System.err.printf(
          "Failed to create a channel %s: %s%n", e.getClass().getSimpleName(), e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  public void write(GeneratedMessageV3 message) {
    channel.write(message.toByteArray());
  }

  public void writeAndFlush(GeneratedMessageV3 message) {
    channel.writeAndFlush(message.toByteArray());
  }

  public int registerRequest(CompletableFuture<Response> future) {
    int callbackId = requestId.incrementAndGet();
    CommonResources.responses.put(callbackId, future);
    return callbackId;
  }

  public void registerConnection(CompletableFuture<Response> future) {
    CommonResources.connectionRequests.add(future);
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
}
