package glide.managers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import redis_request.RedisRequestOuterClass;

@AllArgsConstructor
@Getter
public enum RequestType {
    /** Call a custom command with list of string arguments */
    CUSTOM_COMMAND(RedisRequestOuterClass.RequestType.CustomCommand),
    /**
     * Ping the Redis server.
     *
     * @see <hred=https://redis.io/commands/ping/>command reference</a>
     */
    PING(RedisRequestOuterClass.RequestType.Ping),
    /**
     * Get information and statistics about the Redis server.
     *
     * @see <hred=https://redis.io/commands/info/>command reference</a>
     */
    INFO(RedisRequestOuterClass.RequestType.Info),
    /**
     * Get the value of key.
     *
     * @see: <href=https://redis.io/commands/get/>command reference</a>
     */
    GET_STRING(RedisRequestOuterClass.RequestType.GetString),
    /**
     * Set key to hold the string value.
     *
     * @see: <href=https://redis.io/commands/set/>command reference</a>
     */
    SET_STRING(RedisRequestOuterClass.RequestType.SetString);

    private final RedisRequestOuterClass.RequestType protobufMapping;
}
