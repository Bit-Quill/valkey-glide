package glide.api.commands;

import java.util.concurrent.CompletableFuture;
import response.ResponseOuterClass.Response;

public interface StringCommands {

  public static String handleStringResponse(Response response) {
    // return function to convert protobuf.Response into the response object by
    // calling valueFromPointer
    Object value = BaseCommands.handleResponse(response);
    return value == null ? null : (String) value;
  }

  CompletableFuture<?> get(String key);
}
