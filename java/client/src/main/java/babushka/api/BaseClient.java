package babushka.api;

import java.util.concurrent.CompletableFuture;

/** */
public abstract class BaseClient {
  // ConnectionManager connectionManager;

  // TODO: rename and override for resource management
  public abstract CompletableFuture<RedisClient> close();
}
