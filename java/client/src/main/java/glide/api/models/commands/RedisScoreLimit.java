/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

public class RedisScoreLimit {
    public interface ScoreLimit {
        String toArg();
    }

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

    public static class ScoreBoundary implements ScoreLimit {
        private final double bound;
        private final boolean isInclusive;

        public ScoreBoundary(double bound, boolean isInclusive) {
            this.bound = bound;
            this.isInclusive = isInclusive;
        }

        @Override
        public String toArg() {
            return this.isInclusive ? String.valueOf(this.bound) : "(" + this.bound;
        }
    }
}
