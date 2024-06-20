/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.configuration;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Represents the configuration settings for a Cluster Redis client. Notes: Currently, the
 * reconnection strategy in cluster mode is not configurable, and exponential backoff with fixed
 * values is used.
 */
// TODO add example
@SuperBuilder
@Getter
public class RedisClusterClientConfiguration extends BaseClientConfiguration {

    /** Subscription configuration for the current client. */
    private final ClusterSubscriptionConfiguration subscriptionConfiguration;
}
