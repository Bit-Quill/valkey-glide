/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.configuration;

import glide.api.RedisClient;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Subscription configuration for {@link RedisClient}.
 *
 * @example
 *     <pre>{@code
 * // Configuration with 2 subscriptions, a callback, and a context:
 * StandaloneSubscriptionConfiguration subscriptionConfiguration =
 *     StandaloneSubscriptionConfiguration.builder()
 *         .subscription(EXACT, "notifications")
 *         .subscription(PATTERN, "news.*")
 *         .callback(callback, messageConsumer)
 *         .build();
 * // Now it could be supplied to `RedisClientConfiguration`:
 * RedisClientConfiguration clientConfiguration =
 *     RedisClientConfiguration.builder()
 *         .address(NodeAddress.builder().port(6379).build())
 *         .subscriptionConfiguration(subscriptionConfiguration)
 *         .build();
 * }</pre>
 */
@Getter
@SuperBuilder
public final class StandaloneSubscriptionConfiguration extends BaseSubscriptionConfiguration {

    /**
     * Describes subscription modes for standalone client.
     *
     * @see <a href="https://valkey.io/docs/topics/pubsub/">redis.io</a> for details.
     */
    public enum PubSubChannelMode implements ChannelMode {
        /** Use exact channel names. */
        EXACT,
        /** Use glob-style channel name patterns. */
        PATTERN,
    }

    /**
     * PubSub subscriptions to be used for the client.<br>
     * Will be applied via <code>SUBSCRIBE</code>/<code>PSUBSCRIBE</code> commands during connection
     * establishment.
     */
    private final Map<PubSubChannelMode, Set<String>> subscriptions;

    // all code below is a `SuperBuilder` extension to provide user-friendly
    // API `subscription` to add a single subscription
    private StandaloneSubscriptionConfiguration(BaseSubscriptionConfigurationBuilder<?, ?> b) {
        super(b);
        this.subscriptions = ((StandaloneSubscriptionConfigurationBuilder<?, ?>) b).subscriptions;
    }

    /** Builder for {@link StandaloneSubscriptionConfiguration}. */
    public abstract static class StandaloneSubscriptionConfigurationBuilder<
                    C extends StandaloneSubscriptionConfiguration,
                    B extends StandaloneSubscriptionConfigurationBuilder<C, B>>
            extends BaseSubscriptionConfigurationBuilder<C, B> {

        private Map<PubSubChannelMode, Set<String>> subscriptions = new HashMap<>(2);

        /**
         * Add a subscription to a channel or to multiple channels if {@link PubSubChannelMode#PATTERN}
         * is used.<br>
         * See {@link #subscriptions}.
         */
        public B subscription(PubSubChannelMode mode, String channelOrPattern) {
            addSubscription(subscriptions, mode, channelOrPattern);
            return self();
        }
    }
}
