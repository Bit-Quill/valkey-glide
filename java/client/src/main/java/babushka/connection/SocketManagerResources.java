package babushka.connection;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import response.ResponseOuterClass.Response;

/** Holder for resources owned by {@link SocketManager} and used by {@link ReadHandler}. */
public class SocketManagerResources {

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
}
