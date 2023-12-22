package glide.api.models.commands;

import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;

@Builder
@NonNull
public class SetOptions {

  /**
   * `onlyIfDoesNotExist` - Only set the key if it does not already exist. Equivalent to `NX` in the
   * Redis API. <br>
   * `onlyIfExists` - Only set the key if it already exist. Equivalent to `EX` in the Redis API.
   * <br>
   * if `conditional` is not set the value will be set regardless of prior value existence. <br>
   * If value isn't set because of the condition, return null.
   */
  private ConditionalSet conditionalSet;

  /**
   * Return the old string stored at key, or null if key did not exist. An error is returned and SET
   * aborted if the value stored at key is not a string. Equivalent to `GET` in the Redis API.
   */
  private boolean returnOldValue;

  /** If not set, no expiry time will be set for the value. */
  private TimeToLive expiry;

  public enum ConditionalSet {
    ONLY_IF_EXISTS,
    ONLY_IF_DOES_NOT_EXIST
  }

  @Builder
  public static class TimeToLive {
    private TimeToLiveType type;
    private int count;
  }

  public enum TimeToLiveType {
    /**
     * Retain the time to live associated with the key. Equivalent to `KEEPTTL` in the Redis API.
     */
    KEEP_EXISTING,
    /** Set the specified expire time, in seconds. Equivalent to `EX` in the Redis API. */
    SECONDS,
    /** Set the specified expire time, in milliseconds. Equivalent to `PX` in the Redis API. */
    MILLISECONDS,
    /**
     * Set the specified Unix time at which the key will expire, in seconds. Equivalent to `EXAT` in
     * the Redis API.
     */
    UNIX_SECONDS,
    /**
     * Set the specified Unix time at which the key will expire, in milliseconds. Equivalent to
     * `PXAT` in the Redis API.
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

  public static List createSetOptions(SetOptions options) {
    List optionArgs = new LinkedList();
    if (options.conditionalSet != null) {
      if (options.conditionalSet == ConditionalSet.ONLY_IF_EXISTS) {
        optionArgs.add(CONDITIONAL_SET_ONLY_IF_EXISTS);
      } else if (options.conditionalSet == ConditionalSet.ONLY_IF_DOES_NOT_EXIST) {
        optionArgs.add(CONDITIONAL_SET_ONLY_IF_DOES_NOT_EXIST);
      }
    }

    if (options.returnOldValue) {
      optionArgs.add(RETURN_OLD_VALUE);
    }

    if (options.expiry != null) {
      if (options.expiry.type == TimeToLiveType.KEEP_EXISTING) {
        optionArgs.add(TIME_TO_LIVE_KEEP_EXISTING);
      } else if (options.expiry.type == TimeToLiveType.SECONDS) {
        optionArgs.add(TIME_TO_LIVE_SECONDS + " " + options.expiry.count);
      } else if (options.expiry.type == TimeToLiveType.MILLISECONDS) {
        optionArgs.add(TIME_TO_LIVE_MILLISECONDS + " " + options.expiry.count);
      } else if (options.expiry.type == TimeToLiveType.UNIX_SECONDS) {
        optionArgs.add(TIME_TO_LIVE_UNIX_SECONDS + " " + options.expiry.count);
      } else if (options.expiry.type == TimeToLiveType.UNIX_MILLISECONDS) {
        optionArgs.add(TIME_TO_LIVE_UNIX_MILLISECONDS + " " + options.expiry.count);
      }
    }

    return optionArgs;
  }
}
