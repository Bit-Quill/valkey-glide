package babushka.connection;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import response.ResponseOuterClass;

public class CommonResources {
  // Futures to handle responses. Index is callback id, starting from 1 (0 index is for connection
  // request always).
  // Is it not a concurrent nor sync collection, but it is synced on adding. No removes.
  public static final Map<Integer, CompletableFuture<ResponseOuterClass.Response>> responses =
      new ConcurrentHashMap<>();

  public static final Deque<CompletableFuture<ResponseOuterClass.Response>> connectionRequests =
      new ConcurrentLinkedDeque<>();
}
