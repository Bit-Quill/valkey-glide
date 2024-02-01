/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.managers.models;

import glide.api.models.configuration.RequestRoutingConfiguration.Route;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/** Base Command class to send a single request to Redis. */
@Builder
@Getter
@EqualsAndHashCode
public class Command {

    /** Redis command request type */
    @NonNull final RequestType requestType;

    /** Request routing configuration */
    final Route route;

    /** List of Arguments for the Redis command request */
    @Builder.Default final String[] arguments = new String[] {};

    public enum RequestType {
        /** Call a custom command with list of string arguments */
        CUSTOM_COMMAND,
        /**
         * Ping the Redis server.
         *
         * @see <hred=https://redis.io/commands/ping/>command reference</a>
         */
        PING,
        /**
         * Get information and statistics about the Redis server.
         *
         * @see <hred=https://redis.io/commands/info/>command reference</a>
         */
        INFO,
        /**
         * Get the value of key.
         *
         * @see: <href=https://redis.io/commands/get/>command reference</a>
         */
        GET_STRING,
        /**
         * Set key to hold the string value.
         *
         * @see: <href=https://redis.io/commands/set/>command reference</a>
         */
        SET_STRING,
    }
}
