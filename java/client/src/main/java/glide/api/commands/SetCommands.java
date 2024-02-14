/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import java.util.concurrent.CompletableFuture;

/**
 * Set Commands interface.
 *
 * @see <a href="https://redis.io/commands/?group=set">Set Commands</a>
 */
public interface SetCommands {
    /**
     * Add specified members to the set stored at <code>key</code>. Specified members that are already
     * a member of this set are ignored.
     *
     * @see <a href="https://redis.io/commands/sadd/">redis.io</a> for details.
     * @param key The <code>key</code> where members will be added to its set.
     * @param members A list of members to add to the set stored at <code>key</code>.
     * @return The number of members that were added to the set, excluding members already present.
     * @remarks
     *     <ul>
     *       <li>If <code>key</code> does not exist, a new set is created before adding <code>members
     *           </code>.
     *       <li>If <code>key</code> holds a value that is not a set, an error is returned.
     *     </ul>
     *
     * @example
     *     <p><code>
     *  int result = client.sadd("my_set", new String[]{"member1", "member2"}).get();
     *  </code>
     */
    CompletableFuture<Long> sadd(String key, String... members);

    /**
     * Remove specified members from the set stored at <code>key</code>. Specified members that are
     * not a member of this set are ignored.
     *
     * @see <a href="https://redis.io/commands/srem/">redis.io</a> for details.
     * @param key The <code>key</code> from which members will be removed.
     * @param members A list of members to remove from the set stored at <code>key</code>.
     * @return The number of members that were removed from the set, excluding non-existing members.
     * @remarks
     *     <ul>
     *       <li>If <code>key</code> does not exist, it is treated as an empty set and this command
     *           returns 0.
     *       <li>If <code>key</code> holds a value that is not a set, an error is returned.
     *     </ul>
     *
     * @example
     *     <p><code>
     *  int result = client.srem("my_set", new String[]{"member1", "member2"}).get();
     *  </code>
     */
    CompletableFuture<Long> srem(String key, String[] members);
}
