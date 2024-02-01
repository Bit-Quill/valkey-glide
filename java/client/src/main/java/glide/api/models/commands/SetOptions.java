package glide.api.models.commands;

import glide.api.commands.StringCommands;
import glide.api.models.exceptions.RequestException;
import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;

/**
 * Object builder to add optional arguments to {@link StringCommands#set(String, String,
 * SetOptions)}
 */
@Builder
public class SetOptions {

    /**
     * If <code>conditionalSet</code> is not set the value will be set regardless of prior value
     * existence. If value isn't set because of the condition, command will return <code>null</code>.
     */
    private ConditionalSet conditionalSet;

    /**
     * Set command returns the old string stored at <code>key</code>, or <code>null</code> if <code>
     * key</code> did not exist. An error is returned and <code>SET</code> aborted if the value stored
     * at <code>key
     * </code> is not a string. Equivalent to <code>GET</code> in the Redis API.
     */
    private boolean returnOldValue;

    /** If not set, no expiry time will be set for the value. */
    private TimeToLive expiry;

    public enum ConditionalSet {
        /**
         * Only set the key if it does not already exist. Equivalent to <code>NX</code> in the Redis
         * API.
         */
        ONLY_IF_EXISTS,
        /** Only set the key if it already exists. Equivalent to <code>EX</code> in the Redis API. */
        ONLY_IF_DOES_NOT_EXIST
    }

    @Builder
    public static class TimeToLive {
        /** Expiry type for the time to live */
        @NonNull private TimeToLiveType type;

        /**
         * The amount of time to live before the key expires. Ignored when {@link
         * TimeToLiveType#KEEP_EXISTING} type is set.
         */
        private Integer count;
    }

    public enum TimeToLiveType {
        /**
         * Retain the time to live associated with the key. Equivalent to <code>KEEPTTL</code> in the
         * Redis API.
         */
        KEEP_EXISTING,
        /**
         * Set the specified expire time, in seconds. Equivalent to <code>EX</code> in the Redis API.
         */
        SECONDS,
        /**
         * Set the specified expire time, in milliseconds. Equivalent to <code>PX</code> in the Redis
         * API.
         */
        MILLISECONDS,
        /**
         * Set the specified Unix time at which the key will expire, in seconds. Equivalent to <code>
         * EXAT</code> in the Redis API.
         */
        UNIX_SECONDS,
        /**
         * Set the specified Unix time at which the key will expire, in milliseconds. Equivalent to
         * <code>PXAT</code> in the Redis API.
         */
        UNIX_MILLISECONDS
    }

    public static String CONDITIONAL_SET_ONLY_IF_EXISTS = "XX";
    public static String CONDITIONAL_SET_ONLY_IF_DOES_NOT_EXIST = "NX";
    public static String RETURN_OLD_VALUE = "GET";
    public static String TIME_TO_LIVE_KEEP_EXISTING = "KEEPTTL";
    public static String TIME_TO_LIVE_SECONDS = "EX";
    public static String TIME_TO_LIVE_MILLISECONDS = "PX";
    public static String TIME_TO_LIVE_UNIX_SECONDS = "EXAT";
    public static String TIME_TO_LIVE_UNIX_MILLISECONDS = "PXAT";

    /**
     * Converts SetOptions into a String[]
     *
     * @return String[]
     */
    public String[] toArgs() {
        List<String> optionArgs = new LinkedList<>();
        if (conditionalSet != null) {
            if (conditionalSet == ConditionalSet.ONLY_IF_EXISTS) {
                optionArgs.add(CONDITIONAL_SET_ONLY_IF_EXISTS);
            } else if (conditionalSet == ConditionalSet.ONLY_IF_DOES_NOT_EXIST) {
                optionArgs.add(CONDITIONAL_SET_ONLY_IF_DOES_NOT_EXIST);
            }
        }

        if (returnOldValue) {
            optionArgs.add(RETURN_OLD_VALUE);
        }

        if (expiry != null) {
            if (expiry.type == TimeToLiveType.KEEP_EXISTING) {
                optionArgs.add(TIME_TO_LIVE_KEEP_EXISTING);
            } else {
                if (expiry.count == null) {
                    throw new RequestException(
                            "Set command received expiry type " + expiry.type + "but count was not set.");
                }
                switch (expiry.type) {
                    case SECONDS:
                        optionArgs.add(TIME_TO_LIVE_SECONDS);
                        break;
                    case MILLISECONDS:
                        optionArgs.add(TIME_TO_LIVE_MILLISECONDS);
                        break;
                    case UNIX_SECONDS:
                        optionArgs.add(TIME_TO_LIVE_UNIX_SECONDS);
                        break;
                    case UNIX_MILLISECONDS:
                        optionArgs.add(TIME_TO_LIVE_UNIX_MILLISECONDS);
                        break;
                }
                optionArgs.add(expiry.count.toString());
            }
        }

        return optionArgs.toArray(new String[0]);
    }
}
