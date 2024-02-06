/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import glide.api.models.commands.SetOptions;
import glide.api.models.commands.SetOptions.ConditionalSet;
import glide.api.models.commands.SetOptions.SetOptionsBuilder;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * String Commands interface to handle single commands.
 *
 * @see <a href="https://redis.io/commands/?group=string">String Commands</a>
 */
public interface StringCommands {

    /**
     * Get the value associated with the given key, or null if no such value exists.
     *
     * @see <a href="https://redis.io/commands/get/">redis.io</a> for details.
     * @param key The key to retrieve from the database.
     * @return Response from Redis. <code>key</code> exists, returns the <code>value</code> of <code>
     *     key</code> as a String. Otherwise, return <code>null</code>.
     */
    CompletableFuture<String> get(String key);

    /**
     * Set the given key with the given value.
     *
     * @see <a href="https://redis.io/commands/set/">redis.io</a> for details.
     * @param key The key to store.
     * @param value The value to store with the given <code>key</code>.
     * @return Response from Redis.
     */
    CompletableFuture<String> set(String key, String value);

    /**
     * Set the given key with the given value. Return value is dependent on the passed options.
     *
     * @see <a href="https://redis.io/commands/set/">redis.io</a> for details.
     * @param key The key to store.
     * @param value The value to store with the given key.
     * @param options The Set options.
     * @return Response from Redis containing a <code>String</code> or <code>null</code> response. The
     *     old value as a <code>String</code> if {@link SetOptionsBuilder#returnOldValue(boolean)} is
     *     set. Otherwise, if the value isn't set because of {@link ConditionalSet#ONLY_IF_EXISTS} or
     *     {@link ConditionalSet#ONLY_IF_DOES_NOT_EXIST} conditions, return <code>null</code>.
     *     Otherwise, return <code>OK</code>.
     */
    CompletableFuture<String> set(String key, String value, SetOptions options);

    /**
     * Increment the string representing a floating point number stored at <em>key</em> by
     * <em>amount</em>. By using a negative increment value, the result is that the value stored at
     * <em>key</em> is decremented. If <em>key</em> does not exist, it is set to 0 before performing
     * the operation.
     *
     * @see <a href="https://redis.io/commands/decr/">redis.io</a> for details.
     * @param key The key to increment its value.
     * @return The value of <em>key</em> after the increment. An error is raised if <em>key</em>
     *     contains a value of the wrong type, or the current key content is not parsable as a double
     *     precision floating point number.
     */
    CompletableFuture<Long> decr(String key);

    /**
     * Decrements the number stored at <em>key</em> by <em>amount</em>. If <em>key</em> does not
     * exist, it is set to 0 before performing the operation.
     *
     * @see <a href="https://redis.io/commands/decrby/">redis.io</a> for details.
     * @param key The key to decrement its value.
     * @param amount The amount to decrement.
     * @return The value of <em>key</em> after the decrement. An error is raised if <em>key</em>
     *     contains a value of the wrong type or contains a string that can not be represented as
     *     integer.
     */
    CompletableFuture<Long> decrBy(String key, long amount);

    /**
     * Increments the number stored at <em>key</em> by one. If <em>key</em> does not exist, it is set
     * to 0 before performing the operation.
     *
     * @see <a href="https://redis.io/commands/incr/">redis.io</a> for details.
     * @param key The key to increment its value.
     * @returns The value of <em>key</em> after the increment. An error is raised if <em>key</em>
     *     contains a value of the wrong type or contains a string that can not be represented as
     *     integer.
     */
    CompletableFuture<Long> incr(String key);

    /**
     * Increments the number stored at <em>key</em> by <em>amount</em>. If <em>key</em> does not
     * exist, it is set to 0 before performing the operation.
     *
     * @see <a href="https://redis.io/commands/incrby/">redis.io</a> for details.
     * @param key The key to increment its value.
     * @param amount The amount to increment.
     * @return The value of <em>key</em> after the increment, An error is raised if <em>key</em>
     *     contains a value of the wrong type or contains a string that can not be represented as
     *     integer.
     */
    CompletableFuture<Long> incrBy(String key, long amount);

    /**
     * Increment the string representing a floating point number stored at <em>key</em> by
     * <em>amount</em>. By using a negative increment value, the result is that the value stored at
     * <em>key</em> is decremented. If <em>key</em> does not exist, it is set to 0 before performing
     * the operation.
     *
     * @see <a href="https://redis.io/commands/incrbyfloat/">redis.io</a> for details.
     * @param key The key to increment its value.
     * @param amount The amount to increment.
     * @return The value of <em>key</em> after the increment. An error is raised if <em>key</em>
     *     contains a value of the wrong type, or the current key content is not parsable as a double
     *     precision floating point number.
     */
    CompletableFuture<Double> incrByFloat(String key, double amount);

    /**
     * Retrieve the values of multiple keys.
     *
     * @see <a href="https://redis.io/commands/mget/">redis.io</a> for details.
     * @param keys A list of keys to retrieve values for.
     * @return A list of values corresponding to the provided keys. If a key is not found, its
     *     corresponding value in the list will be null.
     */
    CompletableFuture<String[]> mget(String[] keys);

    /**
     * Set multiple keys to multiple values in a single operation.
     *
     * @see <a href="https://redis.io/commands/mset/">redis.io</a> for details.
     * @param keyValueMap A key-value map consisting of keys and their respective values to set.
     * @return Always "Ok".
     */
    CompletableFuture<String> mset(HashMap<String, String> keyValueMap);
}
