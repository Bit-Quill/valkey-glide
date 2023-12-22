package glide.managers;

import glide.api.commands.Command;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import response.ResponseOuterClass.Response;

public class CommandManager {

  /**
   * @param command
   * @param responseHandler
   * @return
   */
  public <T> CompletableFuture<T> submitNewCommand(
      Command command, Function<Response, T> responseHandler) {
    // register callback
    // create protobuf message from command
    // submit async call
    // handle return type in the thenApplyAsync
    return new CompletableFuture<>();
  }

  public CompletableFuture<Void> closeConnection() {
    return new CompletableFuture<>();
  }
}
