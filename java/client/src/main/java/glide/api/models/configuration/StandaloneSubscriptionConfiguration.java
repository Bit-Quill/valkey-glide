/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

// TODO doc and example
@Getter
public class StandaloneSubscriptionConfiguration
        extends BaseSubscriptionConfiguration<StandaloneSubscriptionConfiguration> {

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
    protected final Map<PubSubChannelMode, Set<String>> subscriptions = new HashMap<>(3);

    /**
     * Add a subscription to a channel or to multiple channels if {@link PubSubChannelMode#PATTERN} is
     * used.<br>
     * See {@link #subscriptions}.
     */
    public StandaloneSubscriptionConfiguration addSubscription(
            PubSubChannelMode mode, String channelOrPattern) {
        addSubscription(subscriptions, mode, channelOrPattern);
        return this;
    }
}
