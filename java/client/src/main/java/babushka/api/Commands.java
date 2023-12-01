package babushka.api;

import babushka.managers.CommandManager;
import java.util.List;
import java.util.concurrent.Future;

public class Commands {

  private final CommandManager commandManager;

  public Commands(CommandManager commandManager) {
    this.commandManager = commandManager;
  }

  /**
   * Sync (blocking) set. See async option in {@link #asyncSet}.<br>
   * See <a href="https://redis.io/commands/set/">REDIS docs for SET</a>.
   *
   * @param key The key name
   * @param value The value to set
   */
  public void set(String key, String value) {
    Awaiter.await(asyncSet(key, value));
    // TODO parse response and rethrow an exception if there is an error
  }

  /**
   * Sync (blocking) get. See async option in {@link #asyncGet}.<br>
   * See <a href="https://redis.io/commands/get/">REDIS docs for GET</a>.
   *
   * @param key The key name
   */
  public String get(String key) {
    return Awaiter.await(asyncGet(key));
    // TODO support non-strings
  }

  /**
   * Async (non-blocking) set. See sync option in {@link #set}.<br>
   * See <a href="https://redis.io/commands/set/">REDIS docs for SET</a>.
   *
   * @param key The key name
   * @param value The value to set
   */
  public Future<String> asyncSet(String key, String value) {
    return commandManager.submitNewCommand(
        redis_request.RedisRequestOuterClass.RequestType.SetString, List.of(key, value));
  }

  /**
   * Async (non-blocking) get. See sync option in {@link #get}.<br>
   * See <a href="https://redis.io/commands/get/">REDIS docs for GET</a>.
   *
   * @param key The key name
   */
  public Future<String> asyncGet(String key) {
    return commandManager.submitNewCommand(
        redis_request.RedisRequestOuterClass.RequestType.GetString, List.of(key));
  }
}
