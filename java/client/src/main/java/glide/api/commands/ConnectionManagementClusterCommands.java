/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import glide.api.models.ClusterValue;
import glide.api.models.configuration.RequestRoutingConfiguration.Route;
import java.util.concurrent.CompletableFuture;

/**
 * Connection Management Commands interface for cluster client.
 *
 * @see <a href="https://redis.io/commands/?group=connection">Connection Management Commands</a>
 */
public interface ConnectionManagementClusterCommands extends ConnectionManagementBaseCommands {

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @param route Routing configuration for the command. Client will route the command to the nodes
     *     defined.
     * @return Response from Redis containing a <code>String</code> with <code>PONG</code>.
     */
    CompletableFuture<String> ping(Route route);

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @param str The ping argument that will be returned.
     * @param route Routing configuration for the command. Client will route the command to the nodes
     *     defined.
     * @return Response from Redis containing a <code>String</code> with a copy of the argument <code>
     *     str</code>.
     */
    CompletableFuture<String> ping(String str, Route route);

    /** {@inheritDoc} The command will be routed a random node. */
    CompletableFuture<Long> clientId();

    /** {@inheritDoc} The command will be routed a random node. */
    CompletableFuture<String> clientGetName();

    /**
     * Get the current connection id.
     *
     * @see <a href="https://redis.io/commands/client-id/">redis.io</a> for details.
     * @param route Routing configuration for the command. Client will route the command to the nodes
     *     defined.
     * @return A {@link ClusterValue} which holds a single value if single node route is used or a
     *     dictionary where each address is the key and its corresponding node response is the value.
     *     The value is the id of the client on that node.
     */
    CompletableFuture<ClusterValue<Long>> clientId(Route route);

    /**
     * Get the name of the current connection.
     *
     * @see <a href="https://redis.io/commands/client-getname/">redis.io</a> for details.
     * @param route Routing configuration for the command. Client will route the command to the nodes
     *     defined.
     * @return A {@link ClusterValue} which holds a single value if single node route is used or a
     *     dictionary where each address is the key and its corresponding node response is the value.
     *     The value is the name of the client connection as a string if a name is set, or null if no
     *     name is assigned.
     */
    CompletableFuture<ClusterValue<String>> clientGetName(Route route);
}
