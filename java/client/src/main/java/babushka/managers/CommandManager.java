package babushka.managers;

import babushka.api.commands.Command;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class CommandManager {

  /**
   * @param command
   * @param responseHandler
   * @return
   */
  public <T> CompletableFuture<T> submitNewCommand(
      Command command, Function<T, response.ResponseOuterClass> responseHandler) {
    // register callback
    // create protobuf message from command
    // submit async call
    // handle return type in the thenApplyAsync
    return new CompletableFuture<>();
  }
}
