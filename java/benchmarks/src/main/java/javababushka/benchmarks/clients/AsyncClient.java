package javababushka.benchmarks.clients;

import javababushka.benchmarks.utils.ConnectionSettings;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** A Redis client with async capabilities */
public interface AsyncClient<T> extends Client {

  long DEFAULT_TIMEOUT = 1000;

  Future<T> asyncConnectToRedis(ConnectionSettings connectionSettings);

  Future<T> asyncSet(String key, String value);

  Future<String> asyncGet(String key);

  default <T> T waitForResult(Future<T> future) {
    return waitForResult(future, DEFAULT_TIMEOUT);
  }

  default <T> T waitForResult(Future<T> future, long timeout) {
    try {
      return future.get(timeout, TimeUnit.MILLISECONDS);
    } catch (Exception ignored) {
      return null;
    }
  }
}
