package babushka.benchmarks.clients;

import babushka.benchmarks.utils.ConnectionSettings;

/** A Redis client interface */
public interface Client {
  void connectToRedis();

  void connectToRedis(ConnectionSettings connectionSettings);

  default void closeConnection() {}

  String getName();
}
