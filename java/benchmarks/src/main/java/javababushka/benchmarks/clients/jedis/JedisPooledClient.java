package javababushka.benchmarks.clients.jedis;

import javababushka.benchmarks.clients.SyncClient;
import javababushka.benchmarks.utils.ConnectionSettings;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;

/** A Jedis client with sync capabilities. See: https://github.com/redis/jedis */
public class JedisPooledClient implements SyncClient {

  //  protected Jedis jedisResource;
  protected JedisPooled pool;

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
        new JedisPooled(connectionSettings.host, connectionSettings.port, connectionSettings.useSsl);

    // check if the pool is properly connected
    assert pool.getPool().getResource().isConnected() : "failed to connect to jedis";
  }

  public String info() {
    return "N/A";
  }

  public String info(String section) {
    return "N/A";
  }

  @Override
  public void set(String key, String value) {
    pool.set(key, value);
  }

  @Override
  public String get(String key) {
    return pool.get(key);
  }
}
