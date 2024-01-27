package glide.api.models.commands;

import glide.api.commands.ServerCommands;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Singular;

/** Object builder to add optional arguments to {@link ServerCommands#info(InfoOptions)} */
@Builder
public class InfoOptions {

    @Singular private final List<Section> sections;

    public enum Section {
        /** SERVER: General information about the Redis server */
        SERVER,
        /** CLIENTS: Client connections section */
        CLIENTS,
        /** MEMORY: Memory consumption related information */
        MEMORY,
        /** PERSISTENCE: RDB and AOF related information */
        PERSISTENCE,
        /** STATS: General statistics */
        STATS,
        /** REPLICATION: Master/replica replication information */
        REPLICATION,
        /** CPU: CPU consumption statistics */
        CPU,
        /** COMMANDSTATS: Redis command statistics */
        COMMANDSTATS,
        /** LATENCYSTATS: Redis command latency percentile distribution statistics */
        LATENCYSTATS,
        /** SENTINEL: Redis Sentinel section (only applicable to Sentinel instances) */
        SENTINEL,
        /** CLUSTER: Redis Cluster section */
        CLUSTER,
        /** MODULES: Modules section */
        MODULES,
        /** KEYSPACE: Database related statistics */
        KEYSPACE,
        /** ERRORSTATS: Redis error statistics */
        ERRORSTATS,
        /** ALL: Return all sections (excluding module generated ones) */
        ALL,
        /** DEFAULT: Return only the default set of sections */
        DEFAULT,
        /** EVERYTHING: Includes all and modules */
        EVERYTHING,
    }

    /**
     * Converts options enum into a String[] to add to a {@link glide.api.models.Command}
     *
     * @return String[]
     */
    public String[] toInfoOptions() {
        List<String> optionArgs = sections.stream().map(Objects::toString).collect(Collectors.toList());
        return optionArgs.toArray(new String[0]);
    }
}
