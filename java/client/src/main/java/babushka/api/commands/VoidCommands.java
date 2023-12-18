package babushka.api.commands;

import java.util.concurrent.CompletableFuture;
import response.ResponseOuterClass;

public interface VoidCommands {

  static Void handleVoidResponse(Object response) {
    ResponseOuterClass.Response respObject = (ResponseOuterClass.Response) response;
    respObject.getConstantResponse();
    return null; // Void
  }

  CompletableFuture<?> set(String key, String value);
}
