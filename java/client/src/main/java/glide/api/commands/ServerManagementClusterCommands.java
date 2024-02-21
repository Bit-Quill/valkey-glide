/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import glide.api.models.ClusterValue;
import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.InfoOptions.Section;
import glide.api.models.configuration.RequestRoutingConfiguration.Route;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Server Management Commands interface.
 *
 * @see <a href="https://redis.io/commands/?group=server">Server Management Commands</a>
 */
public interface ServerManagementClusterCommands {

    /**
     * Get information and statistics about the Redis server using the {@link Section#DEFAULT} option.
     * The command will be routed to all primary nodes.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @return Response from Redis cluster with a <code>Map{@literal <String, String>}</code> with
     *     each address as the key and its corresponding value is the information for the node.
     * @example
     *     <p><code>
     *     {@literal Map<String, String>} routedInfoResult = clusterClient.info().get().getMultiValue();
     *     </code>
     */
    CompletableFuture<ClusterValue<String>> info();

    /**
     * Get information and statistics about the Redis server. If no argument is provided, so the
     * {@link Section#DEFAULT} option is assumed.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @param route Routing configuration for the command. Client will route the command to the nodes
     *     defined.
     * @return Response from Redis cluster with a <code>String</code> with the requested Sections.
     *     When specifying a <code>route</code> other than a single node, it returns a <code>
     *     Map{@literal <String, String>}</code> with each address as the key and its corresponding
     *     value is the information for the node.
     */
    CompletableFuture<ClusterValue<String>> info(Route route);

    /**
     * Get information and statistics about the Redis server. The command will be routed to all
     * primary nodes.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @param options A list of {@link InfoOptions.Section} values specifying which sections of
     *     information to retrieve. When no parameter is provided, the {@link
     *     InfoOptions.Section#DEFAULT} option is assumed.
     * @return Response from Redis cluster with a <code>Map{@literal <String, String>}</code> with
     *     each address as the key and its corresponding value is the information of the sections
     *     requested for the node.
     */
    CompletableFuture<ClusterValue<String>> info(InfoOptions options);

    /**
     * Get information and statistics about the Redis server.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @param options A list of {@link InfoOptions.Section} values specifying which sections of
     *     information to retrieve. When no parameter is provided, the {@link
     *     InfoOptions.Section#DEFAULT} option is assumed.
     * @param route Routing configuration for the command. Client will route the command to the nodes
     *     defined.
     * @return Response from Redis cluster with a <code>String</code> with the requested sections.
     *     When specifying a <code>route</code> other than a single node, it returns a <code>
     *     Map{@literal <String, String>}</code> with each address as the key and its corresponding
     *     value is the information of the sections requested for the node.
     */
    CompletableFuture<ClusterValue<String>> info(InfoOptions options, Route route);

    /**
     * Read the configuration parameters of a running Redis server.<br>
     * The command will be sent to a random node.
     *
     * @see <a href="https://redis.io/commands/config-get/">redis.io</a> for details.
     * @param parameters An <code>array</code> of configuration parameter names to retrieve values
     *     for.
     * @return A <code>map</code> of values corresponding to the configuration parameters.
     * @example
     *     <pre>
     * Map&lt;String, String&gt; configParams = client.configGet("logfile", "*port").get();
     * var logFile = configParams.get("logfile");
     * var port = configParams.get("port");
     * var tlsPort = configParams.get("tls-port");
     * </pre>
     */
    CompletableFuture<Map<String, String>> configGet(String[] parameters);

    /**
     * Set configuration parameters to the specified values.<br>
     * The command will be sent to a random node.
     *
     * @see <a href="https://redis.io/commands/config-set/">redis.io</a> for details.
     * @param parameters A <code>map</code> consisting of configuration parameters and their
     *     respective values to set.
     * @return <code>OK</code> if all configurations have been successfully set. Otherwise, raises an
     *     error.
     * @example
     *     <pre>
     * String response = client.configSet(Map.of("syslog-enabled", "yes")).get();
     * assert response.equals("OK")
     * </pre>
     */
    CompletableFuture<String> configSet(Map<String, String> parameters);

    /**
     * Read the configuration parameters of a running Redis server.
     *
     * @see <a href="https://redis.io/commands/config-get/">redis.io</a> for details.
     * @param parameters An <code>array</code> of configuration parameter names to retrieve values
     *     for.
     * @param route Routing configuration for the command. Client will route the command to the nodes
     *     defined.
     * @return A <code>map</code> of values corresponding to the configuration parameters.<br>
     *     When specifying a route other than a single node, it returns a dictionary where each
     *     address is the key and its corresponding node response is the value.
     * @example
     *     <pre>
     * Map&lt;String, String&gt; configParams = client.configGet("logfile", new SlotIdRoute(...)).get().getSingleValue();
     * var logFile = configParams.get("logfile");
     * </pre>
     *
     * @example
     *     <pre>
     * Map&lt;String, Map&lt;String, String&gt;&gt; configParamsPerNode = client.configGet("logfile", ALL_NODES).get().getMultiValue();
     * var logFileNode1 = configParamsPerNode.get("&lt;node1 address&gt;").get("logfile");
     * var logFileNode2 = configParamsPerNode.get("&lt;node2 address&gt;").get("logfile");
     * </pre>
     */
    CompletableFuture<ClusterValue<Map<String, String>>> configGet(String[] parameters, Route route);

    /**
     * Set configuration parameters to the specified values.
     *
     * @see <a href="https://redis.io/commands/config-set/">redis.io</a> for details.
     * @param parameters A <code>map</code> consisting of configuration parameters and their
     *     respective values to set.
     * @param route Routing configuration for the command. Client will route the command to the nodes
     *     defined.
     * @return <code>OK</code> if all configurations have been successfully set. Otherwise, raises an
     *     error.
     * @example
     *     <pre>
     * String response = client.configSet(Map.of("syslog-enabled", "yes"), ALL_PRIMARIES).get();
     * assert response.equals("OK")
     * </pre>
     */
    CompletableFuture<String> configSet(Map<String, String> parameters, Route route);
}
