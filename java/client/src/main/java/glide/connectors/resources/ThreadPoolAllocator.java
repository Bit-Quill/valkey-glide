package glide.connectors.resources;

/** A class responsible to allocating and deallocating shared thread pools. */
public class ThreadPoolAllocator {
  private static final Object lock = new Object();
  private static ThreadPoolResource defaultThreadPoolResource = null;

  public static ThreadPoolResource createOrGetNettyThreadPool() {
    if (Platform.getCapabilities().isKQueueAvailable()) {
      return getOrCreate(new KQueuePoolResource());
    } else if (Platform.getCapabilities().isEPollAvailable()) {
      return getOrCreate(new EpollResource());
    }
    // TODO support IO-Uring and NIO

    throw new RuntimeException("Current platform supports no known thread pool resources");
  }

  private static ThreadPoolResource getOrCreate(ThreadPoolResource threadPoolResource) {
    synchronized (lock) {
      if (defaultThreadPoolResource == null) {
        defaultThreadPoolResource = threadPoolResource;
      }
    }
    return defaultThreadPoolResource;
  }

  /**
   * A JVM shutdown hook to be registered. It is responsible for closing connection and freeing
   * resources. It is recommended to use a class instead of lambda to ensure that it is called.<br>
   * See {@link Runtime#addShutdownHook}.
   */
  private static class ShutdownHook implements Runnable {
    @Override
    public void run() {
      defaultThreadPoolResource.getEventLoopGroup().shutdownGracefully();
    }
  }

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(), "Glide-shutdown-hook"));
  }
}
