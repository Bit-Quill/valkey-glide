package glide.managers.models;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;

/** Base Command class to send a single request to Redis. */
@Builder
@Getter
@EqualsAndHashCode
public class Command {

    /** Redis command request type */
    @NonNull final RequestType requestType;

    /** List of Arguments for the Redis command request */
    @Builder.Default final String[] arguments = new String[] {};

    /** Command to execute a single, custom command. */
    public static Command customCommand(String[] args) {
        return Command.builder()
                .requestType(Command.RequestType.CUSTOM_COMMAND)
                .arguments(args)
                .build();
    }

    /** Command to Ping the Redis server. */
    public static Command ping() {
        return Command.builder().requestType(Command.RequestType.PING).build();
    }

    /** Command to Ping the Redis server. */
    public static Command ping(String msg) {
        return Command.builder()
                .requestType(Command.RequestType.PING)
                .arguments(new String[] {msg})
                .build();
    }

    /** Command to get information and statistics about the Redis server. */
    public static Command info() {
        return Command.builder().requestType(Command.RequestType.INFO).build();
    }

    /** Command to get information and statistics about the Redis server. */
    public static Command info(String[] options) {
        return Command.builder().requestType(Command.RequestType.INFO).arguments(options).build();
    }

    /** Command to get the value associated with the given key. */
    public static Command get(String key) {
        return Command.builder()
                .requestType(Command.RequestType.GET_STRING)
                .arguments(new String[] {key})
                .build();
    }

    /** Command to set the given key with the given value. */
    public static Command set(String key, String value) {
        return Command.builder()
                .requestType(Command.RequestType.SET_STRING)
                .arguments(new String[] {key, value})
                .build();
    }

    /** Command to set the given key with the given value. */
    public static Command set(String key, String value, String[] options) {
        String[] args = ArrayUtils.addAll(new String[] {key, value}, options);
        Command cmd =
                Command.builder().requestType(Command.RequestType.SET_STRING).arguments(args).build();
        return cmd;
    }

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
