package glide.api.commands;

import glide.api.models.configuration.Route;
import java.util.concurrent.CompletableFuture;

/**
 * Base Commands interface to handle generic command and transaction requests with routing options.
 *
 * @param <T> The data return type.
 */
public interface ClusterBaseCommands<T> extends BaseCommands<T> {

    /**
     * Executes a single custom command, without checking inputs. Every part of the command, including
     * subcommands, should be added as a separate value in args.
     *
     * @param args command and arguments for the custom command call
     * @param route node routing configuration for the command
     * @return CompletableFuture with the response
     */
    CompletableFuture<T> customCommand(String[] args, Route route);
}
