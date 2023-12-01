package babushka.benchmarks.clients.babushka;

import babushka.api.Client;
import babushka.benchmarks.clients.AsyncClient;
import babushka.benchmarks.clients.SyncClient;
import babushka.benchmarks.utils.ConnectionSettings;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import response.ResponseOuterClass.Response;

public class JniNettyClient implements SyncClient, AsyncClient<String> {

  private final Client testClient;
  private String name = "JNI Netty";

  public JniNettyClient(boolean async) {
    name += async ? " async" : " sync";
    testClient = new Client();
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
    return testClient.asyncConnectToRedis(
        connectionSettings.host,
        connectionSettings.port,
        connectionSettings.useSsl,
        connectionSettings.clusterMode);
  }

  @Override
  public Future<String> asyncSet(String key, String value) {
    return testClient.asyncSet(key, value);
  }

  @Override
  public Future<String> asyncGet(String key) {
    return testClient.asyncGet(key);
  }

  @Override
  public void set(String key, String value) {
    testClient.set(key, value);
  }

  @Override
  public String get(String key) {
    return testClient.get(key);
  }
}
