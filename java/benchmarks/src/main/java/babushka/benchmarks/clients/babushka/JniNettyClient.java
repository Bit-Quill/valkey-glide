package babushka.benchmarks.clients.babushka;

import babushka.Client;
import babushka.benchmarks.clients.AsyncClient;
import babushka.benchmarks.clients.SyncClient;
import babushka.benchmarks.utils.ConnectionSettings;
import java.util.concurrent.Future;

public class JniNettyClient implements SyncClient, AsyncClient {

  private final Client testClient = new Client();
  private String name = "JNI Netty";

  public JniNettyClient(boolean async) {
    name += async ? " async" : " sync";
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void closeConnection() {
    testClient.getConnection().closeConnection();
  }

  @Override
  public void connectToRedis(ConnectionSettings connectionSettings) {
    testClient
        .getConnection()
        .connectToRedis(
            connectionSettings.host,
            connectionSettings.port,
            connectionSettings.useSsl,
            connectionSettings.clusterMode);
  }

  @Override
  public Future<Boolean> asyncSet(String key, String value) {
    return testClient.getCommands().asyncSet(key, value);
  }

  @Override
  public Future<String> asyncGet(String key) {
    return testClient.getCommands().asyncGet(key);
  }

  @Override
  public void set(String key, String value) {
    testClient.getCommands().set(key, value);
  }

  @Override
  public String get(String key) {
    return testClient.getCommands().get(key);
  }
}
