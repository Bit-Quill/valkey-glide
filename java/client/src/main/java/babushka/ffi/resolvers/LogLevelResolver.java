package babushka.ffi.resolvers;

public class LogLevelResolver {
  static {
    System.loadLibrary("javababushka");
  }

  public static native void setLogLevel(int level);
}
