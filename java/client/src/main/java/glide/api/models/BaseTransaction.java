/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models;

import static redis_request.RedisRequestOuterClass.RequestType.CustomCommand;
import static redis_request.RedisRequestOuterClass.RequestType.GetString;
import static redis_request.RedisRequestOuterClass.RequestType.Info;
import static redis_request.RedisRequestOuterClass.RequestType.LLen;
import static redis_request.RedisRequestOuterClass.RequestType.LPop;
import static redis_request.RedisRequestOuterClass.RequestType.LPush;
import static redis_request.RedisRequestOuterClass.RequestType.LRange;
import static redis_request.RedisRequestOuterClass.RequestType.LRem;
import static redis_request.RedisRequestOuterClass.RequestType.LTrim;
import static redis_request.RedisRequestOuterClass.RequestType.Ping;
import static redis_request.RedisRequestOuterClass.RequestType.SetString;

import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.InfoOptions.Section;
import glide.api.models.commands.SetOptions;
import glide.api.models.commands.SetOptions.ConditionalSet;
import glide.api.models.commands.SetOptions.SetOptionsBuilder;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;
import redis_request.RedisRequestOuterClass.Command;
import redis_request.RedisRequestOuterClass.Command.ArgsArray;
import redis_request.RedisRequestOuterClass.RequestType;
import redis_request.RedisRequestOuterClass.Transaction;

/**
 * Base class encompassing shared commands for both standalone and cluster mode implementations in a
 * transaction. Transactions allow the execution of a group of commands in a single step.
 *
 * <p>Command Response: An array of command responses is returned by the client exec command, in the
 * order they were given. Each element in the array represents a command given to the transaction.
 * The response for each command depends on the executed Redis command. Specific response types are
 * documented alongside each method.
 *
 * @param <T> child typing for chaining method calls
 */
@Getter
public abstract class BaseTransaction<T extends BaseTransaction<T>> {
    /** Command class to send a single request to Redis. */
    protected final Transaction.Builder protobufTransaction = Transaction.newBuilder();

    protected abstract T getThis();

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
     * Object result = client.customCommand("CLIENT","LIST","TYPE", "PUBSUB").get();
     * </pre>
     *
     * @param args Arguments for the custom command.
     * @return A response from Redis with an <code>Object</code>.
     */
    public T customCommand(String... args) {

        ArgsArray commandArgs = buildArgs(args);
        protobufTransaction.addCommands(buildCommand(CustomCommand, commandArgs));
        return getThis();
    }

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @return A response from Redis with a <code>String</code>.
     */
    public T ping() {
        protobufTransaction.addCommands(buildCommand(Ping));
        return getThis();
    }

    /**
     * Ping the Redis server.
     *
     * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
     * @param msg The ping argument that will be returned.
     * @return A response from Redis with a <code>String</code>.
     */
    public T ping(String msg) {
        ArgsArray commandArgs = buildArgs(msg);

        protobufTransaction.addCommands(buildCommand(Ping, commandArgs));
        return getThis();
    }

    /**
     * Get information and statistics about the Redis server. No argument is provided, so the {@link
     * Section#DEFAULT} option is assumed.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @return A response from Redis with a <code>String</code>.
     */
    public T info() {
        protobufTransaction.addCommands(buildCommand(Info));
        return getThis();
    }

    /**
     * Get information and statistics about the Redis server.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @param options A list of {@link Section} values specifying which sections of information to
     *     retrieve. When no parameter is provided, the {@link Section#DEFAULT} option is assumed.
     * @return Response from Redis with a <code>String</code> containing the requested {@link
     *     Section}s.
     */
    public T info(InfoOptions options) {
        ArgsArray commandArgs = buildArgs(options.toArgs());

        protobufTransaction.addCommands(buildCommand(Info, commandArgs));
        return getThis();
    }

    /**
     * Get the value associated with the given key, or null if no such value exists.
     *
     * @see <a href="https://redis.io/commands/get/">redis.io</a> for details.
     * @param key The key to retrieve from the database.
     * @return Response from Redis. <code>key</code> exists, returns the <code>value</code> of <code>
     *     key</code> as a String. Otherwise, return <code>null</code>.
     */
    public T get(String key) {
        ArgsArray commandArgs = buildArgs(key);

        protobufTransaction.addCommands(buildCommand(GetString, commandArgs));
        return getThis();
    }

