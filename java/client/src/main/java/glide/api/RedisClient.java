/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api;

import static glide.managers.RequestType.CUSTOM_COMMAND;
import static glide.managers.RequestType.INFO;

import glide.api.commands.BaseCommands;
import glide.api.commands.ConnectionCommands;
import glide.api.commands.ServerCommands;
import glide.api.models.Transaction;
import glide.api.models.commands.InfoOptions;
import glide.api.models.configuration.RedisClientConfiguration;
import glide.connectors.handlers.ChannelHandler;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Async (non-blocking) client for Redis in Standalone mode. Use {@link #CreateClient} to request a
 * client to Redis.
 */
public class RedisClient extends BaseClient
        implements BaseCommands, ConnectionCommands, ServerCommands {

    protected RedisClient(ConnectionManager connectionManager, CommandManager commandManager) {
        super(connectionManager, commandManager);
    }

    /**
     * Async request for an async (non-blocking) Redis client in Standalone mode.
     *
     * @param config Redis client Configuration
     * @return A Future to connect and return a RedisClient
     */
    public static CompletableFuture<RedisClient> CreateClient(RedisClientConfiguration config) {
        try {
            ChannelHandler channelHandler = buildChannelHandler();
            ConnectionManager connectionManager = buildConnectionManager(channelHandler);
            CommandManager commandManager = buildCommandManager(channelHandler);
            // TODO: Support exception throwing, including interrupted exceptions
            return connectionManager
                    .connectToRedis(config)
                    .thenApply(ignore -> new RedisClient(connectionManager, commandManager));
        } catch (InterruptedException e) {
            // Something bad happened while we were establishing netty connection to UDS
            var future = new CompletableFuture<RedisClient>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public CompletableFuture<Object> customCommand(String[] args) {
        return commandManager.submitNewCommand(CUSTOM_COMMAND, args, this::handleStringResponse);
    }

    @Override
    public CompletableFuture<Object[]> exec(Transaction transaction) {
        return commandManager.submitNewCommand(
                transaction, Optional.empty(), this::handleArrayResponse);
    }

    @Override
    public CompletableFuture<String> info() {
        return commandManager.submitNewCommand(INFO, new String[0], this::handleStringResponse);
    }

    @Override
    public CompletableFuture<String> info(InfoOptions options) {
        return commandManager.submitNewCommand(INFO, options.toArgs(), this::handleStringResponse);
    }
}
