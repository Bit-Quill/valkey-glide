package javababushka.benchmarks.clients.jedis;

import javababushka.benchmarks.clients.SyncClient;
import javababushka.benchmarks.utils.ConnectionSettings;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/** A Jedis client with sync capabilities. See: https://github.com/redis/jedis */
public class JedisClient implements SyncClient {

  protected Jedis jedisResource;

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
    JedisPool pool =
        new JedisPool(connectionSettings.host, connectionSettings.port, connectionSettings.useSsl);

    // check if the pool is properly connected
    jedisResource = pool.getResource();
    assert jedisResource.isConnected() : "failed to connect to jedis";
  }

  public String info() {
    return jedisResource.info();
  }

  public String info(String section) {
    return jedisResource.info(section);
  }

  @Override
  public void set(String key, String value) {
    jedisResource.set(key, value);
  }

  @Override
  public String get(String key) {
    return jedisResource.get(key);
  }
}
