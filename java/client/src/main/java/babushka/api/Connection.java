package babushka.api;

import babushka.managers.ConnectionManager;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Connection {

  @Getter private final ConnectionManager connectionManager;

  /**
   * Async (non-blocking) connect to REDIS. See sync option in {@link #connectToRedis}.
   *
   * @param host Server address
   * @param port Server port
   * @param useSsl true if communication with the server or cluster should use Transport Level
   *     Security
   * @param clusterMode true if REDIS instance runs in the cluster mode
   */
  // TODO support configuration object which holds more parameters (e.g. multiple addresses, etc)
  public CompletableFuture<Boolean> connectToRedis(
      String host, int port, boolean useSsl, boolean clusterMode) {
    return connectionManager.connectToRedis(host, port, useSsl, clusterMode);
  }
}
