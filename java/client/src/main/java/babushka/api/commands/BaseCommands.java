package babushka.api.commands;

import static babushka.ffi.resolvers.BabushkaCoreNativeDefinitions.valueFromPointer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import response.ResponseOuterClass.Response;

public interface BaseCommands<T> {

  /**
   * @return
   */
  static Object handleResponse(Object response) {
    // return function to convert protobuf.Response into the response object by
    // calling valueFromPointer
    Response respObject = (Response) response;
    return valueFromPointer(respObject.getRespPointer());
  }

  /**
   * Execute a @see{Command} by sending command via socket manager
   *
   * @param cmd to be executed
   * @param args arguments for the command
   * @return a CompletableFuture with response result from Redis
   */
  CompletableFuture<?> customCommand(String cmd, String[] args);

  /**
   * Execute a @see{Command} by sending command via socket manager
   *
   * @param command to be executed
   * @return a CompletableFuture with response result from Redis
   */
  <T> CompletableFuture<T> exec(Command command, Function<Response, T> responseHandler);

  /**
   * Executes a transaction @see{Transaction} by sending a set of commands via socket manager
   *
   * @param transaction set of commands to be executed
   * @return a CompletableFuture with response and a List of results from Redis TODO: change return
   *     value to ClusterValue
   */
  CompletableFuture<?> exec(Transaction transaction);

  public enum RequestType {
    GETSTRING,
    SETSTRING
  }
}
