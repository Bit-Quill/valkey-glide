package babushka.api.config;

import lombok.Builder;

/** Represents the address and port of a node in the cluster. */
@Builder
public class NodeAddress {
  @Builder.Default private String host = "localhost";

  @Builder.Default private int port = 6379;
}
