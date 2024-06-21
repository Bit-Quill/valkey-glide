/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

@Builder
public final class LcsOptions {
    public static final String IDX_COMMAND_STRING = "IDX";
    public static final String MINMATCHLEN_COMMAND_STRING = "MINMATCHLEN";
    public static final String WITHMATCHLEN_COMMAND_STRING = "WITHMATCHLEN";
    private final Long minMatchLen;
    private boolean isWithMatchLen;

    public static class LcsOptionsBuilder {

        /** If the stream doesn't exist, this creates a new stream with a length of <code>0</code>. */
        public LcsOptionsBuilder withMatchLen() {
            return isWithMatchLen(true);
        }
    }

    public String[] toArgs() {
        List<String> optionArgs = new ArrayList<>();

        if (minMatchLen != null) {
            optionArgs.add(WITHMATCHLEN_COMMAND_STRING);
            optionArgs.add(minMatchLen.toString());
        }

        if (isWithMatchLen) {
            optionArgs.add(WITHMATCHLEN_COMMAND_STRING);
        }

        return optionArgs.toArray(new String[0]);
    }
}
