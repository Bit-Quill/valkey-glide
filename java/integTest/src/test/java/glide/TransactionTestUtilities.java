/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import static glide.api.BaseClient.OK;

import glide.api.models.BaseTransaction;
import glide.api.models.commands.SetOptions;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TransactionTestUtilities {
    private static final String stringKey1 = "{key}" + UUID.randomUUID();
    private static final String stringKey2 = "{key}" + UUID.randomUUID();
    private static final String stringKey3 = "{key}" + UUID.randomUUID();
    private static final String hashKey1 = "{key}" + UUID.randomUUID();
    private static final String listKey1 = "{key}" + UUID.randomUUID();
    private static final String listKey2 = "{key}" + UUID.randomUUID();
    private static final String setKey1 = "{key}" + UUID.randomUUID();
    private static final String zsetKey1 = "{key}" + UUID.randomUUID();
    private static final String streamKey1 = "{key}" + UUID.randomUUID();
    private static final String value1 = "value1-" + UUID.randomUUID();
    private static final String value2 = "value2-" + UUID.randomUUID();
    private static final String value3 = "value3-" + UUID.randomUUID();
    private static final String field1 = "field1-" + UUID.randomUUID();
    private static final String field2 = "field2-" + UUID.randomUUID();
    private static final String field3 = "field3-" + UUID.randomUUID();

    public static BaseTransaction<?> transactionTest(BaseTransaction<?> baseTransaction) {

        baseTransaction.set(stringKey1, value1);
        baseTransaction.get(stringKey1);

        baseTransaction.set(stringKey2, value2, SetOptions.builder().returnOldValue(true).build());
        baseTransaction.customCommand(new String[] {"MGET", stringKey1, stringKey2});

        baseTransaction.exists(new String[] {stringKey1});

        baseTransaction.del(new String[] {stringKey1});
        baseTransaction.get(stringKey1);

        baseTransaction.unlink(new String[] {stringKey2});
        baseTransaction.get(stringKey2);

        baseTransaction.mset(Map.of(stringKey1, value2, stringKey2, value1));
        baseTransaction.mget(new String[] {stringKey1, stringKey2});

        baseTransaction.incr(stringKey3);
        baseTransaction.incrBy(stringKey3, 2);

        baseTransaction.decr(stringKey3);
        baseTransaction.decrBy(stringKey3, 2);

        baseTransaction.incrByFloat(stringKey3, 0.5);

        baseTransaction.unlink(new String[] {stringKey3});

        baseTransaction.hset(hashKey1, Map.of(field1, value1, field2, value2));
        baseTransaction.hget(hashKey1, field1);
        baseTransaction.hexists(hashKey1, field2);
        baseTransaction.hmget(hashKey1, new String[] {field1, "non_existing_field", field2});
        baseTransaction.hgetall(hashKey1);
        baseTransaction.hdel(hashKey1, new String[] {field1});

        baseTransaction.hincrBy(hashKey1, field3, 5);
        baseTransaction.hincrByFloat(hashKey1, field3, 5.5);

        baseTransaction.lpush(listKey1, new String[] {value1, value1, value2, value3, value3});
        baseTransaction.llen(listKey1);
        baseTransaction.lrem(listKey1, 1, value1);
        baseTransaction.ltrim(listKey1, 1, -1);
        baseTransaction.lrange(listKey1, 0, -2);
        baseTransaction.lpop(listKey1);
        baseTransaction.lpopCount(listKey1, 2);

        baseTransaction.rpush(listKey2, new String[] {value1, value2, value2, value3});
        baseTransaction.rpop(listKey2);
        baseTransaction.rpopCount(listKey2, 2);

        baseTransaction.sadd(setKey1, new String[] {"baz", "foo"});
        baseTransaction.srem(setKey1, new String[] {"foo"});
        baseTransaction.scard(setKey1);
        baseTransaction.smembers(setKey1);

        baseTransaction.zadd(zsetKey1, Map.of("one", 1.0, "two", 2.0, "three", 3.0));
        baseTransaction.zaddIncr(zsetKey1, "one", 3);
        baseTransaction.zrem(zsetKey1, new String[] {"one"});
        baseTransaction.zcard(zsetKey1);

        baseTransaction.configSet(Map.of("timeout", "1000"));
        baseTransaction.configGet(new String[] {"timeout"});

        baseTransaction.configResetStat();

        baseTransaction
                .type(stringKey3)
                .type(stringKey1)
                .type(hashKey1)
                .type(listKey2)
                .type(setKey1)
                .type(zsetKey1)
                // TODO rework once steam commands implemented
                .customCommand(new String[] {"xadd", streamKey1, "1", "2", "3"})
                .type(streamKey1);

        return baseTransaction;
    }

    public static Object[] transactionTestResult() {
        return new Object[] {
            OK, // set
            value1, // get
            null, // set
            new String[] {value1, value2}, // custom
            1L, // exists
            1L, // del
            null, // get
            1L, // unlink
            null, // get
            OK, // mset
            new String[] {value2, value1}, // mget
            1L, // incr
            3L, // incrby
            2L, // decr
            0L, // decrby
            0.5, // incrbyfload
            1L, // unlink
            2L, // hset
            value1, // hget
            true, // hexists
            new String[] {value1, null, value2}, // hmget
            Map.of(field1, value1, field2, value2), // hgetall
            1L, // hdel
            5L, // hincrby
            10.5, // hincrbyfload
            5L, // lpush
            5L, // llen
            1L, // lrem
            OK, // lltrim
            new String[] {value3, value2}, // lrange
            value3, // lpop
            new String[] {value2, value1}, // lpopcount
            4L, // rpush
            value3, // rpop
            new String[] {value2, value2}, // rpopcount
            2L, // sadd
            1L, // srem
            1L, // scard
            Set.of("baz"), // smembers
            3L, // zadd
            4.0, // zaddincr
            1L, // zrem
            2L, // zcard
            OK, // config set
            Map.of("timeout", "1000"), // config get
            OK, // config resetstat
            "none", // type
            "string", // type
            "hash", // type
            "list", // type
            "set", // type
            "zset", // type
            "1-0", // custom
            "stream" // type
        };
    }
}
