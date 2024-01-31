package glide.api.commands;

import glide.api.models.commands.SetOptions;
import java.util.concurrent.CompletableFuture;

/** String Commands interface to handle single commands that return Strings. */
public interface StringCommands {

    /**
     * Get the value associated with the given key, or null if no such value exists.
     *
     * @see <a href="https://redis.io/commands/get/">redis.io</a> for details.
     * @param key The key to retrieve from the database.
     * @return If <code>key</code> exists, returns the <code>value</code> of <code>key</code> as a
     *     String. Otherwise, return <code>null</code>.
     */
    CompletableFuture<String> get(String key);

    /**
     * Set the given key with the given value.
     *
     * @see <a href="https://redis.io/commands/set/">redis.io</a> for details.
     * @param key The key to store.
     * @param value The value to store with the given <code>key</code>.
     * @return An empty response
     */
    CompletableFuture<Void> set(String key, String value);

    /**
     * Set the given key with the given value. Return value is dependent on the passed options.
     *
     * @see <a href="https://redis.io/commands/set/">redis.io</a> for details.
     * @param key The key to store.
     * @param value The value to store with the given key.
     * @param options The Set options.
     * @return A string or null response. The old value as a string if <code>returnOldValue</code> is
     *     set. Otherwise, if the value isn't set because of <code>onlyIfExists</code> or <code>
     *     onlyIfDoesNotExist</code> conditions, return <code>null</code>. Otherwise, return "OK".
     */
    CompletableFuture<String> set(String key, String value, SetOptions options);
}
