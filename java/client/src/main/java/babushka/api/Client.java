package babushka.api;

import babushka.connectors.SocketConnection;
import babushka.ffi.resolvers.BabushkaCoreNativeDefinitions;
import babushka.managers.CallbackManager;
import babushka.managers.CommandManager;
import babushka.managers.ConnectionManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import redis_request.RedisRequestOuterClass;
import response.ResponseOuterClass.Response;

public class Client {

  private static final long DEFAULT_TIMEOUT_MILLISECONDS = 1000;

  private final ConnectionManager connectionManager;
  private final CommandManager commandManager;

  // TODO: Move this to a Factory/Builder class
  public Client() {
    CallbackManager callbackManager = new CallbackManager();

    BabushkaCoreNativeDefinitions nativeDefinitions = new BabushkaCoreNativeDefinitions();
    String socketPath = nativeDefinitions.getSocket();

    SocketConnection socketConnection = SocketConnection.getInstance(socketPath);

    this.connectionManager = new ConnectionManager(callbackManager, nativeDefinitions, socketConnection);
    this.commandManager = new CommandManager(callbackManager, socketConnection, nativeDefinitions);
  }

  public Client(ConnectionManager connectionManager, CommandManager commandManager) {
    this.connectionManager = connectionManager;
    this.commandManager = commandManager;
  }

  /**
   * Sync (blocking) connect to REDIS. See async option in {@link #asyncConnectToRedis}.
   *
   * @param host Server address
   * @param port Server port
   * @param useSsl true if communication with the server or cluster should use Transport Level
   *     Security
   * @param clusterMode true if REDIS instance runs in the cluster mode
   */
  // TODO support configuration object which holds more parameters (e.g. multiple addresses, etc)
  public void connectToRedis(String host, int port, boolean useSsl, boolean clusterMode) {
    waitForResult(asyncConnectToRedis(host, port, useSsl, clusterMode));
  }

  /**
   * Async (non-blocking) connect to REDIS. See sync option in {@link #connectToRedis}.
   *
   * @param host Server address
   * @param port Server port
   * @param useSsl true if communication with the server or cluster should use Transport Level
   *     Security
   * @param clusterMode true if REDIS instance runs in the cluster mode
   */
  // TODO support configuration object which holds more parameters (e.g. multiple addresses, etc)
  public CompletableFuture<String> asyncConnectToRedis(
      String host, int port, boolean useSsl, boolean clusterMode) {
    return connectionManager.connectToRedis(host, port, useSsl, clusterMode);

  }

  /**
   * Close all connections and release resources
   */
  public void closeConnection() {
    connectionManager.closeConnection();
  }

  /**
   * Sync (blocking) set. See async option in {@link #asyncSet}.<br>
   * See <a href="https://redis.io/commands/set/">REDIS docs for SET</a>.
   *
   * @param key The key name
   * @param value The value to set
   */
  public void set(String key, String value) {
    waitForResult(asyncSet(key, value));
    // TODO parse response and rethrow an exception if there is an error
  }

  /**
   * Sync (blocking) get. See async option in {@link #asyncGet}.<br>
   * See <a href="https://redis.io/commands/get/">REDIS docs for GET</a>.
   *
   * @param key The key name
   */
  public String get(String key) {
    return waitForResult(asyncGet(key));
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
        RedisRequestOuterClass.RequestType.SetString, List.of(key, value));
  }

  /**
   * Async (non-blocking) get. See sync option in {@link #get}.<br>
   * See <a href="https://redis.io/commands/get/">REDIS docs for GET</a>.
   *
   * @param key The key name
   */
  public Future<String> asyncGet(String key) {
    return commandManager.submitNewCommand(RedisRequestOuterClass.RequestType.GetString, List.of(key));
  }

  /** Get the future result with default timeout. */
  <T> T waitForResult(Future<T> future) {
    return waitForResult(future, DEFAULT_TIMEOUT_MILLISECONDS);
  }

  /** Get the future result with given timeout in ms. */
  <T> T waitForResult(Future<T> future, long timeout) {
    try {
      return future.get(timeout, TimeUnit.MILLISECONDS);
    } catch (Exception ignored) {
      return null;
    }
  }
}
