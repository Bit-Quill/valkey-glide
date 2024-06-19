/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import glide.api.models.Transaction;
import glide.api.models.commands.SortStandaloneOptions;
import glide.api.models.configuration.ReadFrom;
import java.util.concurrent.CompletableFuture;

/**
 * Supports commands and transactions for the "Generic Commands" group for a standalone client.
 *
 * @see <a href="https://redis.io/commands/?group=generic">Generic Commands</a>
 */
public interface GenericCommands {

    /**
     * Executes a single command, without checking inputs. Every part of the command, including
     * subcommands, should be added as a separate value in <code>args</code>.
     *
     * @apiNote See <a
     *     href="https://github.com/aws/glide-for-redis/wiki/General-Concepts#custom-command">Glide
     *     for Redis Wiki</a> for details on the restrictions and limitations of the custom command
     *     API.
     * @param args Arguments for the custom command.
     * @return Response from Redis containing an <code>Object</code>.
     * @example
     *     <pre>{@code
     * Object response = (String) client.customCommand(new String[] {"ping", "GLIDE"}).get();
     * assert ((String) response).equals("GLIDE");
     * // Get a list of all pub/sub clients:
     * Object result = client.customCommand(new String[]{ "CLIENT", "LIST", "TYPE", "PUBSUB" }).get();
     * }</pre>
     */
    CompletableFuture<Object> customCommand(String[] args);

    /**
     * Executes a transaction by processing the queued commands.
     *
     * @see <a href="https://redis.io/topics/Transactions/">redis.io</a> for details on Redis
     *     Transactions.
     * @param transaction A {@link Transaction} object containing a list of commands to be executed.
     * @return A list of results corresponding to the execution of each command in the transaction.
     * @remarks
     *     <ul>
     *       <li>If a command returns a value, it will be included in the list.
     *       <li>If a command doesn't return a value, the list entry will be empty.
     *       <li>If the transaction failed due to a <code>WATCH</code> command, <code>exec</code> will
     *           return <code>null</code>.
     *     </ul>
     *
     * @example
     *     <pre>{@code
     * Transaction transaction = new Transaction().customCommand(new String[] {"info"});
     * Object[] result = client.exec(transaction).get();
     * assert ((String) result[0]).contains("# Stats");
     * }</pre>
     */
    CompletableFuture<Object[]> exec(Transaction transaction);

    /**
     * Move <code>key</code> from the currently selected database to the database specified by <code>
     * dbIndex</code>.
     *
     * @see <a href="https://redis.io/commands/move/">redis.io</a> for more details.
     * @param key The key to move.
     * @param dbIndex The index of the database to move <code>key</code> to.
     * @return <code>true</code> if <code>key</code> was moved, or <code>false</code> if the <code>key
     *     </code> already exists in the destination database or does not exist in the source
     *     database.
     * @example
     *     <pre>{@code
     * Boolean moved = client.move("some_key", 1L).get();
     * assert moved;
     * }</pre>
     */
    CompletableFuture<Boolean> move(String key, long dbIndex);

    /**
     * Sorts the elements in the list, set, or sorted set at <code>key</code> and returns the result.
     * The <code>sort</code> command can be used to sort elements based on different criteria and
     * apply transformations on sorted elements.<br>
     * To store the result into a new key, see {@link #sortStore(String, String,
     * SortStandaloneOptions)}.
     *
     * @param key The key of the list, set, or sorted set to be sorted.
     * @param sortStandaloneOptions The {@link SortStandaloneOptions}.
     * @return A <code>Array</code> of sorted elements.
     * @example
     *     <pre>{@code
     * client.hset("user:1", Map.of("name", "Alice", "age", "30")).get();
     * client.hset("user:2", Map.of("name", "Bob", "age", "25")).get();
     * client.lpush("user_ids", new String[] {"2", "1"}).get();
     * String [] payload = client.sort(
     *      "user_ids",
     *      SortStandaloneOptions
     *          .builder()
     *              .byPattern("user:*->age")
     *                  .getPatterns(new String[] {"user:*->name"})
     *                      .build()).get();
     * assertArrayEquals(new String[] {"Bob", "Alice"}, payload); // Returns a list of the names sorted by age
     * }</pre>
     */
    CompletableFuture<String[]> sort(String key, SortStandaloneOptions sortStandaloneOptions);

    /**
     * Sorts the elements in the list, set, or sorted set at <code>key</code> and returns the result.
     * The <code>sortReadOnly</code> command can be used to sort elements based on different criteria
     * and apply transformations on sorted elements.<br>
     * This command is routed depending on the client's {@link ReadFrom} strategy.
     *
     * @since Redis 7.0 and above.
     * @param key The key of the list, set, or sorted set to be sorted.
     * @param sortStandaloneOptions The {@link SortStandaloneOptions}.
     * @return A <code>Array</code> of sorted elements.
     * @example
     *     <pre>{@code
     * client.hset("user:1", Map.of("name", "Alice", "age", "30")).get();
     * client.hset("user:2", Map.of("name", "Bob", "age", "25")).get();
     * client.lpush("user_ids", new String[] {"2", "1"}).get();
     * String [] payload = client.sortReadOnly(
     *      "user_ids",
     *      SortStandaloneOptions
     *          .builder()
     *              .byPattern("user:*->age")
     *                  .getPatterns(new String[] {"user:*->name"})
     *                      .build()).get();
     * assertArrayEquals(new String[] {"Bob", "Alice"}, payload); // Returns a list of the names sorted by age
     * }</pre>
     */
    CompletableFuture<String[]> sortReadOnly(String key, SortStandaloneOptions sortStandaloneOptions);

    /**
     * Sorts the elements in the list, set, or sorted set at <code>key</code> and stores the result in
     * <code>destination</code>. The <code>sort</code> command can be used to sort elements based on
     * different criteria, apply transformations on sorted elements, and store the result in a new
     * key.<br>
     * To get the sort result without storing it into a key, see {@link #sort(String,
     * SortStandaloneOptions)}.
     *
     * @param key The key of the list, set, or sorted set to be sorted.
     * @param sortStandaloneOptions The {@link SortStandaloneOptions}.
     * @param destination The key where the sorted result will be stored.
     * @return The number of elements in the sorted key stored at <code>destination</code>.
     * @example
     *     <pre>{@code
     * client.hset("user:1", Map.of("name", "Alice", "age", "30")).get();
     * client.hset("user:2", Map.of("name", "Bob", "age", "25")).get();
     * client.lpush("user_ids", new String[] {"2", "1"}).get();
     * Long payload = client
     *      .sortStore(
     *          "user_ids",
     *          "destination",
     *          SortStandaloneOptions.builder()
     *              .byPattern("user:*->age")
     *              .getPatterns(new String[] {"user:*->name"})
     *              .build())
     *          .get();
     * assertEquals(2, payload);
     * assertArrayEquals(
     *      new String[] {"Bob", "Alice"},
     *      client.lrange("destination", 0, -1).get()); // The list of the names sorted by age is stored in `destination`
     * }</pre>
     */
    CompletableFuture<Long> sortStore(
            String key, String destination, SortStandaloneOptions sortStandaloneOptions);
}
