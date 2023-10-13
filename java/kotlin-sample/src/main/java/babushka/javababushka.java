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
  }
}
