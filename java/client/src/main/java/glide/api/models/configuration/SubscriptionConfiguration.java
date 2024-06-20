/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.configuration;

public class SubscriptionConfiguration {

    /**
     * Describes subscription modes for standalone client.
     *
     * @see <a href="https://valkey.io/docs/topics/pubsub/">redis.io</a> for details.
     */
    public enum PubSubStandaloneChannelMode {
        /** Use exact channel names. */
        EXACT,
        /** Use glob-style channel name patterns. */
        PATTERN,
    }

    /**
     * Describes subscription modes for cluster client.
     *
     * @see <a href="https://valkey.io/docs/topics/pubsub/">redis.io</a> for details.
     */
    public enum PubSubClusterChannelMode {
        /** Use exact channel names. */
        EXACT,
        /** Use glob-style channel name patterns. */
        PATTERN,
        /** Use sharded pubsub. */
        SHARDED,
    }
}
