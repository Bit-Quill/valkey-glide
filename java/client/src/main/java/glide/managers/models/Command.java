package glide.managers.models;

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

        /**
         * Decrement the number stored at `key` by one. If the key does not exist, it is set to 0 before
         * performing the operation.
         *
         * @see <a href="https://redis.io/commands/decr/">redis.io</a> for details.
         */
        DECR,

        /**
         * Decrements the number stored at `key` by `amount`. If `key` does not exist, it is set to 0
         * before performing the operation. Sets the specified fields to their respective values in the
         * hash stored at `key`.
         *
         * @see <a href="https://redis.io/commands/decrby/">redis.io</a> for details.
         */
        DECR_BY,

        /**
         * Increments the number stored at `key` by one. If `key` does not exist, it is set to 0 before
         * performing the operation.
         *
         * @see <a href="https://redis.io/commands/incr/">redis.io</a> for details.
         */
        INCR,

        /**
         * Increments the number stored at `key` by `amount`. If `key` does not exist, it is set to 0
         * before performing the operation.
         *
         * @see <a href="https://redis.io/commands/incrby/">redis.io</a> for details.
         */
        INCR_BY,

        /**
         * Increment the string representing a floating point number stored at `key` by `amount`. By
         * using a negative increment value, the result is that the value stored at `key` is
         * decremented. If `key` does not exist, it is set to 0 before performing the operation.
         *
         * @see <a href="https://redis.io/commands/incrbyfloat/">redis.io</a> for details.
         */
        INCR_BY_FLOAT,

        /**
         * Retrieve the values of multiple keys.
         *
         * @see <a href="https://redis.io/commands/mget/">redis.io</a> for details.
         */
        MGET,

        /**
         * Set multiple keys to multiple values in a single operation.
         *
         * @see <a href="https://redis.io/commands/mset/">redis.io</a> for details.
         */
        MSET,
    }
}
