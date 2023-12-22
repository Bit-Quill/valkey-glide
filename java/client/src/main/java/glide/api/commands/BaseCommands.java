package glide.api.commands;

import static glide.ffi.resolvers.RedisValueResolver.valueFromPointer;

import glide.api.models.exceptions.ClosingException;
import glide.api.models.exceptions.ConnectionException;
import glide.api.models.exceptions.ExecAbortException;
import glide.api.models.exceptions.RedisException;
import glide.api.models.exceptions.RequestException;
import glide.api.models.exceptions.TimeoutException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import response.ResponseOuterClass.RequestError;
import response.ResponseOuterClass.Response;

public interface BaseCommands<T> {

  /**
   * Extracts value from the RESP pointer
   *
   * @return A generic Object with the Response | null if the response is empty
   */
  static Object handleResponse(Response response) {
    // TODO: handle object if the object is small
    // TODO: handle RESP2 object if configuration is set
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
      // TODO: confirm
      return "Ok";
    }
    return null;
  }

  /**
   * Execute a @see{Command} by sending command via socket manager
   *
   * @param args arguments for the custom command
   * @return a CompletableFuture with response result from Redis
   */
  CompletableFuture<Object> customCommand(String[] args);

  /**
   * Execute a @see{Command} by sending command via socket manager
   *
   * @param command to be executed
   * @return a CompletableFuture with response result from Redis
   */
  <T> CompletableFuture<T> exec(Command command, Function<Response, T> responseHandler);

  /**
   * Execute a transaction by processing the queued commands. <br>
   * See https://redis.io/topics/Transactions/ for details on Redis Transactions. <br>
   *
   * @param transaction with commands to be executed
   * @param responseHandler handler responsible for assigning type to the list of response objects
   * @return A CompletableFuture completed with the results from Redis
   */
  CompletableFuture<List<Object>> exec(Transaction transaction, Function<Response, List<Object>> responseHandler);

  public enum RequestType {
    GETSTRING,
    SETSTRING
  }
}
