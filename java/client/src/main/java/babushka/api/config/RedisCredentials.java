package babushka.api.config;

import lombok.Builder;
import lombok.NonNull;

/** Represents the credentials for connecting to a Redis server. */
@Builder
public class RedisCredentials {
  /** The password that will be used for authenticating connections to the Redis servers. */
  @NonNull private String password;

  /**
   * The username that will be used for authenticating connections to the Redis servers. If not
   * supplied, "default" will be used.
   */
  private String username;
}
