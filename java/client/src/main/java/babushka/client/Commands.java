package babushka.client;

import babushka.FFI.BabushkaCoreNativeDefinitions;
import babushka.tools.Awaiter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import redis_request.RedisRequestOuterClass.RedisRequest;
import redis_request.RedisRequestOuterClass.RequestType;
import response.ResponseOuterClass.ConstantResponse;
import response.ResponseOuterClass.Response;

@RequiredArgsConstructor
public class Commands {

  private final ChannelHolder channel;

  /**
   * Sync (blocking) set. See async option in {@link #asyncSet}.<br>
   * See <a href="https://redis.io/commands/set/">REDIS docs for SET</a>.
   *
   * @param key The key name
   * @param value The value to set
   */
  public boolean set(String key, String value) {
    return Awaiter.await(asyncSet(key, value));
    // TODO parse response and rethrow an exception if there is an error
  }

  /**
   * Sync (blocking) get. See async option in {@link #asyncGet}.<br>
   * See <a href="https://redis.io/commands/get/">REDIS docs for GET</a>.
   *
   * @param key The key name
   */
  public String get(String key) {
    return Awaiter.await(asyncGet(key));
    // TODO support non-strings
  }

  /**
   * Async (non-blocking) set. See sync option in {@link #set}.<br>
   * See <a href="https://redis.io/commands/set/">REDIS docs for SET</a>.
   *
   * @param key The key name
   * @param value The value to set
   */
  public Future<Boolean> asyncSet(String key, String value) {
    return submitRequest(RequestBuilder.prepareRequest(RequestType.SetString, List.of(key, value)))
        .thenApplyAsync(response -> response.getConstantResponse() == ConstantResponse.OK);
  }

  /**
   * Async (non-blocking) get. See sync option in {@link #get}.<br>
   * See <a href="https://redis.io/commands/get/">REDIS docs for GET</a>.
   *
   * @param key The key name
   */
  public Future<String> asyncGet(String key) {
    return submitRequest(RequestBuilder.prepareRequest(RequestType.GetString, List.of(key)))
        .thenApplyAsync(
            response ->
                response.getRespPointer() != 0
                    ? BabushkaCoreNativeDefinitions.valueFromPointer(response.getRespPointer())
                        .toString()
                    : null);
  }

  private CompletableFuture<Response> submitRequest(RedisRequest.Builder builder) {
    // TODO this explicitly uses ForkJoin thread pool. May be we should use another one.
    return CompletableFuture.supplyAsync(() -> channel.write(builder, true))
        .thenComposeAsync(f -> f);
  }
}
