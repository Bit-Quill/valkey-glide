package glide.api.models.exceptions;

/** Encapsulated an error returned from the Redis or during processing of a Redis request */
public class RedisException extends RuntimeException {

  public RedisException() {
    super();
  }

  public RedisException(String message) {
    super(message);
  }

  public RedisException(Throwable cause) {
    super(cause);
  }

  public RedisException(String message, Throwable cause) {
    super(message, cause);
  }
}
