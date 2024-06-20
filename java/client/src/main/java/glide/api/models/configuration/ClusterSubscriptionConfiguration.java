/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

// TODO doc
@Getter
public class ClusterSubscriptionConfiguration
        extends BaseSubscriptionConfiguration<ClusterSubscriptionConfiguration> {

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
    protected final Map<PubSubClusterChannelMode, Set<String>> subscriptions = new HashMap<>();

    /**
     * Add a subscription to a channel or to multiple channels if {@link
     * PubSubClusterChannelMode#PATTERN} is used.<br>
     * See {@link #subscriptions}.
     */
    public ClusterSubscriptionConfiguration addSubscription(
            PubSubClusterChannelMode mode, String channelOrPattern) {
        addSubscription(subscriptions, mode, channelOrPattern);
        return this;
    }
}
