package babushka.connection;

import static response.ResponseOuterClass.Response;

import babushka.BabushkaCoreNativeDefinitions;
import babushka.Client;
import connection_request.ConnectionRequestOuterClass.ConnectionRequest;
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
import redis_request.RedisRequestOuterClass.RedisRequest;

/**
 * A UDS connection manager. This class is responsible for:
 *
 * <ul>
 *   <li>opening a connection (channel) though the UDS;
 *   <li>allocating the corresponding resources, e.g. thread pools (see also {@link
 *       SocketManagerResources});
 *   <li>handling connection requests;
 *   <li>providing unique request ID (callback ID);
 *   <li>handling REDIS requests;
 *   <li>closing connection;
 * </ul>
 *
 * Note: should not be used outside of {@link Client}!
 */
public class SocketManager {

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

  /** Unique request ID (callback ID). Thread-safe. */
  private final AtomicInteger requestId = new AtomicInteger(0);

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

  /** A channel to make socket interactions with. */
  private Channel channel = null;

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
              .connect(new DomainSocketAddress(getSocket()))
              .sync()
              .channel();

    } catch (Exception e) {
      System.err.printf(
          "Failed to create a channel %s: %s%n", e.getClass().getSimpleName(), e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  /** Write a protobuf message to the socket. */
  public CompletableFuture<Response> write(RedisRequest.Builder request, boolean flush) {
    var future = new CompletableFuture<Response>();
    int callbackId = requestId.incrementAndGet();
    request.setCallbackIdx(callbackId);
    SocketManagerResources.responses.put(callbackId, future);
    if (flush) channel.writeAndFlush(request.build().toByteArray());
    else channel.write(request.build().toByteArray());
    return future;
  }

  /** Write a protobuf message to the socket. */
  public CompletableFuture<Response> connect(ConnectionRequest request) {
    var future = new CompletableFuture<Response>();
    SocketManagerResources.connectionRequests.add(future);
    channel.writeAndFlush(request.toByteArray());
    return future;
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
    SocketManagerResources.connectionRequests.clear();
    SocketManagerResources.responses.clear();
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
