/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import glide.api.models.commands.ZaddOptions;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Sorted set Commands interface.
 *
 * @see <a href="https://redis.io/commands/?group=sorted-set">Sorted set Commands</a>
 */
public interface SortedSetBaseCommands {
    /**
     * Adds members with their scores to the sorted set stored at <code>key</code>. If a member is
     * already a part of the sorted set, its score is updated.
     *
     * @see <a href="https://redis.io/commands/zadd/">redis.io</a> for more details.
     * @param key - The key of the sorted set.
     * @param membersScoresMap - A mapping of members to their corresponding scores.
     * @param options - The Zadd options.
     * @param changed - Modify the return value from the number of new elements added, to the total
     *     number of elements changed.
     * @returns The number of elements added to the sorted set. If <code>changed</code> is set,
     *     returns the number of elements updated in the sorted set. If <code>key</code> holds a value
     *     that is not a sorted set, an error is returned.
     * @example
     *     <p><code>
     * client.zadd("mySortedSet", Map.of("member1", 10.5d, "member2", 8.2d), ZaddOptions.builder().build(), false).get();
     * 2 (Indicates that two elements have been added or updated in the sorted set "mySortedSet".)
     *
     * client.zadd("existingSortedSet", Map.of("member1", 15.0d, "member2", 5.5d), ZaddOptions.builder().conditionalChange(ZaddOptions.ConditionalChange.ONLY_IF_EXISTS).build(), false).get();
     * 2 (Updates the scores of two existing members in the sorted set "existingSortedSet".)
     * </code>
     */
    CompletableFuture<Long> zadd(
            String key, Map<String, Double> membersScoresMap, ZaddOptions options, boolean changed);

    /**
     * Adds members with their scores to the sorted set stored at <code>key</code>. If a member is
     * already a part of the sorted set, its score is updated.
     *
     * @see <a href="https://redis.io/commands/zadd/">redis.io</a> for more details.
     * @param key - The key of the sorted set.
     * @param membersScoresMap - A mapping of members to their corresponding scores.
     * @param options - The Zadd options.
     * @returns The number of elements added to the sorted set. If <code>key</code> holds a value that
     *     is not a sorted set, an error is returned.
     * @example
     *     <p><code>
     * client.zadd("mySortedSet", Map.of("member1", 10.5d, "member2", 8.2d), ZaddOptions.builder().build()).get();
     * 2 (Indicates that two elements have been added or updated in the sorted set "mySortedSet".)
     *
     * client.zadd("existingSortedSet", Map.of("member1", 15.0d, "member2", 5.5d), ZaddOptions.builder().conditionalChange(ZaddOptions.ConditionalChange.ONLY_IF_EXISTS).build()).get();
     * 2 (Updates the scores of two existing members in the sorted set "existingSortedSet".)
     * </code>
     */
    CompletableFuture<Long> zadd(
            String key, Map<String, Double> membersScoresMap, ZaddOptions options);

    /**
     * Adds members with their scores to the sorted set stored at <code>key</code>. If a member is
     * already a part of the sorted set, its score is updated.
     *
     * @see <a href="https://redis.io/commands/zadd/">redis.io</a> for more details.
     * @param key - The key of the sorted set.
     * @param membersScoresMap - A mapping of members to their corresponding scores.
     * @param changed - Modify the return value from the number of new elements added, to the total
     *     number of elements changed.
     * @returns The number of elements added to the sorted set. If <code>changed</code> is set,
     *     returns the number of elements updated in the sorted set. If <code>key</code> holds a value
     *     that is not a sorted set, an error is returned.
     * @example
     *     <p><code>
     * client.zadd("mySortedSet", Map.of("member1", 10.5d, "member2", 8.2d), false).get();
     * 2 (Indicates that two elements have been added or updated in the sorted set "mySortedSet".)
     * </code>
     */
    CompletableFuture<Long> zadd(String key, Map<String, Double> membersScoresMap, boolean changed);

