/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import static glide.utils.ArrayTransformUtils.concatenateArrays;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

public class WeightAggregateOptions {
    public static final String WEIGHTS_REDIS_API = "WEIGHTS";
    public static final String AGGREGATE_REDIS_API = "AGGREGATE";

    /**
     * Option for the method of aggregating scores from multiple sets. This option defaults to SUM if
     * not specified.
     */
    @RequiredArgsConstructor
    public enum Aggregate {
        /** Aggregates by summing the scores of each element across sets. */
        SUM("SUM"),
        /** Aggregates by selecting the minimum score for each element across sets. */
        MIN("MIN"),
        /** Aggregates by selecting the maximum score for each element across sets. */
        MAX("MAX");

        private final String redisApi;

        public String[] toArgs() {
            return new String[] {AGGREGATE_REDIS_API, this.redisApi};
        }
    }

    /**
     * Basic interface. Please use one of the following implementations:
     *
     * <ul>
     *   <li>{@link KeyArray}
     *   <li>{@link WeightedKeys}
     * </ul>
     */
    public interface KeysOrWeightedKeys {
        String[] toArgs();
    }

    /** Represents the keys of the sorted sets involved in the aggregation operation. */
    @RequiredArgsConstructor
    public static class KeyArray implements KeysOrWeightedKeys {
        private final String[] keys;

        @Override
        public String[] toArgs() {
            return concatenateArrays(new String[] {Integer.toString(keys.length)}, keys);
        }
    }

    /**
     * Represents the mapping of sorted set keys to their multiplication factors. Each factor is used
     * to adjust the scores of elements in the corresponding sorted set by multiplying them before
     * their scores are aggregated.
     */
    @RequiredArgsConstructor
    public static class WeightedKeys implements KeysOrWeightedKeys {
        private final Map<String, Double> keysWeights;

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
