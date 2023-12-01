package babushka.managers;

import static babushka.models.RequestBuilder.redisSingleCommand;

import babushka.connectors.SocketConnection;
import babushka.models.RequestBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import redis_request.RedisRequestOuterClass.RequestType;
import response.ResponseOuterClass.Response;

/**
 * A UDS connection manager. This class is responsible for:
 *
 * <ul>
 *   <li>opening a connection (channel) though the UDS;
 *   <li>allocating the corresponding resources, e.g. thread pools (see also {@link
 *       CallbackManager});
 *   <li>handling connection requests;
 *   <li>providing unique request ID (callback ID);
 *   <li>handling REDIS requests;
 *   <li>closing connection;
 * </ul>
 */
public class CommandManager {

  private final CallbackManager callbackManager;
  private final SocketConnection socketConnection;

  public CommandManager(CallbackManager callbackManager, SocketConnection socketConnection) {
    this.callbackManager = callbackManager;
    this.socketConnection = socketConnection;
  }

  public CompletableFuture<String> get(String key) {
    return submitNewCommand(
        redis_request.RedisRequestOuterClass.RequestType.GetString, List.of(key));
  }

  public CompletableFuture<String> set(String key, String value) {
    return submitNewCommand(
        redis_request.RedisRequestOuterClass.RequestType.SetString, List.of(key, value));
  }

  /**
   * @param command
   * @param args
   * @return TODO: Make this private
   */
  public CompletableFuture<String> submitNewCommand(RequestType command, List<String> args) {
    // TODO this explicitly uses ForkJoin thread pool. May be we should use another one.
    CompletableFuture<Response> future = new CompletableFuture<>();
    int callbackId = callbackManager.registerRequest(future);

    return CompletableFuture.supplyAsync(
            () -> {
              socketConnection.writeAndFlush(redisSingleCommand(command, args));
              return future;
            })
        .thenCompose(f -> f)
        .thenApply(RequestBuilder::resolveRedisResponseToString);
  }
}
