package glide.connectors.resources;

import java.util.function.Supplier;

/** A class responsible to allocating and deallocating the default Thread Pool Resource. */
public class ThreadPoolResourceAllocator {
  private static final Object lock = new Object();
  private static ThreadPoolResource defaultThreadPoolResource = null;

  public static ThreadPoolResource createOrGetThreadPoolResource() {
    if (Platform.getCapabilities().isKQueueAvailable()) {
      return getOrCreate(KQueuePoolResource::new);
    }

    if (Platform.getCapabilities().isEPollAvailable()) {
      return getOrCreate(EpollResource::new);
    }
    // TODO support IO-Uring and NIO
    throw new RuntimeException("Current platform supports no known thread pool resources");
  }

  private static ThreadPoolResource getOrCreate(Supplier<ThreadPoolResource> supplier) {
    if (defaultThreadPoolResource != null) {
      return defaultThreadPoolResource;
    }

    synchronized (lock) {
      if (defaultThreadPoolResource == null) {
        defaultThreadPoolResource = supplier.get();
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
      if (defaultThreadPoolResource != null) {
        defaultThreadPoolResource.getEventLoopGroup().shutdownGracefully();
      }
    }
  }

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(), "Glide-shutdown-hook"));
  }
}
