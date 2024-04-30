/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import glide.api.models.commands.RangeOptions.InfLexBound;
import glide.api.models.commands.RangeOptions.InfScoreBound;
import glide.api.models.commands.RangeOptions.LexBoundary;
import glide.api.models.commands.RangeOptions.LexRange;
import glide.api.models.commands.RangeOptions.RangeByIndex;
import glide.api.models.commands.RangeOptions.RangeByLex;
import glide.api.models.commands.RangeOptions.RangeByScore;
import glide.api.models.commands.RangeOptions.RangeQuery;
import glide.api.models.commands.RangeOptions.ScoreBoundary;
import glide.api.models.commands.RangeOptions.ScoreRange;
import glide.api.models.commands.RangeOptions.ScoredRangeQuery;
import glide.api.models.commands.WeightAggregateOptions;
import glide.api.models.commands.ZaddOptions;
import glide.api.models.commands.ZmpopOptions.ScoreModifier;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Supports commands and transactions for the "Sorted Set Commands" group for standalone and cluster
 * clients.
 *
 * @see <a href="https://redis.io/commands/?group=sorted-set">Sorted Set Commands</a>
 */
public interface SortedSetBaseCommands {
    /** Redis API keyword used to query sorted set members with their scores. */
    String WITH_SCORES_REDIS_API = "WITHSCORES";

    /** Redis API keyword used to query a sorted set member with its score. */
    String WITH_SCORE_REDIS_API = "WITHSCORE";

    /** Redis API keyword used to extract specific count of members from a sorted set. */
    String COUNT_REDIS_API = "COUNT";

    /**
     * Adds members with their scores to the sorted set stored at <code>key</code>.<br>
     * If a member is already a part of the sorted set, its score is updated.
     *
     * @see <a href="https://redis.io/commands/zadd/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param membersScoresMap A <code>Map</code> of members to their corresponding scores.
     * @param options The Zadd options.
     * @param changed Modify the return value from the number of new elements added, to the total
     *     number of elements changed.
     * @return The number of elements added to the sorted set.<br>
     *     If <code>changed</code> is set, returns the number of elements updated in the sorted set.
     * @example
     *     <pre>{@code
     * ZaddOptions options = ZaddOptions.builder().conditionalChange(ONLY_IF_DOES_NOT_EXIST).build();
     * Long num = client.zadd("mySortedSet", Map.of("member1", 10.5, "member2", 8.2), options, false).get();
     * assert num == 2L; // Indicates that two elements have been added or updated in the sorted set "mySortedSet".
     *
     * options = ZaddOptions.builder().conditionalChange(ONLY_IF_EXISTS).build();
     * Long num = client.zadd("existingSortedSet", Map.of("member1", 15.0, "member2", 5.5), options, false).get();
     * assert num == 2L; // Updates the scores of two existing members in the sorted set "existingSortedSet".
     * }</pre>
     */
    CompletableFuture<Long> zadd(
            String key, Map<String, Double> membersScoresMap, ZaddOptions options, boolean changed);

    /**
     * Adds members with their scores to the sorted set stored at <code>key</code>.<br>
     * If a member is already a part of the sorted set, its score is updated.
     *
     * @see <a href="https://redis.io/commands/zadd/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param membersScoresMap A <code>Map</code> of members to their corresponding scores.
     * @param options The Zadd options.
     * @return The number of elements added to the sorted set.
     * @example
     *     <pre>{@code
     * ZaddOptions options = ZaddOptions.builder().conditionalChange(ONLY_IF_DOES_NOT_EXIST).build();
     * Long num = client.zadd("mySortedSet", Map.of("member1", 10.5, "member2", 8.2), options).get();
     * assert num == 2L; // Indicates that two elements have been added to the sorted set "mySortedSet".
     *
     * options = ZaddOptions.builder().conditionalChange(ONLY_IF_EXISTS).build();
     * Long num = client.zadd("existingSortedSet", Map.of("member1", 15.0, "member2", 5.5), options).get();
     * assert num == 0L; // No new members were added to the sorted set "existingSortedSet".
     * }</pre>
     */
    CompletableFuture<Long> zadd(
            String key, Map<String, Double> membersScoresMap, ZaddOptions options);

    /**
     * Adds members with their scores to the sorted set stored at <code>key</code>.<br>
     * If a member is already a part of the sorted set, its score is updated.
     *
     * @see <a href="https://redis.io/commands/zadd/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param membersScoresMap A <code>Map</code> of members to their corresponding scores.
     * @param changed Modify the return value from the number of new elements added, to the total
     *     number of elements changed.
     * @return The number of elements added to the sorted set.<br>
     *     If <code>changed</code> is set, returns the number of elements updated in the sorted set.
     * @example
     *     <pre>{@code
     * Long num = client.zadd("mySortedSet", Map.of("member1", 10.5, "member2", 8.2), true).get();
     * assert num == 2L; // Indicates that two elements have been added or updated in the sorted set "mySortedSet".
     * }</pre>
     */
    CompletableFuture<Long> zadd(String key, Map<String, Double> membersScoresMap, boolean changed);

    /**
     * Adds members with their scores to the sorted set stored at <code>key</code>.<br>
     * If a member is already a part of the sorted set, its score is updated.
     *
     * @see <a href="https://redis.io/commands/zadd/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param membersScoresMap A <code>Map</code> of members to their corresponding scores.
     * @return The number of elements added to the sorted set.
     * @example
     *     <pre>{@code
     * Long num = client.zadd("mySortedSet", Map.of("member1", 10.5, "member2", 8.2)).get();
     * assert num == 2L; // Indicates that two elements have been added to the sorted set "mySortedSet".
     * }</pre>
     */
    CompletableFuture<Long> zadd(String key, Map<String, Double> membersScoresMap);

