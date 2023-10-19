import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class Loader {
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
  };

  public interface RustLib extends Library {

    public BabushkaResultStr.ByValue static_function_which_throws();
    public BabushkaResultStr.ByValue static_function4();

    //public BabushkaResult.ByValue static_function2_0();
    public BabushkaResult.ByValue static_function2_0();
    public BabushkaResult.ByValue static_function2_1();
    public BabushkaResult.ByValue static_function2_2();
    public BabushkaResult.ByValue static_function2_3();
    public BabushkaResult.ByValue static_function2_4();


    @Structure.FieldOrder({"error", "result"})
    public static class BabushkaResultStr extends Structure {
      public static class ByValue extends BabushkaResultStr implements Structure.ByValue { }
      public String error;
      public String result;
    }

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
      //public BabushkaValue.ByReference value;
      public String string = null;
      public long num = 0;
    };

    public static class BabushkaClient extends Structure {
      public static class ByValue extends BabushkaClient implements Structure.ByValue { }
      public static class ByReference extends BabushkaClient implements Structure.ByReference { }
    }

    public long init_client0(int data);
    public BabushkaResult.ByValue connect0(long client, String address);
    public BabushkaResult.ByValue test0(long client, String address);


    public BabushkaResult.ByValue set0(long client, String key, String value);

    public BabushkaResult.ByValue get0(long client, String key);


  }

  public static void main(String [] args) {
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


    //System.out.println("Working Directory = " + System.getProperty("jna.library.path"));

    var lib = (RustLib) Native.load(
            "javababushka",
            RustLib.class);
    var res = lib.static_function_which_throws();

    var res4 = lib.static_function4();

    var res2_0 = lib.static_function2_0();
    var res2_1 = lib.static_function2_1();
    var res2_2 = lib.static_function2_2();
    var res2_3 = lib.static_function2_3();
    var res2_4 = lib.static_function2_4();
    int a = 5;

    var bab = lib.init_client0(42);
    var t1 = lib.test0(bab, "pewpew");
    var t2 = lib.test0(bab, "ololo");
    var t3 = lib.test0(bab, "ikiki");










    System.out.println("Before connect");
    var con = lib.connect0(bab, "redis://127.0.0.1:6379");
    System.out.println("After connect");
    if (con.value_type == ResultType.Ok.id) {
      System.out.println("Connected");
    } else {
      System.out.printf("Conn failed: Res = %s, str = %s, err = %s%n", ResultType.of(con.value_type), con.string, con.error);
    }

    System.out.println("=======\nBefore set");
    var set = lib.set0(bab, "kkkey", "ololo");
    System.out.println("After set");
    if (set.value_type == ResultType.Ok.id) {
      System.out.println("Set ok");
    } else {
      System.out.printf("Set failed: Res = %s, str = %s, err = %s%n", ResultType.of(set.value_type), set.string, set.error);
    }

    System.out.println("=======\nBefore get");
    var get = lib.get0(bab, "key");
    System.out.println("After get");
    if (get.value_type == ResultType.Str.id) {
      System.out.printf("Get ok: %s%n", get.string);
    } else {
      System.out.printf("Set failed: Res = %s, str = %s, err = %s%n", ResultType.of(get.value_type), get.string, get.error);
    }

    int b = 5;
  }
}
