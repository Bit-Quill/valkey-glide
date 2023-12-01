package babushka.managers;

import static babushka.models.RequestBuilder.getConnectionRequest;

import babushka.connectors.SocketConnection;
import connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import java.util.concurrent.CompletableFuture;
import response.ResponseOuterClass.Response;

/**
 * A UDS connection manager. This class is responsible for:
 *
 * <ul>
 *   <li>opening a connection (channel) though the UDS;
 *   <li>allocating the corresponding resources, e.g. thread pools (see also {@link
 *       CallbackManager});
 *   <li>handling connection requests;
 *   <li>providing unique request ID (callback ID);
 *   <li>handling REDIS requests;
 *   <li>closing connection;
 * </ul>
 */
public class ConnectionManager {

  private final SocketConnection socketConnection;
  private final CallbackManager callbackManager;

  private final CommandManager commandManager;

  public ConnectionManager(CallbackManager callbackManager, SocketConnection socketConnection) {
    this.socketConnection = socketConnection;
    this.callbackManager = callbackManager;
    this.commandManager = new CommandManager(callbackManager, socketConnection);
  }

  public CommandManager getCommandManager() {
    return this.commandManager;
  }

  /**
   * Connect to Redis using a ProtoBuf connection request
   *
   * @param host
   * @param port
   * @param useSsl
   * @param clusterMode
   * @return
   */
  public CompletableFuture<String> connectToRedis(
      String host, int port, boolean useSsl, boolean clusterMode) {
    ConnectionRequest request = getConnectionRequest(host, port, useSsl, clusterMode);
    var future = new CompletableFuture<Response>();
    callbackManager.registerConnection(future);
    socketConnection.writeAndFlush(request);
    return future.thenApply(f -> f.getConstantResponse().toString());
  }

  /**
   * Close socket connection and drop all channels TODO: provide feedback that the connection was
   * properly closed
   */
  public void closeConnection() {
    socketConnection.close();
  }
}
