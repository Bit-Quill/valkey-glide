package glide.api;

import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import lombok.AllArgsConstructor;

/** Base Client class for Redis client */
@AllArgsConstructor
public abstract class BaseClient implements AutoCloseable {

  protected ConnectionManager connectionManager;
  protected CommandManager commandManager;

  // TODO: rename and override for resource management
  // https://github.com/orgs/Bit-Quill/projects/4/views/6?pane=issue&itemId=48063887
  public abstract void close();
}