    /**
     * Increments the score of member in the sorted set stored at <code>key</code> by <code>increment
     * </code>.<br>
     * If <code>member</code> does not exist in the sorted set, it is added with <code>
     * increment</code> as its score (as if its previous score was <code>0.0</code>).<br>
     * If <code>key</code> does not exist, a new sorted set with the specified member as its sole
     * member is created.
     *
     * @see <a href="https://redis.io/commands/zadd/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param member A member in the sorted set to increment.
     * @param increment The score to increment the member.
     * @param options The Zadd options.
     * @return The score of the member.<br>
     *     If there was a conflict with the options, the operation aborts and <code>null</code> is
     *     returned.
     * @example
     *     <pre>{@code
     * ZaddOptions options = ZaddOptions.builder().conditionalChange(ONLY_IF_DOES_NOT_EXIST).build();
     * Double num = client.zaddIncr("mySortedSet", member, 5.0, options).get();
     * assert num == 5.0;
     *
     * options = ZaddOptions.builder().updateOptions(SCORE_LESS_THAN_CURRENT).build();
     * Double num = client.zaddIncr("existingSortedSet", member, 3.0, options).get();
     * assert num == null;
     * }</pre>
     */
    CompletableFuture<Double> zaddIncr(
            String key, String member, double increment, ZaddOptions options);

    /**
     * Increments the score of member in the sorted set stored at <code>key</code> by <code>increment
     * </code>.<br>
     * If <code>member</code> does not exist in the sorted set, it is added with <code>
     * increment</code> as its score (as if its previous score was <code>0.0</code>).<br>
     * If <code>key</code> does not exist, a new sorted set with the specified member as its sole
     * member is created.
     *
     * @see <a href="https://redis.io/commands/zadd/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param member A member in the sorted set to increment.
     * @param increment The score to increment the member.
     * @return The score of the member.
     * @example
     *     <pre>{@code
     * Double num = client.zaddIncr("mySortedSet", member, 5.0).get();
     * assert num == 5.0;
     * }</pre>
     */
    CompletableFuture<Double> zaddIncr(String key, String member, double increment);

    /**
     * Removes the specified members from the sorted set stored at <code>key</code>.<br>
     * Specified members that are not a member of this set are ignored.
     *
     * @see <a href="https://redis.io/commands/zrem/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param members An array of members to remove from the sorted set.
     * @return The number of members that were removed from the sorted set, not including non-existing
     *     members.<br>
     *     If <code>key</code> does not exist, it is treated as an empty sorted set, and this command
     *     returns <code>0</code>.
     * @example
     *     <pre>{@code
     * Long num1 = client.zrem("mySortedSet", new String[] {"member1", "member2"}).get();
     * assert num1 == 2L; // Indicates that two members have been removed from the sorted set "mySortedSet".
     *
     * Long num2 = client.zrem("nonExistingSortedSet", new String[] {"member1", "member2"}).get();
     * assert num2 == 0L; // Indicates that no members were removed as the sorted set "nonExistingSortedSet" does not exist.
     * }</pre>
     */
    CompletableFuture<Long> zrem(String key, String[] members);

    /**
     * Returns the cardinality (number of elements) of the sorted set stored at <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/zcard/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @return The number of elements in the sorted set.<br>
     *     If <code>key</code> does not exist, it is treated as an empty sorted set, and this command
     *     return <code>0</code>.
     * @example
     *     <pre>{@code
     * Long num1 = client.zcard("mySortedSet").get();
     * assert num1 == 3L; // Indicates that there are 3 elements in the sorted set "mySortedSet".
     *
     * Long num2 = client.zcard("nonExistingSortedSet").get();
     * assert num2 == 0L;
     * }</pre>
     */
    CompletableFuture<Long> zcard(String key);

    /**
     * Removes and returns up to <code>count</code> members with the lowest scores from the sorted set
     * stored at the specified <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/zpopmin/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param count Specifies the quantity of members to pop.<br>
     *     If <code>count</code> is higher than the sorted set's cardinality, returns all members and
     *     their scores, ordered from lowest to highest.
     * @return A map of the removed members and their scores, ordered from the one with the lowest
     *     score to the one with the highest.<br>
     *     If <code>key</code> doesn't exist, it will be treated as an empty sorted set and the
     *     command returns an empty <code>Map</code>.
     * @example
     *     <pre>{@code
     * Map<String, Double> payload = client.zpopmax("mySortedSet", 2).get();
     * assert payload.equals(Map.of('member3', 7.5 , 'member2', 8.0)); // Indicates that 'member3' with a score of 7.5 and 'member2' with a score of 8.0 have been removed from the sorted set.
     * }</pre>
     */
    CompletableFuture<Map<String, Double>> zpopmin(String key, long count);

    /**
     * Removes and returns the member with the lowest score from the sorted set stored at the
     * specified <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/zpopmin/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @return A map containing the removed member and its corresponding score.<br>
     *     If <code>key</code> doesn't exist, it will be treated as an empty sorted set and the
     *     command returns an empty <code>Map</code>.
     * @example
     *     <pre>{@code
     * Map<String, Double> payload = client.zpopmin("mySortedSet").get();
     * assert payload.equals(Map.of('member1', 5.0)); // Indicates that 'member1' with a score of 5.0 has been removed from the sorted set.
     * }</pre>
     */
    CompletableFuture<Map<String, Double>> zpopmin(String key);

    /**
     * Removes and returns up to <code>count</code> members with the highest scores from the sorted
     * set stored at the specified <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/zpopmax/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param count Specifies the quantity of members to pop.<br>
     *     If <code>count</code> is higher than the sorted set's cardinality, returns all members and
     *     their scores, ordered from highest to lowest.
     * @return A map of the removed members and their scores, ordered from the one with the highest
     *     score to the one with the lowest.<br>
     *     If <code>key</code> doesn't exist, it will be treated as an empty sorted set and the
     *     command returns an empty <code>Map</code>.
     * @example
     *     <pre>{@code
     * Map<String, Double> payload = client.zpopmax("mySortedSet", 2).get();
     * assert payload.equals(Map.of('member2', 8.0, 'member3', 7.5)); // Indicates that 'member2' with a score of 8.0 and 'member3' with a score of 7.5 have been removed from the sorted set.
     * }</pre>
     */
    CompletableFuture<Map<String, Double>> zpopmax(String key, long count);

