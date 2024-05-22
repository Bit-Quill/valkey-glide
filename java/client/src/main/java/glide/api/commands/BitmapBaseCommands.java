/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import glide.api.models.commands.BitmapIndexType;
import java.util.concurrent.CompletableFuture;

/**
 * Supports commands and transactions for the "Bitmap Commands" group for standalone and cluster
 * clients.
 *
 * @see <a href="https://redis.io/docs/latest/commands/?group=bitmap">Bitmap Commands</a>
 */
public interface BitmapBaseCommands {
    /**
     * Counts the number of set bits (population counting) in a string stored at <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/bitcount/">redis.io</a> for details.
     * @param key The key for the string to count the set bits of.
     * @return The number of set bits in the string. Returns zero if the key is missing as it is
     *     treated as an empty string.
     * @example
     *     <pre>{@code
     * Long payload = client.bitcount("myKey1").get();
     * assert payload == 2L; // The string stored at "myKey1" contains 2 set bits.
     * }</pre>
     */
    CompletableFuture<Long> bitcount(String key);

    /**
     * Counts the number of set bits (population counting) in a string stored at <code>key</code>. The
     * offsets <code>start</code> and <code>end</code> are zero-based indexes, with <code>0</code>
     * being the first element of the list, <code>1</code> being the next element and so on. These
     * offsets can also be negative numbers indicating offsets starting at the end of the list, with
     * <code>-1</code> being the last element of the list, <code>-2</code> being the penultimate, and
     * so on.
     *
     * @see <a href="https://redis.io/commands/bitcount/">redis.io</a> for details.
     * @param key The key for the string to count the set bits of.
     * @param start The starting offset byte index.
     * @param end The ending offset byte index.
     * @return The number of set bits in the string byte interval specified by <code>start</code> and
     *     <code>end</code>. Returns zero if the key is missing as it is treated as an empty string.
     * @example
     *     <pre>{@code
     * Long payload = client.bitcount("myKey1", 1, 3).get();
     * assert payload == 2L; // The second to fourth bytes of the string stored at "myKey1" contains 2 set bits.
     * }</pre>
     */
    CompletableFuture<Long> bitcount(String key, long start, long end);

    /**
     * Counts the number of set bits (population counting) in a string stored at <code>key</code>. The
     * offsets <code>start</code> and <code>end</code> are zero-based indexes, with <code>0</code>
     * being the first element of the list, <code>1</code> being the next element and so on. These
     * offsets can also be negative numbers indicating offsets starting at the end of the list, with
     * <code>-1</code> being the last element of the list, <code>-2</code> being the penultimate, and
     * so on.
     *
     * @since Redis 7.0 and above
     * @see <a href="https://redis.io/commands/bitcount/">redis.io</a> for details.
     * @param key The key for the string to count the set bits of.
     * @param start The starting offset.
     * @param end The ending offset.
     * @param options The index offset type. Could be either {@link BitmapIndexType#BIT} or {@link
     *     BitmapIndexType#BYTE}.
     * @return The number of set bits in the string interval specified by <code>start</code>, <code>
     *     end</code>, and <code>options</code>. Returns zero if the key is missing as it is treated
     *     as an empty string.
     * @example
     *     <pre>{@code
     * Long payload = client.bitcount("myKey1", 1, 1, BIT).get();
     * assert payload == 1L; // Indicates that the second bit of the string stored at "myKey1" is set.
     * }</pre>
     */
    CompletableFuture<Long> bitcount(String key, long start, long end, BitmapIndexType options);

    /**
     * Return the position of the first bit matching the given <code>bit</code> value.
     *
     * @see <a href="https://redis.io/commands/bitpos/">redis.io</a> for details.
     * @param key The key of the string.
     * @param bit The bit value to match.
     * @return The position of the first occurrence matching <code>bit</code> in the binary value of
     *     the string held at <code>key</code>. If <code>bit</code> is not found, a <code>-1</code> is
     *     returned.
     * @example
     *     <pre>{@code
     * Long payload = client.bitpos("myKey1", 0).get();
     * assert payload == 3L; // Indicates that the first occurrence of a 0 bit value of the string stored at "myKey1" is at the fourth position.
     * }</pre>
     */
    CompletableFuture<Long> bitpos(String key, long bit);

