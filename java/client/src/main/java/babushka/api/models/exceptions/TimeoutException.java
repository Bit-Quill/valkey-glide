package babushka.api.models.exceptions;

public class TimeoutException extends RedisException {
  public TimeoutException(String message) {
    super(message);
  }
}
