package babushka.api.commands;

import java.util.concurrent.CompletableFuture;

public interface StringCommands {

  public static String handleStringResponse(Object response) {
    // return function to convert protobuf.Response into the response object by
    // calling valueFromPointer
    return (String) BaseCommands.handleResponse(response);
  }

  CompletableFuture<?> get(String key);
}
