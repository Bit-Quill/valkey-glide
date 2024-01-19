package glide.api.models.commands;

import glide.api.models.exceptions.RequestException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

@Builder
@NonNull
public class InfoOptions extends Options {

  @Singular List<Section> sections;

  public enum Section {
    /**
     * SERVER: General information about the Redis server
     */
    SERVER,
    /**
     * CLIENTS: Client connections section
     */
    CLIENTS,
    /**
     * MEMORY: Memory consumption related information
     */
    MEMORY,
    /**
     * PERSISTENCE: RDB and AOF related information
     */
    PERSISTENCE,
    /**
     * STATS: General statistics
     */
    STATS,
    /**
     * REPLICATION: Master/replica replication information
     */
    REPLICATION,
    /**
     * CPU: CPU consumption statistics
     */
    CPU,
    /**
     * COMMANDSTATS: Redis command statistics
     */
    COMMANDSTATS,
    /**
     * LATENCYSTATS: Redis command latency percentile distribution statistics
     */
    LATENCYSTATS,
    /**
     * SENTINEL: Redis Sentinel section (only applicable to Sentinel instances)
     */
    SENTINEL,
    /**
     * CLUSTER: Redis Cluster section
     */
    Cluster = "cluster",
    /**
     * MODULES: Modules section
     */
    Modules = "modules",
    /**
     * KEYSPACE: Database related statistics
     */
    Keyspace = "keyspace",
    /**
     * ERRORSTATS: Redis error statistics
     */
    Errorstats = "errorstats",
    /**
     * ALL: Return all sections (excluding module generated ones)
     */
    All = "all",
    /**
     * DEFAULT: Return only the default set of sections
     */
    Default = "default",
    /**
     * EVERYTHING: Includes all and modules
     */
    Everything = "everything",
  }

  public String[] toSetOptions() {
    optionArgs = sections.stream().map(Objects::toString).collect(Collectors.toList());
    return toArgs();
  }
}
