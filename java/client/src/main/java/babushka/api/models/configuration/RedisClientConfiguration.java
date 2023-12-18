package babushka.api.models.configuration;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RedisClientConfiguration {

  String host;
  int port;
  boolean isTls;
  boolean clusterMode;
}
