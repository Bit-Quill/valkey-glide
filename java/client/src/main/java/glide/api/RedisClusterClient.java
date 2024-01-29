package glide.api;

import static glide.api.RedisClient.buildChannelHandler;
import static glide.api.RedisClient.buildCommandManager;
import static glide.api.RedisClient.buildConnectionManager;

import glide.api.commands.ClusterBaseCommands;
import glide.api.commands.ClusterServerCommands;
import glide.api.commands.Transaction;
import glide.api.models.ClusterValue;
import glide.api.models.commands.InfoOptions;
import glide.api.models.configuration.RedisClusterClientConfiguration;
import glide.api.models.configuration.Route;
import glide.connectors.handlers.ChannelHandler;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import glide.managers.models.Command;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Async (non-blocking) client for Redis in Cluster mode. Use {@link #CreateClient} to request a
 * client to Redis.
 */
public class RedisClusterClient extends BaseClient
        implements ClusterBaseCommands, ClusterServerCommands {

    protected RedisClusterClient(ConnectionManager connectionManager, CommandManager commandManager) {
        super(connectionManager, commandManager);
    }

    /**
     * Async request for an async (non-blocking) Redis client in Cluster mode.
     *
     * @param config Redis cluster client Configuration
     * @return a Future to connect and return a RedisClusterClient
     */
    public static CompletableFuture<RedisClusterClient> CreateClient(
            RedisClusterClientConfiguration config) {
        try {
            ChannelHandler channelHandler = buildChannelHandler();
            ConnectionManager connectionManager = buildConnectionManager(channelHandler);
            CommandManager commandManager = buildCommandManager(channelHandler);
            // TODO: Support exception throwing, including interrupted exceptions
            return connectionManager
                    .connectToRedis(config)
                    .thenApply(ignored -> new RedisClusterClient(connectionManager, commandManager));
        } catch (InterruptedException e) {
            // Something bad happened while we were establishing netty connection to UDS
            var future = new CompletableFuture<RedisClusterClient>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public CompletableFuture<ClusterValue<Object>> customCommand(String[] args) {
        return commandManager.submitNewCommand(
                Command.customCommand(args),
                Optional.empty(),
                response -> ClusterValue.of(handleObjectResponse(response)));
    }

    @Override
    public CompletableFuture<ClusterValue<Object>> customCommand(String[] args, Route route) {
        return commandManager.submitNewCommand(
                Command.customCommand(args),
                Optional.of(route),
                response -> ClusterValue.of(handleObjectResponse(response)));
    }

    @Override
    public CompletableFuture<ClusterValue<Object[]>> exec(Transaction transaction) {
        return commandManager.submitNewTransaction(
                transaction, Optional.empty(), response -> ClusterValue.of(handleArrayResponse(response)));
    }

    @Override
    public CompletableFuture<ClusterValue<Object[]>> exec(Transaction transaction, Route route) {
        switch (route.getRouteType()) {
            case ALL_NODES:
            case ALL_PRIMARIES:
            default:
                throw new RuntimeException("Transaction requests require a single-node route option");
            case RANDOM:
            case PRIMARY_SLOT_ID:
            case REPLICA_SLOT_ID:
            case PRIMARY_SLOT_KEY:
            case REPLICA_SLOT_KEY:
                // no error
                break;
        }
        return commandManager.submitNewTransaction(
                transaction,
                Optional.of(route),
                response -> ClusterValue.of(handleArrayResponse(response)));
    }

    @Override
    public CompletableFuture<ClusterValue<Map>> info() {
        return commandManager.submitNewCommand(
                Command.info(), Optional.empty(), response -> ClusterValue.of(handleMapResponse(response)));
    }

    @Override
    public CompletableFuture<ClusterValue<Map>> info(Route route) {
        return commandManager.submitNewCommand(
                Command.info(),
                Optional.of(route),
                response -> ClusterValue.of(handleMapResponse(response)));
    }

    @Override
    public CompletableFuture<ClusterValue<Map>> info(InfoOptions options) {
        return commandManager.submitNewCommand(
                Command.info(options.toInfoOptions()),
                Optional.empty(),
                response -> ClusterValue.of(handleMapResponse(response)));
    }

    @Override
    public CompletableFuture<ClusterValue<Map>> info(InfoOptions options, Route route) {
        return commandManager.submitNewCommand(
                Command.info(options.toInfoOptions()),
                Optional.of(route),
                response -> ClusterValue.of(handleMapResponse(response)));
    }
}
