package glide.connectors.handlers;

import babushka.connectors.ClientState;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import response.ResponseOuterClass.Response;

/** Holder for resources required to dispatch responses and used by {@link ReadHandler}. */
@RequiredArgsConstructor
public class CallbackDispatcher {

  /** Reserved callback ID for connection request. */
  private final int CONNECTION_PROMISE_ID = 0;

  /** Client state reference. */
  private final ClientState.ReadOnlyClientState clientState;

  /**
   * Unique request ID (callback ID). Thread-safe and overflow-safe.<br>
   * Note: Protobuf packet contains callback ID as uint32, but it stores data as a bit field.<br>
   * Negative java values would be shown as positive on rust side. Meanwhile, no data loss happen,
   * because callback ID remains unique.
   */
  private final AtomicInteger requestId = new AtomicInteger(0);

  /**
   * Storage of Futures to handle responses. Map key is callback id, which starts from 1.<br>
   * Each future is a promise for every submitted by user request.
   */
  private final Map<Integer, CompletableFuture<Response>> responses = new ConcurrentHashMap<>();

  /**
   * Register a new request to be sent. Once response received, the given future completes with it.
   *
   * @return A pair of unique callback ID which should set into request and a client promise for
   *     response.
   */
  public Pair<Integer, CompletableFuture<Response>> registerRequest() {
    int callbackId = requestId.getAndIncrement();
    var future = new CompletableFuture<Response>();
    responses.put(callbackId, future);
    return Pair.of(callbackId, future);
  }

  public CompletableFuture<Response> registerConnection() {
    var res = registerRequest();
    if (res.getKey() != CONNECTION_PROMISE_ID) {
      throw new IllegalStateException();
    }
    return res.getValue();
  }

  /**
   * Complete the corresponding client promise and free resources.
   *
   * @param response A response received
   */
  public void completeRequest(Response response) {
    int callbackId =
        clientState.isInitializing() ? response.getCallbackIdx() : CONNECTION_PROMISE_ID;
    var future = responses.get(callbackId);
    if (future != null) {
      future.completeAsync(() -> response);
    } else {
      // TODO: log an error.
      // probably a response was received after shutdown or `registerRequest` call was missing
    }
    responses.remove(callbackId);
  }

  public void shutdownGracefully() {
    responses.values().forEach(future -> future.cancel(false));
    responses.clear();
  }
}
