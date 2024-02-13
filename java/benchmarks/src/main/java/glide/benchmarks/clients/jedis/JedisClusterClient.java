/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.benchmarks.clients.jedis;

import glide.benchmarks.clients.SyncClient;
import glide.benchmarks.utils.ConnectionSettings;
import java.util.Set;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

/** A Jedis cluster client with sync capabilities. See: https://github.com/redis/jedis */
public class JedisClusterClient implements SyncClient {
    private JedisCluster jedisCluster;

    @Override
    public void closeConnection() {
        jedisCluster.close();
    }

    @Override
    public String getName() {
        return "Jedis";
    }

    @Override
    public void connectToRedis(ConnectionSettings connectionSettings) {
        if (connectionSettings.clusterMode) {
            jedisCluster =
                    new JedisCluster(
                            Set.of(new HostAndPort(connectionSettings.host, connectionSettings.port)),
                            DefaultJedisClientConfig.builder().ssl(connectionSettings.useSsl).build());
        }
        throw new RuntimeException("Use JedisClient for standalone client");
    }

    @Override
    public void set(String key, String value) {
        jedisCluster.set(key, value);
    }

    @Override
    public String get(String key) {
        return jedisCluster.get(key);
    }
}
