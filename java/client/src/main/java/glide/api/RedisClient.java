package glide.api;

import static glide.ffi.resolvers.SocketListenerResolver.getSocket;

import glide.api.commands.ConnectionCommands;
import glide.api.commands.GenericCommands;
import glide.api.commands.ServerCommands;
import glide.api.commands.StringCommands;
import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.SetOptions;
import glide.api.models.configuration.RedisClientConfiguration;
import glide.connectors.handlers.CallbackDispatcher;
import glide.connectors.handlers.ChannelHandler;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import glide.managers.models.Command;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Async (non-blocking) client for Redis in Standalone mode. Use {@link #CreateClient} to request a
 * client to Redis.
 */
public class RedisClient extends BaseClient
        implements ConnectionCommands, GenericCommands, ServerCommands, StringCommands {

    protected RedisClient(ConnectionManager connectionManager, CommandManager commandManager) {
        super(connectionManager, commandManager);
    }

    protected RedisClient(ConnectionManager connectionManager, CommandManager commandManager) {
        super(connectionManager, commandManager);
    }

    /**
     * Async request for an async (non-blocking) Redis client in Standalone mode.
     *
     * @param config Redis client Configuration
     * @return a Future to connect and return a RedisClient
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

    protected static ChannelHandler buildChannelHandler() throws InterruptedException {
        CallbackDispatcher callbackDispatcher = new CallbackDispatcher();
        return new ChannelHandler(callbackDispatcher, getSocket());
    }

    protected static ConnectionManager buildConnectionManager(ChannelHandler channelHandler) {
        return new ConnectionManager(channelHandler);
    }

    protected static CommandManager buildCommandManager(ChannelHandler channelHandler) {
        return new CommandManager(channelHandler);
    }

    @Override
    public CompletableFuture<Object> customCommand(String[] args) {
        Command command =
                Command.builder().requestType(Command.RequestType.CUSTOM_COMMAND).arguments(args).build();
        return commandManager.submitNewCommand(command, Optional.empty(), this::handleObjectResponse);
    }

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @return the String "PONG"
     */
    @Override
    public CompletableFuture<String> ping() {
        return commandManager.submitNewCommand(Command.ping(), BaseClient::handleStringResponse);
    }

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @param msg - the ping argument that will be returned.
     * @return return a copy of the argument.
     */
    @Override
    public CompletableFuture<String> ping(String msg) {
        return commandManager.submitNewCommand(Command.ping(msg), BaseClient::handleStringResponse);
    }

    /**
     * Get information and statistics about the Redis server. DEFAULT option is assumed
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @return CompletableFuture with the response
     */
    @Override
    public CompletableFuture<Map> info() {
        return commandManager.submitNewCommand(Command.info(), BaseClient::handleMapResponse);
    }

    /**
     * Get information and statistics about the Redis server.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @param options - A list of InfoSection values specifying which sections of information to
     *     retrieve. When no parameter is provided, the default option is assumed.
     * @return CompletableFuture with the response
     */
    @Override
    public CompletableFuture<Map> info(InfoOptions options) {
        return commandManager.submitNewCommand(
            Command.info(options.toInfoOptions()), BaseClient::handleMapResponse);
    }

    /**
     * Get the value associated with the given key, or null if no such value exists.
     *
     * @see <a href="https://redis.io/commands/get/">redis.io</a> for details.
     * @param key - The key to retrieve from the database.
     * @return If `key` exists, returns the value of `key` as a string. Otherwise, return null
     */
    @Override
    public CompletableFuture<String> get(String key) {
        return commandManager.submitNewCommand(Command.get(key), BaseClient::handleStringResponse);
    }

    /**
     * Set the given key with the given value.
     *
     * @see <a href="https://redis.io/commands/set/">redis.io</a> for details.
     * @param key - The key to store.
     * @param value - The value to store with the given key.
     * @return null
     */
    @Override
    public CompletableFuture<Void> set(String key, String value) {
        return commandManager.submitNewCommand(Command.set(key, value), BaseClient::handleVoidResponse);
    }
}
