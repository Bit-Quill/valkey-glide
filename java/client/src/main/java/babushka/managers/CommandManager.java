package babushka.managers;

import babushka.connectors.handlers.ChannelHandler;
import babushka.models.RequestBuilder;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import redis_request.RedisRequestOuterClass.RequestType;

@RequiredArgsConstructor
public class CommandManager {

  private final ChannelHandler channel;

  public CompletableFuture<String> get(String key) {
    return submitNewCommand(RequestType.GetString, List.of(key));
  }

  public CompletableFuture<String> set(String key, String value) {
    return submitNewCommand(RequestType.SetString, List.of(key, value));
  }

  /**
   * @param command
   * @param args
   * @return
   */
  @VisibleForTesting
  private CompletableFuture<String> submitNewCommand(RequestType command, List<String> args) {
    // TODO this explicitly uses ForkJoin thread pool. May be we should use another one.
    return CompletableFuture.supplyAsync(
            () -> channel.write(RequestBuilder.prepareRequest(command, args), true))
        // TODO: is there a better way to execute this?
        .thenComposeAsync(f -> f)
        .thenApplyAsync(RequestBuilder::resolveRedisResponseToString);
  }
}
