package glide.api.models;

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

    /** List of Arguments for the Redis command request */
    @Builder.Default final String[] arguments = new String[] {};

    public enum RequestType {
        /** Call a custom command with list of string arguments */
        CUSTOM_COMMAND,

        /** CONNECTION MANAGEMENT COMMANDS * */

        /**
         * Ping the Redis server.
         *
         * @see <a href="https://redis.io/commands/ping/">redis.io</a> for details.
         */
        PING,

        /** SERVER MANAGEMENT COMMANDS * */

        /**
         * Get information and statistics about the Redis server
         *
         * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
         */
        INFO,

        /** STRING COMMANDS * */

        /**
         * Get the value associated with the given key, or null if no such value exists.
         *
         * @see <a href="https://redis.io/commands/get/">redis.io</a> for details.
         */
        GET_STRING,
        /**
         * Set the given key with the given value.
         *
         * @see <a href="https://redis.io/commands/set/">redis.io</a> for details.
         */
        SET_STRING,
    }
}
