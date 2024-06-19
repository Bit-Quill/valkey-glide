/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import glide.api.commands.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.*;

/**
 * Optional arguments to {@link GenericBaseCommands#restore(byte[], long, byte[], RestoreOptions)}
 *
 * @see <a href="https://valkey.io/commands/restore/">valkey.io</a>
 */
@Getter
@Builder
public final class RestoreOptions {
    /** <code>REPLACE</code> subcommand string to replace existing key */
    public static final String REPLACE_REDIS_API = "REPLACE";

    /**
     * <code>ABSTTL</code> subcommand string to represent absolute timestamp (in milliseconds) for TTL
     */
    public static final String ABSTTL_REDIS_API = "ABSTTL";

    /** <code>IDELTIME</code> subcommand string to set Object Idletime */
    public static final String IDLETIME_REDIS_API = "IDLETIME";

    /** <code>FREQ</code> subcommand string to set Object Frequency */
    public static final String FREQ_REDIS_API = "FREQ";

    /** When `true`, it represents <code>REPLACE</code> keyword has been used */
    private final boolean hasReplace;

    /** When `true`, it represents <code>ABSTTL</code> keyword has been used */
    private final boolean hasAbsttl;

    /** It represents the idletime of object */
    private final Optional<Long> idletime;

    /** It represents the frequency of object */
    private final Optional<Long> frequency;

    /**
     * Creates the argument to be used in {@link GenericBaseCommands#restore(byte[], long, byte[],
     * RestoreOptions)}
     *
     * @return a byte array that holds the sub commands and their arguments.
     */
    public List<byte[]> toArgs(byte[] key, long ttl, byte[] value) {
        List<byte[]> resultList = new ArrayList<>();

        resultList.add(key);
        resultList.add(Long.toString(ttl).getBytes());
        resultList.add(value);

        if (hasReplace) {
            resultList.add(REPLACE_REDIS_API.getBytes());
        }

        if (hasAbsttl) {
            resultList.add(ABSTTL_REDIS_API.getBytes());
        }

        if (idletime != null) {
            idletime.ifPresent(
                    sec -> {
                        resultList.add(IDLETIME_REDIS_API.getBytes());
                        resultList.add(Long.toString(sec).getBytes());
                    });
        }

        if (frequency != null) {
            frequency.ifPresent(
                    freq -> {
                        resultList.add(FREQ_REDIS_API.getBytes());
                        resultList.add(Long.toString(freq).getBytes());
                    });
        }

        return resultList;
    }

    // Custom setter methods for long values
    public static class RestoreOptionsBuilder {
        public RestoreOptionsBuilder idletime(long idletime) {
            this.idletime = idletime > 0 ? Optional.of(idletime) : Optional.empty();
            return this;
        }

        public RestoreOptionsBuilder frequency(long frequency) {
            this.frequency = frequency > 0 ? Optional.of(frequency) : Optional.empty();
            return this;
        }
    }
}
