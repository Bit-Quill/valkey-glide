package glide.api.commands;

import lombok.Builder;
import lombok.EqualsAndHashCode;

/** Base Command class to send a single request to Redis. */
@Builder
@EqualsAndHashCode
public class Command {

  /** Redis command request type */
  final RequestType requestType;

  /** List of Arguments for the Redis command request */
  final String[] arguments;

  public enum RequestType {
    /** Call a custom command with list of string arguments */
    CUSTOM_COMMAND,
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
