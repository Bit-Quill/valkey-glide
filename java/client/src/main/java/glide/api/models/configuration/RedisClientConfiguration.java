/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.configuration;

import glide.api.models.configuration.SubscriptionConfiguration.PubSubStandaloneChannelMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/** Represents the configuration settings for a Standalone Redis client. */
@Getter
@SuperBuilder
public class RedisClientConfiguration extends BaseClientConfiguration {
    /** Strategy used to determine how and when to reconnect, in case of connection failures. */
    private final BackoffStrategy reconnectStrategy;

    /** Index of the logical database to connect to. */
    private final Integer databaseId;

    /**
     * PubSub subscriptions to be used for the client.<br>
     * Will be applied via <code>SUBSCRIBE</code>/<code>PSUBSCRIBE</code> commands during connection
     * establishment.
     */
    private final Map<PubSubStandaloneChannelMode, Set<String>> subscriptions;

    // some lombok dark magic - needed for `addSubscription` method
    public abstract static class RedisClientConfigurationBuilder<
                    C extends RedisClientConfiguration, B extends RedisClientConfigurationBuilder<C, B>>
            extends BaseClientConfigurationBuilder<C, B> {
        /**
         * Add a subscription to a channel or to multiple channels if {@link
         * PubSubStandaloneChannelMode#PATTERN} is used.
         */
        public B addSubscription(PubSubStandaloneChannelMode mode, String channelOrPattern) {
            if (subscriptions == null) {
                subscriptions = new HashMap<>(2);
            }
            if (!subscriptions.containsKey(mode)) {
                subscriptions.put(mode, new HashSet<>());
            }
            subscriptions.get(mode).add(channelOrPattern);
            return self();
        }

        // make this private to remove from API - the method isn't very user-friendly
        private B subscriptions(Map<PubSubStandaloneChannelMode, Set<String>> subscriptions) {
            return self();
        }
    }
}
