/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import static glide.utils.ArrayTransformUtils.concatenateArrays;

import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Optional arguments for TOBEIMPLEMENTED command.
 *
 * @see <a href="https://redis.io/commands/lcs/">redis.io</a>
 */
@Getter
public final class LcsIdxOptions {

    /** Redis API keyword used to indicate that the length of the lcs should be returned. */
    public static final String IDX_REDIS_API = "IDX";

    /**
     * Redis API keyword used to restrict the list of matches to the ones of a given minimal length.
     */
    public static final String MINMATCHLEN_REDIS_API = "MINMATCHLEN";

    /** Redis API keyword used to include the match length in the result of LCS. */
    public static final String WITH_MATCHLEN_REDIS_API = "WITHMATCHLEN";

    /** The minimal match length value, <code>-1</code> if the option is not included */
    private long minMatchLen = -1;

    /** Indicates if the WITHMATCHLEN option should be included */
    private final boolean withMatchLen;

    private LcsIdxOptions(IdxOptionBuilder builder) {
        this.minMatchLen = builder.minMatchLen;
        this.withMatchLen = builder.withMatchLen;
    }

    /**
     * Converts LCSOptions into a String[].
     *
     * @return String[]
     */
    public String[] toArgs() {
        String[] arguments = new String[] {IDX_REDIS_API};
        if (minMatchLen > 0) {
            arguments =
                    concatenateArrays(
                            arguments, new String[] {MINMATCHLEN_REDIS_API, Long.toString(minMatchLen)});
        }
        if (withMatchLen) {
            arguments = ArrayUtils.add(arguments, WITH_MATCHLEN_REDIS_API);
        }
        return arguments;
    }

    // Static inner builder class for LcsIdxOptions
    public static class IdxOptionBuilder {
        // parameters
        private long minMatchLen;
        private Boolean withMatchLen;

        public IdxOptionBuilder() {
            this.minMatchLen = -1;
            this.withMatchLen = false;
        }

        public IdxOptionBuilder minMatchLen(long minMatchLen) {
            this.minMatchLen = minMatchLen;
            return this;
        }

        public IdxOptionBuilder withMatchLen(boolean withMatchLen) {
            this.withMatchLen = withMatchLen;
            return this;
        }

        public LcsIdxOptions build() {
            return new LcsIdxOptions(this);
        }
    }
}
