/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import static glide.api.BaseClient.OK;

import glide.api.models.BaseTransaction;
import glide.api.models.commands.SetOptions;
import java.util.UUID;

public class TestUtilities {
    private static final String key1 = "{key}" + UUID.randomUUID();
    private static final String key2 = "{key}" + UUID.randomUUID();
    private static final String key4 = "{key}" + UUID.randomUUID();
    private static final String value1 = "{value}" + UUID.randomUUID();
    private static final String value2 = "{value}" + UUID.randomUUID();


    public static BaseTransaction transactionTest(BaseTransaction baseTransaction) {
        baseTransaction.set(key1, value1);
        baseTransaction.set(key2, value2, SetOptions.builder().returnOldValue(true).build());
        baseTransaction.customCommand("MGET", key1, key2);

        baseTransaction.lpush(key4, new String[] {value1, value1, value2, value2});
        baseTransaction.llen(key4);
        baseTransaction.lpop(key4);
        baseTransaction.lrem(key4, 1, value1);
        baseTransaction.ltrim(key4, 0, 1);
        baseTransaction.lrange(key4, 0, -1);
        baseTransaction.lpopCount(key4, 2);

        return baseTransaction;
    }

    public static Object[] transactionTestResult() {
        return new Object[] {
            "OK",
            null,
            new String[] {value1, value2},
            4L,
            4L,
            value2,
            1L,
            OK,
            new String[] {value2, value1},
            new String[] {value2, value1}
        };
    }
}
