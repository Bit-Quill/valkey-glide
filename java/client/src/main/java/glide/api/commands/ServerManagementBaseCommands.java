/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import java.util.concurrent.CompletableFuture;

/**
 * Server Management Commands interface for both standalone and cluster clients.
 *
 * @see <a href="https://redis.io/commands/?group=server">Server Management Commands</a>
 */
public interface ServerManagementBaseCommands {

    /**
     * Rewrite the configuration file with the current configuration.
     *
     * @see <a href="https://redis.io/commands/config-rewrite/">redis.io</a> for details.
     * @return <code>OK</code> when the configuration was rewritten properly, otherwise an error is
     *     raised.
     */
    CompletableFuture<String> configRewrite();

    /**
     * Reset the statistics reported by Redis using the <code>INFO</code> and <code>LATENCY HISTOGRAM
     * </code> commands.
     *
     * @see <a href="https://redis.io/commands/config-resetstat/">redis.io</a> for details.
     * @return <code>OK</code> to confirm that the statistics were successfully reset.
     */
    CompletableFuture<String> configResetStat();
}
