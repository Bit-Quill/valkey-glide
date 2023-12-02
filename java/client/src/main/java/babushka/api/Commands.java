package babushka.api;

import babushka.managers.CommandManager;
import java.util.concurrent.CompletableFuture;

public class Commands {

  private final CommandManager commandManager;

  public Commands(CommandManager commandManager) {
    this.commandManager = commandManager;
  }

  /**
   * Async (non-blocking) set. See sync option in {@link #set}.<br>
   * See <a href="https://redis.io/commands/set/">REDIS docs for SET</a>.
   *
   * @param key The key name
   * @param value The value to set
   */
  public CompletableFuture<String> set(String key, String value) {
    return commandManager.set(key, value);
  }

  /**
   * Async (non-blocking) get. See sync option in {@link #get}.<br>
   * See <a href="https://redis.io/commands/get/">REDIS docs for GET</a>.
   *
   * @param key The key name
   */
  public CompletableFuture<String> get(String key) {
    return commandManager.get(key);
  }
}
