/** Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands.stream;

import glide.api.commands.StreamBaseCommands;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

/**
 * Optional arguments to {@link StreamBaseCommands#xclaim(String, String, String, long, String[],
 * StreamClaimOptions)}
 *
 * @see <a href="https://redis.io/commands/xclaim/">redis.io</a>
 */
@Builder
public class StreamClaimOptions {

    /** Redis api string to designate IDLE time in milliseconds */
    public static final String IDLE_REDIS_API = "IDLE";

    /** Redis api string to designate TIME time in unix-milliseconds */
    public static final String TIME_REDIS_API = "TIME";

    /** Redis api string to designate RETRYCOUNT */
    public static final String RETRY_COUNT_REDIS_API = "RETRYCOUNT";

    /** Redis api string to designate FORCE */
    public static final String FORCE_REDIS_API = "FORCE";

    /** Redis api string to designate JUSTID */
    public static final String JUST_ID_REDIS_API = "JUSTID";

    /** Redis api string to designate LASTID */
    public static final String LAST_ID_REDIS_API = "LASTID";

    /**
     * Set the idle time (last time it was delivered) of the message. If <code>idle</code> is not
     * specified, an <code>idle</code> of <code>0</code> is assumed, that is, the time count is reset
     * because the message has now a new owner trying to process it.
     */
    private final Long idle; // in milliseconds

    /**
     * This is the same as idle but instead of a relative amount of milliseconds, it sets the idle
     * time to a specific Unix time (in milliseconds). This is useful in order to rewrite the AOF file
     * generating <code>XCLAIM</code> commands.
     */
    private final Long idleUnixTime; // in unix-time milliseconds

    /**
     * Set the retry counter to the specified value. This counter is incremented every time a message
     * is delivered again. Normally <code>XCLAIM</code> does not alter this counter, which is just
     * served to clients when the <code>XPENDING</code> command is called: this way clients can detect
     * anomalies, like messages that are never processed for some reason after a big number of
     * delivery attempts.
     */
    private final Long retryCount;

    /**
     * Creates the pending message entry in the PEL even if certain specified IDs are not already in
     * the PEL assigned to a different client. However, the message must exist in the stream,
     * otherwise the IDs of non-existing messages are ignored.
     */
    private final boolean isForce;

    /** Filter up to the <code>lastid</code> when claiming messages */
    private final String lastId;

    public static class StreamClaimOptionsBuilder {

        /**
         * Creates the pending message entry in the PEL even if certain specified IDs are not already in
         * the PEL assigned to a different client. However, the message must exist in the stream,
         * otherwise the IDs of non-existing messages are ignored.
         */
        public StreamClaimOptionsBuilder force() {
            return isForce(true);
        }
    }

    /**
     * Converts options for Xclaim into a String[].
     *
     * @return String[]
     */
    public String[] toArgs(boolean isJustId) {
        List<String> optionArgs = new ArrayList<>();

        if (idle != null) {
            optionArgs.add(IDLE_REDIS_API);
            optionArgs.add(Long.toString(idle));
        }

        if (idleUnixTime != null) {
            optionArgs.add(TIME_REDIS_API);
            optionArgs.add(Long.toString(idleUnixTime));
        }

        if (retryCount != null) {
            optionArgs.add(RETRY_COUNT_REDIS_API);
            optionArgs.add(Long.toString(retryCount));
        }

        if (isForce) {
            optionArgs.add(FORCE_REDIS_API);
        }

        if (isJustId) {
            optionArgs.add(JUST_ID_REDIS_API);
        }

        if (lastId != null) {
            optionArgs.add(LAST_ID_REDIS_API);
            optionArgs.add(lastId);
        }

        return optionArgs.toArray(new String[0]);
    }
}
