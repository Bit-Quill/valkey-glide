/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import glide.api.models.commands.ExpireOptions;
import java.util.concurrent.CompletableFuture;

/**
 * Generic Commands interface to handle generic commands for all server requests.
 *
 * @see <a href="https://redis.io/commands/?group=generic">Generic Commands</a>
 */
public interface GenericBaseCommands {

    /**
     * Removes the specified <code>keys</code> from the database. A key is ignored if it does not
     * exist.
     *
     * @see <a href="https://redis.io/commands/del/">redis.io</a> for details.
     * @param keys The keys we wanted to remove.
     * @return The number of keys that were removed.
     */
    CompletableFuture<Long> del(String[] keys);

    /**
     * Returns the number of keys in <code>keys</code> that exist in the database.
     *
     * @see <a href="https://redis.io/commands/exists/">redis.io</a> for details.
     * @param keys The keys list to check.
     * @return The number of keys that exist. If the same existing key is mentioned in <code>keys
     *     </code> multiple times, it will be counted multiple times.
     * @example
     *     <p><code>
     * long result = client.exists(new String[] {"my_key", "invalid_key"}).get();
     * assert result == 1L;
     * </code>
     */
    CompletableFuture<Long> exists(String[] keys);

    /**
     * Unlink (delete) multiple <code>keys</code> from the database. A key is ignored if it does not
     * exist. This command, similar to <a href="https://redis.io/commands/del/">DEL</a>, removes
     * specified keys and ignores non-existent ones. However, this command does not block the server,
     * while <a href="https://redis.io/commands/del/">DEL</a> does.
     *
     * @see <a href="https://redis.io/commands/unlink/">redis.io</a> for details.
     * @param keys The list of keys to unlink.
     * @return The number of <code>keys</code> that were unlinked.
     * @example
     *     <p>
     *     <pre>
     * long result = client.unlink("my_key").get();
     * assert result == 1L;
     * </pre>
     */
    CompletableFuture<Long> unlink(String[] keys);

    /**
     * Sets a timeout on <code>key</code> in seconds. After the timeout has expired, the <code>key
     * </code> will automatically be deleted. If <code>key</code> already has an existing <code>expire
     * </code> set, the time to live is updated to the new value. If <code>seconds</code> is
     * non-positive number, the <code>key</code> will be deleted rather than expired. The timeout will
     * only be cleared by commands that delete or overwrite the contents of <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/expire/">redis.io</a> for details.
     * @param key - The key to set timeout on it.
     * @param seconds - The timeout in seconds.
     * @return <code>true</code> if the timeout was set. <code>false</code> if the timeout was not
     *     set. e.g. key doesn't exist, or operation skipped due to the provided arguments.
     */
    CompletableFuture<Boolean> expire(String key, long seconds);

    /**
     * Sets a timeout on <code>key</code> in seconds. After the timeout has expired, the <code>key
     * </code> will automatically be deleted. If <code>key</code> already has an existing <code>expire
     * </code> set, the time to live is updated to the new value. If <code>seconds</code> is
     * non-positive number, the <code>key</code> will be deleted rather than expired. The timeout will
     * only be cleared by commands that delete or overwrite the contents of <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/expire/">redis.io</a> for details.
     * @param key - The key to set timeout on it.
     * @param seconds - The timeout in seconds.
     * @param expireOptions - The expire options.
     * @return <code>true</code> if the timeout was set. <code>false</code> if the timeout was not
     *     set. e.g. <code>key</code> doesn't exist, or operation skipped due to the provided
     *     arguments.
     */
    CompletableFuture<Boolean> expire(String key, long seconds, ExpireOptions expireOptions);

    /**
     * Sets a timeout on <code>key</code>. It takes an absolute Unix timestamp (seconds since January
     * 1, 1970) instead of specifying the number of seconds. A timestamp in the past will delete the
     * <code>key</code> immediately. After the timeout has expired, the <code>key</code> will
     * automatically be deleted. If <code>key</code> already has an existing <code>expire</code> set,
     * the time to live is updated to the new value. The timeout will only be cleared by commands that
     * delete or overwrite the contents of <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/expireat/">redis.io</a> for details.
     * @param key - The key to set timeout on it.
     * @param unixSeconds - The timeout in an absolute Unix timestamp.
     * @return <code>true</code> if the timeout was set. <code>false</code> if the timeout was not
     *     set. e.g. <code>key</code> doesn't exist, or operation skipped due to the provided
     *     arguments.
     */
    CompletableFuture<Boolean> expireAt(String key, long unixSeconds);

