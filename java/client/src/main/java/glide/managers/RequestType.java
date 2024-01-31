package glide.managers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import redis_request.RedisRequestOuterClass;

/**
 * Typing used to map client command calls to protobuf request types
 */
@AllArgsConstructor
@Getter
public enum RequestType {
    CUSTOM_COMMAND(RedisRequestOuterClass.RequestType.CustomCommand),
    PING(RedisRequestOuterClass.RequestType.Ping),
    INFO(RedisRequestOuterClass.RequestType.Info),
    GET_STRING(RedisRequestOuterClass.RequestType.GetString),
    SET_STRING(RedisRequestOuterClass.RequestType.SetString);

    private final RedisRequestOuterClass.RequestType protobufMapping;
}