    /**
     * Removes and returns the member with the highest score from the sorted set stored at the
     * specified <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/zpopmax/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @return A map containing the removed member and its corresponding score.<br>
     *     If <code>key</code> doesn't exist, it will be treated as an empty sorted set and the
     *     command returns an empty <code>Map</code>.
     * @example
     *     <pre>{@code
     * Map<String, Double> payload = client.zpopmax("mySortedSet").get();
     * assert payload.equals(Map.of('member1', 10.0)); // Indicates that 'member1' with a score of 10.0 has been removed from the sorted set.
     * }</pre>
     */
    CompletableFuture<Map<String, Double>> zpopmax(String key);

    /**
     * Blocks the connection until it removes and returns a member with the highest score from the
     * sorted sets stored at the specified <code>keys</code>. The sorted sets are checked in the order
     * they are provided.<br>
     * <code>BZPOPMAX</code> is the blocking variant of {@link #zpopmax(String)}.<br>
     *
     * @see <a href="https://redis.io/commands/bzpopmax/">redis.io</a> for more details.
     * @apiNote <code>BZPOPMAX</code> is a client blocking command, see <a
     *     href="https://github.com/aws/glide-for-redis/wiki/General-Concepts#blocking-commands">Blocking
     *     Commands</a> for more details and best practices.
     * @param keys The keys of the sorted sets.
     * @param timeout The number of seconds to wait for a blocking operation to complete. A value of
     *     <code>0</code> will block indefinitely.
     * @return An <code>array</code> containing the key where the member was popped out, the member
     *     itself, and the member score.<br>
     *     If no member could be popped and the <code>timeout</code> expired, returns </code>null
     *     </code>.
     * @example
     *     <pre>{@code
     * Object[] data = client.bzpopmax(new String[] {"zset1", "zset2"}, 0.5).get();
     * System.out.printf("Popped '%s' with score %d from sorted set '%s'%n", data[1], data[2], data[0]);
     * }</pre>
     */
    CompletableFuture<Object[]> bzpopmax(String[] keys, double timeout);

    /**
     * Returns the score of <code>member</code> in the sorted set stored at <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/zscore/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param member The member whose score is to be retrieved.
     * @return The score of the member.<br>
     *     If <code>member</code> does not exist in the sorted set, <code>null</code> is returned.<br>
     *     If <code>key</code> does not exist, <code>null</code> is returned.
     * @example
     *     <pre>{@code
     * Double num1 = client.zscore("mySortedSet", "member").get();
     * assert num1 == 10.5; // Indicates that the score of "member" in the sorted set "mySortedSet" is 10.5.
     *
     * Double num2 = client.zscore("mySortedSet", "nonExistingMember").get();
     * assert num2 == null;
     * }</pre>
     */
    CompletableFuture<Double> zscore(String key, String member);

    /**
     * Returns the specified range of elements in the sorted set stored at <code>key</code>.<br>
     * <code>ZRANGE</code> can perform different types of range queries: by index (rank), by the
     * score, or by lexicographical order.<br>
     * To get the elements with their scores, see {@link #zrangeWithScores}.
     *
     * @see <a href="https://redis.io/commands/zrange/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param rangeQuery The range query object representing the type of range query to perform.<br>
     *     <ul>
     *       <li>For range queries by index (rank), use {@link RangeByIndex}.
     *       <li>For range queries by lexicographical order, use {@link RangeByLex}.
     *       <li>For range queries by score, use {@link RangeByScore}.
     *     </ul>
     *
     * @param reverse If true, reverses the sorted set, with index 0 as the element with the highest
     *     score.
     * @return An array of elements within the specified range. If <code>key</code> does not exist, it
     *     is treated as an empty sorted set, and the command returns an empty array.
     * @example
     *     <pre>{@code
     * RangeByScore query1 = new RangeByScore(new ScoreBoundary(10), new ScoreBoundary(20));
     * String[] payload1 = client.zrange("mySortedSet", query1, true).get(); // Returns members with scores between 10 and 20.
     * assert payload1.equals(new String[] {'member3', 'member2', 'member1'}); // Returns all members in descending order.
     *
     * RangeByScore query2 = new RangeByScore(InfScoreBound.NEGATIVE_INFINITY, new ScoreBoundary(3));
     * String[] payload2 = client.zrange("mySortedSet", query2, false).get();
     * assert payload2.equals(new String[] {'member2', 'member3'}); // Returns members with scores within the range of negative infinity to 3, in ascending order.
     * }</pre>
     */
    CompletableFuture<String[]> zrange(String key, RangeQuery rangeQuery, boolean reverse);

    /**
     * Returns the specified range of elements in the sorted set stored at <code>key</code>.<br>
     * <code>ZRANGE</code> can perform different types of range queries: by index (rank), by the
     * score, or by lexicographical order.<br>
     * To get the elements with their scores, see {@link #zrangeWithScores}.
     *
     * @see <a href="https://redis.io/commands/zrange/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param rangeQuery The range query object representing the type of range query to perform.<br>
     *     <ul>
     *       <li>For range queries by index (rank), use {@link RangeByIndex}.
     *       <li>For range queries by lexicographical order, use {@link RangeByLex}.
     *       <li>For range queries by score, use {@link RangeByScore}.
     *     </ul>
     *
     * @return An of array elements within the specified range. If <code>key</code> does not exist, it
     *     is treated as an empty sorted set, and the command returns an empty array.
     * @example
     *     <pre>{@code
     * RangeByIndex query1 = new RangeByIndex(0, -1);
     * String[] payload1 = client.zrange("mySortedSet",query1).get();
     * assert payload1.equals(new String[] {'member1', 'member2', 'member3'}); // Returns all members in ascending order.
     *
     * RangeByScore query2 = new RangeByScore(InfScoreBound.NEGATIVE_INFINITY, new ScoreBoundary(3));
     * String[] payload2 = client.zrange("mySortedSet", query2).get();
     * assert payload2.equals(new String[] {'member2', 'member3'}); // Returns members with scores within the range of negative infinity to 3, in ascending order.
     * }</pre>
     */
    CompletableFuture<String[]> zrange(String key, RangeQuery rangeQuery);

