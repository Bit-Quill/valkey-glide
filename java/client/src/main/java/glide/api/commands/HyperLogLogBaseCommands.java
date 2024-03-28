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
     * Gets the approximated cardinality (size) of the union of the HyperLogLogs passed, by internally
     * merging into a temporary HyperLogLog.
     *
     * @see <a href="https://redis.io/commands/pfcount/">redis.io</a> for details.
     * @param keys The data structure(s) to count cardinality.
     * @return The approximated cardinality of given HyperLogLogs data structures or <code>0</code> if
     *     the variable does not exist.
     * @example
     *     <pre>{@code
     * Long result = client.pfcount("hll_1", "hll_2").get();
     * assert result == 42L; // Count of unique elements in multiple data structures
     *
     * result = client.pfcount("empty_hll").get();
     * assert result == 0L;
     *
     * result = client.pfcount("not_existing_hll").get();
     * assert result == 0L;
     * }</pre>
     */
    CompletableFuture<Long> pfcount(String[] keys);

    /**
     * Merges multiple HyperLogLog values into a unique value.<br>
     * If the destination variable exists, it is treated as one of the source HyperLogLog data sets,
     * otherwise a new HyperLogLog is created.
     *
     * @see <a href="https://redis.io/commands/pfmerge/">redis.io</a> for details.
     * @param destKey Identifier of the destination HyperLogLog where the merged data sets will be
     *     stored.
     * @param sourceKeys The source HyperLogLog to merge.
     * @return <code>OK</code>
     * @example
     *     <pre>{@code
     * String response = client.pfmerge("new_HLL", "old_HLL_1", "old_HLL_2").get();
     * assert response.equals("OK"); // new HLL was created with merged content of old ones
     *
     * String response = client.pfmerge("old_HLL_1", "old_HLL_2", "old_HLL_3").get();
     * assert response.equals("OK"); // content of existing HLLs was merged into existing variable
     * }</pre>
     */
    CompletableFuture<String> pfmerge(String destKey, String[] sourceKeys);
}
