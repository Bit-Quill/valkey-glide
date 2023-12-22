package babushka.api.models.exceptions;

/** TODO: does this extend RuntimeException? */
public class RedisException extends RuntimeException {

  public RedisException(String message) {
    super(message);
  }
}
