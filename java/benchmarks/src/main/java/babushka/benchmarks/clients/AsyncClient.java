package babushka.benchmarks.clients;

import babushka.benchmarks.utils.ConnectionSettings;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** A Redis client with async capabilities */
public interface AsyncClient<T> extends Client {

  long DEFAULT_TIMEOUT_MILLISECOND = 1000;

  Future<T> asyncConnectToRedis(ConnectionSettings connectionSettings);

  Future<T> asyncSet(String key, String value);

  Future<T> asyncGet(String key);

  default <T> T waitForResult(Future<T> future) {
    return waitForResult(future, DEFAULT_TIMEOUT_MILLISECOND);
  }

  default <T> T waitForResult(Future<T> future, long timeout) {
    try {
      return future.get(timeout, TimeUnit.MILLISECONDS);
    } catch (Exception ignored) {
      return null;
    }
  }
}
