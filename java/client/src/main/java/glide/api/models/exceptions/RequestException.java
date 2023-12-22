package glide.api.models.exceptions;

public class RequestException extends RedisException {
  public RequestException(String message) {
    super(message);
  }
}
