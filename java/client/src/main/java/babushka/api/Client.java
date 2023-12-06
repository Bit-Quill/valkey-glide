package babushka.api;

import babushka.connectors.SocketConnection;
import babushka.ffi.resolvers.BabushkaCoreNativeDefinitions;
import babushka.managers.CallbackManager;
import babushka.managers.CommandManager;
import babushka.managers.ConnectionManager;

/** Factory class for creating Babushka-Redis client connections */
public class Client {

  static {
    SocketConnection.setSocketPath(BabushkaCoreNativeDefinitions.getSocket());
  }

  public static Connection CreateConnection() {
    CallbackManager callbackManager = new CallbackManager();

    SocketConnection socketConnection = SocketConnection.getInstance();
    var channelHandler = socketConnection.openNewChannel(callbackManager);

    CommandManager commandManager = new CommandManager(channelHandler);
    ConnectionManager connectionManager = new ConnectionManager(channelHandler, commandManager);

    return new Connection(connectionManager);
  }

  public static Connection ConnectToRedis(String host, int port) {
    CallbackManager callbackManager = new CallbackManager();

    SocketConnection socketConnection = SocketConnection.getInstance();
    var channelHandler = socketConnection.openNewChannel(callbackManager);

    CommandManager commandManager = new CommandManager(channelHandler);
    ConnectionManager connectionManager = new ConnectionManager(channelHandler, commandManager);

    Awaiter.await(connectionManager.connectToRedis(host, port, false, false));

    return new Connection(connectionManager);
  }

  public static Commands GetAsyncCommands(Connection connection) {
    return new Commands(connection.getConnectionManager().getCommandManager());
  }
}
