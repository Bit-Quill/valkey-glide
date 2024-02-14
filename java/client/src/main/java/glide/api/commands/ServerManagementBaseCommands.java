/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ServerManagementBaseCommands {

    /**
     * Read the configuration parameters of a running Redis server.
     *
     * @param parameters An <code>array</code> of configuration parameter names to retrieve values
     *     for.
     * @return A <code>map</code> of values corresponding to the configuration parameters.
     * @see <a href="https://redis.io/commands/config-get/">redis.io</a> for details.
     */
    CompletableFuture<Map<String, String>> configGet(String[] parameters);

    /**
     * Set configuration parameters to the specified values.
     *
     * @see <a href="https://redis.io/commands/config-set/">redis.io</a> for details.
     * @param parameters A <code>map</code> consisting of configuration parameters and their
     *     respective values to set.
     * @return <code>OK</code> if all configurations have been successfully set. Otherwise, raises an
     *     error.
     */
    CompletableFuture<String> configSet(Map<String, String> parameters);
}
