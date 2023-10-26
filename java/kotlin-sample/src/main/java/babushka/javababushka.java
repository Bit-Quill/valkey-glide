package babushka;

import static babushka.KotlibbabushkaKt.staticFunction;
import static babushka.KotlibbabushkaKt.staticFunctionWhichThrows;

public class javababushka {

  public static void main(String[] args) {

    var d = staticFunction();

    try {
      var c = staticFunctionWhichThrows();
    } catch (BabushkaException e) {
      throw new RuntimeException(e);
    }

    var b = new BabushkaClient().classFunction();
    var a = 42;

    System.out.println("Before babushka");
    var babushka = new BabushkaClient();
    System.out.println("after babushka");
    BabushkaClientData data = null;

    try {
      data = babushka.connect("redis://127.0.0.1:6379");
      System.out.println("connected");
    } catch (BabushkaRedisException e) {
      System.out.printf("Failed to connect: %s%n", e.getMessage());
      e.printStackTrace();
    }

    try {
      var missing = babushka.get(data, "keyadsf");
      System.out.printf("get missing: %s %n", missing);
    } catch (BabushkaRedisException e) {
      System.out.printf("failed to get3 missing: %s%n", e.getMessage());
      e.printStackTrace();
    }

    try {
      babushka.set(data, "key", "val");
      System.out.println("set");
    } catch (BabushkaRedisException e) {
      System.out.printf("failed to set: %s%n", e.getMessage());
      e.printStackTrace();
    }

    try {
      var value = babushka.get(data, "key");
      System.out.printf("get non missing: %s %n", value);
    } catch (BabushkaRedisException e) {
      System.out.printf("failed to get value: %s%n", e.getMessage());
      e.printStackTrace();
    }

    try {
      var value = babushka.get(data, "keyuuuu");
      System.out.printf("get missing: %s %n", value);
    } catch (BabushkaRedisException e) {
      System.out.printf("failed to get value: %s%n", e.getMessage());
      e.printStackTrace();
    }
  }
}
