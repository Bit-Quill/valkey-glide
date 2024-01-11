package glide.api;

import static glide.api.RedisClient.buildChannelHandler;
import static glide.api.RedisClient.buildCommandManager;
import static glide.api.RedisClient.buildConnectionManager;

import glide.api.commands.ClusterBaseCommands;
import glide.api.commands.Command;
import glide.api.models.ClusterValue;
import glide.api.models.configuration.RedisClusterClientConfiguration;
import glide.api.models.configuration.Route;
import glide.connectors.handlers.ChannelHandler;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.concurrent.CompletableFuture;

/**
 * Async (non-blocking) client for Redis in Cluster mode. Use {@link #CreateClient} to request a
 * client to Redis.
 */
public class ClusterClient extends BaseClient implements ClusterBaseCommands<ClusterValue<Object>> {

    protected ClusterClient(ConnectionManager connectionManager, CommandManager commandManager) {
        super(connectionManager, commandManager);
    }

    /**
     * Async request for an async (non-blocking) Redis client in Cluster mode.
     *
     * @param config Redis cluster client Configuration
     * @return a Future to connect and return a ClusterClient
     */
    public static CompletableFuture<ClusterClient> CreateClient(
            RedisClusterClientConfiguration config) {
        ChannelHandler channelHandler = buildChannelHandler();
        ConnectionManager connectionManager = buildConnectionManager(channelHandler);
        CommandManager commandManager = buildCommandManager(channelHandler);
        // TODO: Support exception throwing, including interrupted exceptions
        return connectionManager
                .connectToRedis(config)
                .thenApply(ignored -> new ClusterClient(connectionManager, commandManager));
    }

    @Override
    public CompletableFuture<ClusterValue<Object>> customCommand(String[] args) {
        Command command =
                Command.builder().requestType(Command.RequestType.CUSTOM_COMMAND).arguments(args).build();
        return commandManager.submitNewCommand(
                command, response -> ClusterValue.of(handleObjectResponse(response)));
    }

    @Override
    public CompletableFuture<ClusterValue<Object>> customCommand(String[] args, Route route) {
        Command command =
                Command.builder().requestType(Command.RequestType.CUSTOM_COMMAND).arguments(args).build();
        return commandManager.submitNewCommand(
                command, route, response -> ClusterValue.of(handleObjectResponse(response)));
    }
}
