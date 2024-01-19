package glide.api;

import static glide.ffi.resolvers.SocketListenerResolver.getSocket;

import glide.api.commands.BaseCommands;
import glide.api.models.configuration.RedisClientConfiguration;
import glide.connectors.handlers.CallbackDispatcher;
import glide.connectors.handlers.ChannelHandler;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import glide.managers.models.Command;
import java.util.concurrent.CompletableFuture;

/**
 * Async (non-blocking) client for Redis in Standalone mode. Use {@link
 * #CreateClient(RedisClientConfiguration)} to request a client to Redis.
 */
public class RedisClient extends BaseClient implements BaseCommands {

    /**
     * Request an async (non-blocking) Redis client in Standalone mode.
     *
     * @param config - Redis Client Configuration
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

    protected RedisClient(ConnectionManager connectionManager, CommandManager commandManager) {
        super(connectionManager, commandManager);
    }

    /**
     * Executes a single command, without checking inputs. Every part of the command, including
     * subcommands, should be added as a separate value in args.
     *
     * @remarks This function should only be used for single-response commands. Commands that don't
     *     return response (such as SUBSCRIBE), or that return potentially more than a single response
     *     (such as XREAD), or that change the client's behavior (such as entering pub/sub mode on
     *     RESP2 connections) shouldn't be called using this function.
     * @example Returns a list of all pub/sub clients:
     *     <pre>
     * Object result = client.customCommand(new String[]{"CLIENT","LIST","TYPE", "PUBSUB"}).get();
     * </pre>
     *
     * @param args arguments for the custom command
     * @return a CompletableFuture with response result from Redis
     */
    public CompletableFuture<Object> customCommand(String[] args) {
        Command command =
                Command.builder().requestType(Command.RequestType.CUSTOM_COMMAND).arguments(args).build();
        return commandManager.submitNewCommand(command, BaseClient::handleObjectResponse);
    }

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @returns the String "PONG"
     */
    @Override
    public CompletableFuture<String> ping() {
        Command command = Command.builder().requestType(Command.RequestType.PING).build();
        return commandManager.submitNewCommand(command, ResponseHandlers::handleStringResponse);
    }

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @param msg - the ping argument that will be returned.
     * @returns return a copy of the argument.
     */
    @Override
    public CompletableFuture<String> ping(String msg) {
        Command command =
                Command.builder()
                        .requestType(Command.RequestType.PING)
                        .arguments(new String[] {msg})
                        .build();
        return commandManager.submitNewCommand(command, ResponseHandlers::handleStringResponse);
    }

    /**
     * Get information and statistics about the Redis server. DEFAULT option is assumed
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @return CompletableFuture with the response
     */
    @Override
    public CompletableFuture<Map> info() {
        Command command = Command.builder().requestType(Command.RequestType.INFO).build();
        return commandManager.submitNewCommand(command, ResponseHandlers::handleMapResponse);
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
        Command command =
                Command.builder()
                        .requestType(Command.RequestType.INFO)
                        .arguments(options.toInfoOptions())
                        .build();
        return commandManager.submitNewCommand(command, ResponseHandlers::handleMapResponse);
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
        Command command =
                Command.builder()
                        .requestType(Command.RequestType.GET_STRING)
                        .arguments(new String[] {key})
                        .build();
        return commandManager.submitNewCommand(command, ResponseHandlers::handleStringResponse);
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
        Command command =
                Command.builder()
                        .requestType(Command.RequestType.SET_STRING)
                        .arguments(new String[] {key, value})
                        .build();
        return commandManager.submitNewCommand(command, ResponseHandlers::handleVoidResponse);
    }

    /**
     * Set the given key with the given value. Return value is dependent on the passed options.
     *
     * @see <a href="https://redis.io/commands/set/">redis.io</a> for details.
     * @param key - The key to store.
     * @param value - The value to store with the given key.
     * @param options - The Set options
     * @return string or null If value isn't set because of `onlyIfExists` or `onlyIfDoesNotExist`
     *     conditions, return null. If `returnOldValue` is set, return the old value as a string.
     */
    @Override
    public CompletableFuture<String> set(String key, String value, SetOptions options) {
        Command command =
                Command.builder()
                        .requestType(Command.RequestType.SET_STRING)
                        .arguments(options.toSetOptions(List.of(key, value)))
                        .build();
        return commandManager.submitNewCommand(command, ResponseHandlers::handleStringResponse);
    }
}
