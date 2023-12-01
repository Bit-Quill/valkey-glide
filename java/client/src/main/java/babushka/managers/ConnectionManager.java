package babushka.managers;

import babushka.connectors.SocketConnection;
import babushka.ffi.resolvers.BabushkaCoreNativeDefinitions;
import com.google.common.annotations.VisibleForTesting;
import connection_request.ConnectionRequestOuterClass;
import connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import java.util.concurrent.CompletableFuture;
import response.ResponseOuterClass;
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
  private final BabushkaCoreNativeDefinitions nativeDefinitions;
  private final CallbackManager callbackManager;

  public ConnectionManager(CallbackManager callbackManager,
                           BabushkaCoreNativeDefinitions nativeDefinitions,
                           SocketConnection socketConnection) {
    this.socketConnection = socketConnection;
    this.nativeDefinitions = nativeDefinitions;
    this.callbackManager = callbackManager;
  }

  /**
   * Connect to Redis using a ProtoBuf connection request
   * @param host
   * @param port
   * @param useSsl
   * @param clusterMode
   * @return
   */
  public CompletableFuture<String> connectToRedis(
      String host, int port, boolean useSsl, boolean clusterMode) {
    ConnectionRequest
        request = getConnectionRequest(host, port, useSsl, clusterMode);
    var future = new CompletableFuture<Response>();
    callbackManager.registerConnection(future);
    socketConnection.writeAndFlush(request);
    return future.thenApply(f -> f.getConstantResponse().toString());
  }

  /**
   * Close socket connection and drop all channels
   * TODO: provide feedback that the connection was properly closed
   */
  public void closeConnection() {
    socketConnection.close();
  }

  /** Build a protobuf connection request object */
  // TODO support more parameters and/or configuration object
  @VisibleForTesting
  private static ConnectionRequest getConnectionRequest(
      String host, int port, boolean useSsl, boolean clusterMode) {
    return ConnectionRequest.newBuilder()
        .addAddresses(
            ConnectionRequestOuterClass.NodeAddress.newBuilder().setHost(host).setPort(port).build())
        .setTlsMode(useSsl ? ConnectionRequestOuterClass.TlsMode.SecureTls : ConnectionRequestOuterClass.TlsMode.NoTls)
        .setClusterModeEnabled(clusterMode)
        .setReadFrom(ConnectionRequestOuterClass.ReadFrom.Primary)
        .setDatabaseId(0)
        .build();
  }
}
