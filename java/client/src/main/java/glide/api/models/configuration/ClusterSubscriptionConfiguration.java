/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.configuration;

import glide.api.RedisClusterClient;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Subscription configuration for {@link RedisClusterClient}.
 *
 * @example
 *     <pre>{@code
 * // Configuration with 3 subscriptions and a callback:
 * ClusterSubscriptionConfiguration subscriptionConfiguration =
 *     ClusterSubscriptionConfiguration.builder()
 *         .subscription(EXACT, "notifications")
 *         .subscription(EXACT, "news")
 *         .subscription(SHARDED, "data")
 *         .callback(callback)
 *         .build();
 * // Now it could be supplied to `RedisClusterClientConfiguration`:
 * RedisClusterClientConfiguration clientConfiguration =
 *     RedisClusterClientConfiguration.builder()
 *         .address(NodeAddress.builder().port(6379).build())
 *         .subscriptionConfiguration(subscriptionConfiguration)
 *         .build();
 * }</pre>
 */
@Getter
@SuperBuilder
public final class ClusterSubscriptionConfiguration extends BaseSubscriptionConfiguration {

    /**
     * Describes subscription modes for cluster client.
     *
     * @see <a href="https://valkey.io/docs/topics/pubsub/">redis.io</a> for details.
     */
    public enum PubSubClusterChannelMode implements ChannelMode {
        /** Use exact channel names. */
        EXACT,
        /** Use glob-style channel name patterns. */
        PATTERN,
        /** Use sharded pubsub. */
        SHARDED,
    }

    /**
     * PubSub subscriptions to be used for the client.<br>
     * Will be applied via <code>SUBSCRIBE</code>/<code>PSUBSCRIBE</code>/<code>SSUBSCRIBE</code>
     * commands during connection establishment.
     */
    private final Map<PubSubClusterChannelMode, Set<String>> subscriptions;

    // all code below is a `SuperBuilder` extension to provide user-friendly
    // API `subscription` to add a single subscription
    private ClusterSubscriptionConfiguration(BaseSubscriptionConfigurationBuilder<?, ?> b) {
        super(b);
        this.subscriptions = ((ClusterSubscriptionConfigurationBuilder<?, ?>) b).subscriptions;
    }

    /** Builder for {@link ClusterSubscriptionConfiguration}. */
    public abstract static class ClusterSubscriptionConfigurationBuilder<
                    C extends ClusterSubscriptionConfiguration,
                    B extends ClusterSubscriptionConfigurationBuilder<C, B>>
            extends BaseSubscriptionConfigurationBuilder<C, B> {

        private Map<PubSubClusterChannelMode, Set<String>> subscriptions = new HashMap<>(3);

        /**
         * Add a subscription to a channel or to multiple channels if {@link
         * PubSubClusterChannelMode#PATTERN} is used.<br>
         * See {@link #subscriptions}.
         */
        public B subscription(PubSubClusterChannelMode mode, String channelOrPattern) {
            addSubscription(subscriptions, mode, channelOrPattern);
            return self();
        }
    }
}
