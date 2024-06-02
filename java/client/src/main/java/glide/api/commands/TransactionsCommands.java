/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import java.util.concurrent.CompletableFuture;

public interface TransactionsCommands {
    CompletableFuture<String> watch(String[] keys);
}
