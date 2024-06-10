/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import glide.api.models.ClusterTransaction;
import glide.api.models.ClusterValue;
import glide.api.models.Transaction;
import glide.api.models.configuration.RequestRoutingConfiguration.Route;
import glide.api.models.configuration.RequestRoutingConfiguration.SingleNodeRoute;
import java.util.concurrent.CompletableFuture;

/**
 * Supports commands for the "Generic Commands" group for a cluster client.
 *
 * @see <a href="https://redis.io/commands/?group=generic">Generic Commands</a>
 */
public interface GenericClusterCommands {

    /**
     * Executes a single command, without checking inputs. Every part of the command, including
     * subcommands, should be added as a separate value in <code>args</code>.
     *
     * <p>The command will be routed to all primaries.
     *
     * @apiNote See <a
     *     href="https://github.com/aws/glide-for-redis/wiki/General-Concepts#custom-command">Glide
     *     for Redis Wiki</a> for details on the restrictions and limitations of the custom command
     *     API.
     * @param args Arguments for the custom command including the command name.
     * @return Response from Redis containing an <code>Object</code>.
     * @example
     *     <pre>{@code
     * ClusterValue<Object> data = client.customCommand(new String[] {"ping"}).get();
     * assert ((String) data.getSingleValue()).equals("PONG");
     * }</pre>
     */
    CompletableFuture<ClusterValue<Object>> customCommand(String[] args);

    /**
     * Executes a single command, without checking inputs. Every part of the command, including
     * subcommands, should be added as a separate value in <code>args</code>.
     *
     * <p>Client will route the command to the nodes defined by <code>route</code>.
     *
     * @apiNote See <a
     *     href="https://github.com/aws/glide-for-redis/wiki/General-Concepts#custom-command">Glide
     *     for Redis Wiki</a> for details on the restrictions and limitations of the custom command
     *     API.
     * @param args Arguments for the custom command including the command name
     * @param route Specifies the routing configuration for the command. The client will route the
     *     command to the nodes defined by <code>route</code>.
     * @return Response from Redis containing an <code>Object</code>.
     * @example
     *     <pre>{@code
     * ClusterValue<Object> result = clusterClient.customCommand(new String[]{ "CONFIG", "GET", "maxmemory"}, ALL_NODES).get();
     * Map<String, Object> payload = result.getMultiValue();
     * assert ((String) payload.get("node1")).equals("1GB");
     * assert ((String) payload.get("node2")).equals("100MB");
     * }</pre>
     */
    CompletableFuture<ClusterValue<Object>> customCommand(String[] args, Route route);

    /**
     * Executes a transaction by processing the queued commands.
     *
     * <p>The transaction will be routed to the slot owner of the first key found in the transaction.
     * If no key is found, the command will be sent to a random node.
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
     * ClusterTransaction transaction = new ClusterTransaction().customCommand(new String[] {"info"});
     * Object[] result = clusterClient.exec(transaction).get();
     * assert ((String) result[0]).contains("# Stats");
     * }</pre>
     */
    CompletableFuture<Object[]> exec(ClusterTransaction transaction);

    /**
     * Executes a transaction by processing the queued commands.
     *
     * @see <a href="https://redis.io/topics/Transactions/">redis.io</a> for details on Redis
     *     Transactions.
     * @param transaction A {@link Transaction} object containing a list of commands to be executed.
     * @param route A single-node routing configuration for the transaction. The client will route the
     *     transaction to the node defined by <code>route</code>.
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
     * ClusterTransaction transaction = new ClusterTransaction().ping().info();
     * Object[] result = clusterClient.exec(transaction, RANDOM).get();
     * assert ((String) result[0]).equals("PONG");
     * assert ((String) result[1]).contains("# Stats");
     * }</pre>
     */
    CompletableFuture<Object[]> exec(ClusterTransaction transaction, SingleNodeRoute route);

    /**
     * Returns a random key.
     *
     * @see <a href="https://redis.io/docs/latest/commands/randomkey/">redis.io</a> for details.
     * @param route
     * @return A random <code>key</code> from the database.
     * @example
     *     <pre>{@code
     * String value = client.set("key", "value").get();
     * String value_1 = client.set("key1", "value_1").get();
     * String key = client.randomKey(RANDOM).get();
     * System.out.println("The random key is: " + key);
     * // The value of key is either "key" or "key1"
     * }</pre>
     */
    CompletableFuture<String> randomKey(SingleNodeRoute route);

    /**
     * Returns a random key.
     *
     * @see <a href="https://redis.io/docs/latest/commands/randomkey/">redis.io</a> for details.
     * @return A random <code>key</code> from the database.
     * @example
     *     <pre>{@code
     * String value = client.set("key", "value").get();
     * String value_1 = client.set("key1", "value_1").get();
     * String key = client.randomKey().get();
     * System.out.println("The random key is: " + key);
     * // The value of key is either "key" or "key1"
     * }</pre>
     */
    CompletableFuture<String> randomKey();
}
