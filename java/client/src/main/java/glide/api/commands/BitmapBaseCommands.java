/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import glide.api.models.commands.bitmap.BitmapIndexType;
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
     * Sets or clears the bit at <code>offset</code> in the string value stored at <code>key</code>.
     * The <code>offset</code> is a zero-based index, with <code>0</code> being the first element of
     * the list, <code>1</code> being the next element, and so on. The <code>offset</code> must be
     * less than <code>2^32</code> and greater than or equal to <code>0</code>. If a key is
     * non-existent then the bit at <code>offset</code> is set to <code>value</code> and the preceding
     * bits are set to <code>0</code>.
     *
     * @see <a href="https://redis.io/commands/setbit/">redis.io</a> for details.
     * @param key The key of the string.
     * @param offset The index of the bit to be set.
     * @param value The bit value to set at <code>offset</code>. The value must be <code>0</code> or
     *     <code>1</code>.
     * @return The bit value that was previously stored at <code>offset</code>.
     * @example
     *     <pre>{@code
     * Long payload = client.setbit("myKey1", 1, 1).get();
     * assert payload == 0L; // The second bit value was 0 before setting to 1.
     * }</pre>
     */
    CompletableFuture<Long> setbit(String key, long offset, long value);

    /**
     * Returns the bit value at <code>offset</code> in the string value stored at <code>key</code>.
     * <code>offset</code> should be greater than or equal to zero.
     *
     * @see <a href="https://redis.io/commands/getbit/">redis.io</a> for details.
     * @param key The key of the string.
     * @param offset The index of the bit to return.
     * @return The bit at offset of the string. Returns zero if the key is empty or if the positive
     *     <code>offset</code> exceeds the length of the string.
     * @example
     *     <pre>{@code
     * client.set("sampleKey", "A"); // "A" has binary value 01000001
     * Long payload = client.getbit("sampleKey", 1).get();
     * assert payload == 1L; // The second bit for string stored at "sampleKey" is set to 1.
     * }</pre>
     */
    CompletableFuture<Long> getbit(String key, long offset);

    /**
     * Returns the position of the first bit matching the given <code>bit</code> value.
     *
     * @see <a href="https://redis.io/commands/bitpos/">redis.io</a> for details.
     * @param key The key of the string.
     * @param bit The bit value to match. The value must be <code>0</code> or <code>1</code>.
     * @return The position of the first occurrence matching <code>bit</code> in the binary value of
     *     the string held at <code>key</code>. If <code>bit</code> is not found, a <code>-1</code> is
     *     returned.
     * @example
     *     <pre>{@code
     * Long payload = client.bitpos("myKey1", 0).get();
     * // Indicates that the first occurrence of a 0 bit value is the fourth bit of the binary value
     * // of the string stored at "myKey1".
     * assert payload == 3L;
     * }</pre>
     */
    CompletableFuture<Long> bitpos(String key, long bit);

    /**
     * Returns the position of the first bit matching the given <code>bit</code> value. The offset
     * <code>start</code> is a zero-based index, with <code>0</code> being the first byte of the list,
     * <code>1</code> being the next byte and so on. These offsets can also be negative numbers
     * indicating offsets starting at the end of the list, with <code>-1</code> being the last byte of
     * the list, <code>-2</code> being the penultimate, and so on.
     *
     * @see <a href="https://redis.io/commands/bitpos/">redis.io</a> for details.
     * @param key The key of the string.
     * @param bit The bit value to match. The value must be <code>0</code> or <code>1</code>.
     * @param start The starting offset.
     * @return The position of the first occurrence beginning at the <code>start</code> offset of the
     *     <code>bit</code> in the binary value of the string held at <code>key</code>. If <code>bit
     *     </code> is not found, a <code>-1</code> is returned.
     * @example
     *     <pre>{@code
     * Long payload = client.bitpos("myKey1", 1, 4).get();
     * // Indicates that the first occurrence of a 1 bit value starting from fifth byte is the 34th
     * // bit of the binary value of the string stored at "myKey1".
     * assert payload == 33L;
     * }</pre>
     */
    CompletableFuture<Long> bitpos(String key, long bit, long start);

    /**
     * Returns the position of the first bit matching the given <code>bit</code> value. The offsets
     * <code>start</code> and <code>end</code> are zero-based indexes, with <code>0</code> being the
     * first byte of the list, <code>1</code> being the next byte and so on. These offsets can also be
     * negative numbers indicating offsets starting at the end of the list, with <code>-1</code> being
     * the last byte of the list, <code>-2</code> being the penultimate, and so on.
     *
     * @see <a href="https://redis.io/commands/bitpos/">redis.io</a> for details.
     * @param key The key of the string.
     * @param bit The bit value to match. The value must be <code>0</code> or <code>1</code>.
     * @param start The starting offset.
     * @param end The ending offset.
     * @return The position of the first occurrence from the <code>start</code> to the <code>end
     *     </code> offsets of the <code>bit</code> in the binary value of the string held at <code>key
     *     </code>. If <code>bit</code> is not found, a <code>-1</code> is returned.
     * @example
     *     <pre>{@code
     * Long payload = client.bitpos("myKey1", 1, 4, 6).get();
     * // Indicates that the first occurrence of a 1 bit value starting from the fifth to seventh
     * // bytes is the 34th bit of the binary value of the string stored at "myKey1".
     * assert payload == 33L;
     * }</pre>
     */
    CompletableFuture<Long> bitpos(String key, long bit, long start, long end);

    /**
     * Returns the position of the first bit matching the given <code>bit</code> value. The offset
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
     * @param bit The bit value to match. The value must be <code>0</code> or <code>1</code>.
     * @param start The starting offset.
     * @param end The ending offset.
     * @param offsetType The index offset type. Could be either {@link BitmapIndexType#BIT} or {@link
     *     BitmapIndexType#BYTE}.
     * @return The position of the first occurrence from the <code>start</code> to the <code>end
     *     </code> offsets of the <code>bit</code> in the binary value of the string held at <code>key
     *     </code>. If <code>bit</code> is not found, a <code>-1</code> is returned.
     * @example
     *     <pre>{@code
     * Long payload = client.bitpos("myKey1", 1, 4, 6, BIT).get();
     * // Indicates that the first occurrence of a 1 bit value starting from the fifth to seventh
     * // bits is the sixth bit of the binary value of the string stored at "myKey1".
     * assert payload == 5L;
     * }</pre>
     */
    CompletableFuture<Long> bitpos(
            String key, long bit, long start, long end, BitmapIndexType offsetType);
}