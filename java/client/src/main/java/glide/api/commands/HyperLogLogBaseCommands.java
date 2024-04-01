/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import java.util.concurrent.CompletableFuture;

/**
 * Supports commands and transactions for the "HyperLogLog Commands" group for standalone and
 * cluster clients.
 *
 * @see <a href="https://redis.io/commands/?group=hyperloglog">HyperLogLog Commands</a>
 */
public interface HyperLogLogBaseCommands {

    /**
     * Adds all elements to the HyperLogLog data structure stored at the specified <code>key</code>,
     * creating a new structure if the <code>key</code> does not exist.
     *
     * <p>A command call without <code>elements</code> results in no operation being performed if the
     * <code>key</code> already exists, or just the creation of the data structure if the <code>key
     * </code> does not exist (in the latter case <code>1</code> is returned).
     *
     * @see <a href="https://redis.io/commands/pfadd/">redis.io</a> for details.
     * @param key The data structure to add elements into.
     * @param elements The elements to add.
     * @return <code>1</code> if a HyperLogLog internal register was altered or <code>0</code>
     *     otherwise.
     * @example
     *     <pre>{@code
     * Long result = client.pfadd("hll_1", new String[] { "a", "b", "c" }).get();
     * assert result == 1L; // A data structure was created or modified
     *
     * result = client.pfadd("hll_2", new String[0]).get();
     * assert result == 1L; // A new empty data structure was created
     * }</pre>
     */
    CompletableFuture<Long> pfadd(String key, String[] elements);
}
