package babushka.managers;

import babushka.api.commands.Command;
import babushka.connectors.handlers.ChannelHandler;
import babushka.ffi.resolvers.RedisValueResolver;
import babushka.models.RequestBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import response.ResponseOuterClass.Response;

@RequiredArgsConstructor
public class CommandManager {

  /** UDS connection representation. */
  private final ChannelHandler channel;

  /**
   * Build a command and submit it Netty to send.
   *
   * @param command Command
   * @param responseHandler to handle the response object
   * @return A result promise of type T
   */
  public <T> CompletableFuture<T> submitNewRequest(
      Command command, Function<Response, T> responseHandler) {
    // TODO this explicitly uses ForkJoin thread pool. May be we should use another one.
    return CompletableFuture.supplyAsync(
            () ->
                channel.write(
                    RequestBuilder.prepareRedisRequest(
                        command.getRequestType(), command.getArguments()),
                    true))
        // TODO: is there a better way to execute this?
        .thenComposeAsync(f -> f)
        .thenApplyAsync(responseHandler);
  }

  /**
   * Check response and extract data from it.
   *
   * @param response A response received from Babushka
   * @return A String from the Redis RESP2 response, or Ok. Otherwise, returns null
   */
  private String extractValueFromResponse(Response response) {
    if (response.hasRequestError()) {
      // TODO do we need to support different types of exceptions and distinguish them by type?
      throw new RuntimeException(
          String.format(
              "%s: %s",
              response.getRequestError().getType(), response.getRequestError().getMessage()));
    } else if (response.hasClosingError()) {
      closeConnection();
      throw new RuntimeException("Connection closed: " + response.getClosingError());
    } else if (response.hasConstantResponse()) {
      return response.getConstantResponse().toString();
    } else if (response.hasRespPointer()) {
      return RedisValueResolver.valueFromPointer(response.getRespPointer()).toString();
    }
    throw new IllegalStateException("A malformed response received: " + response.toString());
  }

  /** Close the connection and the corresponding channel. */
  public CompletableFuture<Void> closeConnection() {
    return CompletableFuture.runAsync(channel::close);
  }
}
