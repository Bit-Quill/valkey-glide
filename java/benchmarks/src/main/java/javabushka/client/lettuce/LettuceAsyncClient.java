/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package javabushka.client.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LettuceAsyncClient {

    public final static String DEFAULT_HOST = "localhost";
    public final static int DEFAULT_PORT = 6379;
    public final static boolean DEFAULT_TLS = false;

    RedisClient client;
    RedisAsyncCommands lettuceSync;
    StatefulRedisConnection<String, String> connection;

    public final long MAX_TIMEOUT_MS = 1000;

    public void connectToRedis() {
        connectToRedis(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_TLS);
    }

    public void connectToRedis(String host, int port, boolean tls) {
        RedisURI uri = RedisURI.builder()
            .withHost(host)
            .withPort(port)
            .withSsl(tls)
            .build();
        client = RedisClient.create("redis://" + host + ":" + port);
        connection = client.connect();
        lettuceSync = connection.async();
    }

    public RedisFuture set(String key, String value) {
        RedisFuture<String> future = lettuceSync.set(key, value);
        return future;
    }

    public RedisFuture get(String key) {
        RedisFuture future = lettuceSync.get(key);
        return future;
    }

    public Object waitForResult(RedisFuture future)
        throws ExecutionException, InterruptedException, TimeoutException {
        return this.waitForResult(future, MAX_TIMEOUT_MS);
    }

    public Object waitForResult(RedisFuture future, long timeoutMS)
        throws ExecutionException, InterruptedException, TimeoutException {
        return future.get(timeoutMS, TimeUnit.MILLISECONDS);
    }

    public void closeConnection() {
        connection.close();
        client.shutdown();
    }
}
