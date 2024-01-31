package glide.api.commands;

import java.util.concurrent.CompletableFuture;

/**
 * Connection Management Commands interface.
 *
 * @see: <a href="https://redis.io/commands/?group=connection">Server Management Commands</a>
 */
public interface ConnectionCommands {

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @return A <em>CompletableFuture</em> with the String <code>"PONG"</code>
     */
    CompletableFuture<String> ping();

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @param msg The ping argument that will be returned.
     * @return A <em>CompletableFuture</em> with a copy of the argument.
     */
    CompletableFuture<String> ping(String msg);
}
