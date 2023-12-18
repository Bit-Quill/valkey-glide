package babushka.api.commands;

import java.util.concurrent.CompletableFuture;
import response.ResponseOuterClass.Response;

public interface StringCommands {

  public static String handleStringResponse(Response response) {
    // return function to convert protobuf.Response into the response object by
    // calling valueFromPointer
    return (String) BaseCommands.handleResponse(response);
  }

  CompletableFuture<?> get(String key);
}
