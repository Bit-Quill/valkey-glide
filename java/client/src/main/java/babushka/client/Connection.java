package babushka.client;

import babushka.connection.SocketManager;
import babushka.tools.Awaiter;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import response.ResponseOuterClass.ConstantResponse;

public class Connection {

  // TODO: not used yet, not implemented on rust side
  // https://github.com/aws/babushka/issues/635
  private final int connectionId = 0;

  private final AtomicBoolean isConnected = new AtomicBoolean(false);

  private final SocketManager socketManager;

  public Connection() {
    socketManager = SocketManager.getInstance();
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
  public boolean connectToRedis(String host, int port, boolean useSsl, boolean clusterMode) {
    return Awaiter.await(asyncConnectToRedis(host, port, useSsl, clusterMode));
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
  public CompletableFuture<Boolean> asyncConnectToRedis(
      String host, int port, boolean useSsl, boolean clusterMode) {
    var request = RequestBuilder.createConnectionRequest(host, port, useSsl, clusterMode);
    return socketManager
        .connect(request)
        .thenApply(
            response ->
                isConnected.compareAndSet(
                    true, response.getConstantResponse() == ConstantResponse.OK));
  }

  /** Sync (blocking) disconnect. See async option in {@link #asyncCloseConnection}. */
  public void closeConnection() {
    Awaiter.await(asyncCloseConnection());
  }

  /** Async (non-blocking) disconnect. See sync option in {@link #closeConnection}. */
  // TODO Not implemented yet in rust core lib.
  public CompletableFuture<Void> asyncCloseConnection() {
    isConnected.setPlain(false);
    return CompletableFuture.runAsync(() -> {});
  }

  public Commands getCommands() {
    if (!isConnected.get()) {
      throw new NotYetConnectedException();
    }
    return new Commands(socketManager);
  }
}
