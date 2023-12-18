package babushka.api.models.configuration;

import lombok.Builder;
import lombok.Getter;

/** TODO: Describe client configuration */
@Builder
@Getter
public class RedisClientConfiguration {

  String host;
  int port;
  boolean isTls;
  boolean clusterMode;
}
