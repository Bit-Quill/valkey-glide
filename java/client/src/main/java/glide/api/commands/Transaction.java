package glide.api.commands;

import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.SetOptions;
import glide.managers.models.Command;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;

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
@Builder
@Getter
@EqualsAndHashCode
public class Transaction {
    @Singular List<Command> commands = new LinkedList<>();

    public static class TransactionBuilder {

        /**
         * Executes a single command, without checking inputs. Every part of the command, including
         * subcommands, should be added as a separate value in args.
         *
         * @remarks This function should only be used for single-response commands. Commands that don't
         *     return response (such as SUBSCRIBE), or that return potentially more than a single
         *     response (such as XREAD), or that change the client's behavior (such as entering pub/sub
         *     mode on RESP2 connections) shouldn't be called using this function.
         * @example Returns a list of all pub/sub clients:
         *     <pre>
         * Object result = client.customCommand(new String[]{"CLIENT","LIST","TYPE", "PUBSUB"}).get();
         * </pre>
         *
         * @param args arguments for the custom command
         * @return a CompletableFuture with response result from Redis
         */
        public TransactionBuilder customCommand(String[] args) {
            createOrAddCommand(Command.customCommand(args));
            return this;
        }

        /**
         * Ping the Redis server.
         *
         * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
         * @return the String "PONG"
         */
        public TransactionBuilder ping() {
            createOrAddCommand(Command.ping());
            return this;
        }

        /**
         * Ping the Redis server.
         *
         * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
         * @param msg - the ping argument that will be returned.
         * @return return a copy of the argument.
         */
        public TransactionBuilder ping(String msg) {
            createOrAddCommand(Command.ping(msg));
            return this;
        }

        /**
         * Get information and statistics about the Redis server. DEFAULT option is assumed
         *
         * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
         * @return CompletableFuture with the response
         */
        public TransactionBuilder info() {
            createOrAddCommand(Command.info());
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
        public TransactionBuilder info(InfoOptions options) {
            createOrAddCommand(Command.info(options.toInfoOptions()));
            return this;
        }

        /**
         * Get the value associated with the given key, or null if no such value exists.
         *
         * @see <a href="https://redis.io/commands/get/">redis.io</a> for details.
         * @param key - The key to retrieve from the database.
         * @return If `key` exists, returns the value of `key` as a string. Otherwise, return null
         */
        public TransactionBuilder get(String key) {
            createOrAddCommand(Command.get(key));
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
        public TransactionBuilder set(String key, String value) {
            createOrAddCommand(Command.set(key, value));
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
        public TransactionBuilder set(String key, String value, SetOptions options) {
            createOrAddCommand(Command.set(key, value, options.toArgs()));
            return this;
        }

        private void createOrAddCommand(Command command) {
            if (this.commands == null) {
                this.commands = new ArrayList<>();
            }
            this.commands.add(command);
        }
    }
}
