package glide.api.models;

import static glide.managers.CommandManager.mapRequestTypes;

import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.SetOptions;
import glide.managers.CommandManager.RequestType;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import redis_request.RedisRequestOuterClass.Command;
import redis_request.RedisRequestOuterClass.Command.ArgsArray;
import redis_request.RedisRequestOuterClass.Transaction;

/**
 * Base class encompassing shared commands for both standalone and cluster mode implementations in a
 * transaction. Transactions allow the execution of a group of commands in a single step.
 *
 * <p>Command Response: An array of command responses is returned by the client exec command, in the
 * order they were given. Each element in the array represents a command given to the transaction.
 * The response for each command depends on the executed Redis command. Specific response types are
 * documented alongside each method.
 */
@Getter
public abstract class BaseTransaction {
    /** Command class to send a single request to Redis. */
    Transaction.Builder transactionBuilder = Transaction.newBuilder();

    /**
     * Executes a single command, without checking inputs. Every part of the command, including
     * subcommands, should be added as a separate value in args.
     *
     * @remarks This function should only be used for single-response commands. Commands that don't
     *     return response (such as <em>SUBSCRIBE</em>), or that return potentially more than a single
     *     response (such as <em>XREAD</em>), or that change the client's behavior (such as entering
     *     <em>pub</em>/<em>sub</em> mode on <em>RESP2</em> connections) shouldn't be called using
     *     this function.
     * @example Returns a list of all pub/sub clients:
     *     <pre>
     * Object result = client.customCommand(new String[]{"CLIENT","LIST","TYPE", "PUBSUB"}).get();
     * </pre>
     *
     * @param args Arguments for the custom command.
     * @return When executed, a <code>CompletableFuture</code> with response result from Redis.
     */
    public BaseTransaction customCommand(String[] args) {
        ArgsArray.Builder commandArgs = addAllArgs(args);

        transactionBuilder.addCommands(
                Command.newBuilder()
                        .setRequestType(mapRequestTypes(RequestType.CUSTOM_COMMAND))
                        .setArgsArray(commandArgs.build())
                        .build());
        return this;
    }

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @return When executed, a <em>CompletableFuture</em> with the String <code>"PONG"</code>
     */
    public BaseTransaction ping() {
        transactionBuilder.addCommands(
                Command.newBuilder().setRequestType(mapRequestTypes(RequestType.PING)).build());
        return this;
    }

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @param msg The ping argument that will be returned.
     * @return When executed, a <em>CompletableFuture</em> with a copy of the argument.
     */
    public BaseTransaction ping(String msg) {
        ArgsArray.Builder commandArgs = addAllArgs(msg);

        transactionBuilder.addCommands(
                Command.newBuilder()
                        .setRequestType(mapRequestTypes(RequestType.PING))
                        .setArgsArray(commandArgs.build())
                        .build());
        return this;
    }

    /**
     * Get information and statistics about the Redis server. No argument is provided, so the <code>DEFAULT</code> option is assumed.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @return A <em>CompletableFuture</em> with String response from Redis
     */
    public BaseTransaction info() {
        transactionBuilder.addCommands(
                Command.newBuilder().setRequestType(mapRequestTypes(RequestType.INFO)).build());
        return this;
    }

    /**
     * Get information and statistics about the Redis server.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @param options A list of InfoSection values specifying which sections of information to
     *     retrieve. When no parameter is provided, the <code>DEFAULT</code> option is assumed.
     * @return A <em>CompletableFuture</em> with String response from Redis
     */
    public BaseTransaction info(InfoOptions options) {
        ArgsArray.Builder commandArgs = addAllArgs(options.toArgs());

        transactionBuilder.addCommands(
                Command.newBuilder()
                        .setRequestType(mapRequestTypes(RequestType.INFO))
                        .setArgsArray(commandArgs.build())
                        .build());
        return this;
    }

    /**
     * Get the value associated with the given key, or null if no such value exists.
     *
     * @see <a href="https://redis.io/commands/get/">redis.io</a> for details.
     * @param key The key to retrieve from the database.
     * @return If <code>key</code> exists, returns the <code>value</code> of <code>key</code> as a String. Otherwise, return <code>null</code>.
     */
    public BaseTransaction get(String key) {
        ArgsArray.Builder commandArgs = addAllArgs(key);

        transactionBuilder.addCommands(
                Command.newBuilder()
                        .setRequestType(mapRequestTypes(RequestType.GET_STRING))
                        .setArgsArray(commandArgs.build())
                        .build());
        return this;
    }

    /**
     * Set the given key with the given value.
     *
     * @see <a href="https://redis.io/commands/set/">redis.io</a> for details.
     * @param key The key to store.
     * @param value The value to store with the given <code>key</code>.
     * @return An empty response
     */
    public BaseTransaction set(String key, String value) {
        ArgsArray.Builder commandArgs = addAllArgs(key, value);

        transactionBuilder.addCommands(
                Command.newBuilder()
                        .setRequestType(mapRequestTypes(RequestType.SET_STRING))
                        .setArgsArray(commandArgs.build())
                        .build());
        return this;
    }

    /**
     * Set the given key with the given value. Return value is dependent on the passed options.
     *
     * @see <a href="https://redis.io/commands/set/">redis.io</a> for details.
     * @param key The key to store.
     * @param value The value to store with the given key.
     * @param options The Set options.
     * @return A string or null response. The old value as a string if <code>returnOldValue</code> is set. Otherwise, if the
     *     value isn't set because of <code>onlyIfExists</code> or <code>onlyIfDoesNotExist</code> conditions, return <code>null</code>.
     *     Otherwise, return "OK".
     */
    public BaseTransaction set(String key, String value, SetOptions options) {
        ArgsArray.Builder commandArgs =
                addAllArgs(ArrayUtils.addAll(new String[] {key, value}, options.toArgs()));

        transactionBuilder.addCommands(
                Command.newBuilder()
                        .setRequestType(mapRequestTypes(RequestType.SET_STRING))
                        .setArgsArray(commandArgs.build())
                        .build());
        return this;
    }

    protected ArgsArray.Builder addAllArgs(String... stringArgs) {
        ArgsArray.Builder commandArgs = ArgsArray.newBuilder();

        for (String string : stringArgs) {
            commandArgs.addArgs(string);
        }

        return commandArgs;
    }
}
