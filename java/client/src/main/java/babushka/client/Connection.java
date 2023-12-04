package babushka.client;

import babushka.tools.Awaiter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import response.ResponseOuterClass.ConstantResponse;

@RequiredArgsConstructor
public class Connection {

  private final AtomicBoolean isConnected = new AtomicBoolean(false);

  private final ChannelHolder channel;

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
    return channel
        .connect(request)
        .thenApplyAsync(
            response ->
                isConnected.compareAndSet(
                    false, response.getConstantResponse() == ConstantResponse.OK));
  }

  /** Sync (blocking) disconnect. See async option in {@link #asyncCloseConnection}. */
  public void closeConnection() {
    Awaiter.await(asyncCloseConnection());
  }

  /** Async (non-blocking) disconnect. See sync option in {@link #closeConnection}. */
  public CompletableFuture<Void> asyncCloseConnection() {
    isConnected.setPlain(false);
    return CompletableFuture.runAsync(channel::close);
  }

  /** Check that connection established. This doesn't validate whether it is alive. */
  public boolean isConnected() {
    return isConnected.get();
  }
}
