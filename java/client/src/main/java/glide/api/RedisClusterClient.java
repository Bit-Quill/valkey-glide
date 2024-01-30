package glide.api;

import glide.api.commands.ClusterBaseCommands;
import glide.api.commands.ClusterServerCommands;
import glide.api.models.ClusterTransaction;
import glide.api.models.ClusterValue;
import glide.api.models.commands.InfoOptions;
import glide.api.models.configuration.RedisClusterClientConfiguration;
import glide.api.models.configuration.RequestRoutingConfiguration.Route;
import glide.connectors.handlers.ChannelHandler;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
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
     * @return A Future to connect and return a RedisClusterClient
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
        // TODO if a command returns a map as a single value, ClusterValue misleads user
        return commandManager.submitNewCommand(
                prepareRedisRequest(RequestType.CUSTOM_COMMAND, args, Optional.empty()),
                response -> ClusterValue.of(handleObjectResponse(response)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<ClusterValue<Object>> customCommand(String[] args, Route route) {
        return commandManager.submitNewCommand(
                prepareRedisRequest(RequestType.CUSTOM_COMMAND, args, Optional.ofNullable(route)),
                response ->
                        route.isSingleNodeRoute()
                                ? ClusterValue.ofSingleValue(handleObjectResponse(response))
                                : ClusterValue.ofMultiValue((Map<String, Object>) handleObjectResponse(response)));
    }

    @Override
    public CompletableFuture<Object[]> exec(ClusterTransaction transaction) {
        return commandManager.submitNewCommand(
                prepareRedisRequest(transaction, Optional.empty()), this::handleArrayResponse);
    }

    @Override
    public CompletableFuture<Object[]> exec(ClusterTransaction transaction, Route route) {
        return commandManager.submitNewCommand(
                prepareRedisRequest(transaction, Optional.ofNullable(route)), this::handleArrayResponse);
    }

    @Override
    public CompletableFuture<ClusterValue<Map>> info() {
        return commandManager.submitNewCommand(
                prepareRedisRequest(RequestType.INFO, new String[0], Optional.empty()),
                response -> ClusterValue.of(handleMapResponse(response)));
    }

    @Override
    public CompletableFuture<ClusterValue<Map>> info(Route route) {
        return commandManager.submitNewCommand(
                prepareRedisRequest(RequestType.INFO, new String[0], Optional.ofNullable(route)),
                response -> ClusterValue.of(handleMapResponse(response)));
    }

    @Override
    public CompletableFuture<ClusterValue<Map>> info(InfoOptions options) {
        return commandManager.submitNewCommand(
                prepareRedisRequest(RequestType.INFO, options.toArgs(), Optional.empty()),
                response -> ClusterValue.of(handleMapResponse(response)));
    }

    @Override
    public CompletableFuture<ClusterValue<Map>> info(InfoOptions options, Route route) {
        return commandManager.submitNewCommand(
                prepareRedisRequest(RequestType.INFO, options.toArgs(), Optional.ofNullable(route)),
                response -> ClusterValue.of(handleMapResponse(response)));
    }
}
