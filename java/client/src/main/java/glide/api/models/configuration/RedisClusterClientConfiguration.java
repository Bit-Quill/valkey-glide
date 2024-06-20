/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.configuration;

import glide.api.models.configuration.SubscriptionConfiguration.PubSubClusterChannelMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Represents the configuration settings for a Cluster Redis client. Notes: Currently, the
 * reconnection strategy in cluster mode is not configurable, and exponential backoff with fixed
 * values is used.
 */
@SuperBuilder
@Getter
public class RedisClusterClientConfiguration extends BaseClientConfiguration {
    /**
     * PubSub subscriptions to be used for the client.<br>
     * Will be applied via <code>SUBSCRIBE</code>/<code>PSUBSCRIBE</code>/<code>SSUBSCRIBE</code>
     * commands during connection establishment.
     */
    private final Map<PubSubClusterChannelMode, Set<String>> subscriptions;

    // some lombok dark magic - needed for `addSubscription` method
    public abstract static class RedisClusterClientConfigurationBuilder<
                    C extends RedisClusterClientConfiguration,
                    B extends RedisClusterClientConfigurationBuilder<C, B>>
            extends BaseClientConfigurationBuilder<C, B> {
        /**
         * Add a subscription to a channel or to multiple channels if {@link
         * PubSubClusterChannelMode#PATTERN} is used.
         */
        public B addSubscription(PubSubClusterChannelMode mode, String channelOrPattern) {
            if (subscriptions == null) {
                subscriptions = new HashMap<>(3);
            }
            if (!subscriptions.containsKey(mode)) {
                subscriptions.put(mode, new HashSet<>());
            }
            subscriptions.get(mode).add(channelOrPattern);
            return self();
        }

        // make this private to remove from API - the method isn't very user-friendly
        private B subscriptions(Map<PubSubClusterChannelMode, Set<String>> subscriptions) {
            return self();
        }
    }
}
