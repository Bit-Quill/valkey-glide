package babushka.benchmarks.clients.babushka;

import babushka.api.RedisClient;
import babushka.api.models.commands.SetOptions;
import babushka.api.models.configuration.NodeAddress;
import babushka.api.models.configuration.RedisClientConfiguration;
import babushka.benchmarks.clients.AsyncClient;
import babushka.benchmarks.utils.ConnectionSettings;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/** A Babushka Redis client with async capabilities see */
public class BabushkaAsyncClient implements AsyncClient<String> {

  private RedisClient client;

  @Override
  public void connectToRedis(ConnectionSettings connectionSettings) {
    RedisClientConfiguration config =
        RedisClientConfiguration.builder()
            .address(
                NodeAddress.builder()
                    .host(connectionSettings.host)
                    .port(connectionSettings.port)
                    .build())
            .useTLS(connectionSettings.useSsl)
            .build();
    if (!connectionSettings.clusterMode) {
      CompletableFuture<RedisClient> clientResponse = RedisClient.CreateClient(config);
      try {
        client = clientResponse.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException("Cluster Mode not yet available");
    }
  }

  @Override
  public CompletableFuture<String> asyncSet(String key, String value) {
    SetOptions options = SetOptions.builder().returnOldValue(true).build();
    return client.set(key, value, options);
  }

  @Override
  public CompletableFuture<String> asyncGet(String key) {
    return client.get(key);
  }

  @Override
  public void closeConnection() {
    client.close();
  }

  @Override
  public String getName() {
    return "Babushka Async";
  }
}
