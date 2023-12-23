package glide.api.commands;

import glide.api.models.exceptions.RedisException;
import java.util.concurrent.CompletableFuture;
import response.ResponseOuterClass.Response;

public interface StringCommands {

  public static String handleStringResponse(Response response) {
    // return function to convert protobuf.Response into the response object by
    // calling valueFromPointer
    Object value = BaseCommands.applyBaseCommandResponseResolver().apply(response);
    if (value instanceof String) {
      return (String) value;
    }
    throw new RedisException(
        "Unexpected return type from Redis: got " + value.getClass() + " expected String");
  }

  CompletableFuture<?> get(String key);
}
