package babushka.api.config;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/** Represents the address and port of a node in the cluster. */
@Getter
@Builder
public class NodeAddress {
  @NonNull @Builder.Default private final String host = "localhost";

  @NonNull @Builder.Default private final Integer port = 6379;
}
