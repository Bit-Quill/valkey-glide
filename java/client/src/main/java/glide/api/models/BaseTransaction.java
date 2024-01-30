package glide.api.models;

import static glide.managers.CommandManager.mapRequestTypes;

import glide.api.BaseClient;
import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.SetOptions;
import lombok.Getter;

/**
 * Base class encompassing shared commands for both standalone and cluster mode implementations in a
 * transaction. Transactions allow the execution of a group of commands in a single step.
 *
 * <p>Command Response: An array of command responses is returned by the client exec command, in the
 * order they were given. Each element in the array represents a command given to the transaction.
 * The response for each command depends on the executed Redis command. Specific response types are
 * documented alongside each method.
 *
 * @example transaction = new Transaction.Builder() .set("key", "value"); .get("key"); .build();
 *     Object[] result = client.exec(transaction).get(); assertEqual(new Object[] {OK , "value"});
 */
@Getter
public abstract class BaseTransaction {
    /** Command class to send a single request to Redis. */
    redis_request.RedisRequestOuterClass.Transaction.Builder transactionBuilder =
            redis_request.RedisRequestOuterClass.Transaction.newBuilder();

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
    public BaseTransaction customCommand(String[] args) {
        redis_request.RedisRequestOuterClass.Command.ArgsArray.Builder commandArgs =
                redis_request.RedisRequestOuterClass.Command.ArgsArray.newBuilder();

        for (var arg : args) {
            commandArgs.addArgs(arg);
        }

        transactionBuilder.addCommands(
                redis_request.RedisRequestOuterClass.Command.newBuilder()
                        .setRequestType(mapRequestTypes(BaseClient.RequestType.CUSTOM_COMMAND))
                        .setArgsArray(commandArgs.build())
                        .build());
        return this;
    }

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @return the String "PONG"
     */
    public BaseTransaction ping() {
        transactionBuilder.addCommands(
                redis_request.RedisRequestOuterClass.Command.newBuilder()
                        .setRequestType(mapRequestTypes(BaseClient.RequestType.PING))
                        .build());
        return this;
    }

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @param msg - the ping argument that will be returned.
     * @return return a copy of the argument.
     */
    public BaseTransaction ping(String msg) {
        redis_request.RedisRequestOuterClass.Command.ArgsArray.Builder commandArgs =
                redis_request.RedisRequestOuterClass.Command.ArgsArray.newBuilder();

        commandArgs.addArgs(msg);

        transactionBuilder.addCommands(
                redis_request.RedisRequestOuterClass.Command.newBuilder()
                        .setRequestType(mapRequestTypes(BaseClient.RequestType.PING))
                        .setArgsArray(commandArgs.build())
                        .build());
        return this;
    }

    /**
     * Get information and statistics about the Redis server. DEFAULT option is assumed
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @return CompletableFuture with the response
     */
    public BaseTransaction info() {
        transactionBuilder.addCommands(
                redis_request.RedisRequestOuterClass.Command.newBuilder()
                        .setRequestType(mapRequestTypes(BaseClient.RequestType.INFO))
                        .build());
        return this;
    }

    /**
     * Get information and statistics about the Redis server.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @param options - A list of InfoSection values specifying which sections of information to
     *     retrieve. When no parameter is provided, the default option is assumed.
     * @return CompletableFuture with the response
     */
    public BaseTransaction info(InfoOptions options) {
        redis_request.RedisRequestOuterClass.Command.ArgsArray.Builder commandArgs =
                redis_request.RedisRequestOuterClass.Command.ArgsArray.newBuilder();

        for (var arg : options.toArgs()) {
            commandArgs.addArgs(arg);
        }

        transactionBuilder.addCommands(
                redis_request.RedisRequestOuterClass.Command.newBuilder()
                        .setRequestType(mapRequestTypes(BaseClient.RequestType.INFO))
                        .setArgsArray(commandArgs.build())
                        .build());
        return this;
    }

    /**
     * Get the value associated with the given key, or null if no such value exists.
     *
     * @see <a href="https://redis.io/commands/get/">redis.io</a> for details.
     * @param key - The key to retrieve from the database.
     * @return If `key` exists, returns the value of `key` as a string. Otherwise, return null
     */
    public BaseTransaction get(String key) {
        redis_request.RedisRequestOuterClass.Command.ArgsArray.Builder commandArgs =
                redis_request.RedisRequestOuterClass.Command.ArgsArray.newBuilder();

        commandArgs.addArgs(key);

        transactionBuilder.addCommands(
                redis_request.RedisRequestOuterClass.Command.newBuilder()
                        .setRequestType(mapRequestTypes(BaseClient.RequestType.GET_STRING))
                        .setArgsArray(commandArgs.build())
                        .build());
        return this;
    }

    /**
     * Set the given key with the given value.
     *
     * @see <a href="https://redis.io/commands/set/">redis.io</a> for details.
     * @param key - The key to store.
     * @param value - The value to store with the given key.
     * @return null
     */
    public BaseTransaction set(String key, String value) {
        redis_request.RedisRequestOuterClass.Command.ArgsArray.Builder commandArgs =
                redis_request.RedisRequestOuterClass.Command.ArgsArray.newBuilder();

        commandArgs.addArgs(key);
        commandArgs.addArgs(value);

        transactionBuilder.addCommands(
                redis_request.RedisRequestOuterClass.Command.newBuilder()
                        .setRequestType(mapRequestTypes(BaseClient.RequestType.SET_STRING))
                        .setArgsArray(commandArgs.build())
                        .build());
        return this;
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
    public BaseTransaction set(String key, String value, SetOptions options) {
        redis_request.RedisRequestOuterClass.Command.ArgsArray.Builder commandArgs =
                redis_request.RedisRequestOuterClass.Command.ArgsArray.newBuilder();

        commandArgs.addArgs(key);
        commandArgs.addArgs(value);

        for (var arg : options.toArgs()) {
            commandArgs.addArgs(arg);
        }

        transactionBuilder.addCommands(
                redis_request.RedisRequestOuterClass.Command.newBuilder()
                        .setRequestType(mapRequestTypes(BaseClient.RequestType.SET_STRING))
                        .setArgsArray(commandArgs.build())
                        .build());
        return this;
    }
}