    /**
     * Returns the specified range of elements with their scores in the sorted set stored at <code>key
     * </code>. Similar to {@link #zrange} but with a <code>WITHSCORE</code> flag.
     *
     * @see <a href="https://redis.io/commands/zrange/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param rangeQuery The range query object representing the type of range query to perform.<br>
     *     <ul>
     *       <li>For range queries by index (rank), use {@link RangeByIndex}.
     *       <li>For range queries by score, use {@link RangeByScore}.
     *     </ul>
     *
     * @param reverse If true, reverses the sorted set, with index 0 as the element with the highest
     *     score.
     * @return A <code>Map</code> of elements and their scores within the specified range. If <code>
     *     key</code> does not exist, it is treated as an empty sorted set, and the command returns an
     *     empty <code>Map</code>.
     * @example
     *     <pre>{@code
     * RangeByScore query1 = new RangeByScore(new ScoreBoundary(10), new ScoreBoundary(20));
     * Map<String, Double> payload1 = client.zrangeWithScores("mySortedSet", query1, true).get();
     * assert payload1.equals(Map.of('member2', 15.2, 'member1', 10.5)); // Returns members with scores between 10 and 20 (inclusive) with their scores.
     *
     * RangeByScore query2 = new RangeByScore(InfScoreBound.NEGATIVE_INFINITY, new ScoreBoundary(3));
     * Map<String, Double> payload2 = client.zrangeWithScores("mySortedSet", query2, false).get();
     * assert payload2.equals(Map.of('member4', -2.0, 'member7', 1.5)); // Returns members with with scores within the range of negative infinity to 3, with their scores.
     * }</pre>
     */
    CompletableFuture<Map<String, Double>> zrangeWithScores(
            String key, ScoredRangeQuery rangeQuery, boolean reverse);

    /**
     * Returns the specified range of elements with their scores in the sorted set stored at <code>key
     * </code>. Similar to {@link #zrange} but with a <code>WITHSCORE</code> flag.
     *
     * @see <a href="https://redis.io/commands/zrange/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param rangeQuery The range query object representing the type of range query to perform.<br>
     *     <ul>
     *       <li>For range queries by index (rank), use {@link RangeByIndex}.
     *       <li>For range queries by score, use {@link RangeByScore}.
     *     </ul>
     *
     * @return A <code>Map</code> of elements and their scores within the specified range. If <code>
     *     key</code> does not exist, it is treated as an empty sorted set, and the command returns an
     *     empty <code>Map</code>.
     * @example
     *     <pre>{@code
     * RangeByScore query1 = new RangeByScore(new ScoreBoundary(10), new ScoreBoundary(20));
     * Map<String, Double> payload1 = client.zrangeWithScores("mySortedSet", query1).get();
     * assert payload1.equals(Map.of('member1', 10.5, 'member2', 15.2)); // Returns members with scores between 10 and 20 (inclusive) with their scores.
     *
     * RangeByScore query2 = new RangeByScore(InfScoreBound.NEGATIVE_INFINITY, new ScoreBoundary(3));
     * Map<String, Double> payload2 = client.zrangeWithScores("mySortedSet", query2).get();
     * assert payload2.equals(Map.of('member4', -2.0, 'member7', 1.5)); // Returns members with with scores within the range of negative infinity to 3, with their scores.
     * }</pre>
     */
    CompletableFuture<Map<String, Double>> zrangeWithScores(String key, ScoredRangeQuery rangeQuery);

    /**
     * Stores a specified range of elements from the sorted set at <code>source</code>, into a new
     * sorted set at <code>destination</code>. If <code>destination</code> doesn't exist, a new sorted
     * set is created; if it exists, it's overwritten.<br>
     *
     * @see <a href="https://redis.io/commands/zrangestore/">redis.io</a> for more details.
     * @param destination The key for the destination sorted set.
     * @param source The key of the source sorted set.
     * @param rangeQuery The range query object representing the type of range query to perform.<br>
     *     <ul>
     *       <li>For range queries by index (rank), use {@link RangeByIndex}.
     *       <li>For range queries by lexicographical order, use {@link RangeByLex}.
     *       <li>For range queries by score, use {@link RangeByScore}.
     *     </ul>
     *
     * @param reverse If <code>true</code>, reverses the sorted set, with index <code>0</code> as the
     *     element with the highest score.
     * @return The number of elements in the resulting sorted set.
     * @example
     *     <pre>{@code
     * RangeByIndex query1 = new RangeByIndex(0, -1); // Query for all members.
     * Long payload1 = client.zrangestore("destinationKey", "mySortedSet", query1, true).get();
     * assert payload1 == 7L;
     *
     * RangeByScore query2 = new RangeByScore(InfScoreBound.NEGATIVE_INFINITY, new ScoreBoundary(3)); // Query for members with scores within the range of negative infinity to 3.
     * Long payload2 = client.zrangestore("destinationKey", "mySortedSet", query2, false).get();
     * assert payload2 == 5L;
     * }</pre>
     */
    CompletableFuture<Long> zrangestore(
            String destination, String source, RangeQuery rangeQuery, boolean reverse);

