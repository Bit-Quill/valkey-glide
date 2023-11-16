package javababushka;

public class RustWrapper {
  public static native String startSocketListenerExternal() throws Exception;

  public static native Object valueFromPointer(long pointer);

  static {
    System.loadLibrary("javababushka");
  }
}
