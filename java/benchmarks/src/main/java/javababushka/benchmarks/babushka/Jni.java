package javababushka.benchmarks.babushka;

import java.nio.file.Paths;
import lombok.Builder;

public class Jni {
  static {
    System.setProperty(
        "java.library.path",
        Paths.get(System.getProperty("user.dir"), "target", "debug").toString());
    System.loadLibrary("javababushka");
  }

  public static native long init_client(int a);

  public static native BabushkaResult test(long ptr, String address);

  public static native BabushkaResult connect(long ptr, String address);

  public static native BabushkaResult set(long ptr, String name, String value);

  public static native BabushkaResult get(long ptr, String name);

  public static native BabushkaResult from_str(String str);

  public static native BabushkaResult from_number(long num);

  public static native BabushkaResult from_ok();

  public static native BabushkaResult nnew(String str, int resultType, String string, long number);

  public static enum ResultType {
    STR, // * const c_char
    NUM, // i64
    NIL,
    DATA, // Vec<u8>
    BULK, // Vec<Value>
    OK,
    ERR;

    public static ResultType of(int ordinal) {
      switch (ordinal) {
        case 0:
          return STR;
        case 1:
          return NUM;
        case 2:
          return NIL;
        case 3:
          return DATA;
        case 4:
          return BULK;
        case 5:
          return OK;
        default:
          return ERR;
      }
    }
  }

  @Builder
  public static class BabushkaResult {
    public String error = null;
    public ResultType resultType;
    public String string = null;
    public long number = 0;

    public BabushkaResult(String error, ResultType resultType, String string, long number) {
      this.error = error;
      this.resultType = resultType;
      this.string = string;
      this.number = number;
    }

    public BabushkaResult(String error, int resultType, String string, long number) {
      this(error, ResultType.of(resultType), string, number);
    }

    // private BabushkaResult(){}

    public static BabushkaResult from_str(String str) {
      return BabushkaResult.builder().string(str).resultType(ResultType.STR).build();
    }

    public static BabushkaResult from_empty_str() {
      return BabushkaResult.builder().resultType(ResultType.STR).build();
    }

    public static BabushkaResult from_number(long num) {
      return BabushkaResult.builder().number(num).resultType(ResultType.NUM).build();
    }

    public static BabushkaResult from_error(String err) {
      return BabushkaResult.builder().error(err).resultType(ResultType.ERR).build();
    }

    public static BabushkaResult from_ok() {
      return BabushkaResult.builder().resultType(ResultType.OK).build();
    }

    public static BabushkaResult from_nil() {
      return BabushkaResult.builder().resultType(ResultType.NIL).build();
    }
  }

  public static void main(String[] args) {
    var t = BabushkaResult.from_empty_str();

    var ptr = init_client(42);
    var a = test(ptr, "12334");
    System.out.printf("Test1: %s%n", a.string);
    var b = test(ptr, "abadfgdg");
    System.out.printf("Test2: %s%n", b.string);

    var c = connect(ptr, "redis://localhost:6379");
    System.out.printf("Connect: str = %s, err = %s, type = %s%n", c.string, c.error, c.resultType);

    var set = set(ptr, "JNI value", "JJJ");
    System.out.printf(
        "Set: str = %s, err = %s, type = %s%n", set.string, set.error, set.resultType);

    var get1 = get(ptr, "JNI value");
    System.out.printf(
        "Get1: str = %s, err = %s, type = %s%n", get1.string, get1.error, get1.resultType);

    var get2 = get(ptr, "JJJ");
    System.out.printf(
        "Get2: str = %s, err = %s, type = %s%n", get2.string, get2.error, get2.resultType);

    int d = 5;
  }
}