    /**
     * Set the given key with the given value.
     *
     * @see <a href="https://redis.io/commands/set/">redis.io</a> for details.
     * @param key The key to store.
     * @param value The value to store with the given <code>key</code>.
     * @return Response from Redis.
     */
    public T set(String key, String value) {
        ArgsArray commandArgs = buildArgs(key, value);

        protobufTransaction.addCommands(buildCommand(SetString, commandArgs));
        return getThis();
    }

    /**
     * Set the given key with the given value. Return value is dependent on the passed options.
     *
     * @see <a href="https://redis.io/commands/set/">redis.io</a> for details.
     * @param key The key to store.
     * @param value The value to store with the given key.
     * @param options The Set options.
     * @return Response from Redis with a <code>String</code> or <code>null</code> response. The old
     *     value as a <code>String</code> if {@link SetOptionsBuilder#returnOldValue(boolean)} is set.
     *     Otherwise, if the value isn't set because of {@link ConditionalSet#ONLY_IF_EXISTS} or
     *     {@link ConditionalSet#ONLY_IF_DOES_NOT_EXIST} conditions, return <code>null</code>.
     *     Otherwise, return <code>OK</code>.
     */
    public T set(String key, String value, SetOptions options) {
        ArgsArray commandArgs =
                buildArgs(ArrayUtils.addAll(new String[] {key, value}, options.toArgs()));

        protobufTransaction.addCommands(buildCommand(SetString, commandArgs));
        return getThis();
    }

    /**
     * Insert all the specified values at the head of the list stored at <code>key</code>. <code>
     * elements</code> are inserted one after the other to the head of the list, from the leftmost
     * element to the rightmost element. If <code>key</code> does not exist, it is created as an empty
     * list before performing the push operations.
     *
     * @see <a href="https://redis.io/commands/lpush/">redis.io</a> for details.
     * @param key The key of the list.
     * @param elements The elements to insert at the head of the list stored at <code>key</code>.
     * @return Command Response - The length of the list after the push operations.<br>
     *     If <code>key</code> holds a value that is not a list, an error is raised.<br>
     */
    public T lpush(@NonNull String key, @NonNull String[] elements) {
        ArgsArray commandArgs = buildArgs(ArrayUtils.addFirst(elements, key));

        protobufTransaction.addCommands(buildCommand(LPush, commandArgs));
        return getThis();
    }

    /**
     * Removes and returns the first elements of the list stored at <code>key</code>. The command pops
     * a single element from the beginning of the list.
     *
     * @see <a href="https://redis.io/commands/lpop/">redis.io</a> for details.
     * @param key The key of the list.
     * @return Command Response - The value of the first element. <br>
     *     If <code>key</code> does not exist null will be returned. <br>
     *     If <code>key</code> holds a value that is not a list, an error is raised. <br>
     */
    public T lpop(@NonNull String key) {
        ArgsArray commandArgs = buildArgs(key);

        protobufTransaction.addCommands(buildCommand(LPop, commandArgs));
        return getThis();
    }

    /**
     * Removes and returns up to <code>count</code> elements of the list stored at <code>key</code>,
     * depending on the list's length.
     *
     * @see <a href="https://redis.io/commands/lpop/">redis.io</a> for details.
     * @param key The key of the list.
     * @param count The count of the elements to pop from the list.
     * @return Command Response - An array of the popped elements will be returned depending on the
     *     list's length.<br>
     *     If <code>key</code> does not exist null will be returned.<br>
     *     If <code>key</code> holds a value that is not a list, an error is raised.<br>
     */
    public T lpopCount(@NonNull String key, long count) {
        ArgsArray commandArgs = buildArgs(key, Long.toString(count));

        protobufTransaction.addCommands(buildCommand(LPop, commandArgs));
        return getThis();
    }

    /**
     * Returns the specified elements of the list stored at <code>key</code>. The offsets <code>start
     * </code> and <code>end</code> are zero-based indexes, with 0 being the first element of the
     * list, 1 being the next element and so on. These offsets can also be negative numbers indicating
     * offsets starting at the end of the list, with -1 being the last element of the list, -2 being
     * the penultimate, and so on.
     *
     * @see <a href="https://redis.io/commands/lrange/">redis.io</a> for details.
     * @param key The key of the list.
     * @param start The starting point of the range.
     * @param end The end of the range.
     * @return Command Response - Array of elements in the specified range.<br>
     *     If <code>start</code> exceeds the end of the list, or if <code>start</code> is greater than
     *     <code>end</code>, an empty array will be returned.<br>
     *     If <code>end</code> exceeds the actual end of the list, the range will stop at the actual
     *     end of the list.<br>
     *     If <code>key</code> does not exist an empty array will be returned.<br>
     *     If <code>key</code> holds a value that is not a list, an error is raised.<br>
     */
    public T lrange(@NonNull String key, long start, long end) {
        ArgsArray commandArgs = buildArgs(key, Long.toString(start), Long.toString(end));

        protobufTransaction.addCommands(buildCommand(LRange, commandArgs));
        return getThis();
    }

