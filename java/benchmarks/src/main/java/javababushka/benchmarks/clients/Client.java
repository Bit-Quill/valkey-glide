package javababushka.benchmarks.clients;

import javababushka.benchmarks.utils.ConnectionSettings;

/** A Redis client interface */
public interface Client {
  static ConnectionSettings DEFAULT_CONNECTION_STRING =
      new ConnectionSettings("localhost", 6379, false, false);

  void connectToRedis();

  void connectToRedis(ConnectionSettings connectionSettings);

  default void closeConnection() {}

  String getName();
}
