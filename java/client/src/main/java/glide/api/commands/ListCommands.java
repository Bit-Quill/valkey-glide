/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import java.util.concurrent.CompletableFuture;

/**
 * List Commands interface.
 *
 * @see <a href="https://redis.io/commands/?group=list">List Commands</a>
 */
public interface ListCommands {
    /**
     * Insert all the specified values at the tail of the list stored at <code>key</code>. <code>
     * elements</code> are inserted one after the other to the tail of the list, from the leftmost
     * element to the rightmost element. If <code>key</code> does not exist, it is created as an empty
     * list before performing the push operations.
     *
     * @see <a href="https://redis.io/commands/rpush/">redis.io</a> for details.
     * @param key The key of the list.
     * @param elements The elements to insert at the tail of the list stored at <code>key</code>.
     * @return The length of the list after the push operations.<br>
     *     If <code>key</code> holds a value that is not a list, an error is raised.<br>
     */
    CompletableFuture<Long> rpush(String key, String[] elements);

    /**
     * Removes and returns the last elements of the list stored at <code>key</code>. The command pops
     * a single element from the end of the list.
     *
     * @see <a href="https://redis.io/commands/rpop/">redis.io</a> for details.
     * @param key The key of the list.
     * @return The value of the last element.<br>
     *     If <code>key</code> does not exist null will be returned.<br>
     *     If <code>key</code> holds a value that is not a list, an error is raised.<br>
     */
    CompletableFuture<String> rpop(String key);

    /**
     * Removes and returns up to <code>count</code> elements from the list stored at <code>key</code>,
     * depending on the list's length.
     *
     * @see <a href="https://redis.io/commands/rpop/">redis.io</a> for details.
     * @param count The count of the elements to pop from the list.
     * @returns An array of popped elements will be returned depending on the list's length.<br>
     *     If <code>key</code> does not exist null will be returned.<br>
     *     If <code>key</code> holds a value that is not a list, an error is raised.<br>
     */
    CompletableFuture<String[]> rpopCount(String key, long count);
}
