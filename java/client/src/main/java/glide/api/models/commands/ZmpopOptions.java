/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import glide.api.commands.SortedSetBaseCommands;

// TODO add @link to ZMPOP when implemented
/**
 * Mandatory option for {@link SortedSetBaseCommands#bzmpop(double, String[], ScoreModifier)} and
 * for {@link SortedSetBaseCommands#bzmpop(double, String[], ScoreModifier, long)}.
 */
public interface ZmpopOptions {
    /** Defines which elements to pop from the sorted set. */
    enum ScoreModifier {
        /** Pop elements with the lowest scores. */
        MIN,
        /** Pop elements with the highest scores. */
        MAX
    }
}
