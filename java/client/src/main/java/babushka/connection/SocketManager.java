package babushka.connection;

import babushka.FFI.BabushkaCoreNativeDefinitions;
import babushka.client.ChannelHolder;
import babushka.client.Commands;
import babushka.client.Connection;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * A UDS connection manager. This class is responsible for:
 *
 * <ul>
 *   <li>opening a connection (channel) though the UDS;
 *   <li>allocating the corresponding resources, e.g. thread pools (see also {@link
 *       CallbackManager});
 *   <li>freeing shared resources;
 * </ul>
 *
 * Note: should not be used outside of {@link Commands} or {@link Connection}!
 */
public class SocketManager {

  private static final String socketPath = getSocket();

  /**
   * Make an FFI call to obtain the socket path.
   *
   * @return A UDS path.
   */
  private static String getSocket() {
    try {
      return BabushkaCoreNativeDefinitions.startSocketListenerExternal();
    } catch (Exception | UnsatisfiedLinkError e) {
      System.err.printf("Failed to create a UDS connection: %s%n%n", e);
      throw new RuntimeException(e);
    }
  }

  // At the moment, Windows is not supported
  // Probably we should use NIO (NioEventLoopGroup) for Windows.
  private static final boolean isMacOs = isMacOs();

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

  /** Thread pool supplied to <em>Netty</em> to perform all async IO. */
  private EventLoopGroup group = null;

  /**
   * Creates (if not yet created) and returns the singleton instance of the {@link SocketManager}.
   *
   * @return a {@link SocketManager} instance.
   */
  public static synchronized SocketManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new SocketManager();
    }
    return INSTANCE;
  }

  /** The singleton instance. */
  private static SocketManager INSTANCE = new SocketManager();

  /** Constructor for the single instance. */
  private SocketManager() {
    try {
      int cpuCount = Runtime.getRuntime().availableProcessors();
      group =
          isMacOs
              ? new KQueueEventLoopGroup(
                  cpuCount, new DefaultThreadFactory("SocketManager-kqueue-elg", true))
              : new EpollEventLoopGroup(
                  cpuCount, new DefaultThreadFactory("SocketManager-epoll-elg", true));
    } catch (Exception e) {
      System.err.printf(
          "Failed to create a channel %s: %s%n", e.getClass().getSimpleName(), e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  /** Open a new channel for a new client. */
  public ChannelHolder openNewChannel(CallbackManager callbackManager) {
    try {
      Channel channel =
          new Bootstrap()
              .group(group)
              .channel(isMacOs ? KQueueDomainSocketChannel.class : EpollDomainSocketChannel.class)
              .handler(new ChannelBuilder(callbackManager))
              .connect(new DomainSocketAddress(socketPath))
              .sync()
              .channel();
      return new ChannelHolder(channel, callbackManager);
    } catch (InterruptedException e) {
      System.err.printf(
          "Failed to create a channel %s: %s%n", e.getClass().getSimpleName(), e.getMessage());
      e.printStackTrace(System.err);
      throw new RuntimeException(e);
    }
  }

  /**
   * Close the UDS connection and frees corresponding resources. A consecutive call to {@link
   * #getInstance()} will create a new resource pool.
   */
  public void close() {
    group.shutdownGracefully();
    INSTANCE = null;
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
        .addShutdownHook(new Thread(new ShutdownHook(), "SocketManager-shutdown-hook"));
  }
}
