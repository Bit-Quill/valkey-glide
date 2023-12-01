package babushka.tools;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Awaiter {
  private static final long DEFAULT_TIMEOUT_MILLISECONDS = 1000;

  /** Get the future result with default timeout. */
  public static <T> T await(Future<T> future) {
    return await(future, DEFAULT_TIMEOUT_MILLISECONDS);
  }

  /** Get the future result with given timeout in ms. */
  public static <T> T await(Future<T> future, long timeout) {
    try {
      return future.get(timeout, TimeUnit.MILLISECONDS);
    } catch (Exception ignored) {
      return null;
    }
  }
}
