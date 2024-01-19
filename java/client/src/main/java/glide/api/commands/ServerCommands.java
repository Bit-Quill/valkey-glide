package glide.api.commands;

import glide.api.models.commands.InfoOptions;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Server Management Commands interface.
 *
 * @see: <a href="https://redis.io/commands/?group=server">Server Management Commands</a>
 */
public interface ServerCommands {

    CompletableFuture<Map> info();

    CompletableFuture<Map> info(InfoOptions options);
}
