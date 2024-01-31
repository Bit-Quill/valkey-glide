package glide.api.commands;

import glide.api.models.commands.InfoOptions;
import java.util.concurrent.CompletableFuture;

/**
 * Server Management Commands interface.
 *
 * @see: <a href="https://redis.io/commands/?group=server">Server Management Commands</a>
 */
public interface ServerCommands {

    /**
     * Get information and statistics about the Redis server. No argument is provided, so the <code>DEFAULT</code> option is assumed.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @return A <em>CompletableFuture</em> with String response from Redis
     */
    CompletableFuture<String> info();

    /**
     * Get information and statistics about the Redis server.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @param options A list of InfoSection values specifying which sections of information to
     *     retrieve. When no parameter is provided, the <code>DEFAULT</code> option is assumed.
     * @return A <em>CompletableFuture</em> with String response from Redis
     */
    CompletableFuture<String> info(InfoOptions options);
}
