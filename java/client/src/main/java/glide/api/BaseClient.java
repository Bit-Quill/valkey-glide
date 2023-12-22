package glide.api;

import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;

/**
 * Base Client class for Redis client
 */
@AllArgsConstructor
public abstract class BaseClient {

  protected ConnectionManager connectionManager;
  protected CommandManager commandManager;

  // TODO: rename and override for resource management
  public abstract CompletableFuture<? extends BaseClient> close();
}
