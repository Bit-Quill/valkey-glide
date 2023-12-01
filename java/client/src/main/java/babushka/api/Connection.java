package babushka.api;

import static babushka.api.Awaiter.await;

import babushka.managers.ConnectionManager;
import java.util.concurrent.CompletableFuture;

public class Connection {

  private final ConnectionManager connectionManager;

  public Connection(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  public ConnectionManager getConnectionManager() {
    return connectionManager;
  }

  /**
   * Sync (blocking) connect to REDIS. See async option in {@link #asyncConnectToRedis}.
   *
   * @param host Server address
   * @param port Server port
   * @param useSsl true if communication with the server or cluster should use Transport Level
   *     Security
   * @param clusterMode true if REDIS instance runs in the cluster mode
   */
  // TODO support configuration object which holds more parameters (e.g. multiple addresses, etc)
  public void connectToRedis(String host, int port, boolean useSsl, boolean clusterMode) {
    await(asyncConnectToRedis(host, port, useSsl, clusterMode));
  }

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
  public CompletableFuture<String> asyncConnectToRedis(
      String host, int port, boolean useSsl, boolean clusterMode) {
    return connectionManager.connectToRedis(host, port, useSsl, clusterMode);
  }

  /** Close all connections and release resources */
  public void closeConnection() {
    connectionManager.closeConnection();
  }
}