    /**
     * Stores a specified range of elements from the sorted set at <code>source</code>, into a new
     * sorted set at <code>destination</code>. If <code>destination</code> doesn't exist, a new sorted
     * set is created; if it exists, it's overwritten.<br>
     *
     * @see <a href="https://redis.io/commands/zrangestore/">redis.io</a> for more details.
     * @param destination The key for the destination sorted set.
     * @param source The key of the source sorted set.
     * @param rangeQuery The range query object representing the type of range query to perform.<br>
     *     <ul>
     *       <li>For range queries by index (rank), use {@link RangeByIndex}.
     *       <li>For range queries by lexicographical order, use {@link RangeByLex}.
     *       <li>For range queries by score, use {@link RangeByScore}.
     *     </ul>
     *
     * @return The number of elements in the resulting sorted set.
     * @example
     *     <pre>{@code
     * RangeByIndex query1 = new RangeByIndex(0, -1); // Query for all members.
     * Long payload1 = client.zrangestore("destinationKey", "mySortedSet", query1).get();
     * assert payload1 == 7L;
     *
     * RangeByScore query2 = new RangeByScore(InfScoreBound.NEGATIVE_INFINITY, new ScoreBoundary(3)); // Query for members with scores within the range of negative infinity to 3.
     * Long payload2 = client.zrangestore("destinationKey", "mySortedSet", query2).get();
     * assert payload2 == 5L;
     * }</pre>
     */
    CompletableFuture<Long> zrangestore(String destination, String source, RangeQuery rangeQuery);

    /**
     * Returns the rank of <code>member</code> in the sorted set stored at <code>key</code>, with
     * scores ordered from low to high, starting from <code>0</code>.<br>
     * To get the rank of <code>member</code> with its score, see {@link #zrankWithScore}.
     *
     * @see <a href="https://redis.io/commands/zrank/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param member The member whose rank is to be retrieved.
     * @return The rank of <code>member</code> in the sorted set.<br>
     *     If <code>key</code> doesn't exist, or if <code>member</code> is not present in the set,
     *     <code>null</code> will be returned.
     * @example
     *     <pre>{@code
     * Long num1 = client.zrank("mySortedSet", "member2").get();
     * assert num1 == 3L; // Indicates that "member2" has the second-lowest score in the sorted set "mySortedSet".
     *
     * Long num2 = client.zcard("mySortedSet", "nonExistingMember").get();
     * assert num2 == null; // Indicates that "nonExistingMember" is not present in the sorted set "mySortedSet".
     * }</pre>
     */
    CompletableFuture<Long> zrank(String key, String member);

    /**
     * Returns the rank of <code>member</code> in the sorted set stored at <code>key</code> with its
     * score, where scores are ordered from the lowest to highest, starting from <code>0</code>.<br>
     *
     * @see <a href="https://redis.io/commands/zrank/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param member The member whose rank is to be retrieved.
     * @return An array containing the rank (as <code>Long</code>) and score (as <code>Double</code>)
     *     of <code>member</code> in the sorted set.<br>
     *     If <code>key</code> doesn't exist, or if <code>member</code> is not present in the set,
     *     <code>null</code> will be returned.
     * @example
     *     <pre>{@code
     * Object[] result1 = client.zrankWithScore("mySortedSet", "member2").get();
     * assert ((Long) result1[0]) == 1L && ((Double) result1[1]) == 6.0; // Indicates that "member2" with score 6.0 has the second-lowest score in the sorted set "mySortedSet".
     *
     * Object[] result2 = client.zrankWithScore("mySortedSet", "nonExistingMember").get();
     * assert result2 == null; // Indicates that "nonExistingMember" is not present in the sorted set "mySortedSet".
     * }</pre>
     */
    CompletableFuture<Object[]> zrankWithScore(String key, String member);

    /**
     * Returns the rank of <code>member</code> in the sorted set stored at <code>key</code>, where
     * scores are ordered from the highest to lowest, starting from <code>0</code>.<br>
     * To get the rank of <code>member</code> with its score, see {@link #zrevrankWithScore}.
     *
     * @see <a href="https://redis.io/commands/zrevrank/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param member The member whose rank is to be retrieved.
     * @return The rank of <code>member</code> in the sorted set, where ranks are ordered from high to
     *     low based on scores.<br>
     *     If <code>key</code> doesn't exist, or if <code>member</code> is not present in the set,
     *     <code>null</code> will be returned.
     * @example
     *     <pre>{@code
     * Long num1 = client.zrevrank("mySortedSet", "member2").get();
     * assert num1 == 1L; // Indicates that "member2" has the second-highest score in the sorted set "mySortedSet".
     *
     * Long num2 = client.zrevrank("mySortedSet", "nonExistingMember").get();
     * assert num2 == null; // Indicates that "nonExistingMember" is not present in the sorted set "mySortedSet".
     * }</pre>
     */
    CompletableFuture<Long> zrevrank(String key, String member);

    /**
     * Returns the rank of <code>member</code> in the sorted set stored at <code>key</code> with its
     * score, where scores are ordered from the highest to lowest, starting from <code>0</code>.
     *
     * @see <a href="https://redis.io/commands/zrevrank/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param member The member whose rank is to be retrieved.
     * @return An array containing the rank (as <code>Long</code>) and score (as <code>Double</code>)
     *     of <code>member</code> in the sorted set, where ranks are ordered from high to low based on
     *     scores.<br>
     *     If <code>key</code> doesn't exist, or if <code>member</code> is not present in the set,
     *     <code>null</code> will be returned.
     * @example
     *     <pre>{@code
     * Object[] result1 = client.zrevrankWithScore("mySortedSet", "member2").get();
     * assert ((Long) result1[0]) == 1L && ((Double) result1[1]) == 6.0; // Indicates that "member2" with score 6.0 has the second-highest score in the sorted set "mySortedSet".
     *
     * Object[] result2 = client.zrevrankWithScore("mySortedSet", "nonExistingMember").get();
     * assert result2 == null; // Indicates that "nonExistingMember" is not present in the sorted set "mySortedSet".
     * }</pre>
     */
    CompletableFuture<Object[]> zrevrankWithScore(String key, String member);

