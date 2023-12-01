package babushka.managers;

import babushka.connectors.SocketConnection;
import babushka.ffi.resolvers.BabushkaCoreNativeDefinitions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import redis_request.RedisRequestOuterClass;
import redis_request.RedisRequestOuterClass.RedisRequest;
import redis_request.RedisRequestOuterClass.RequestType;
import response.ResponseOuterClass;
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
  private final BabushkaCoreNativeDefinitions nativeDefinitions;

  public CommandManager(CallbackManager callbackManager, SocketConnection socketConnection, BabushkaCoreNativeDefinitions nativeDefinitions) {
    this.callbackManager = callbackManager;
    this.socketConnection = socketConnection;
    this.nativeDefinitions = nativeDefinitions;
  }

  private static RedisRequest redisSingleCommand(RequestType command, List<String> args) {
    var commandArgs = RedisRequestOuterClass.Command.ArgsArray.newBuilder();
    for (var arg : args) {
      commandArgs.addArgs(arg);
    }

    RedisRequest.Builder builder =
        RedisRequest.newBuilder()
            .setSingleCommand(
                RedisRequestOuterClass.Command.newBuilder()
                    .setRequestType(command)
                    .setArgsArray(commandArgs.build())
                    .build())
            .setRoute(RedisRequestOuterClass.Routes.newBuilder().setSimpleRoutes(
                RedisRequestOuterClass.SimpleRoutes.AllNodes).build());

    return builder.build();
  }

  /**
   * Returns a String from the redis response if a resp2 response exists, or Ok.
   * Otherwise, returns null
   * @param response Redis Response
   * @return String or null
   */
  public static String resolveRedisResponseToString(Response response) {
    if (response.hasConstantResponse()) {
      return BabushkaCoreNativeDefinitions.valueFromPointer(response.getRespPointer()).toString();
    }
    if (response.hasRespPointer()) {
      return response.getConstantResponse().toString();
    }
    return null;
  }

  /**
   *
   * @param command
   * @param args
   * @return
   */
  public CompletableFuture<String> submitNewCommand(RequestType command, List<String> args) {
    // TODO this explicitly uses ForkJoin thread pool. May be we should use another one.
    CompletableFuture<Response> future = new CompletableFuture<>();
    int callbackId = callbackManager.registerRequest(future);

    RedisRequest request = redisSingleCommand(command, args);

    return CompletableFuture.supplyAsync(
            () -> {
              socketConnection.writeAndFlush(request);
              return future;
            })
        .thenCompose(f -> f)
        .thenApply(CommandManager::resolveRedisResponseToString);
  }
}
