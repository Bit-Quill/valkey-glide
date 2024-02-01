/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api;

import glide.api.commands.BaseCommands;
import glide.api.commands.ConnectionCommands;
import glide.api.commands.GenericCommands;
import glide.api.commands.ServerCommands;
import glide.api.commands.StringCommands;
import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.SetOptions;
import glide.api.models.configuration.RedisClientConfiguration;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import glide.managers.models.Command;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Async (non-blocking) client for Redis in Standalone mode. Use {@link #CreateClient} to request a
 * client to Redis.
 */
public class RedisClient extends BaseClient
        implements BaseCommands, GenericCommands, ConnectionCommands, ServerCommands, StringCommands {

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
        return CreateClient(config, RedisClient::new);
    }

    @Override
    public CompletableFuture<Object> customCommand(String[] args) {
        Command command =
                Command.builder().requestType(Command.RequestType.CUSTOM_COMMAND).arguments(args).build();
        return commandManager.submitNewCommand(command, this::handleObjectResponse);
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
}
