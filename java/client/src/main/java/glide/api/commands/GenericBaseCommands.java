/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import java.util.concurrent.CompletableFuture;

/**
 * Generic Commands interface to handle generic commands for all server requests.
 *
 * @see <a href="https://redis.io/commands/?group=generic">Generic Commands</a>
 */
public interface GenericBaseCommands {

    /**
     * Returns the number of keys in <code>keys</code> that exist in the database.
     *
     * @see <a href="https://redis.io/commands/exists/">redis.io</a> for details.
     * @param keys The keys list to check.
     * @return the number of keys that exist. If the same existing key is mentioned in <code>keys
     *     </code> multiple times, it will be counted multiple times.
     */
    CompletableFuture<Long> exists(String[] keys);
}