    /**
     * Returns the scores associated with the specified <code>members</code> in the sorted set stored
     * at <code>key</code>.
     *
     * @see <a href="https://redis.io/commands/zmscore/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param members An array of members in the sorted set.
     * @return An <code>Array</code> of scores of the <code>members</code>.<br>
     *     If a <code>member</code> does not exist, the corresponding value in the <code>Array</code>
     *     will be <code>null</code>.
     * @example
     *     <pre>{@code
     * Double[] payload = client.zmscore(key1, new String[] {"one", "nonExistentMember", "three"}).get();
     * assert payload.equals(new Double[] {1.0, null, 3.0});
     * }</pre>
     */
    CompletableFuture<Double[]> zmscore(String key, String[] members);

    /**
     * Returns the difference between the first sorted set and all the successive sorted sets.<br>
     * To get the elements with their scores, see {@link #zdiffWithScores}.
     *
     * @see <a href="https://redis.io/commands/zdiff/">redis.io</a> for more details.
     * @param keys The keys of the sorted sets.
     * @return An <code>array</code> of elements representing the difference between the sorted sets.
     *     <br>
     *     If the first <code>key</code> does not exist, it is treated as an empty sorted set, and the
     *     command returns an empty <code>array</code>.
     * @example
     *     <pre>{@code
     * String[] payload = client.zdiff(new String[] {"sortedSet1", "sortedSet2", "sortedSet3"}).get();
     * assert payload.equals(new String[]{"element1"});
     * }</pre>
     */
    CompletableFuture<String[]> zdiff(String[] keys);

    /**
     * Returns the difference between the first sorted set and all the successive sorted sets.
     *
     * @see <a href="https://redis.io/commands/zdiff/">redis.io</a> for more details.
     * @param keys The keys of the sorted sets.
     * @return A <code>Map</code> of elements and their scores representing the difference between the
     *     sorted sets.<br>
     *     If the first <code>key</code> does not exist, it is treated as an empty sorted set, and the
     *     command returns an empty <code>Map</code>.
     * @example
     *     <pre>{@code
     * Map<String, Double> payload = client.zdiffWithScores(new String[] {"sortedSet1", "sortedSet2", "sortedSet3"}).get();
     * assert payload.equals(Map.of("element1", 1.0));
     * }</pre>
     */
    CompletableFuture<Map<String, Double>> zdiffWithScores(String[] keys);

    /**
     * Calculates the difference between the first sorted set and all the successive sorted sets at
     * <code>keys</code> and stores the difference as a sorted set to <code>destination</code>,
     * overwriting it if it already exists. Non-existent keys are treated as empty sets.
     *
     * @see <a href="https://redis.io/commands/zdiffstore/">redis.io</a> for more details.
     * @param destination The key for the resulting sorted set.
     * @param keys The keys of the sorted sets to compare.
     * @return The number of members in the resulting sorted set stored at <code>destination</code>.
     * @example
     *     <pre>{@code
     * Long payload = client.zdiffstore("mySortedSet", new String[] {"key1", "key2"}).get();
     * assert payload > 0; // At least one member differed in "key1" compared to "key2", and this difference was stored in "mySortedSet".
     * }</pre>
     */
    CompletableFuture<Long> zdiffstore(String destination, String[] keys);

    /**
     * Returns the number of members in the sorted set stored at <code>key</code> with scores between
     * <code>minScore</code> and <code>maxScore</code>.
     *
     * @see <a href="https://redis.io/commands/zcount/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param minScore The minimum score to count from. Can be an implementation of {@link
     *     InfScoreBound} representing positive/negative infinity, or {@link ScoreBoundary}
     *     representing a specific score and inclusivity.
     * @param maxScore The maximum score to count up to. Can be an implementation of {@link
     *     InfScoreBound} representing positive/negative infinity, or {@link ScoreBoundary}
     *     representing a specific score and inclusivity.
     * @return The number of members in the specified score range.<br>
     *     If <code>key</code> does not exist, it is treated as an empty sorted set, and the command
     *     returns <code>0</code>.<br>
     *     If <code>maxScore < minScore</code>, <code>0</code> is returned.
     * @example
     *     <pre>{@code
     * Long num1 = client.zcount("my_sorted_set", new ScoreBoundary(5.0, true), InfScoreBound.POSITIVE_INFINITY).get();
     * assert num1 == 2L; // Indicates that there are 2 members with scores between 5.0 (inclusive) and +inf in the sorted set "my_sorted_set".
     *
     * Long num2 = client.zcount("my_sorted_set", new ScoreBoundary(5.0, true), new ScoreBoundary(10.0, false)).get();
     * assert num2 == 1L; // Indicates that there is one member with ScoreBoundary 5.0 <= score < 10.0 in the sorted set "my_sorted_set".
     * }</pre>
     */
    CompletableFuture<Long> zcount(String key, ScoreRange minScore, ScoreRange maxScore);

    /**
     * Removes all elements in the sorted set stored at <code>key</code> with rank between <code>start
     * </code> and <code>end</code>. Both <code>start</code> and <code>end</code> are zero-based
     * indexes with <code>0</code> being the element with the lowest score. These indexes can be
     * negative numbers, where they indicate offsets starting at the element with the highest score.
     *
     * @see <a href="https://redis.io/commands/zremrangebyrank/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param start The starting point of the range.
     * @param end The end of the range.
     * @return The number of elements removed.<br>
     *     If <code>start</code> exceeds the end of the sorted set, or if <code>start</code> is
     *     greater than <code>end</code>, <code>0</code> returned.<br>
     *     If <code>end</code> exceeds the actual end of the sorted set, the range will stop at the
     *     actual end of the sorted set.<br>
     *     If <code>key</code> does not exist <code>0</code> will be returned.
     * @example
     *     <pre>{@code
     * Long payload1 = client.zremrangebyrank("mySortedSet", 0, 4).get();
     * assert payload1 == 5L; // Indicates that 5 elements, with ranks ranging from 0 to 4 (inclusive), have been removed from "mySortedSet".
     *
     * Long payload2 = client.zremrangebyrank("mySortedSet", 0, 4).get();
     * assert payload2 == 0L; // Indicates that nothing was removed.
     * }</pre>
     */
    CompletableFuture<Long> zremrangebyrank(String key, long start, long end);

