package babushka.managers;

import babushka.connectors.handlers.ReadHandler;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.tuple.Pair;
import response.ResponseOuterClass.Response;

/** Holder for resources owned by {@link CommandManager} and used by {@link ReadHandler}. */
public class CallbackManager {

  // TODO: let's make these non-static class variables

  /**
   * Storage of Futures to handle responses. Map key is callback id, which starts from 1.<br>
   * Each future is a promise for every submitted by user request.
   */
  public static final Map<Integer, CompletableFuture<Response>> responses =
      new ConcurrentHashMap<>();

  /**
   * Storage for connection requests similar to {@link #responses}. Unfortunately, connection
   * requests can't be stored in the same storage, because callback ID = 0 is hardcoded for
   * connection requests. Will be removed once <a
   * href="https://github.com/aws/babushka/issues/600">issue #600 on GH</a> fixed.
   */
  public static final Deque<CompletableFuture<Response>> connectionRequests =
      new ConcurrentLinkedDeque<>();

  /** Unique request ID (callback ID). Thread-safe. */
  private final AtomicInteger requestId = new AtomicInteger(0);

  /**
   * Register a new request to be sent. Socket Manager takes responsibility for tracking the
   * returned callback ID in all incoming responses. Once response received, the given future
   * completes with it.
   *
   * @param future A client promise for response.
   * @return Unique callback ID which should set into request.
   */
  public int registerRequest(CompletableFuture<Response> future) {
    int callbackId = requestId.incrementAndGet();
    CallbackManager.responses.put(callbackId, future);
    return callbackId;
  }

  /**
   * Register a new connection request similar to {@link #registerRequest}.<br>
   * No callback ID returned, because connection request/response pair have no such field (subject
   * to change). Track <a href="https://github.com/aws/babushka/issues/600">issue #600</a> for more
   * details.
   */
  public void registerConnection(CompletableFuture<Response> future) {
    CallbackManager.connectionRequests.add(future);
  }

  public static void completeAsync(Response response) {
    int callbackId = response.getCallbackIdx();
    if (callbackId == 0) {
      // can't distinguish connection requests since they have no
      // callback ID
      // https://github.com/aws/babushka/issues/600
      connectionRequests.pop().completeAsync(() -> response);
    } else {
      responses.get(callbackId).completeAsync(() -> response);
      responses.remove(callbackId);
    }
  }

  public static void shutdownGracefully() {
    connectionRequests.forEach(
        future -> {
          future.completeExceptionally(new InterruptedException());
        });
    connectionRequests.clear();
    responses.forEach(
        (callbackId, future) -> {
          future.completeExceptionally(new InterruptedException());
        });
    responses.clear();
  }

  /**
   * Create a unique callback ID (request ID) and a corresponding registered future for the
   * response.<br>
   *
   * @return New callback ID and new future to be returned to user.
   */
  private synchronized Pair<Integer, CompletableFuture<Response>> getNextCallback() {
    var future = new CompletableFuture<Response>();
    int callbackId = registerRequest(future);
    return Pair.of(callbackId, future);
  }
}
