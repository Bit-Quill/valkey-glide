package glide.managers;

import glide.api.commands.Command;
import glide.api.commands.RedisExceptionCheckedFunction;
import glide.connectors.handlers.ChannelHandler;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import response.ResponseOuterClass.Response;

/**
 * Service responsible for submitting command requests to a socket channel handler and unpack
 * responses from the same socket channel handler.
 */
@RequiredArgsConstructor
public class CommandManager {

  /** UDS connection representation. */
  private final ChannelHandler channel;

  /**
   * Build a command and submit it Netty to send.
   *
   * @param command
   * @param responseHandler - to handle the response object
   * @return A result promise of type T
   */
  public <T> CompletableFuture<T> submitNewCommand(
      Command command, RedisExceptionCheckedFunction<Response, T> responseHandler) {
    // register callback
    // create protobuf message from command
    // submit async call
    return channel
        .write(prepareRedisRequest(command.getRequestType(), command.getArguments()), true)
        .thenApplyAsync(response -> responseHandler.apply(response));
  }

  /**
   * Build a protobuf command/transaction request object.<br>
   * Used by {@link CommandManager}.
   *
   * @return An uncompleted request. CallbackDispatcher is responsible to complete it by adding a
   *     callback id.
   */
  private redis_request.RedisRequestOuterClass.RedisRequest.Builder prepareRedisRequest(
      Command.RequestType command, String[] args) {
    redis_request.RedisRequestOuterClass.Command.ArgsArray.Builder commandArgs =
        redis_request.RedisRequestOuterClass.Command.ArgsArray.newBuilder();
    for (var arg : args) {
      commandArgs.addArgs(arg);
    }

    return redis_request.RedisRequestOuterClass.RedisRequest.newBuilder()
        .setSingleCommand( // set command
            redis_request.RedisRequestOuterClass.Command.newBuilder()
                .setRequestType(mapRequestTypes(command)) // set command name
                .setArgsArray(commandArgs.build()) // set arguments
                .build())
        .setRoute( // set route
            redis_request.RedisRequestOuterClass.Routes.newBuilder()
                .setSimpleRoutes(
                    redis_request.RedisRequestOuterClass.SimpleRoutes.AllNodes) // set route type
                .build());
  }

  private redis_request.RedisRequestOuterClass.RequestType mapRequestTypes(
      Command.RequestType inType) {
    switch (inType) {
      case CUSTOM_COMMAND:
        return redis_request.RedisRequestOuterClass.RequestType.CustomCommand;
      case GET_STRING:
        return redis_request.RedisRequestOuterClass.RequestType.GetString;
      case SET_STRING:
        return redis_request.RedisRequestOuterClass.RequestType.SetString;
    }
    throw new RuntimeException("Unsupported request type");
  }

  /** Close the connection and the corresponding channel. */
  public CompletableFuture<Void> closeConnection() {
    return new CompletableFuture<>();
  }
}