    /**
     * Removes all elements in the sorted set stored at <code>key</code> with a lexicographical order
     * between <code>minLex</code> and <code>maxLex</code>.
     *
     * @see <a href="https://redis.io/commands/zremrangebylex/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param minLex The minimum bound of the lexicographical range. Can be an implementation of
     *     {@link InfLexBound} representing positive/negative infinity, or {@link LexBoundary}
     *     representing a specific lex and inclusivity.
     * @param maxLex The maximum bound of the lexicographical range. Can be an implementation of
     *     {@link InfLexBound} representing positive/negative infinity, or {@link LexBoundary}
     *     representing a specific lex and inclusivity.
     * @return The number of members removed from the sorted set.<br>
     *     If <code>key</code> does not exist, it is treated as an empty sorted set, and the command
     *     returns <code>0</code>.<br>
     *     If <code>minLex</code> is greater than <code>maxLex</code>, <code>0</code> is returned.
     * @example
     *     <pre>{@code
     * Long payload1 = client.zremrangebylex("mySortedSet", new LexBoundary("a", false), new LexBoundary("e")).get();
     * assert payload1 == 4L; // Indicates that 4 members, with lexicographical values ranging from "a" (exclusive) to "e" (inclusive), have been removed from "mySortedSet".
     *
     * Long payload2 = client.zremrangebylex("mySortedSet", InfLexBound.NEGATIVE_INFINITY , new LexBoundary("e")).get();
     * assert payload2 == 0L; // Indicates that no elements were removed.
     * }</pre>
     */
    CompletableFuture<Long> zremrangebylex(String key, LexRange minLex, LexRange maxLex);

    /**
     * Removes all elements in the sorted set stored at <code>key</code> with a score between <code>
     * minScore</code> and <code>maxScore</code>.
     *
     * @see <a href="https://redis.io/commands/zremrangebyscore/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param minScore The minimum score to remove from. Can be an implementation of {@link
     *     InfScoreBound} representing positive/negative infinity, or {@link ScoreBoundary}
     *     representing a specific score and inclusivity.
     * @param maxScore The maximum score to remove to. Can be an implementation of {@link
     *     InfScoreBound} representing positive/negative infinity, or {@link ScoreBoundary}
     *     representing a specific score and inclusivity.
     * @return The number of members removed.<br>
     *     If <code>key</code> does not exist, it is treated as an empty sorted set, and the command
     *     returns <code>0</code>.<br>
     *     If <code>minScore</code> is greater than <code>maxScore</code>, <code>0</code> is returned.
     * @example
     *     <pre>{@code
     * Long payload1 = client.zremrangebyscore("mySortedSet", new ScoreBoundary(1, false), new ScoreBoundary(5)).get();
     * assert payload1 == 4L; // Indicates that 4 members, with scores ranging from 1 (exclusive) to 5 (inclusive), have been removed from "mySortedSet".
     *
     * Long payload2 = client.zremrangebyscore("mySortedSet", InfScoreBound.NEGATIVE_INFINITY , new ScoreBoundary(-42)).get();
     * assert payload2 == 0L; // Indicates that no elements were removed.
     * }</pre>
     */
    CompletableFuture<Long> zremrangebyscore(String key, ScoreRange minScore, ScoreRange maxScore);

    /**
     * Returns the number of members in the sorted set stored at <code>key</code> with scores between
     * <code>minLex</code> and <code>maxLex</code>.
     *
     * @see <a href="https://redis.io/commands/zlexcount/">redis.io</a> for more details.
     * @param key The key of the sorted set.
     * @param minLex The minimum lex to count from. Can be an implementation of {@link InfLexBound}
     *     representing positive/negative infinity, or {@link LexBoundary} representing a specific lex
     *     and inclusivity.
     * @param maxLex The maximum lex to count up to. Can be an implementation of {@link InfLexBound}
     *     representing positive/negative infinity, or {@link LexBoundary} representing a specific lex
     *     and inclusivity.
     * @return The number of members in the specified lex range.<br>
     *     If <code>key</code> does not exist, it is treated as an empty sorted set, and the command
     *     returns <code>0</code>.<br>
     *     If <code>maxLex < minLex</code>, <code>0</code> is returned.
     * @example
     *     <pre>{@code
     * Long num1 = client.zlexcount("my_sorted_set", new LexBoundary("c", true), InfLexBound.POSITIVE_INFINITY).get();
     * assert num1 == 2L; // Indicates that there are 2 members with lex scores between "c" (inclusive) and positive infinity in the sorted set "my_sorted_set".
     *
     * Long num2 = client.zlexcount("my_sorted_set", new ScoreBoundary("c", true), new ScoreBoundary("k", false)).get();
     * assert num2 == 1L; // Indicates that there is one member with LexBoundary "c" <= score < "k" in the sorted set "my_sorted_set".
     * }</pre>
     */
    CompletableFuture<Long> zlexcount(String key, LexRange minLex, LexRange maxLex);

