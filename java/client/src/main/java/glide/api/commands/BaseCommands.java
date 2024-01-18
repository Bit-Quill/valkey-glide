package glide.api.commands;

import glide.ffi.resolvers.RedisValueResolver;
import java.util.concurrent.CompletableFuture;
import response.ResponseOuterClass.Response;

/** Base Commands interface to handle generic command and transaction requests. */
public interface BaseCommands {

  /**
   * The default Object handler from response
   *
   * @return BaseCommandResponseResolver to deliver the response
   */
  static BaseCommandResponseResolver applyBaseCommandResponseResolver() {
    return new BaseCommandResponseResolver(RedisValueResolver::valueFromPointer);
  }

  /**
   * Extracts the response from the Protobuf response and either throws an exception or returns the
   * appropriate response as an Object
   *
   * @param response Redis protobuf message
   * @return Response Object
   */
  static Object handleObjectResponse(Response response) {
    // return function to convert protobuf.Response into the response object by
    // calling valueFromPointer
    return applyBaseCommandResponseResolver().apply(response);
  }

  static Object[] handleTransactionResponse(Response response) {
    // return function to convert protobuf.Response into the response object by
    // calling valueFromPointer

    return (Object[]) applyBaseCommandResponseResolver().apply(response);
  }

  /**
   * Execute a custom {@link Command}.
   *
   * @param args arguments for the custom command
   * @return a CompletableFuture with response result from Redis
   */
  CompletableFuture<Object> customCommand(String[] args);

  /**
   * Execute a transaction of custom {@link Command}s.
   *
   * @param args arguments for the custom command
   * @return a CompletableFuture with response result from Redis
   */
  CompletableFuture<Object[]> customTransaction(String[][] args);
}
