package babushka.api.config;

import lombok.experimental.SuperBuilder;

/** Represents the configuration settings for a Standalone Redis client. */
@SuperBuilder
public class RedisClientConfiguration extends BaseClientConfiguration {
  /** Strategy used to determine how and when to reconnect, in case of connection failures. */
  public BackoffStrategy reconnectStrategy;

  /** Index of the logical database to connect to. */
  public Integer databaseId;
}