    /**
     * Computes the intersection of sorted sets given by the specified <code>keys</code>, and stores
     * the result in <code>destination</code>. If <code>destination</code> already exists, it is
     * overwritten. Otherwise, a new sorted set will be created.
     *
     * @apiNote When in cluster mode, <code>destination</code> and all <code>keys</code> must map to
     *     the same <code>hash slot</code>.
     * @see <a href="https://redis.io/commands/zinterstore/">redis.io</a> for more details.
     * @param destination The key of the destination sorted set.
     * @param keys The keys of sorted sets to intersect.
     * @param options Weight and Aggregate options.
     * @return The number of elements in the resulting sorted set stored at <code>destination</code>.
     * @example
     *     <pre>{@code
     * WeightAggregateOptions options =
     *     WeightAggregateOptions.builder()
     *             .aggregate(Aggregate.MAX)
     *             .weight(1.0)
     *             .weight(2.0)
     *             .build();
     * Long payload = client.zinterstore("newSortedSet", new String[] {"mySortedSet1", "mySortedSet2"}, options).get()
     * assert payload == 3L; // Indicates the new sorted set contains three members from the intersection of "mySortedSet1" and "mySortedSet2".
     * }</pre>
     */
    CompletableFuture<Long> zinterstore(
            String destination, String[] keys, WeightAggregateOptions options);

    /**
     * Computes the intersection of sorted sets given by the specified <code>keys</code>, and stores
     * the result in <code>destination</code>. If <code>destination</code> already exists, it is
     * overwritten. Otherwise, a new sorted set will be created.<br>
     * To perform a <code>zinterstore</code> operation while specifying custom weights and aggregation
     * settings, use {@link #zinterstore(String, String[], WeightAggregateOptions)}
     *
     * @apiNote When in cluster mode, <code>destination</code> and all <code>keys</code> must map to
     *     the same <code>hash slot</code>.
     * @see <a href="https://redis.io/commands/zinterstore/">redis.io</a> for more details.
     * @param destination The key of the destination sorted set.
     * @param keys The keys of sorted sets to intersect.
     * @return The number of elements in the resulting sorted set stored at <code>destination</code>.
     * @example
     *     <pre>{@code
     * Long payload = client.zinterstore("newSortedSet", new String[] {"mySortedSet1", "mySortedSet2"}).get()
     * assert payload == 3L; // Indicates the new sorted set contains three members from the intersection of "mySortedSet1" and "mySortedSet2".
     * }</pre>
     */
    CompletableFuture<Long> zinterstore(String destination, String[] keys);

    // TODO add @link to ZMPOP when implemented
    /**
     * Blocks the connection until it pops and returns a member-score pair from the sorted sets stored
     * at the specified <code>keys</code>. The sorted sets are checked in the order they are provided.
     * <br>
     * To pop more than one element use {@link #bzmpop(double, String[], ScoreModifier, long)}.<br>
     * <code>BZMPOP</code> is the blocking variant of <code>ZMPOP</code>.
     *
     * @apiNote
     *     <ol>
     *       <li>When in cluster mode, all <code>keys</code> must map to the same <code>hash slot
     *           </code>.
     *       <li><code>BZMPOP</code> is a client blocking command, see <a
     *           href="https://github.com/aws/glide-for-redis/wiki/General-Concepts#blocking-commands">Blocking
     *           Commands</a> for more details and best practices.
     *     </ol>
     *
     * @see <a href="https://redis.io/commands/bzmpop/">redis.io</a> for more details.
     * @param timeout The number of seconds to wait for a blocking operation to complete. A value of
     *     <code>0</code> will block indefinitely.
     * @param keys The keys of the sorted sets.
     * @param modifier The element pop criteria - either {@link ScoreModifier#MIN} or {@link
     *     ScoreModifier#MAX} to pop members with the lowest/highest scores accordingly.
     * @return A two-element <code>array</code> containing the key name of the set from which an
     *     element was popped, and a member-score <code>Map</code>.<br>
     *     If no member could be popped and the timeout expired, returns </code>null</code>.
     * @example
     *     <pre>{@code
     * Object[] result = client.bzmpop(0.1, new String[] { "zSet1", "zSet2" }, MAX).get();
     * Map<String, Double> data = (Map<String, Double>)result[1];
     * String element = data.keySet().toArray(String[]::new)[0];
     * System.out.printf("Popped '%s' with score %d from '%s'%n", element, data.get(element), result[0]);
     * }</pre>
     */
    CompletableFuture<Object[]> bzmpop(double timeout, String[] keys, ScoreModifier modifier);

    // TODO add @link to ZMPOP when implemented
    /**
     * Blocks the connection until it pops and returns multiple member-score pairs from the sorted
     * sets stored at the specified <code>keys</code>. The sorted sets are checked in the order they
     * are provided.<br>
     * <code>BZMPOP</code> is the blocking variant of <code>ZMPOP</code>.
     *
     * @apiNote
     *     <ol>
     *       <li>When in cluster mode, all <code>keys</code> must map to the same <code>hash slot
     *           </code>.
     *       <li><code>BZMPOP</code> is a client blocking command, see <a
     *           href="https://github.com/aws/glide-for-redis/wiki/General-Concepts#blocking-commands">Blocking
     *           Commands</a> for more details and best practices.
     *     </ol>
     *
     * @see <a href="https://redis.io/commands/bzmpop/">redis.io</a> for more details.
     * @param timeout The number of seconds to wait for a blocking operation to complete. A value of
     *     <code>0</code> will block indefinitely.
     * @param keys The keys of the sorted sets.
     * @param modifier The element pop criteria - either {@link ScoreModifier#MIN} or {@link
     *     ScoreModifier#MAX} to pop members with the lowest/highest scores accordingly.
     * @param count The amount elements to pop.
     * @return A two-element <code>array</code> containing the key name of the set from which elements
     *     were popped, and a member-score <code>Map</code>.<br>
     *     If no members could be popped and the timeout expired, returns </code>null</code>.
     * @example
     *     <pre>{@code
     * Object[] result = client.bzmpop(0.1, new String[] { "zSet1", "zSet2" }, MAX).get();
     * Map<String, Double> data = (Map<String, Double>)result[1];
     * for (Map.Entry<String, Double> entry : data.entrySet()) {
     *     System.out.printf("Popped '%s' with score %d from '%s'%n", entry.getKey(), entry.getValue(), result[0]);
     * }
     * }</pre>
     */
    CompletableFuture<Object[]> bzmpop(
            double timeout, String[] keys, ScoreModifier modifier, long count);
}
