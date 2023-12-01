package babushka.benchmarks.clients.babushka;

import babushka.api.Client;
import babushka.api.Commands;
import babushka.api.Connection;
import babushka.benchmarks.clients.AsyncClient;
import babushka.benchmarks.clients.SyncClient;
import babushka.benchmarks.utils.ConnectionSettings;
import java.util.concurrent.Future;

public class JniNettyClient implements SyncClient, AsyncClient<String> {

  private final Connection connection;
  private final Commands asyncCommands;

  private String name = "JNI Netty";

  public JniNettyClient(boolean async) {
    name += async ? " async" : " sync";
    connection = Client.CreateConnection();
    asyncCommands = Client.GetAsyncCommands(connection);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void connectToRedis() {
    connectToRedis(new ConnectionSettings("localhost", 6379, false, false));
  }

  @Override
  public void connectToRedis(ConnectionSettings connectionSettings) {
    waitForResult(asyncConnectToRedis(connectionSettings));
  }

  @Override
  public Future<String> asyncConnectToRedis(ConnectionSettings connectionSettings) {
    return connection.asyncConnectToRedis(
        connectionSettings.host,
        connectionSettings.port,
        connectionSettings.useSsl,
        connectionSettings.clusterMode);
  }

  @Override
  public Future<String> asyncSet(String key, String value) {
    return asyncCommands.asyncSet(key, value);
  }

  @Override
  public Future<String> asyncGet(String key) {
    return asyncCommands.asyncGet(key);
  }

  @Override
  public void set(String key, String value) {
    asyncCommands.set(key, value);
  }

  @Override
  public String get(String key) {
    return asyncCommands.get(key);
  }
}
