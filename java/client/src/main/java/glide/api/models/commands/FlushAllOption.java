/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import glide.api.RedisClient;
import glide.api.RedisClusterClient;
import glide.api.models.configuration.RequestRoutingConfiguration.Route;

/**
 * Defines flushing mode for <code>FLUSHALL</code> command implemented by {@link
 * RedisClient#flushall(FlushAllOption)}, {@link RedisClusterClient#flushall(FlushAllOption)}, and
 * {@link RedisClusterClient#flushall(FlushAllOption, Route)}.
 *
 * @see <a href="https://redis.io/commands/flushall/">redis.io</a>
 */
public enum FlushAllOption {
    /** Flushes the databases synchronously. */
    SYNC,
    /** Flushes the databases asynchronously. */
    ASYNC
}