    /**
     * Returns the length of the list stored at <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/llen/">redis.io</a> for details.
     * @param key The key of the list.
     * @return Command Response - The length of the list at <code>key</code>. <br>
     *     If <code>key</code> does not exist, it is interpreted as an empty list and 0 is returned.
     *     <br>
     *     If <code>key</code> holds a value that is not a list, an error is raised. <br>
     */
    public T llen(@NonNull String key) {
        ArgsArray commandArgs = buildArgs(key);

        protobufTransaction.addCommands(buildCommand(LLen, commandArgs));
        return getThis();
    }

    /**
     * Trim an existing list so that it will contain only the specified range of elements specified.
     * The offsets <code>start</code> and <code>end</code> are zero-based indexes, with 0 being the
     * first element of the list, 1 being the next element and so on. These offsets can also be
     * negative numbers indicating offsets starting at the end of the list, with -1 being the last
     * element of the list, -2 being the penultimate, and so on.
     *
     * @see <a href="https://redis.io/commands/ltrim/">redis.io</a> for details.
     * @param key The key of the list.
     * @param start The starting point of the range.
     * @param end The end of the range.
     * @return Command Response - Always <code>OK</code>. <br>
     *     If <code>start</code> exceeds the end of the list, or if <code>start</code> is greater than
     *     <code>end</code>, the result will be an empty list (which causes key to be removed). <br>
     *     If <code>end</code> exceeds the actual end of the list, it will be treated like the last
     *     element of the list. <br>
     *     If <code>key</code> does not exist the command will be ignored. <br>
     *     If <code>key</code> holds a value that is not a list, an error is raised. <br>
     */
    public T ltrim(@NonNull String key, long start, long end) {
        ArgsArray commandArgs = buildArgs(key, Long.toString(start), Long.toString(end));

        protobufTransaction.addCommands(buildCommand(LTrim, commandArgs));
        return getThis();
    }

    /**
     * Removes the first <code>count</code> occurrences of elements equal to <code>element</code> from
     * the list stored at <code>key</code>.<br>
     * If <code>count</code> is positive: Removes elements equal to <code>element</code> moving from
     * head to tail.<br>
     * If <code>count</code> is negative: Removes elements equal to <code>element</code> moving from
     * tail to head.<br>
     * If <code>count</code> is 0 or <code>count</code> is greater than the occurrences of elements
     * equal to <code>element</code>: Removes all elements equal to <code>element</code>.<br>
     *
     * @see <a href="https://redis.io/commands/lrem/">redis.io</a> for details.
     * @param key The key of the list.
     * @param count The count of the occurrences of elements equal to <code>element</code> to remove.
     * @param element The element to remove from the list.
     * @return Command Response - The number of the removed elements.<br>
     *     If <code>key</code> does not exist, 0 is returned.<br>
     *     If <code>key</code> holds a value that is not a list, an error is raised.<br>
     */
    public T lrem(@NonNull String key, long count, @NonNull String element) {
        ArgsArray commandArgs = buildArgs(key, Long.toString(count), element);

        protobufTransaction.addCommands(buildCommand(LRem, commandArgs));
        return getThis();
    }

    /** Build protobuf {@link Command} object for given command and arguments. */
    protected Command buildCommand(RequestType requestType) {
        return buildCommand(requestType, buildArgs());
    }

    /** Build protobuf {@link Command} object for given command and arguments. */
    protected Command buildCommand(RequestType requestType, ArgsArray args) {
        return Command.newBuilder().setRequestType(requestType).setArgsArray(args).build();
    }

    /** Build protobuf {@link ArgsArray} object for given arguments. */
    protected ArgsArray buildArgs(String... stringArgs) {
        ArgsArray.Builder commandArgs = ArgsArray.newBuilder();

        for (String string : stringArgs) {
            commandArgs.addArgs(string);
        }

        return commandArgs.build();
    }
}
