package glide.api.commands;

import java.util.concurrent.CompletableFuture;

/**
 * Connection Management Commands interface.
 *
 * @see: <a href="https://redis.io/commands/?group=connection">Server Management Commands</a>
 */
public interface ConnectionCommands {

    CompletableFuture<String> ping();

    CompletableFuture<String> ping(String msg);
}
