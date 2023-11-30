package babushka;

import babushka.connection.ReadHandler;
import babushka.connection.SocketManager;
import com.google.common.annotations.VisibleForTesting;
import connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import connection_request.ConnectionRequestOuterClass.NodeAddress;
import connection_request.ConnectionRequestOuterClass.ReadFrom;
import connection_request.ConnectionRequestOuterClass.TlsMode;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import redis_request.RedisRequestOuterClass.Command;
import redis_request.RedisRequestOuterClass.Command.ArgsArray;
import redis_request.RedisRequestOuterClass.RedisRequest;
import redis_request.RedisRequestOuterClass.RequestType;
import redis_request.RedisRequestOuterClass.Routes;
import redis_request.RedisRequestOuterClass.SimpleRoutes;
import response.ResponseOuterClass.Response;

public class Client {
  private static final long DEFAULT_TIMEOUT_MILLISECONDS = 1000;

  private final SocketManager socketManager;

  public Client() {
    this(SocketManager.getInstance());
  }

  @VisibleForTesting
  Client(SocketManager socketManager) {
    this.socketManager = socketManager;
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
   * Create a unique callback ID (request ID) and a corresponding registered future for the
   * response.<br>
   * Should be used for every request submitted to ensure that it can be tracked by {@link
   * SocketManager} and {@link ReadHandler}.
   *
   * @return New callback ID and new future to be returned to user.
   */
  private synchronized Pair<Integer, CompletableFuture<Response>> getNextCallback() {
    var future = new CompletableFuture<Response>();
    int callbackId = socketManager.registerRequest(future);
    return Pair.of(callbackId, future);
  }

  /** Build a protobuf connection request. See {@link #connectToRedis}. */
  // TODO support more parameters and/or configuration object
  @VisibleForTesting
  static ConnectionRequest getConnectionRequest(
      String host, int port, boolean useSsl, boolean clusterMode) {
    return ConnectionRequest.newBuilder()
        .addAddresses(NodeAddress.newBuilder().setHost(host).setPort(port).build())
        .setTlsMode(useSsl ? TlsMode.SecureTls : TlsMode.NoTls)
        .setClusterModeEnabled(clusterMode)
        .setReadFrom(ReadFrom.Primary)
        .setDatabaseId(0)
        .build();
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
  public CompletableFuture<Response> asyncConnectToRedis(
      String host, int port, boolean useSsl, boolean clusterMode) {
    var request = getConnectionRequest(host, port, useSsl, clusterMode);
    var future = new CompletableFuture<Response>();
    socketManager.registerConnection(future);
    socketManager.writeAndFlush(request);
    return future;
  }

  private CompletableFuture<Response> submitNewCommand(RequestType command, List<String> args) {
    var commandId = getNextCallback();
    // TODO this explicitly uses ForkJoin thread pool. May be we should use another one.
    return CompletableFuture.supplyAsync(
            () -> {
              var commandArgs = ArgsArray.newBuilder();
              for (var arg : args) {
                commandArgs.addArgs(arg);
              }

              RedisRequest request =
                  RedisRequest.newBuilder()
                      .setCallbackIdx(commandId.getKey())
                      .setSingleCommand(
                          Command.newBuilder()
                              .setRequestType(command)
                              .setArgsArray(commandArgs.build())
                              .build())
                      .setRoute(Routes.newBuilder().setSimpleRoutes(SimpleRoutes.AllNodes).build())
                      .build();
              socketManager.writeAndFlush(request);
              return commandId.getValue();
            })
        .thenCompose(f -> f);
  }

  /**
   * Async (non-blocking) set. See sync option in {@link #set}.<br>
   * See <a href="https://redis.io/commands/set/">REDIS docs for SET</a>.
   *
   * @param key The key name
   * @param value The value to set
   */
  public Future<Response> asyncSet(String key, String value) {
    return submitNewCommand(RequestType.SetString, List.of(key, value));
  }

  /**
   * Async (non-blocking) get. See sync option in {@link #get}.<br>
   * See <a href="https://redis.io/commands/get/">REDIS docs for GET</a>.
   *
   * @param key The key name
   */
  public Future<String> asyncGet(String key) {
    return submitNewCommand(RequestType.GetString, List.of(key))
        .thenApply(
            response ->
                response.getRespPointer() != 0
                    ? BabushkaCoreNativeDefinitions.valueFromPointer(response.getRespPointer())
                        .toString()
                    : null);
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
