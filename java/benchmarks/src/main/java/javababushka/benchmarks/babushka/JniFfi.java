package javababushka.benchmarks.babushka;

import javababushka.benchmarks.SyncClient;
import javababushka.benchmarks.utils.ConnectionSettings;

public class JniFfi implements SyncClient {

  private long ptr = 0;

  @Override
  public void connectToRedis() {
    connectToRedis(new ConnectionSettings("localhost", 6379, false));
  }

  @Override
  public void connectToRedis(ConnectionSettings connectionSettings) {
    ptr = Jni.init_client(0);
    Jni.connect(
        ptr,
        String.format(
            "%s://%s:%d",
            connectionSettings.useSsl ? "rediss" : "redis",
            connectionSettings.host,
            connectionSettings.port));
  }

  @Override
  public String getName() {
    return "JNI FFI Sync client";
  }

  @Override
  public void set(String key, String value) {
    Jni.set(ptr, key, value);
  }

  @Override
  public String get(String key) {
    return Jni.get(ptr, key).string;
  }
}
