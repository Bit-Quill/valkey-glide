package babushka.managers;

import babushka.connectors.SocketConnection;
import babushka.connectors.handlers.ChannelHandler;
import babushka.models.RequestBuilder;
import connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import response.ResponseOuterClass.ConstantResponse;

/**
 * A UDS connection manager. This class is responsible for:
 *
 * <ul>
 *   <li>opening a connection (channel) though the UDS;
 *   <li>allocating the corresponding resources, e.g. thread pools (see also {@link
 *       SocketConnection});
 *   <li>handling connection requests;
 *   <li>handling REDIS requests;
 *   <li>closing connection;
 * </ul>
 */
@RequiredArgsConstructor
public class ConnectionManager {

  private final ChannelHandler channel;
  @Getter private final CommandManager commandManager;

  private final AtomicBoolean isConnected = new AtomicBoolean(false);

  /**
   * Connect to Redis using a ProtoBuf connection request
   *
   * @param host Server address
   * @param port Server port
   * @param useSsl true if communication with the server or cluster should use Transport Level
   *     Security
   * @param clusterMode true if REDIS instance runs in the cluster mode
   */
  public CompletableFuture<Boolean> connectToRedis(
      String host, int port, boolean useSsl, boolean clusterMode) {
    ConnectionRequest request =
        RequestBuilder.createConnectionRequest(host, port, useSsl, clusterMode);
    return channel
        .connect(request)
        .thenApplyAsync(
            response ->
                isConnected.compareAndSet(
                    false, response.getConstantResponse() == ConstantResponse.OK));
  }

  /**
   * Close socket connection and drop all channels.<br>
   * TODO: provide feedback that the connection was properly closed
   */
  public CompletableFuture<String> closeConnection() {
    isConnected.setPlain(false);
    channel.close();
    return new CompletableFuture<String>();
  }

  /** Check that connection established. This doesn't validate whether it is alive. */
  public boolean isConnected() {
    return isConnected.get();
  }
}
