package babushka.api.commands;

import babushka.api.models.exceptions.ClosingException;
import babushka.api.models.exceptions.ConnectionException;
import babushka.api.models.exceptions.ExecAbortException;
import babushka.api.models.exceptions.RedisException;
import babushka.api.models.exceptions.RequestException;
import babushka.api.models.exceptions.TimeoutException;
import java.util.concurrent.CompletableFuture;
import response.ResponseOuterClass.RequestError;
import response.ResponseOuterClass.Response;

public interface VoidCommands {

  static Void handleVoidResponse(Object respObject) {
    Response response = (Response) respObject;
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
      throw new RuntimeException(
          "Unexpected object returned in response - expected constantResponse or null");
    }
    if (response.hasConstantResponse()) {
      return null; // Void
    }
    // TODO commented out due to #710: empty response means a successful connection
    // https://github.com/aws/babushka/issues/710
    return null;
  }

  CompletableFuture<?> set(String key, String value);
}
