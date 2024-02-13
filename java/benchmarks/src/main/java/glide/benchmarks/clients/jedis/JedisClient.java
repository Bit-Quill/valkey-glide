/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.benchmarks.clients.jedis;

import glide.benchmarks.clients.SyncClient;
import glide.benchmarks.utils.ConnectionSettings;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

/** A Jedis client with sync capabilities. See: https://github.com/redis/jedis */
public class JedisClient implements SyncClient {
    private JedisPool jedisPool;
    private JedisCluster jedisCluster;

    @Override
    public void closeConnection() {
        jedisPool.close();
    }

    @Override
    public String getName() {
        return "Jedis";
    }

    @Override
    public void connectToRedis(ConnectionSettings connectionSettings) {
        if (connectionSettings.clusterMode) {
            throw new RuntimeException("Use JedisClsuterClient for cluster-mode client");
        }
        jedisPool =
                new JedisPool(connectionSettings.host, connectionSettings.port, connectionSettings.useSsl);
    }

    @Override
    public void set(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        }
    }

    @Override
    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }
}
