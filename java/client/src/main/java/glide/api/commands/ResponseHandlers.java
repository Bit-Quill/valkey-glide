package glide.api.commands;

import glide.api.models.exceptions.RedisException;
import glide.ffi.resolvers.RedisValueResolver;
import glide.managers.BaseCommandResponseResolver;
import java.util.HashMap;
import response.ResponseOuterClass;

public class ResponseHandlers {
  /**
   * Extracts the response from the Protobuf response and either throws an exception or returns the
   * appropriate response has an Object
   *
   * @param response Redis protobuf message
   * @return Response Object
   */
  public static Object handleObjectResponse(ResponseOuterClass.Response response) {
    // return function to convert protobuf.Response into the response object by
    // calling valueFromPointer
    return (new BaseCommandResponseResolver(RedisValueResolver::valueFromPointer)).apply(response);
  }

  /**
   * Check for errors in the Response and return null Throws an error if an unexpected value is
   * returned
   *
   * @return null if the response is empty
   */
  public static Void handleVoidResponse(ResponseOuterClass.Response response) {
    // return function to convert protobuf.Response into the response object by
    // calling valueFromPointer
    Object value = handleObjectResponse(response);
    if (value == null) {
      return null;
    }
    throw new RedisException(
        "Unexpected return type from Redis: got " + value.getClass().getSimpleName() + " expected null");
  }

  /**
   * Extracts the response from the Protobuf response and either throws an exception or returns the
   * appropriate response has a String
   *
   * @param response Redis protobuf message
   * @return Response as a String
   */
  public static String handleStringResponse(ResponseOuterClass.Response response) {
    // return function to convert protobuf.Response into the response object by
    // calling valueFromPointer
    Object value = handleObjectResponse(response);
    if (value instanceof String) {
      return (String) value;
    }
    throw new RedisException(
        "Unexpected return type from Redis: got " + value.getClass().getSimpleName() + " expected String");
  }

  /**
   * Extracts the response from the Protobuf response and either throws an exception or returns the
   * appropriate response has a HashMap
   *
   * @param response Redis protobuf message
   * @return Response as a String
   */
  public static HashMap handleMapResponse(ResponseOuterClass.Response response) {
    // return function to convert protobuf.Response into the response object by
    // calling valueFromPointer
    Object value = handleObjectResponse(response);
    if (value instanceof HashMap) {
      return (HashMap) value;
    }
    throw new RedisException(
        "Unexpected return type from Redis: got " + value.getClass().getSimpleName() + " expected HashMap");
  }
}
