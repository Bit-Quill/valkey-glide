/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Optional arguments to {@link SortedSetCommands#zadd(String, Map, ZaddOptions, boolean)
 * SortedSetCommands.zadd(String, Map&lt;String, Double&gt;, ZaddOptions, boolean)} and {@link
 * SortedSetCommands#zaddIncr(String, String, Double, ZaddOptions)}
 *
 * @see <a href="https://redis.io/commands/zadd/">redis.io</a>
 */
@Builder
public final class ZaddOptions {
    private final ConditionalChange conditionalChange;

    private final UpdateOptions updateOptions;

    @RequiredArgsConstructor
    @Getter
    public enum ConditionalChange {
        /**
         * Only update elements that already exist. Don't add new elements. Equivalent to `XX` in the
         * Redis API.
         */
        ONLY_IF_EXISTS("XX"),
        /**
         * Only add new elements. Don't update already existing elements. Equivalent to `NX` in the
         * Redis API.
         */
        ONLY_IF_DOES_NOT_EXIST("NX");

        private final String redisApi;
    }

    @RequiredArgsConstructor
    @Getter
    public enum UpdateOptions {
        /**
         * Only update existing elements if the new score is less than the current score. Equivalent to
         * `LT` in the Redis API.
         */
        SCORE_LESS_THAN_CURRENT("LT"),
        /**
         * Only update existing elements if the new score is greater than the current score. Equivalent
         * to `GT` in the Redis API.
         */
        SCORE_GREATER_THAN_CURRENT("GT");

        private final String redisApi;
    }

    /**
     * Converts ZaddOptions into a String[] to add to a {@link Command} arguments.
     *
     * @return String[]
     */
    public String[] toArgs() {
        if (conditionalChange == ConditionalChange.ONLY_IF_DOES_NOT_EXIST && updateOptions != null) {
            throw new IllegalArgumentException(
                    "The GT, LT, and NX options are mutually exclusive. Cannot choose both "
                            + updateOptions.redisApi
                            + " and NX.");
        }

        List<String> optionArgs = new ArrayList<>();

        if (conditionalChange != null) {
            optionArgs.add(conditionalChange.redisApi);
        }

        if (updateOptions != null) {
            optionArgs.add(updateOptions.redisApi);
        }

        return optionArgs.toArray(new String[0]);
    }
}
