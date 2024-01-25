package glide.examples.clients;

import glide.examples.ExamplesApp;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisStringAsyncCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import java.time.Duration;

/** Connect to lettuce client - see: https://lettuce.io/ */
public class LettuceAsyncClient {
    static final int ASYNC_OPERATION_TIMEOUT_SEC = 1;

    private AbstractRedisClient client;
    public RedisStringAsyncCommands<String, String> asyncCommands;
    private StatefulConnection<String, String> connection;

    public LettuceAsyncClient(ExamplesApp.ConnectionSettings connectionSettings) {
        RedisURI uri =
                RedisURI.builder()
                        .withHost(connectionSettings.host)
                        .withPort(connectionSettings.port)
                        .withSsl(connectionSettings.useSsl)
                        .build();
        if (!connectionSettings.clusterMode) {
            client = RedisClient.create(uri);
            connection = ((RedisClient) client).connect();
            asyncCommands = ((StatefulRedisConnection<String, String>) connection).async();
        } else {
            client = RedisClusterClient.create(uri);
            connection = ((RedisClusterClient) client).connect();
            asyncCommands = ((StatefulRedisClusterConnection<String, String>) connection).async();
        }
        connection.setTimeout(Duration.ofSeconds(ASYNC_OPERATION_TIMEOUT_SEC));
    }

    public void closeConnection() {
        connection.close();
        client.shutdown();
    }
}
