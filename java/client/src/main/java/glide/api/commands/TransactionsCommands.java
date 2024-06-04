/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import java.util.concurrent.CompletableFuture;

/**
 * Supports commands for the "Transactions Commands" group for a standalone client.
 *
 * @see <a href="https://redis.io/commands/?group=transactions">Transactions Commands</a>
 */
public interface TransactionsCommands {
    /**
     * Marks the given keys to be watched for conditional execution of a transaction. Transactions
     * will only execute commands if the watched keys are not modified before execution of the
     * transaction.
     *
     * @see <a href="https://redis.io/docs/latest/commands/watch/">redis.io</a> for details.
     * @param keys The keys to watch.
     * @return The string "OK"
     * @example
     *     <pre>{@code
     * client.watch("sampleKey");
     * transaction.set("sampleKey", "foobar");
     * client.exec(transaction).get(); // Executes successfully and keys are unwatched.
     * }</pre>
     */
    CompletableFuture<String> watch(String[] keys);

    /**
     * Flushes all the previously watched keys for a transaction. Executing a transaction will
     * automatically flush all previously watched keys.
     *
     * @see <a href="https://redis.io/docs/latest/commands/unwatch/">redis.io</a> for details.
     * @return The string "OK"
     * @example
     *     <pre>{@code
     * client.watch("sampleKey");
     * client.unwatch();
     * }</pre>
     */
    CompletableFuture<String> unwatch();
}
