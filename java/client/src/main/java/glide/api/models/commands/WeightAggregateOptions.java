/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import static glide.utils.ArrayTransformUtils.concatenateArrays;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

public class WeightAggregateOptions {
    public static final String WEIGHTS_REDIS_API = "WEIGHTS";
    public static final String AGGREGATE_REDIS_API = "AGGREGATE";

    /**
     * Option for the method of aggregating scores from multiple sets. This option defaults to SUM if
     * not specified.
     */
    public enum Aggregate {
        /** Aggregates by summing the scores of each element across sets. */
        SUM,
        /** Aggregates by selecting the minimum score for each element across sets. */
        MIN,
        /** Aggregates by selecting the maximum score for each element across sets. */
        MAX;
    }

    /**
     * Basic interface. Please use one of the following implementations:
     *
     * <ul>
     *   <li>{@link KeyArray}
     *   <li>{@link WeightedKeys}
     * </ul>
     */
    public interface WeightableKeys {
        String[] toArgs();
    }

    public static class KeyArray implements WeightableKeys {
        private final String[] keys;

        /**
         * The keys of the sorted sets involved in the union operation.
         *
         * @param keys An array of keys.
         */
        public KeyArray(@NonNull String[] keys) {
            this.keys = keys;
        }

        @Override
        public String[] toArgs() {
            return concatenateArrays(new String[] {Integer.toString(keys.length)}, keys);
        }
    }

    public static class WeightedKeys implements WeightableKeys {
        private final Map<String, Double> keysWeights;

        /**
         * Constructs a new instance mapping sorted set keys to their multiplication factors. Each
         * factor is used to adjust the scores of elements in the corresponding sorted set by
         * multiplying them before their scores are aggregated.
         *
         * @param keysWeights Mapping of sorted set keys to their multiplication factors.
         */
        public WeightedKeys(@NonNull Map<String, Double> keysWeights) {
            this.keysWeights = keysWeights;
        }

        @Override
        public String[] toArgs() {
            List<String> keys = new ArrayList<>();
            List<Double> weights = new ArrayList<>();
            List<String> argumentsList = new ArrayList<>();

            for (Map.Entry<String, Double> entry : keysWeights.entrySet()) {
                keys.add(entry.getKey());
                weights.add(entry.getValue());
            }
            argumentsList.add(Integer.toString(keys.size()));
            argumentsList.addAll(keys);
            argumentsList.add(WEIGHTS_REDIS_API);
            for (Double weight : weights) {
                argumentsList.add(weight.toString());
            }

            return argumentsList.toArray(new String[0]);
        }
    }
}
