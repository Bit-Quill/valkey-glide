package javababushka.client;

public class RedisClient {
  public static native void startSocketListenerExternal(RedisClient callback);

  public static native String startSocketListenerExternal() throws Exception;

  public static native Object valueFromPointer(long pointer);

  static {
    System.loadLibrary("javababushka");
  }

  public String socketPath;

  public void startSocketListener(RedisClient client) {
    client.startSocketListenerExternal(client);
  }

  public void initCallback(String socketPath, String errorMessage) throws Exception {
    if (errorMessage != null) {
      throw new Exception("Failed to initialize the socket connection: " + errorMessage);
    } else if (socketPath == null) {
      throw new Exception("Received null as the socketPath");
    } else {
      this.socketPath = socketPath;
    }
  }
}
