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
  public static class ResultType {
    public static final int Str = 0;
    public static final int Int = 1;
    public static final int Nil = 2;
    public static final int Data = 3;
    public static final int Bulk = 4;
    public static final int Ok = 5;
    public static final int Err = 6;
    public static final int Undef = -1;
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

    @Structure.FieldOrder({"str", "num"/*, "data", "bulk"*/})
    public static class BabushkaValue extends Structure {
      public BabushkaValue() {
        str = null;
        num = 0;
      }
      //public static class ByReference extends BabushkaValue implements Structure.ByReference {
        //public ByReference() { }
      //}
      public static class ByValue extends BabushkaValue implements Structure.ByValue {
        //public ByValue() { }
      }
      public String str;
      public long num;
//      List<Byte> data;
//      List<BabushkaValue> bulk;
    };

    @Structure.FieldOrder({"error", "value_type", "value"})
    public static class BabushkaResult extends Structure {
      public BabushkaResult() {
        error = null;
        value_type = 0;
        value = null;
      }
      public static class ByValue extends BabushkaResult implements Structure.ByValue { }
      public String error = null;
      public byte value_type = 0;
      //public BabushkaValue.ByReference value;
      public BabushkaValue.ByValue value = null;
    };
  }

  public static void main(String [] args) {
    var targetDir = Paths.get("jna-stuff", "build", "resources", "main", "win32-x86-64").toAbsolutePath();

    //System.setProperty("jna.debug_load", "true");
    System.setProperty("jna.library.path", targetDir.toString());

    var created = targetDir.toFile().mkdirs();
    try {
      Files.copy(
          Paths.get(System.getProperty("user.dir"), "target", "debug", "javababushka.dll"),
          Paths.get(targetDir.toString(), "javababushka.dll"),
          StandardCopyOption.REPLACE_EXISTING);
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
  }
}
