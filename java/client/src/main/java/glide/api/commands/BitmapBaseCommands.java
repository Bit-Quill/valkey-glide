/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import java.util.concurrent.CompletableFuture;

/**
 * Supports commands and transactions for the "Bitmap Commands" group for standalone and cluster
 * clients.
 *
 * @see <a href="https://redis.io/docs/latest/commands/?group=bitmap">Bitmap Commands</a>
 */
public interface BitmapBaseCommands {
    /**
     * Returns the bit value at <code>offset</code> in the string value stored at <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/getbit/">redis.io</a> for details.
     * @param key The key for the string to get the bit at offset of.
     * @param offset The index of the bit to return.
     * @return The bit at offset of the string. Returns zero if the key is missing as it is treated as
     *     an empty string. Returns zero if the positive offset exceeds the length of the string as it
     *     is assumed to be padded zeroes.
     * @example
     *     <pre>{@code
     * Long payload = client.getbit("myKey1", 1).get();
     * assert payload == 1L; // The second bit for string stored at "myKey1" is set to 1.
     * }</pre>
     */
    CompletableFuture<Long> getbit(String key, long offset);
}
