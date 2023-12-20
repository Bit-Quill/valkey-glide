package glide.ffi.resolvers;

<<<<<<<< HEAD:java/client/src/main/java/glide/ffi/resolvers/GlideCoreNativeDefinitions.java
<<<<<<<< HEAD:java/client/src/main/java/glide/ffi/resolvers/GlideCoreNativeDefinitions.java
public class GlideCoreNativeDefinitions {
  public static native String startSocketListenerExternal() throws Exception;
========
public class SocketListenerResolver {
>>>>>>>> 337bcd7 (Managers):java/client/src/main/java/glide/ffi/resolvers/SocketListenerResolver.java
========
public class BabushkaCoreNativeDefinitions {
  public static native String startSocketListenerExternal() throws Exception;
>>>>>>>> 5d172a4 (Revert "Managers"):java/client/src/main/java/glide/ffi/resolvers/BabushkaCoreNativeDefinitions.java

  public static native Object valueFromPointer(long pointer);

  static {
    System.loadLibrary("glide-rs");
  }

  /**
   * Make an FFI call to obtain the socket path.
   *
   * @return A UDS path.
   */
  public static String getSocket() {
    try {
      return startSocketListenerExternal();
    } catch (Exception | UnsatisfiedLinkError e) {
      System.err.printf("Failed to create a UDS connection: %s%n%n", e);
      throw new RuntimeException(e);
    }
  }
}
