/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A thread-safe queue which allows to get received {@link Message}s.<br>
 * Backed by a {@link ConcurrentLinkedDeque}.
 */
public interface MessageQueue {

    /**
     * Pop a {@link Message} from the queue.
     *
     * @return A {@link Message} or <code>null</code> if queue is empty.
     */
    Message poll();

    /**
     * Get a number of unread messages.
     *
     * @return The number of elements in the queue.
     */
    int size();

    /** Clear the queue and delete all unread messages. */
    void clear();

    /** Check whether queue is empty. */
    boolean isEmpty();
}
