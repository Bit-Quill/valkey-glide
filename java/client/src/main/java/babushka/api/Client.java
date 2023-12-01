package babushka.api;

import babushka.connectors.SocketConnection;
import babushka.ffi.resolvers.BabushkaCoreNativeDefinitions;
import babushka.managers.CallbackManager;
import babushka.managers.ConnectionManager;

/** Factory class for creating Babushka-Redis client connections */
public class Client {

  public static Connection CreateConnection() {
    CallbackManager callbackManager = new CallbackManager();

    BabushkaCoreNativeDefinitions nativeDefinitions = new BabushkaCoreNativeDefinitions();
    SocketConnection.setSocketPath(nativeDefinitions.getSocket());
    SocketConnection socketConnection = SocketConnection.getInstance();

    ConnectionManager connectionManager = new ConnectionManager(callbackManager, socketConnection);

    return new Connection(connectionManager);
  }

  public static Connection ConnectToRedis(String host, int port) {
    CallbackManager callbackManager = new CallbackManager();

    BabushkaCoreNativeDefinitions nativeDefinitions = new BabushkaCoreNativeDefinitions();
    SocketConnection.setSocketPath(nativeDefinitions.getSocket());
    SocketConnection socketConnection = SocketConnection.getInstance();

    ConnectionManager connectionManager = new ConnectionManager(callbackManager, socketConnection);

    Awaiter.await(connectionManager.connectToRedis(host, port, false, false));

    return new Connection(connectionManager);
  }

  public static Commands GetAsyncCommands(Connection connection) {
    return new Commands(connection.getConnectionManager().getCommandManager());
  }
}
