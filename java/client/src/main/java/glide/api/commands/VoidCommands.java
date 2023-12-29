package glide.api.commands;

import glide.api.models.exceptions.ClosingException;
import glide.api.models.exceptions.ConnectionException;
import glide.api.models.exceptions.ExecAbortException;
import glide.api.models.exceptions.RedisException;
import glide.api.models.exceptions.RequestException;
import glide.api.models.exceptions.TimeoutException;
import java.util.concurrent.CompletableFuture;
import response.ResponseOuterClass.RequestError;
import response.ResponseOuterClass.Response;

/** String Commands interface to handle single commands that have no payload. */
public interface VoidCommands {

  /**
   * Check for errors in the Response and return null Throws an error if an unexpected value is
   * returned
   *
   * @return null if the response is empty
   */
  static Void handleVoidResponse(Response response) {
    // return function to convert protobuf.Response into the response object by
    // calling valueFromPointer
    Object value = BaseCommands.applyBaseCommandResponseResolver().apply(response);
    if (value == null) {
      return null;
    }
    throw new RedisException(
        "Unexpected return type from Redis: got " + value.getClass() + " expected null");
  }

  CompletableFuture<Void> set(String key, String value);
}
