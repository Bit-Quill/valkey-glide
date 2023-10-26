package javababushka.benchmarks.babushka;

import babushka.BabushkaClient;
import babushka.BabushkaClientData;
import babushka.BabushkaRedisException;
import javababushka.benchmarks.SyncClient;
import javababushka.benchmarks.utils.ConnectionSettings;

public class KotlinClient implements SyncClient {

  private BabushkaClient client = new BabushkaClient();
  private BabushkaClientData data = null;

  @Override
  public void connectToRedis() {
    connectToRedis(new ConnectionSettings("localhost", 6379, false));
  }

  @Override
  public void connectToRedis(ConnectionSettings connectionSettings) {
    try {
      data =
          client.connect(
              String.format(
                  "%s://%s:%d",
                  connectionSettings.useSsl ? "rediss" : "redis",
                  connectionSettings.host,
                  connectionSettings.port));
    } catch (BabushkaRedisException e) {
      System.out.printf("Failed to connect: %s%n", e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  public String getName() {
    return "kotlin";
  }

  @Override
  public void set(String key, String value) {
    try {
      client.set(data, key, value);
    } catch (BabushkaRedisException e) {
      System.out.printf("failed to get3: %s%n", e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  public String get(String key) {
    try {
      return client.get(data, key);
    } catch (BabushkaRedisException e) {
      System.out.printf("failed to get3: %s%n", e.getMessage());
      e.printStackTrace();
    }
    return null;
  }
}
