/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import glide.api.commands.BitmapBaseCommands;
import lombok.RequiredArgsConstructor;

/**
 * Optional arguments for {@link BitmapBaseCommands#bitcount(String, long, long, BitcountOptions)}.
 *
 * @see <a href="https://redis.io/commands/bitcount/">redis.io</a>
 */
@RequiredArgsConstructor
public enum BitcountOptions {
    /** By default, the additional arguments start and end specify a byte index */
    BYTE("BYTE"),
    /** Specifies a bit index */
    BIT("BIT");

    private final String redisApi;

    /**
     * Converts BitcountOptions into a String[].
     *
     * @return String[]
     */
    public String[] toArgs() {
        return new String[] {this.redisApi};
    }
}
