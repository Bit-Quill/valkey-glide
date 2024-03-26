/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import static glide.api.BaseClient.OK;

import glide.api.models.BaseTransaction;
import glide.api.models.commands.SetOptions;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TransactionTestUtilities {
    private static final String key1 = "{key}" + UUID.randomUUID();
    private static final String key2 = "{key}" + UUID.randomUUID();
    private static final String key3 = "{key}" + UUID.randomUUID();
    private static final String key4 = "{key}" + UUID.randomUUID();
    private static final String key5 = "{key}" + UUID.randomUUID();
    private static final String key6 = "{key}" + UUID.randomUUID();
    private static final String key7 = "{key}" + UUID.randomUUID();
    private static final String key8 = "{key}" + UUID.randomUUID();
    private static final String value1 = UUID.randomUUID().toString();
    private static final String value2 = UUID.randomUUID().toString();
    private static final String value3 = UUID.randomUUID().toString();
    private static final String field1 = UUID.randomUUID().toString();
    private static final String field2 = UUID.randomUUID().toString();
    private static final String field3 = UUID.randomUUID().toString();

    public static BaseTransaction<?> transactionTest(BaseTransaction<?> baseTransaction) {

        baseTransaction.set(key1, value1);
        baseTransaction.get(key1);

        baseTransaction.set(key2, value2, SetOptions.builder().returnOldValue(true).build());
        baseTransaction.customCommand(new String[] {"MGET", key1, key2});

        baseTransaction.exists(new String[] {key1});

        baseTransaction.del(new String[] {key1});
        baseTransaction.get(key1);

        baseTransaction.unlink(new String[] {key2});
        baseTransaction.get(key2);

        baseTransaction.mset(Map.of(key1, value2, key2, value1));
        baseTransaction.mget(new String[] {key1, key2});

        baseTransaction.incr(key3);
        baseTransaction.incrBy(key3, 2);

        baseTransaction.decr(key3);
        baseTransaction.decrBy(key3, 2);

        baseTransaction.incrByFloat(key3, 0.5);

        baseTransaction.unlink(new String[] {key3});

        baseTransaction.hset(key4, Map.of(field1, value1, field2, value2));
        baseTransaction.hget(key4, field1);
        baseTransaction.hexists(key4, field2);
        baseTransaction.hmget(key4, new String[] {field1, "non_existing_field", field2});
        baseTransaction.hgetall(key4);
        baseTransaction.hdel(key4, new String[] {field1});

        baseTransaction.hincrBy(key4, field3, 5);
        baseTransaction.hincrByFloat(key4, field3, 5.5);

        baseTransaction.lpush(key5, new String[] {value1, value1, value2, value3, value3});
        baseTransaction.llen(key5);
        baseTransaction.lrem(key5, 1, value1);
        baseTransaction.ltrim(key5, 1, -1);
        baseTransaction.lrange(key5, 0, -2);
        baseTransaction.lpop(key5);
        baseTransaction.lpopCount(key5, 2);

        baseTransaction.rpush(key6, new String[] {value1, value2, value2});
        baseTransaction.rpop(key6);
        baseTransaction.rpopCount(key6, 2);

        baseTransaction.sadd(key7, new String[] {"baz", "foo"});
        baseTransaction.srem(key7, new String[] {"foo"});
        baseTransaction.scard(key7);
        baseTransaction.smembers(key7);

        baseTransaction.zadd(key8, Map.of("one", 1.0, "two", 2.0, "three", 3.0));
        baseTransaction.zaddIncr(key8, "one", 3);
        baseTransaction.zrem(key8, new String[] {"one"});
        baseTransaction.zcard(key8);

        baseTransaction.configSet(Map.of("timeout", "1000"));
        baseTransaction.configGet(new String[] {"timeout"});

        baseTransaction.configResetStat();

        return baseTransaction;
    }

    public static Object[] transactionTestResult() {
        return new Object[] {
            OK, // set(key1, value1)
            value1, // get(key1)
            null, // set(key2, value2, returnOldValue(true))
            new String[] {value1, value2}, // customCommand(new String[] {"MGET", key1, key2})
            1L, // exists(new String[] {key1});
            1L, // del(new String[] {key1});
            null, // get(key1);
            1L, // unlink(new String[] {key2});
            null, // get(key2);
            OK, // mset(Map.of(key1, value2, key2, value1));
            new String[] {value2, value1}, // mget(new String[] {key1, key2});
            1L, // incr(key3);
            3L, // incrBy(key3, 2);
            2L, // decr(key3);
            0L, // decrBy(key3, 2);
            0.5, // incrByFloat(key3, 0.5);
            1L, // unlink(new String[] {key3});
            2L, // hset(key4, Map.of(field1, value1, field2, value2));
            value1, // hget(key4, field1);
            true, // hexists(key4, field2);
            new String[] {
                value1, null, value2
            }, // hmget(key4, new String[] {field1, "non_existing_field", field2});
            Map.of(field1, value1, field2, value2), // hgetall(key4);
            1L, // hdel(key4, new String[] {field1});
            5L, // hincrBy(key4, field3, 5);
            10.5, // hincrByFloat(key4, field3, 5.5);
            5L, // lpush(key5, new String[] {value1, value1, value2, value3, value3});
            5L, // llen(key5);
            1L, // lrem(key5, 1, value1);
            OK, // ltrim(key5, 1, -1);
            new String[] {value3, value2}, // lrange(key5, 0, -2);
            value3, // lpop(key5);
            new String[] {value2, value1}, // lpopCount(key5, 2);
            3L, // rpush(key6, new String[] {value1, value2, value2});
            value2, // rpop(key6);
            new String[] {value2, value1}, // rpopCount(key6, 2);
            2L, // sadd(key7, new String[] {"baz", "foo"});
            1L, // srem(key7, new String[] {"foo"});
            1L, // scard(key7);
            Set.of("baz"), // smembers(key7);
            3L, // zadd(key8, Map.of("one", 1.0, "two", 2.0, "three", 3.0));
            4.0, // zaddIncr(key8, "one", 3);
            1L, // zrem(key8, new String[] {"one"});
            2L, // zcard(key8);
            OK, // configSet(Map.of("timeout", "1000"));
            Map.of("timeout", "1000"), // configGet(new String[] {"timeout"});
            OK // configResetStat();
        };
    }
}
