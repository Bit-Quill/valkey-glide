package babushka.api.models.exceptions;

public class ConnectionException extends RedisException {
  public ConnectionException(String message) {
    super(message);
  }
}
