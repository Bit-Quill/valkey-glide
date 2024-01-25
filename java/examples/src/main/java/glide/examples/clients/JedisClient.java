package glide.examples.clients;

import glide.examples.ExamplesApp;
import java.util.Set;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.commands.JedisCommands;

/** Connect to Jedis client. See: https://github.com/redis/jedis */
public class JedisClient {

    public static JedisCommands connectToRedis(ExamplesApp.ConnectionSettings connectionSettings) {
        JedisCommands jedis;
        if (connectionSettings.clusterMode) {
            jedis =
                    new JedisCluster(
                            Set.of(new HostAndPort(connectionSettings.host, connectionSettings.port)),
                            DefaultJedisClientConfig.builder().ssl(connectionSettings.useSsl).build());
        } else {
            try (JedisPool pool =
                    new JedisPool(
                            connectionSettings.host, connectionSettings.port, connectionSettings.useSsl)) {
                jedis = pool.getResource();
            }
        }
        return jedis;
    }
}