    /**
     * Adds members with their scores to the sorted set stored at <code>key</code>. If a member is
     * already a part of the sorted set, its score is updated.
     *
     * @see <a href="https://redis.io/commands/zadd/">redis.io</a> for more details.
     * @param key - The key of the sorted set.
     * @param membersScoresMap - A mapping of members to their corresponding scores.
     * @returns The number of elements added to the sorted set. If <code>key</code> holds a value that
     *     is not a sorted set, an error is returned.
     * @example
     *     <p><code>
     * client.zadd("mySortedSet", Map.of("member1", 10.5d, "member2", 8.2d)).get();
     * 2 (Indicates that two elements have been added or updated in the sorted set "mySortedSet".)
     * </code>
     */
    CompletableFuture<Long> zadd(String key, Map<String, Double> membersScoresMap);

    /** Increments the score of member in the sorted set stored at <code>key</code> by <code>increment</code>.
     * If <code>member</code> does not exist in the sorted set, it is added with <code>increment</code> as its score (as if its previous score was 0.0).
     * If <code>key</code> does not exist, a new sorted set with the specified member as its sole member is created.
     *
     * @see <a href="https://redis.io/commands/zadd/">redis.io</a> for more details.
     * @param key - The key of the sorted set.
     * @param member - A member in the sorted set to increment.
     * @param increment - The score to increment the member.
     * @param options - The Zadd options.
     * @returns The score of the member.
     * If there was a conflict with the options, the operation aborts and <code>null</code> is returned.
     * If <code>key</code> holds a value that is not a sorted set, an error is returned.
     *
     * @example
     * <p><code>
     * client.zaddIncr("mySortedSet", member, 5.0d, ZaddOptions.builder().build()).get();
     * 5.0
     *
     * client.zaddIncr("existingSortedSet", member, 3.0d, ZaddOptions.builder().updateOptions(ZaddOptions.UpdateOptions.SCORE_LESS_THAN_CURRENT).build()).get();
     * null
     */
    CompletableFuture<Double> zaddIncr(
            String key, String member, double increment, ZaddOptions options);

    /** Increments the score of member in the sorted set stored at <code>key</code> by <code>increment</code>.
     * If <code>member</code> does not exist in the sorted set, it is added with <code>increment</code> as its score (as if its previous score was 0.0).
     * If <code>key</code> does not exist, a new sorted set with the specified member as its sole member is created.
     *
     * @see <a href="https://redis.io/commands/zadd/">redis.io</a> for more details.
     * @param key - The key of the sorted set.
     * @param member - A member in the sorted set to increment.
     * @param increment - The score to increment the member.
     * @returns The score of the member.
     * If <code>key</code> holds a value that is not a sorted set, an error is returned.
     *
     * @example
     * <p><code>
     * client.zaddIncr("mySortedSet", member, 5.0d).get();
     * 5.0
     */
    CompletableFuture<Double> zaddIncr(String key, String member, double increment);

    /**
     * Removes the specified members from the sorted set stored at <code>key</code>. Specified members
     * that are not a member of this set are ignored.
     *
     * @see <a href="https://redis.io/commands/zrem/">redis.io</a> for more details.
     * @param key - The key of the sorted set.
     * @param members - A list of members to remove from the sorted set.
     * @returns The number of members that were removed from the sorted set, not including
     *     non-existing members. If <code>key</code> does not exist, it is treated as an empty sorted
     *     set, and this command returns <code>0</code>. If <code>key</code> holds a value that is not
     *     a sorted set, an error is returned.
     */
    CompletableFuture<Long> zrem(String key, String[] members);

    /**
     * Returns the cardinality (number of elements) of the sorted set stored at <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/zcard/">redis.io</a> for more details.
     * @param key - The key of the sorted set.
     * @returns The number of elements in the sorted set. If <code>key</code> does not exist, it is
     *     treated as an empty sorted set, and this command returns <code>0</code>. If <code>key
     *     </code> holds a value that is not a sorted set, an error is returned.
     */
    CompletableFuture<Long> zcard(String key);
}
