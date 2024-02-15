/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import glide.api.models.BaseTransaction;
import glide.api.models.commands.SetOptions;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

public class TestUtilities {

    public static BaseTransaction transactionTest(BaseTransaction baseTransaction) {
        String key1 = "{key}" + UUID.randomUUID();
        String key2 = "{key}" + UUID.randomUUID();
        String key3 = "{key}" + UUID.randomUUID();

        baseTransaction.set(key1, "bar");
        baseTransaction.set(key2, "baz", SetOptions.builder().returnOldValue(true).build());
        baseTransaction.customCommand("MGET", key1, key2);

        baseTransaction.sadd(key3, "bar", "foo");
        baseTransaction.srem(key3, "foo");
        baseTransaction.scard(key3);
        baseTransaction.smembers(key3);

        return baseTransaction;
    }

    public static Object[] transactionTestResult() {
        return new Object[] {
            "OK", null, new String[] {"bar", "baz"}, 2L, 1L, 1L, new HashSet<>(Arrays.asList("bar"))
        };
    }
}
