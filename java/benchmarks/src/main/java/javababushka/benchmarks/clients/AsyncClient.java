package javababushka.benchmarks.clients;

import java.util.concurrent.Future;

/** A Redis client with async capabilities */
public interface AsyncClient extends Client {

  long DEFAULT_TIMEOUT = 1000;

  Future<?> asyncSet(String key, String value);

  Future<String> asyncGet(String key);

  <T> T waitForResult(Future<T> future);

  <T> T waitForResult(Future<T> future, long timeout);
}
