/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import java.util.concurrent.CompletableFuture;

/**
 * Supports commands and transactions for the "HyperLogLog Commands" group for standalone clients
 * and cluster clients.
 *
 * @see <a href="https://redis.io/commands/?group=hyperloglog">HyperLogLog Commands</a>
 */
public interface HyperLogLogBaseCommands {

    /**
     * Adds all the elements to the HyperLogLog data structure stored and create a new structure if it
     * is missing.
     *
     * <p>As a side effect of this command the HyperLogLog internals may be updated to reflect a
     * different estimation of the number of unique items added so far (the cardinality of the set).
     *
     * <p>If the approximated cardinality estimated by the HyperLogLog changed after executing the
     * command, <code>PFADD</code> returns <code>1</code>, otherwise <code>0</code> is returned. The
     * command automatically creates an empty HyperLogLog structure if the specified key does not
     * exist.
     *
     * <p>A command call without elements, this will result into no operation performed if the
     * variable already exists, or just the creation of the data structure if the key does not exist
     * (in the latter case <code>1</code> is returned).
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
     * result = client.pfadd("hll_1", new String[] { "b", "c" }).get();
     * assert result == 0L; // No data structure was modified
     *
     * result = client.pfadd("hll_1", new String[0]).get();
     * assert result == 0L; // No data structure was modified
     *
     * result = client.pfadd("hll_2", new String[0]).get();
     * assert result == 1L; // A new empty data structure was created
     * }</pre>
     */
    CompletableFuture<Long> pfadd(String key, String[] elements);

    /**
     * Estimates the cardinality of the data stored in a HyperLogLog structure for a single key or
     * calculates the combined cardinality of multiple keys by merging their HyperLogLogs temporarily.
     *
     * @see <a href="https://redis.io/commands/pfcount/">redis.io</a> for details.
     * @param keys The keys of the HyperLogLog data structures to be analyzed.
     * @return The approximated cardinality of given HyperLogLog data structures.<br>
     *     The cardinality of a key that does not exist is <code>0</code>.
     * @example
     *     <pre>{@code
     * Long result = client.pfcount("hll_1", "hll_2").get();
     * assert result == 42L; // Count of unique elements in multiple data structures
     * }</pre>
     */
    CompletableFuture<Long> pfcount(String[] keys);
}
