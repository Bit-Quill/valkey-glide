/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import glide.api.commands.BitmapBaseCommands;
import lombok.RequiredArgsConstructor;

/**
 * Optional arguments for {@link BitmapBaseCommands#bitcount(String, long, long, BitcountOptions)}.
 * Specifies if start and end arguments are BYTE indices or BIT indices
 *
 * @see <a href="https://redis.io/commands/bitcount/">redis.io</a>
 */
@RequiredArgsConstructor
public enum BitcountOptions {
    /** Specifies a byte index * */
    BYTE("BYTE"),
    /** Specifies a bit index */
    BIT("BIT");

    private final String redisApi;
}
