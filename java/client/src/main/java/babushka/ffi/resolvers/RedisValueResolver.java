package babushka.ffi.resolvers;

import response.ResponseOuterClass.Response;

public class RedisValueResolver {

  static {
    System.loadLibrary("javababushka");
  }

  /**
   * Resolve a value received from Redis using given C-style pointer.
   *
   * @param pointer A memory pointer from {@link Response}
   * @return A RESP3 value
   */
  public static native Object valueFromPointer(long pointer);
}
