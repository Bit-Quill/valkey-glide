package javababushka.benchmarks.clients.jedis;

import javababushka.benchmarks.clients.SyncClient;
import javababushka.benchmarks.utils.ConnectionSettings;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/** A Jedis client with sync capabilities. See: https://github.com/redis/jedis */
public class JedisPoolClient implements SyncClient {

  //  protected Jedis jedisResource;
  protected JedisPool pool;

  // protected JedisPooled pooledConnection;
  @Override
  public void closeConnection() {
    // nothing to do
  }

  @Override
  public String getName() {
    return "Jedis";
  }

  @Override
  public void connectToRedis() {
    connectToRedis(DEFAULT_CONNECTION_STRING);
  }

  @Override
  public void connectToRedis(ConnectionSettings connectionSettings) {
    assert connectionSettings.clusterMode == false
        : "JedisClient does not support clusterMode: use JedisClusterClient instead";
    pool =
        new JedisPool(connectionSettings.host, connectionSettings.port, connectionSettings.useSsl);

    // check if the pool is properly connected
    try (Jedis jedis = pool.getResource()) {
      assert jedis.isConnected() : "failed to connect to jedis";
    }
  }

  public String info() {
    try (Jedis jedis = pool.getResource()) {
      return jedis.info();
    }
  }

  public String info(String section) {
    try (Jedis jedis = pool.getResource()) {
      return jedis.info(section);
    }
  }

  @Override
  public void set(String key, String value) {
    try (Jedis jedis = pool.getResource()) {
      jedis.set(key, value);
    }
  }

  @Override
  public String get(String key) {
    try (Jedis jedis = pool.getResource()) {
      return jedis.get(key);
    }
  }
}
