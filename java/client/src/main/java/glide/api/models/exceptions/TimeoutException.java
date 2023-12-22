package glide.api.models.exceptions;

public class TimeoutException extends RedisException {
  public TimeoutException(String message) {
    super(message);
  }
}
