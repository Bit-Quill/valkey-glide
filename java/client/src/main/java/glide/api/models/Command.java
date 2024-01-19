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

        /** */
        PING,

        /** SERVER MANAGEMENT COMMANDS * */

        /** */
        INFO,

        /** STRING COMMANDS * */

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
