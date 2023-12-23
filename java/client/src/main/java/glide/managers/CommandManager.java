package glide.managers;

import glide.api.commands.Command;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import response.ResponseOuterClass.Response;

@AllArgsConstructor
public class CommandManager {

  CompletableFuture<Response> channel;

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
    return channel.thenApplyAsync(response -> responseHandler.apply(response));
  }

  public CompletableFuture<Void> closeConnection() {
    return new CompletableFuture<>();
  }
}
