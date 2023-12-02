package babushka.benchmarks.clients.babushka;

import babushka.api.Awaiter;
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
    return connection.connectToRedis(
        connectionSettings.host,
        connectionSettings.port,
        connectionSettings.useSsl,
        connectionSettings.clusterMode);
  }

  @Override
  public Future<String> asyncSet(String key, String value) {
    return asyncCommands.set(key, value);
  }

  @Override
  public Future<String> asyncGet(String key) {
    return asyncCommands.get(key);
  }

  @Override
  public void set(String key, String value) {
    Awaiter.await(asyncCommands.set(key, value));
  }

  @Override
  public String get(String key) {
    return Awaiter.await(asyncCommands.get(key));
  }
}
