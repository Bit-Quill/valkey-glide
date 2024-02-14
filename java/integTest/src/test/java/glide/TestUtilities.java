/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import glide.api.models.BaseTransaction;
import glide.api.models.commands.SetOptions;
import java.util.UUID;

public class TestUtilities {

    public static BaseTransaction transactionTest(BaseTransaction baseTransaction) {
        String key1 = "{key}" + UUID.randomUUID();
        String key2 = "{key}" + UUID.randomUUID();
        String value1 = UUID.randomUUID().toString();
        String value2 = UUID.randomUUID().toString();

        baseTransaction.set(key1, "apples");
        baseTransaction.set(key2, "bananas", SetOptions.builder().returnOldValue(true).build());
        baseTransaction.customCommand("MGET", key1, key2);
        baseTransaction.exists(new String[] {key1, key2});

        return baseTransaction;
    }

    public static Object[] transactionTestResult() {
        return new Object[] {"OK", null, new String[] {"apples", "bananas"}, 2L};
    }
}
