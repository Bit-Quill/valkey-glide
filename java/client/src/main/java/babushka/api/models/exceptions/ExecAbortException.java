package babushka.api.models.exceptions;

public class ExecAbortException extends RedisException {
  public ExecAbortException(String message) {
    super(message);
  }
}
