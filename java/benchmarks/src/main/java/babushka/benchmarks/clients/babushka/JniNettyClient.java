package babushka.benchmarks.clients.babushka;

import babushka.benchmarks.clients.AsyncClient;
import babushka.benchmarks.clients.SyncClient;
import babushka.benchmarks.utils.ConnectionSettings;
import babushka.client.Commands;
import babushka.client.Connection;
import java.util.concurrent.Future;

public class JniNettyClient implements SyncClient, AsyncClient {

  private final Connection connection;
  private Commands commands = null;
  private String name = "JNI Netty";

  public JniNettyClient(boolean async) {
    name += async ? " async" : " sync";
    connection = new Connection();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void connectToRedis(ConnectionSettings connectionSettings) {
    connection.connectToRedis(
        connectionSettings.host,
        connectionSettings.port,
        connectionSettings.useSsl,
        connectionSettings.clusterMode);
    commands = connection.getCommands();
  }

  @Override
  public Future<Boolean> asyncSet(String key, String value) {
    return commands.asyncSet(key, value);
  }

  @Override
  public Future<String> asyncGet(String key) {
    return commands.asyncGet(key);
  }

  @Override
  public void set(String key, String value) {
    commands.set(key, value);
  }

  @Override
  public String get(String key) {
    return commands.get(key);
  }
}
