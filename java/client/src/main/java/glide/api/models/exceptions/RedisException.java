package glide.api.models.exceptions;

/** TODO: does this extend RuntimeException? */
public class RedisException extends RuntimeException {

  public RedisException() { super(); }

  public RedisException(String message) { super(message); }

  public RedisException(Throwable cause) {
    super(cause);
  }

  public RedisException(String message, Throwable cause) {
    super(message, cause);
  }
}
