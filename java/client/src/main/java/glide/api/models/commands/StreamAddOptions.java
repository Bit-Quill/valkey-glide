/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import glide.api.commands.StreamBaseCommands;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;

/**
 * Optional arguments to {@link StreamBaseCommands#xadd}
 *
 * @see <a href="https://redis.io/commands/xadd/">redis.io</a>
 */
@Builder
public final class StreamAddOptions {

    public static final String NO_MAKE_STREAM_REDIS_API = "NOMKSTREAM";
    public static final String ID_WILDCARD_REDIS_API = "*";
    public static final String TRIM_MAXLEN_REDIS_API = "MAXLEN";
    public static final String TRIM_MINID_REDIS_API = "MINID";
    public static final String TRIM_EXACT_REDIS_API = "=";
    public static final String TRIM_NOT_EXACT_REDIS_API = "~";
    public static final String TRIM_LIMIT_REDIS_API = "LIMIT";

    /** If set, the new entry will be added with this <code>id</code>. */
    private final String id;

    /**
     * If set to <code>false</code>, a new stream won't be created if no stream matches the given key.
     * <br>
     * Equivalent to <code>NOMKSTREAM</code> in the Redis API.
     */
    private final Boolean makeStream;

    /** If set, the add operation will also trim the older entries in the stream. */
    private final StreamTrimOptions trim;

    public abstract static class StreamTrimOptions {
        /**
         * If <code>true</code>, the stream will be trimmed exactly. Equivalent to <code>=</code> in the Redis API. Otherwise,
         * the stream will be trimmed in a near-exact manner, which is more efficient, equivalent to <code>~</code>
         * in the Redis API.
         */
        protected Boolean exact;

        /** If set, sets the maximal amount of entries that will be deleted. */
        protected Long limit;

        protected abstract String getMethod();

        protected abstract String getThreshold();

        public List<String> addTrimOptions() {
            List<String> optionArgs = new ArrayList<>();

            optionArgs.add(this.getMethod());
            optionArgs.add(this.exact ? TRIM_EXACT_REDIS_API : TRIM_NOT_EXACT_REDIS_API);
            optionArgs.add(this.getThreshold());

            if (this.limit != null) {
                optionArgs.add(TRIM_LIMIT_REDIS_API);
                optionArgs.add(this.limit.toString());
            }

            return optionArgs;
        }
    }

    public static class MinId extends StreamTrimOptions {
        /** Trim the stream according to entry ID. Equivalent to <code>MINID</code> in the Redis API. */
        private final String threshold;

        public MinId(boolean exact, @NonNull String threshold) {
            this.threshold = threshold;
            this.exact = exact;
        }

        public MinId(boolean exact, @NonNull String threshold, long limit) {
            this.threshold = threshold;
            this.exact = exact;
            this.limit = limit;
        }

        public String getMethod() {
            return TRIM_MINID_REDIS_API;
        }

        public String getThreshold() {
            return threshold;
        }
    }

    public static class Maxlen extends StreamTrimOptions {
        /**
         * Trim the stream according to length. <br>
         * Equivalent to <code>MAXLEN</code> in the Redis API.
         */
        private final Long threshold;

        public Maxlen(boolean exact, long threshold) {
            this.threshold = threshold;
            this.exact = exact;
        }

        public Maxlen(boolean exact, long threshold, long limit) {
            this.threshold = threshold;
            this.exact = exact;
            this.limit = limit;
        }

        public String getMethod() {
            return TRIM_MAXLEN_REDIS_API;
        }

        public String getThreshold() {
            return threshold.toString();
        }
    }

    /**
     * Converts options for Xadd into a String[].
     *
     * @return String[]
     */
    public String[] toArgs() {
        List<String> optionArgs = new ArrayList<>();

        if (makeStream != null && !makeStream) {
            optionArgs.add(NO_MAKE_STREAM_REDIS_API);
        }

        if (trim != null) {
            optionArgs.addAll(trim.addTrimOptions());
        }

        if (id != null) {
            optionArgs.add(id);
        } else {
            optionArgs.add(ID_WILDCARD_REDIS_API);
        }

        return optionArgs.toArray(new String[0]);
    }
}
