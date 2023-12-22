package babushka.api.commands;

import static babushka.ffi.resolvers.RedisValueResolver.valueFromPointer;

import babushka.api.models.exceptions.ClosingException;
import babushka.api.models.exceptions.ConnectionException;
import babushka.api.models.exceptions.ExecAbortException;
import babushka.api.models.exceptions.RedisException;
import babushka.api.models.exceptions.RequestException;
import babushka.api.models.exceptions.TimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import response.ResponseOuterClass.RequestError;
import response.ResponseOuterClass.Response;

public interface BaseCommands<T> {

  /**
   * Extracts value from the RESP pointer TODO: handle object if the object is small TODO: handle
   * RESP2 object if configuration is set
   *
   * @return
   */
  static Object handleResponse(Response response) {
    if (response.hasRequestError()) {
      RequestError error = response.getRequestError();
      String msg = error.getMessage();
      switch (error.getType()) {
        case Unspecified:
          throw new RedisException("Unexpected result: " + msg);
        case ExecAbort:
          throw new ExecAbortException("ExecAbortException: " + msg);
        case Timeout:
          throw new TimeoutException("TimeoutException: " + msg);
        case Disconnect:
          throw new ConnectionException("Disconnection: " + msg);
      }
      throw new RequestException(response.getRequestError().getMessage());
    }
    if (response.hasClosingError()) {
      throw new ClosingException(response.getClosingError());
    }
    if (response.hasRespPointer()) {
      return valueFromPointer(response.getRespPointer());
    }
    if (response.hasConstantResponse()) {
      return "Ok";
    }
    return null;
  }

  /**
   * Execute a @see{Command} by sending command via socket manager
   *
   * @param cmd to be executed
   * @param args arguments for the command
   * @return a CompletableFuture with response result from Redis
   */
  CompletableFuture<Object> customCommand(String cmd, String[] args);

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
}
