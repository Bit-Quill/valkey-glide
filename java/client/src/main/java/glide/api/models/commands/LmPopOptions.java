/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import glide.api.commands.ListBaseCommands;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration representing element pop direction and count for the {@link ListBaseCommands#lmpop}
 * command.
 */
@RequiredArgsConstructor
@Getter
public enum LmPopOptions {
    /** Represents the option that elements shoot be pop from the left side of a list */
    LEFT("LEFT"),
    /** Represents the option that elements shoot be pop from the right side of a list */
    RIGHT("RIGHT"),
    /** Represents the COUNT indicator that comes before the count argument */
    COUNT("COUNT");

    private final String redisApi;
}
