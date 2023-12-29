package glide.api.models.exceptions;

/**
 * Error returned from Redis client: request has timed out
 */
public class TimeoutException extends RedisException {
  public TimeoutException() {
    super();
  }

  public TimeoutException(String message) {
    super(message);
  }

  public TimeoutException(Throwable cause) {
    super(cause);
  }

  public TimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}
