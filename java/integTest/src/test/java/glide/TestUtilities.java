/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import glide.api.models.BaseTransaction;
import glide.api.models.commands.SetOptions;
import java.util.Map;
import java.util.UUID;

public class TestUtilities {
    private static final String key1 = "{key}" + UUID.randomUUID();
    private static final String key2 = "{key}" + UUID.randomUUID();
    private static final String key3 = "{key}" + UUID.randomUUID();
    private static final String key4 = "{key}" + UUID.randomUUID();
    private static final String value1 = "{value}" + UUID.randomUUID();
    private static final String value2 = "{value}" + UUID.randomUUID();
    private static final String field1 = "{value}" + UUID.randomUUID();
    private static final String field2 = "{value}" + UUID.randomUUID();

    public static BaseTransaction transactionTest(BaseTransaction baseTransaction) {
        baseTransaction.set(key1, value1);
        baseTransaction.set(key2, value2, SetOptions.builder().returnOldValue(true).build());
        baseTransaction.customCommand("MGET", key1, key2);

        baseTransaction.hset(key4, Map.of(field1, value1, field2, value2));
        baseTransaction.hget(key4, field1);
        baseTransaction.hdel(key4, new String[] {field1});

        return baseTransaction;
    }

    public static Object[] transactionTestResult() {
        return new Object[] {"OK", null, new String[] {value1, value2}, 2L, value1, 1L};
    }
}