    /**
     * Return the position of the first bit matching the given <code>bit</code> value. The offsets
     * <code>start</code> is a zero-based index, with <code>0</code> being the first byte of the list,
     * <code>1</code> being the next byte and so on. These offsets can also be negative numbers
     * indicating offsets starting at the end of the list, with <code>-1</code> being the last byte of
     * the list, <code>-2</code> being the penultimate, and so on.
     *
     * @see <a href="https://redis.io/commands/bitpos/">redis.io</a> for details.
     * @param key The key of the string.
     * @param bit The bit value to match.
     * @param start The starting offset.
     * @return The position of the first occurrence beginning at the <code>start</code> offset of the
     *     <code>bit</code> in the binary value of the string held at <code>key</code>.
     * @example
     *     <pre>{@code
     * Long payload = client.bitpos("myKey1", 1, 4).get();
     * assert payload == 9L; // Indicates that the first occurrence of a 1 bit value starting from fifth byte of the string stored at "myKey1" is at the tenth position.
     * }</pre>
     */
    CompletableFuture<Long> bitpos(String key, long bit, long start);

    /**
     * Return the position of the first bit matching the given <code>bit</code> value. The offsets
     * <code>start</code> and <code>end</code> are zero-based indexes, with <code>0</code> being the
     * first byte of the list, <code>1</code> being the next byte and so on. These offsets can also be
     * negative numbers indicating offsets starting at the end of the list, with <code>-1</code> being
     * the last byte of the list, <code>-2</code> being the penultimate, and so on.
     *
     * @see <a href="https://redis.io/commands/bitpos/">redis.io</a> for details.
     * @param key The key of the string.
     * @param bit The bit value to match.
     * @param start The starting offset.
     * @param end The ending offset.
     * @return The position of the first occurrence from the <code>start</code> to the <code>end
     *     </code> offsets of the <code>bit</code> in the binary value of the string held at <code>key
     *     </code>.
     * @example
     *     <pre>{@code
     * Long payload = client.bitpos("myKey1", 1, 4, 6).get();
     * assert payload == 7L;// Indicates that the first occurrence of a 1 bit value starting from fifth to the sixth bytes of the string stored at "myKey1" is at the 8th position.
     * }</pre>
     */
    CompletableFuture<Long> bitpos(String key, long bit, long start, long end);

    /**
     * Return the position of the first bit matching the given <code>bit</code> value. The offset
     * <code>offsetType</code> specifies whether the offset is a BIT or BYTE. If BIT is specified,
     * <code>start==0</code> and <code>end==2</code> means to look at the first three bits. If BYTE is
     * specified, <code>start==0</code> and <code>end==2</code> means to look at the first three bytes
     * The offsets are zero-based indexes, with <code>0</code> being the first element of the list,
     * <code>1</code> being the next, and so on. These offsets can also be negative numbers indicating
     * offsets starting at the end of the list, with <code>-1</code> being the last element of the
     * list, <code>-2</code> being the penultimate, and so on.
     *
     * @since Redis 7.0 and above.
     * @see <a href="https://redis.io/commands/bitpos/">redis.io</a> for details.
     * @param key The key of the string.
     * @param bit The bit value to match.
     * @param start The starting offset.
     * @param end The ending offset.
     * @param offsetType The index offset type. Could be either {@link BitmapIndexType#BIT} or {@link
     *     BitmapIndexType#BYTE}.
     * @return The position of the first occurrence from the <code>start</code> to the <code>end
     *     </code> offsets of the <code>bit</code> in the binary value of the string held at <code>key
     *     </code>.
     * @example
     *     <pre>{@code
     * Long payload = client.bitpos("myKey1", 1, 4, 6, BIT).get();
     * assert payload == 7L;// Indicates that the first occurrence of a 1 bit value starting from fifth to the sixth bits of the string stored at "myKey1" is at the 8th position.
     * }</pre>
     */
    CompletableFuture<Long> bitpos(
            String key, long bit, long start, long end, BitmapIndexType offsetType);
}
