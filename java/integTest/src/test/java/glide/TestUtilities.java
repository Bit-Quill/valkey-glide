/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import glide.api.models.BaseTransaction;
import glide.api.models.commands.SetOptions;
import java.util.Map;
import java.util.UUID;

public class TestUtilities {

    public static BaseTransaction transactionTest(BaseTransaction baseTransaction) {
        String key1 = "{key}" + UUID.randomUUID();
        String key2 = "{key}" + UUID.randomUUID();
        String keySortedSet = "{key}" + UUID.randomUUID();

        baseTransaction.set(key1, "bar");
        baseTransaction.set(key2, "baz", SetOptions.builder().returnOldValue(true).build());
        baseTransaction.customCommand("MGET", key1, key2);

        Map<String, Double> membersScores = Map.of("baz", 1.0, "foo", 2.0);
        baseTransaction.zadd(keySortedSet, membersScores);
        baseTransaction.zaddIncr(keySortedSet, "baz", 2.0d);
        baseTransaction.zrem(keySortedSet, new String[] {"foo"});
        baseTransaction.zcard(keySortedSet);

        return baseTransaction;
    }

    public static Object[] transactionTestResult() {
        return new Object[] {"OK", null, new String[] {"bar", "baz"}, 2L, 3.0d, 1L, 1L};
    }
}
