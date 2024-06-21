/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import glide.api.commands.StringBaseCommands;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

/**
 * Optional arguments to {@link StringBaseCommands#lcsIdx(String, String, LcsOptions)}
 *
 * @see <a href="https://valkey.io/commands/lcs/">valkey.io</a>.
 */
@Builder
public final class LcsOptions {
    /** <code>IDX</code> option string to include in the <code>LCS</code> command. */
    public static final String IDX_COMMAND_STRING = "IDX";

    /** <code>MINMATCHLEN</code> option string to include in the <code>LCS</code> command. */
    public static final String MINMATCHLEN_COMMAND_STRING = "MINMATCHLEN";

    /** <code>WITHMATCHLEN</code> option string to include in the <code>LCS</code> command. */
    public static final String WITHMATCHLEN_COMMAND_STRING = "WITHMATCHLEN";

    /** Minimum length of matches to include in the result. */
    private final Long minMatchLen;

    /** Will include match lengths in the result if set to <code>true</code>. */
    private boolean isWithMatchLen;

    /** Sets <code>isWithMatchLen</code> to <code>true</code>. */
    public static class LcsOptionsBuilder {
        /** Sets <code>isWithMatchLen</code> to <code>true</code>. */
        public LcsOptionsBuilder withMatchLen() {
            return isWithMatchLen(true);
        }
    }

    /**
     * Converts LcsOptions into a String[].
     *
     * @return String[]
     */
    public String[] toArgs() {
        List<String> optionArgs = new ArrayList<>();

        if (minMatchLen != null) {
            optionArgs.add(MINMATCHLEN_COMMAND_STRING);
            optionArgs.add(minMatchLen.toString());
        }

        if (isWithMatchLen) {
            optionArgs.add(WITHMATCHLEN_COMMAND_STRING);
        }

        return optionArgs.toArray(new String[0]);
    }
}
