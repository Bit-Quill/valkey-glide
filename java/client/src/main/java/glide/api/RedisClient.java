package glide.api;

import static glide.ffi.resolvers.SocketListenerResolver.getSocket;

import glide.api.commands.BaseCommands;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Async (non-blocking) client for Redis in Standalone mode. Use {@link
 * #CreateClient(RedisClientConfiguration)} to request a client to Redis.
 */
public class RedisClient extends BaseClient
        implements BaseCommands, GenericCommands, ConnectionCommands, ServerCommands, StringCommands {

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
        return commandManager.submitNewCommand(command, BaseClient::handleStringResponse);
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
        return commandManager.submitNewCommand(command, BaseClient::handleStringResponse);
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
        return commandManager.submitNewCommand(command, BaseClient::handleMapResponse);
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
        return commandManager.submitNewCommand(command, BaseClient::handleMapResponse);
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
        return commandManager.submitNewCommand(command, BaseClient::handleStringResponse);
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
        return commandManager.submitNewCommand(command, BaseClient::handleVoidResponse);
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
        return commandManager.submitNewCommand(command, BaseClient::handleStringResponse);
    }

    /**
     * Decrements the number stored at `key` by one. If `key` does not exist, it is set to 0 before
     * performing the operation.
     *
     * @see <a href="https://redis.io/commands/decr/">redis.io</a> for details.
     * @param key - The key to decrement its value.
     * @return the value of `key` after the decrement. An error is raised if `key` contains a value of
     *     the wrong type or contains a string that can not be represented as integer.
     */
    @Override
    public CompletableFuture<Long> decr(String key) {
        Command command =
                Command.builder()
                        .requestType(Command.RequestType.DECR)
                        .arguments(new String[] {key})
                        .build();
        return commandManager.submitNewCommand(command, BaseClient::handleLongResponse);
    }

    /**
     * Decrements the number stored at `key` by `amount`. If `key` does not exist, it is set to 0
     * before performing the operation.
     *
     * @see <a href="https://redis.io/commands/decrby/">redis.io</a> for details.
     * @param key - The key to decrement its value.
     * @param amount - The amount to decrement.
     * @return the value of `key` after the decrement. An error is raised if `key` contains a value of
     *     the wrong type or contains a string that can not be represented as integer.
     */
    @Override
    public CompletableFuture<Long> decrBy(String key, long amount) {
        Command command =
                Command.builder()
                        .requestType(Command.RequestType.DECR_BY)
                        .arguments(new String[] {key, Long.toString(amount)})
                        .build();
        return commandManager.submitNewCommand(command, BaseClient::handleLongResponse);
    }

    /**
     * Increments the number stored at `key` by one. If `key` does not exist, it is set to 0 before
     * performing the operation.
     *
     * @see <a href="https://redis.io/commands/incr/">redis.io</a> for details.
     * @param key - The key to increment its value.
     * @return the value of `key` after the increment, An error is raised if `key` contains a value of
     *     the wrong type or contains a string that can not be represented as integer.
     */
    @Override
    public CompletableFuture<Long> incr(String key) {
        Command command =
                Command.builder()
                        .requestType(Command.RequestType.INCR)
                        .arguments(new String[] {key})
                        .build();
        return commandManager.submitNewCommand(command, BaseClient::handleLongResponse);
    }

    /**
     * Increments the number stored at `key` by `amount`. If `key` does not exist, it is set to 0
     * before performing the operation.
     *
     * @see <a href="https://redis.io/commands/incrby/">redis.io</a> for details.
     * @param key - The key to increment its value.
     * @param amount - The amount to increment.
     * @returns the value of `key` after the increment, An error is raised if `key` contains a value
     *     of the wrong type or contains a string that can not be represented as integer.
     */
    @Override
    public CompletableFuture<Long> incrBy(String key, long amount) {
        Command command =
                Command.builder()
                        .requestType(Command.RequestType.INCR_BY)
                        .arguments(new String[] {key, Long.toString(amount)})
                        .build();
        return commandManager.submitNewCommand(command, BaseClient::handleLongResponse);
    }

    /**
     * Increment the string representing a floating point number stored at `key` by `amount`. By using
     * a negative increment value, the result is that the value stored at `key` is decremented. If
     * `key` does not exist, it is set to 0 before performing the operation.
     *
     * @see <a href="https://redis.io/commands/incrbyfloat/">redis.io</a> for details.
     * @param key - The key to increment its value.
     * @param amount - The amount to increment.
     * @returns the value of `key` after the increment. An error is raised if `key` contains a value
     *     of the wrong type, or the current key content is not parsable as a double precision
     *     floating point number.
     */
    @Override
    public CompletableFuture<Double> incrByFloat(String key, double amount) {
        Command command =
                Command.builder()
                        .requestType(Command.RequestType.INCR_BY_FLOAT)
                        .arguments(new String[] {key, Double.toString(amount)})
                        .build();
        return commandManager.submitNewCommand(command, BaseClient::handleDoubleResponse);
    }

    /**
     * Retrieve the values of multiple keys.
     *
     * @see <a href="https://redis.io/commands/mget/">redis.io</a> for details.
     * @param keys - A list of keys to retrieve values for.
     * @returns A list of values corresponding to the provided keys. If a key is not found, its
     *     corresponding value in the list will be null.
     */
    @Override
    public CompletableFuture<Object[]> mget(String[] keys) {
        Command command =
                Command.builder().requestType(Command.RequestType.MGET).arguments(keys).build();
        return commandManager.submitNewCommand(command, BaseClient::handleObjectArrayResponse);
    }

    /**
     * Set multiple keys to multiple values in a single operation.
     *
     * @see <a href="https://redis.io/commands/mset/">redis.io</a> for details.
     * @param keyValueMap - A key-value map consisting of keys and their respective values to set.
     * @returns null
     */
    @Override
    public CompletableFuture<Void> mset(HashMap<String, String> keyValueMap) {
        List<String> flatMap = new ArrayList<>();

        for (Map.Entry<String, String> entry : keyValueMap.entrySet()) {
            flatMap.add(entry.getKey());
            flatMap.add(entry.getValue());
        }

        String[] args = flatMap.toArray(new String[0]);

        Command command =
                Command.builder().requestType(Command.RequestType.MSET).arguments(args).build();
        return commandManager.submitNewCommand(command, BaseClient::handleVoidResponse);
    }
}
