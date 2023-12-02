package babushka.connectors;

import babushka.connectors.handlers.ChannelBuilder;
import babushka.managers.CallbackManager;
import babushka.managers.ConnectionManager;
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

public class SocketConnection {

  /** A channel to make socket interactions with. */
  private Channel channel;

  /** Thread pool supplied to <em>Netty</em> to perform all async IO. */
  private EventLoopGroup group;

  /** The singleton instance. */
  private static SocketConnection INSTANCE = null;

  private static String socketPath;

  public static void setSocketPath(String socketPath) {
    if (SocketConnection.socketPath == null) {
      SocketConnection.socketPath = socketPath;
      return;
    }
    throw new RuntimeException("socket path can only be declared once");
  }

  /**
   * Creates (if not yet created) and returns the singleton instance of the {@link
   * ConnectionManager}.
   *
   * @return a {@link ConnectionManager} instance.
   */
  public static synchronized SocketConnection getInstance() {
    if (INSTANCE == null) {
      assert socketPath != null : "socket path must be defined";
      INSTANCE = new SocketConnection();
    }
    return INSTANCE;
  }

  // At the moment, Windows is not supported
  // Probably we should use NIO (NioEventLoopGroup) for Windows.
  // TODO support IO-Uring and NIO
  /**
   * Detect platform to identify which native implementation to use for UDS interaction. Currently
   * supported platforms are: Linux and macOS.<br>
   * Subject to change in future to support more platforms and implementations.
   */
  private static boolean isMacOs() {
    try {
      Class.forName("io.netty.channel.kqueue.KQueue");
      return KQueue.isAvailable();
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /** Constructor for the single instance. */
  private SocketConnection() {
    boolean isMacOs = isMacOs();
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
              .handler(new ChannelBuilder())
              .connect(new DomainSocketAddress(socketPath))
              .sync()
              .channel();

    } catch (Exception e) {
      System.err.printf(
          "Failed to create a channel %s: %s%n", e.getClass().getSimpleName(), e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  /** Write a protobuf message to the socket. */
  public void write(GeneratedMessageV3 message) {
    channel.write(message.toByteArray());
  }

  /** Write a protobuf message to the socket and flush it. */
  public void writeAndFlush(GeneratedMessageV3 message) {
    channel.writeAndFlush(message.toByteArray());
  }

  /**
   * Closes the UDS connection and frees corresponding resources. A consecutive call to {@link
   * #getInstance()} will create a new connection with new resource pool.
   */
  public void close() {
    channel.close();
    group.shutdownGracefully();
    INSTANCE = null;
    // TODO should we reply in uncompleted futures?
    CallbackManager.shutdownGracefully();
    CallbackManager.connectionRequests.clear();
    CallbackManager.responses.clear();
  }

  /**
   * A JVM shutdown hook to be registered. It is responsible for closing connection and freeing
   * resources by calling {@link #close()}. It is recommended to use a class instead of lambda to
   * ensure that it is called.<br>
   * See {@link Runtime#addShutdownHook}.
   */
  private static class ShutdownHook implements Runnable {
    @Override
    public void run() {
      if (INSTANCE != null) {
        INSTANCE.close();
        INSTANCE = null;
      }
    }
  }

  static {
    Runtime.getRuntime()
        .addShutdownHook(new Thread(new ShutdownHook(), "NettyWrapper-shutdown-hook"));
  }
}