    /**
     * Sets a timeout on <code>key</code>. It takes an absolute Unix timestamp (seconds since January
     * 1, 1970) instead of specifying the number of seconds. A timestamp in the past will delete the
     * <code>key</code> immediately. After the timeout has expired, the <code>key</code> will
     * automatically be deleted. If <code>key</code> already has an existing <code>expire</code> set,
     * the time to live is updated to the new value. The timeout will only be cleared by commands that
     * delete or overwrite the contents of <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/expireat/">redis.io</a> for details.
     * @param key - The key to set timeout on it.
     * @param unixSeconds - The timeout in an absolute Unix timestamp.
     * @param expireOptions - The expire options.
     * @return <code>true</code> if the timeout was set. <code>false</code> if the timeout was not
     *     set. e.g. <code>key</code> doesn't exist, or operation skipped due to the provided
     *     arguments.
     */
    CompletableFuture<Boolean> expireAt(String key, long unixSeconds, ExpireOptions expireOptions);

    /**
     * Sets a timeout on <code>key</code> in milliseconds. After the timeout has expired, the <code>
     * key</code> will automatically be deleted. If <code>key</code> already has an existing <code>
     * expire</code> set, the time to live is updated to the new value. If <code>milliseconds</code>
     * is non-positive number, the <code>key</code> will be deleted rather than expired. The timeout
     * will only be cleared by commands that delete or overwrite the contents of <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/pexpire/">redis.io</a> for details.
     * @param key - The key to set timeout on it.
     * @param milliseconds - The timeout in milliseconds.
     * @return <code>true</code> if the timeout was set. <code>false</code> if the timeout was not
     *     set. e.g. <code>key</code> doesn't exist, or operation skipped due to the provided
     *     arguments.
     */
    CompletableFuture<Boolean> pexpire(String key, long milliseconds);

    /**
     * Sets a timeout on <code>key</code> in milliseconds. After the timeout has expired, the <code>
     * key</code> will automatically be deleted. If <code>key</code> already has an existing expire
     * set, the time to live is updated to the new value. If <code>milliseconds</code> is non-positive
     * number, the <code>key</code> will be deleted rather than expired. The timeout will only be
     * cleared by commands that delete or overwrite the contents of <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/pexpire/">redis.io</a> for details.
     * @param key - The key to set timeout on it.
     * @param milliseconds - The timeout in milliseconds.
     * @param expireOptions - The expire options.
     * @return <code>true</code> if the timeout was set. <code>false</code> if the timeout was not
     *     set. e.g. <code>key</code> doesn't exist, or operation skipped due to the provided
     *     arguments.
     */
    CompletableFuture<Boolean> pexpire(String key, long milliseconds, ExpireOptions expireOptions);

    /**
     * Sets a timeout on <code>key</code>. It takes an absolute Unix timestamp (milliseconds since
     * January 1, 1970) instead of specifying the number of milliseconds. A timestamp in the past will
     * delete the <code>key</code> immediately. After the timeout has expired, the <code>key</code>
     * will automatically be deleted. If <code>key</code> already has an existing <code>expire</code>
     * set, the time to live is updated to the new value. The timeout will only be cleared by commands
     * that delete or overwrite the contents of <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/pexpireat/">redis.io</a> for details.
     * @param key - The <code>key</code> to set timeout on it.
     * @param unixMilliseconds - The timeout in an absolute Unix timestamp.
     * @return <code>true</code> if the timeout was set. <code>false</code> if the timeout was not
     *     set. e.g. <code>key</code> doesn't exist, or operation skipped due to the provided
     *     arguments.
     */
    CompletableFuture<Boolean> pexpireAt(String key, long unixMilliseconds);

    /**
     * Sets a timeout on <code>key</code>. It takes an absolute Unix timestamp (milliseconds since
     * January 1, 1970) instead of specifying the number of milliseconds. A timestamp in the past will
     * delete the <code>key</code> immediately. After the timeout has expired, the <code>key</code>
     * will automatically be deleted. If <code>key</code> already has an existing <code>expire</code>
     * set, the time to live is updated to the new value. The timeout will only be cleared by commands
     * that delete or overwrite the contents of <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/pexpireat/">redis.io</a> for details.
     * @param key - The <code>key</code> to set timeout on it.
     * @param unixMilliseconds - The timeout in an absolute Unix timestamp.
     * @param expireOptions - The expire option.
     * @return <code>true</code> if the timeout was set. <code>false</code> if the timeout was not
     *     set. e.g. <code>key</code> doesn't exist, or operation skipped due to the provided
     *     arguments.
     */
    CompletableFuture<Boolean> pexpireAt(
        String key, long unixMilliseconds, ExpireOptions expireOptions);

    /**
     * Returns the remaining time to live of <code>key</code> that has a timeout.
     *
     * @see <a href="https://redis.io/commands/ttl/">redis.io</a> for details.
     * @param key - The <code>key</code> to return its timeout.
     * @returns TTL in seconds, -2 if <code>key</code> does not exist or -1 if <code>key</code> exists
     *     but has no associated expire.
     */
    CompletableFuture<Long> ttl(String key);
}
