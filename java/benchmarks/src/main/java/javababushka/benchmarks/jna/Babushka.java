package javababushka.benchmarks.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import javababushka.benchmarks.SyncClient;
import javababushka.benchmarks.utils.ConnectionSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Babushka implements SyncClient {

  private final long ptr = lib.init_client0(42);

  // = enum
  public static enum ResultType {
    Str(0),
    Int(1),
    Nil(2),
    Data(3),
    Bulk(4),
    Ok(5),
    Err(6),
    Undef(-1);

    private int id;
    ResultType(int id) {
      this.id = id;
    }

    public static ResultType of(int val) {
      switch (val) {
        case 0: return ResultType.Str;
        case 1: return ResultType.Int;
        case 2: return ResultType.Nil;
        case 3: return ResultType.Data;
        case 4: return ResultType.Bulk;
        case 5: return ResultType.Ok;
        case 6: return ResultType.Err;
        default: return ResultType.Undef;
      }
    }
  }

  public interface RustLib extends Library {

    @Structure.FieldOrder({"error", "value_type", "string", "num"})
    public static class BabushkaResult extends Structure {
      public BabushkaResult() {
        error = null;
        value_type = 0;
        string = null;
        num = 0;
      }
      public static class ByValue extends BabushkaResult implements Structure.ByValue { }
      public String error = null;
      public int value_type = 0;
      public String string = null;
      public long num = 0;
    };

    public long init_client0(int data);
    public BabushkaResult.ByValue connect0(long client, String address);
    public BabushkaResult.ByValue set0(long client, String key, String value);
    public BabushkaResult.ByValue get0(long client, String key);
  }

  private static final RustLib lib;
  static {
    var is_win = System.getProperty("os.name").contains("Windows");
    var targetDir = Paths.get("jna-stuff", "build", "resources", "main", is_win ? "win32-x86-64" : "linux-x86-64").toAbsolutePath();

    //System.setProperty("jna.debug_load", "true");
    System.setProperty("jna.library.path", targetDir.toString());

    var created = targetDir.toFile().mkdirs();
    try {
      if (is_win) {
        Files.copy(
            Paths.get(System.getProperty("user.dir"), "target", "debug", "javababushka.dll"),
            Paths.get(targetDir.toString(), "javababushka.dll"),
            StandardCopyOption.REPLACE_EXISTING);
      } else {
        Files.copy(
            Paths.get(System.getProperty("user.dir"), "..", "target", "debug", "libjavababushka.so"),
            Paths.get(targetDir.toString(), "libjavababushka.so"),
            StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      System.out.printf("Failed to copy lib: %s%n", e.getMessage());
      e.printStackTrace();
    }

    lib = Native.load("javababushka", RustLib.class);
  }

  @Override
  public void connectToRedis() {
    connectToRedis(new ConnectionSettings("localhost", 6379, false));
  }

  @Override
  public void connectToRedis(ConnectionSettings connectionSettings) {
    var connStr = String.format(
        "%s://%s:%d",
        connectionSettings.useSsl ? "rediss" : "redis",
        connectionSettings.host,
        connectionSettings.port);
    lib.connect0(ptr, connStr);
  }

  @Override
  public String getName() {
    return "JNA babushka";
  }

  @Override
  public void set(String key, String value) {

  }

  @Override
  public String get(String key) {
    var res = lib.get0(ptr, key);
    if (res.value_type == ResultType.Str.id) {
      return res.string;
    }
    return res.error;
  }
}
