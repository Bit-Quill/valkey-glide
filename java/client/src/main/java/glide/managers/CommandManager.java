package glide.managers;

import glide.api.commands.Command;
import glide.api.commands.RedisExceptionCheckedFunction;
import glide.connectors.handlers.ChannelHandler;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import redis_request.RedisRequestOuterClass;
import redis_request.RedisRequestOuterClass.Command.ArgsArray;
import redis_request.RedisRequestOuterClass.RedisRequest;
import redis_request.RedisRequestOuterClass.RequestType;
import redis_request.RedisRequestOuterClass.Transaction;
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
   * Build a command and send.
   *
   * @param command The command to execute
   * @param responseHandler The handler of the response object
   * @return A result promise of type T
   */
  public <T> CompletableFuture<T> submitNewCommand(
      Command command, RedisExceptionCheckedFunction<Response, T> responseHandler) {
    // register callback
    // create protobuf message from command
    // submit async call
    return channel
        .write(
            RedisRequest.newBuilder()
                .setSingleCommand(
                    prepareRedisCommand(command.getRequestType(), command.getArguments())),
            true)
        .thenApplyAsync(responseHandler::apply);
  }

  /**
   * Build a transaction and send.
   *
   * @param transaction The command to execute
   * @param responseHandler The handler of the response object
   * @return A result promise of type T
   */
  public <T> CompletableFuture<T> submitNewTransaction(
      List<Command> transaction, RedisExceptionCheckedFunction<Response, T> responseHandler) {
    // register callback
    // create protobuf message from command
    // submit async call
    var transactionBuilder = Transaction.newBuilder();
    for (var command : transaction) {
      transactionBuilder.addCommands(
          prepareRedisCommand(command.getRequestType(), command.getArguments()));
    }

    return channel
        .write(RedisRequest.newBuilder().setTransaction(transactionBuilder.build()), true)
        .thenApplyAsync(responseHandler::apply);
  }

  /**
   * Build a protobuf command/transaction request object.<br>
   * Used by {@link CommandManager}.
   *
   * @return An uncompleted request. CallbackDispatcher is responsible to complete it by adding a
   *     callback id.
   */
  private RedisRequestOuterClass.Command prepareRedisCommand(
      Command.RequestType command, String[] args) {
    ArgsArray.Builder commandArgs = ArgsArray.newBuilder();
    for (var arg : args) {
      commandArgs.addArgs(arg);
    }

    return RedisRequestOuterClass.Command.newBuilder()
        .setRequestType(mapRequestTypes(command))
        .setArgsArray(commandArgs.build())
        .build();
  }

  private RequestType mapRequestTypes(Command.RequestType inType) {
    switch (inType) {
      case CUSTOM_COMMAND:
        return RequestType.CustomCommand;
    }
    throw new RuntimeException("Unsupported request type");
  }
}
