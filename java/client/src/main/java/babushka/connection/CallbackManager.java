package babushka.connection;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.tuple.Pair;
import response.ResponseOuterClass.Response;

/** Holder for resources owned by {@link SocketManager} and used by {@link ReadHandler}. */
class CallbackManager {

  /** Unique request ID (callback ID). Thread-safe. */
  private static final AtomicInteger requestId = new AtomicInteger(0);

  /**
   * Storage of Futures to handle responses. Map key is callback id, which starts from 1.<br>
   * Each future is a promise for every submitted by user request.
   */
  private static final Map<Integer, CompletableFuture<Response>> responses =
      new ConcurrentHashMap<>();

  /**
   * Storage for connection requests similar to {@link #responses}. Unfortunately, connection
   * requests can't be stored in the same storage, because callback ID = 0 is hardcoded for
   * connection requests. Will be removed once <a
   * href="https://github.com/aws/babushka/issues/600">issue #600 on GH</a> fixed.
   */
  private static final Deque<CompletableFuture<Response>> connectionRequests =
      new ConcurrentLinkedDeque<>();

  /**
   * Register a new request to be sent. Once response received, the given future completes with it.
   *
   * @return A pair of unique callback ID which should set into request and a client promise for
   *     response.
   */
  public static Pair<Integer, CompletableFuture<Response>> registerRequest() {
    int callbackId = requestId.incrementAndGet();
    var future = new CompletableFuture<Response>();
    responses.put(callbackId, future);
    return Pair.of(callbackId, future);
  }

  /**
   * Register a new connection request similar to {@link #registerRequest}.<br>
   * No callback ID returned, because connection request/response pair have no such field (subject
   * to change). Track <a href="https://github.com/aws/babushka/issues/600">issue #600</a> for more
   * details.
   */
  public static CompletableFuture<Response> registerConnection() {
    var future = new CompletableFuture<Response>();
    connectionRequests.add(future);
    return future;
  }

  /**
   * Complete the corresponding client promise and free resources.
   * @param response A response received
   */
  public static void completeRequest(Response response) {
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

  public static void clean() {
    // TODO should we reply in uncompleted futures?
    connectionRequests.clear();
    responses.clear();
  }
}
