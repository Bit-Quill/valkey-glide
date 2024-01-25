package glide.api.commands;

import glide.api.models.commands.SetOptions;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * String Commands interface.
 *
 * @see: <a href="https://redis.io/commands/?group=string">String Commands</a>
 */
public interface StringCommands {

    CompletableFuture<String> get(String key);

    CompletableFuture<Void> set(String key, String value);

    CompletableFuture<String> set(String key, String value, SetOptions options);

    CompletableFuture<Long> decr(String key);

    CompletableFuture<Long> decrBy(String key, long amount);

    CompletableFuture<Long> incr(String key);

    CompletableFuture<Long> incrBy(String key, long amount);

    CompletableFuture<Double> incrByFloat(String key, double amount);

    CompletableFuture<Object[]> mget(String[] keys);

    CompletableFuture<Void> mset(HashMap<String, String> keyValueMap);
}
