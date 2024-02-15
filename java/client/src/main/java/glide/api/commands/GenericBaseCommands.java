/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import java.util.concurrent.CompletableFuture;

/**
 * Generic Commands interface to handle generic commands for all server requests.
 *
 * @see <a href="https://redis.io/commands/?group=generic">Generic Commands</a>
 */
public interface GenericBaseCommands {

    /**
     * Removes the specified <code>keys</code>. A key is ignored if it does not exist. This command,
     * similar to DEL, removes specified keys and ignores non-existent ones. However, this command
     * does not block the server, while <a href="https://redis.io/commands/del/">DEL</a> does.
     *
     * @see <a href="https://redis.io/commands/unlink/">redis.io</a> for details.
     * @param keys The <code>keys</code> we wanted to unlink.
     * @return the number of <code>keys</code> that were unlinked.
     */
    CompletableFuture<Long> unlink(String[] keys);
}
