package glide.ffi.resolvers;

<<<<<<<< HEAD:java/client/src/main/java/glide/ffi/resolvers/GlideCoreNativeDefinitions.java
public class GlideCoreNativeDefinitions {
  public static native String startSocketListenerExternal() throws Exception;
========
public class SocketListenerResolver {
>>>>>>>> 337bcd7 (Managers):java/client/src/main/java/glide/ffi/resolvers/SocketListenerResolver.java

  /** Make an FFI call to Babushka to open a UDS socket to connect to. */
  private static native String startSocketListener() throws Exception;

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
      return startSocketListener();
    } catch (Exception | UnsatisfiedLinkError e) {
      System.err.printf("Failed to create a UDS connection: %s%n%n", e);
      throw new RuntimeException(e);
    }
  }
}
