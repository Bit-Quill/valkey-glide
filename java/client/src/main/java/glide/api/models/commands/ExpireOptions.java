/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import lombok.RequiredArgsConstructor;

/**
 * Optional arguments for {@link glide.api.commands.GenericBaseCommands#expire(String, long,
 * ExpireOptions)}, and similar commands.
 *
 * @see <a href="https://redis.io/commands/expire/">redis.io</a>
 */
@RequiredArgsConstructor
public enum ExpireOptions {
    /** <code>HAS_NO_EXPIRY</code> - Sets expiry only when the key has no expiry. */
    HAS_NO_EXPIRY("NX"),
    /** <code>HAS_EXISTING_EXPIRY</code> - Sets expiry only when the key has an existing expiry. */
    HAS_EXISTING_EXPIRY("XX"),
    /**
     * <code>NEW_EXPIRY_GREATER_THAN_CURRENT</code> - Sets expiry only when the new expiry is greater
     * than current one.
     */
    NEW_EXPIRY_GREATER_THAN_CURRENT("GT"),
    /**
     * <code>NEW_EXPIRY_LESS_THAN_CURRENT</code> - Sets expiry only when the new expiry is less than
     * current one.
     */
    NEW_EXPIRY_LESS_THAN_CURRENT("LT");

    private final String redisApi;

    /**
     * Converts ExpireOptions into a String[].
     *
     * @return String[]
     */
    public String[] toArgs() {
        return new String[] {this.redisApi};
    }
}
