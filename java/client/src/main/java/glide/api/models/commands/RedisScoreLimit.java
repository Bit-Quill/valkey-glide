/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

public class RedisScoreLimit {
    public interface ScoreLimit {
        String toArg();
    }

    /** Enumeration representing numeric positive and negative infinity bounds for a sorted set. */
    public enum InfBound implements ScoreLimit {
        POSITIVE_INFINITY("+inf"),
        NEGATIVE_INFINITY("-inf");

        private final String representation;

        InfBound(String representation) {
            this.representation = representation;
        }

        @Override
        public String toArg() {
            return representation;
        }
    }

    /** Represents a specific numeric score boundary in a sorted set. */
    public static class ScoreBoundary implements ScoreLimit {
        /** The score value. */
        private final double bound;

        /** Whether the score value is inclusive. */
        private final boolean isInclusive;

        public ScoreBoundary(double bound, boolean isInclusive) {
            this.bound = bound;
            this.isInclusive = isInclusive;
        }

        public ScoreBoundary(double bound) {
            this.bound = bound;
            this.isInclusive = true;
        }

        @Override
        public String toArg() {
            return this.isInclusive ? String.valueOf(this.bound) : "(" + this.bound;
        }
    }
}
