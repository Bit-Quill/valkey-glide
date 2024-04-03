/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import static glide.api.BaseClient.OK;
import static glide.api.models.commands.LInsertOptions.InsertPosition.AFTER;

import glide.api.models.BaseTransaction;
import glide.api.models.commands.RangeOptions.InfScoreBound;
import glide.api.models.commands.RangeOptions.RangeByIndex;
import glide.api.models.commands.RangeOptions.ScoreBoundary;
import glide.api.models.commands.SetOptions;
import glide.api.models.commands.StreamAddOptions;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class TransactionTestUtilities {
    private static final String key1 = "{key}" + UUID.randomUUID();
    private static final String key2 = "{key}" + UUID.randomUUID();
    private static final String key3 = "{key}" + UUID.randomUUID();
    private static final String key4 = "{key}" + UUID.randomUUID();
    private static final String key5 = "{key}" + UUID.randomUUID();
    private static final String key6 = "{key}" + UUID.randomUUID();
    private static final String listKey3 = "{key}:listKey3-" + UUID.randomUUID();
    private static final String key7 = "{key}" + UUID.randomUUID();
    private static final String key8 = "{key}" + UUID.randomUUID();
    private static final String zSetKey2 = "{key}:zsetKey2-" + UUID.randomUUID();
    private static final String key9 = "{key}" + UUID.randomUUID();
    private static final String hllKey1 = "{key}:hllKey1-" + UUID.randomUUID();
    private static final String hllKey2 = "{key}:hllKey2-" + UUID.randomUUID();
    private static final String hllKey3 = "{key}:hllKey3-" + UUID.randomUUID();

    private static final String value1 = "value1-" + UUID.randomUUID();
    private static final String value2 = "value2-" + UUID.randomUUID();
    private static final String value3 = "value3-" + UUID.randomUUID();
    private static final String field1 = "field1-" + UUID.randomUUID();
    private static final String field2 = "field2-" + UUID.randomUUID();
    private static final String field3 = "field3-" + UUID.randomUUID();

    public interface TransactionBuilder extends Function<BaseTransaction<?>, Object[]> {}

/*
        baseTransaction.set(key1, value1);
        baseTransaction.get(key1);
        baseTransaction.type(key1);

        baseTransaction.set(key2, value2, SetOptions.builder().returnOldValue(true).build());
        baseTransaction.strlen(key2);
        baseTransaction.customCommand(new String[] {"MGET", key1, key2});

        baseTransaction.exists(new String[] {key1});
        baseTransaction.persist(key1);

        baseTransaction.unlink(new String[] {key3});
        baseTransaction.setrange(key3, 0, "GLIDE");

        baseTransaction.hset(key4, Map.of(field1, value1, field2, value2));
        baseTransaction.hget(key4, field1);
        baseTransaction.hlen(key4);
        baseTransaction.hexists(key4, field2);
        baseTransaction.hsetnx(key4, field1, value1);
        baseTransaction.hmget(key4, new String[] {field1, "non_existing_field", field2});
        baseTransaction.hgetall(key4);
        baseTransaction.hdel(key4, new String[] {field1});
        baseTransaction.hvals(key4);

        baseTransaction.lpush(key5, new String[] {value1, value1, value2, value3, value3});
        baseTransaction.llen(key5);
        baseTransaction.lindex(key5, 0);
        baseTransaction.lrem(key5, 1, value1);
        baseTransaction.ltrim(key5, 1, -1);
        baseTransaction.lrange(key5, 0, -2);
        baseTransaction.lpop(key5);
        baseTransaction.lpopCount(key5, 2);

        baseTransaction.sadd(key7, new String[] {"baz", "foo"});
        baseTransaction.srem(key7, new String[] {"foo"});
        baseTransaction.scard(key7);
        baseTransaction.sismember(key7, "baz");
        baseTransaction.smembers(key7);

        baseTransaction.zadd(key8, Map.of("one", 1.0, "two", 2.0, "three", 3.0));
        baseTransaction.zrank(key8, "one");
        baseTransaction.zaddIncr(key8, "one", 3);
        baseTransaction.zrem(key8, new String[] {"one"});
        baseTransaction.zcard(key8);
        baseTransaction.zmscore(key8, new String[] {"two", "three"});
        baseTransaction.zrange(key8, new RangeByIndex(0, 1));
        baseTransaction.zrangeWithScores(key8, new RangeByIndex(0, 1));
        baseTransaction.zscore(key8, "two");
        baseTransaction.zcount(key8, new ScoreBoundary(2, true), InfScoreBound.POSITIVE_INFINITY);
        baseTransaction.zpopmin(key8);
        baseTransaction.zpopmax(key8);
        baseTransaction.zremrangebyrank(key8, 5, 10);
        baseTransaction.zdiffstore(key8, new String[] {key8, key8});

        baseTransaction.zadd(zSetKey2, Map.of("one", 1.0, "two", 2.0));
        baseTransaction.zdiff(new String[] {zSetKey2, key8});
        baseTransaction.zdiffWithScores(new String[] {zSetKey2, key8});

        baseTransaction.xadd(
                key9, Map.of("field1", "value1"), StreamAddOptions.builder().id("0-1").build());
        baseTransaction.xadd(
                key9, Map.of("field2", "value2"), StreamAddOptions.builder().id("0-2").build());
        baseTransaction.xadd(
                key9, Map.of("field3", "value3"), StreamAddOptions.builder().id("0-3").build());

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

        baseTransaction.rpush(listKey2, new String[] {value1, value2, value2});
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

        baseTransaction.echo("GLIDE");

        baseTransaction.rpushx(listKey3, new String[] {"_"}).lpushx(listKey3, new String[] {"_"});
        baseTransaction
                .lpush(listKey3, new String[] {value1, value2, value3})
                .linsert(listKey3, AFTER, value2, value2);

        baseTransaction.blpop(new String[] {listKey3}, 0.01).brpop(new String[] {listKey3}, 0.01);

        baseTransaction.pfadd(hllKey1, new String[] {"a", "b", "c"});
        baseTransaction.pfcount(new String[] {hllKey1, hllKey2});
        baseTransaction
                .pfmerge(hllKey3, new String[] {hllKey1, hllKey2})
                .pfcount(new String[] {hllKey3});

        return baseTransaction;
    }*/

    public static TransactionBuilder StringCommandTransactionBuilder =
            TransactionTestUtilities::stringCommands;
    public static TransactionBuilder HashCommandTransactionBuilder =
            TransactionTestUtilities::hashCommands;
    public static TransactionBuilder ListCommandTransactionBuilder =
            TransactionTestUtilities::listCommands;
    public static TransactionBuilder SetCommandTransactionBuilder =
            TransactionTestUtilities::setCommands;
    public static TransactionBuilder SortedSetCommandTransactionBuilder =
            TransactionTestUtilities::sortedSetCommands;
    public static TransactionBuilder ServerManagementCommandTransactionBuilder =
            TransactionTestUtilities::serverManagementCommands;

    private static Object[] stringCommands(BaseTransaction<?> transaction){
        String key1 = "{StringKey}-1-" + UUID.randomUUID();
        String key2 = "{StringKey}-2-" + UUID.randomUUID();
        String key3 = "{StringKey}-3-" + UUID.randomUUID();

        transaction
            .set(key1, value1)
            .get(key1)
            .set(key2, value2, SetOptions.builder().returnOldValue(true).build())
            .customCommand(new String[]{"MGET", key1, key2})
            .exists(new String[]{key1})
            .del(new String[]{key1})
            .get(key1)
            .unlink(new String[]{key2})
            .get(key2)
            .mset(Map.of(key1, value2, key2, value1))
            .mget(new String[]{key1, key2})
            .incr(key3)
            .incrBy(key3, 2)
            .decr(key3)
            .decrBy(key3, 2)
            .incrByFloat(key3, 0.5)
            .unlink(new String[]{key3});

        return new Object[] {
            OK, // set(stringKey1, value1);
            value1, // get(stringKey1);
            null, // set(stringKey2, value2, returnOldValue(true));
            new String[] {value1, value2}, // customCommand(new String[] {"MGET", ...});
            1L, // exists(new String[] {stringKey1});
            1L, // del(new String[] {stringKey1});
            null, // get(stringKey1);
            1L, // unlink(new String[] {stringKey2});
            null, // get(stringKey2);
            OK, // mset(Map.of(stringKey1, value2, stringKey2, value1));
            new String[] {value2, value1}, // mget(new String[] {stringKey1, stringKey2});
            1L, // incr(stringKey3);
            3L, // incrBy(stringKey3, 2);
            2L, // decr(stringKey3);
            0L, // decrBy(stringKey3, 2);
            0.5, // incrByFloat(stringKey3, 0.5);
            1L, // unlink(new String[] {stringKey3});
        };
    }

      private static Object[] hashCommands(BaseTransaction<?> transaction) {
        String hashKey1 = "{HashKey}-1-" + UUID.randomUUID();

        transaction
            .hset(hashKey1, Map.of(field1, value1, field2, value2))
            .hget(hashKey1, field1)
            .hexists(hashKey1, field2)
            .hmget(hashKey1, new String[] {field1, "non_existing_field", field2})
            .hgetall(hashKey1)
            .hdel(hashKey1, new String[] {field1})
            .hincrBy(hashKey1, field3, 5)
            .hincrByFloat(hashKey1, field3, 5.5);

        return new Object[] {
            2L, // hset(hashKey1, Map.of(field1, value1, field2, value2));
            value1, // hget(hashKey1, field1);
            true, // hexists(hashKey1, field2);
            new String[] {value1, null, value2}, // hmget(hashKey1, new String[] {...});
            Map.of(field1, value1, field2, value2), // hgetall(hashKey1);
            1L, // hdel(hashKey1, new String[] {field1});
            5L, // hincrBy(hashKey1, field3, 5);
            10.5, // hincrByFloat(hashKey1, field3, 5.5);
        };
      }

      private static Object[] listCommands(BaseTransaction<?> transaction) {
        String listKey1 = "{ListKey}-1-" + UUID.randomUUID();
        String listKey2 = "{ListKey}-2-" + UUID.randomUUID();

        transaction
            .lpush(listKey1, new String[] {value1, value1, value2, value3, value3})
            .llen(listKey1)
            .lrem(listKey1, 1, value1)
            .ltrim(listKey1, 1, -1)
            .lrange(listKey1, 0, -2)
            .lpop(listKey1)
            .lpopCount(listKey1, 2)
            .rpush(listKey2, new String[] {value1, value2, value2})
            .rpop(listKey2)
            .rpopCount(listKey2, 2);

        return new Object[] {
            5L, // lpush(listKey1, new String[] {value1, value1, value2, value3, value3});
            5L, // llen(listKey1);
            1L, // lrem(listKey1, 1, value1);
            OK, // ltrim(listKey1, 1, -1);
            new String[] {value3, value2}, // lrange(listKey1, 0, -2);
            value3, // lpop(listKey1);
            new String[] {value2, value1}, // lpopCount(listKey1, 2);
            3L, // rpush(listKey2, new String[] {value1, value2, value2});
            value2, // rpop(listKey2);
            new String[] {value2, value1}, // rpopCount(listKey2, 2);
        };
      }

      private static Object[] setCommands(BaseTransaction<?> transaction) {
        String setKey1 = "{SetKey1}-1-" + UUID.randomUUID();

        transaction
            .sadd(setKey1, new String[] {"baz", "foo"})
            .srem(setKey1, new String[] {"foo"})
            .scard(setKey1)
            .smembers(setKey1);

        return new Object[] {
            2L, // sadd(setKey1, new String[] {"baz", "foo"});
            1L, // srem(setKey1, new String[] {"foo"});
            1L, // scard(setKey1);
            Set.of("baz"), // smembers(setKey1);
        };
      }

      private static Object[] sortedSetCommands(BaseTransaction<?> transaction) {
        String zsetKey1 = "{ZSetKey1}-1-" + UUID.randomUUID();

        transaction
            .zadd(zsetKey1, Map.of("one", 1.0, "two", 2.0, "three", 3.0))
            .zaddIncr(zsetKey1, "one", 3)
            .zrem(zsetKey1, new String[] {"one"})
            .zcard(zsetKey1);

        return new Object[] {
            3L, // zadd(zsetKey1, Map.of("one", 1.0, "two", 2.0, "three", 3.0));
            4.0, // zaddIncr(zsetKey1, "one", 3);
            1L, // zrem(zsetKey1, new String[] {"one"});
            2L, // zcard(zsetKey1);
        };
      }

      private static Object[] serverManagementCommands(BaseTransaction<?> transaction) {
        transaction
            .configSet(Map.of("timeout", "1000"))
            .configGet(new String[]{"timeout"})
            .configResetStat();

        return new Object[]{
            OK, // configSet(Map.of("timeout", "1000"));
            Map.of("timeout", "1000"), // configGet(new String[] {"timeout"});
            OK // configResetStat();
        };
      }
/*
    public static Object[] transactionTestResult() {
        return new Object[] {
            OK,
            value1,
            "string", // type(key1)
            null,
            (long) value1.length(), // strlen(key2)
            new String[] {value1, value2},
            1L,
            Boolean.FALSE, // persist(key1)
            1L,
            null,
            1L,
            null,
            OK,
            new String[] {value2, value1},
            1L,
            3L,
            2L,
            0L,
            0.5,
            1L,
            5L, // setrange(key3, 0, "GLIDE")
            2L,
            value1,
            2L, // hlen(key4)
            true,
            Boolean.FALSE, // hsetnx(key4, field1, value1)
            new String[] {value1, null, value2},
            Map.of(field1, value1, field2, value2),
            1L,
            new String[] {value2}, // hvals(key4)
            5L,
            10.5,
            5L,
            5L,
            value3, // lindex(key5, 0)
            1L,
            OK,
            new String[] {value3, value2},
            value3,
            new String[] {value2, value1},
            3L,
            value2,
            new String[] {value2, value1},
            2L,
            1L,
            1L,
            true, // sismember(key7, "baz")
            Set.of("baz"),
            3L,
            0L, // zrank(key8, "one")
            4.0,
            1L,
            2L,
            new Double[] {2.0, 3.0}, // zmscore(key8, new String[] {"two", "three"})
            new String[] {"two", "three"}, // zrange
            Map.of("two", 2.0, "three", 3.0), // zrangeWithScores
            2.0, // zscore(key8, "two")
            2L, // zcount(key8, new ScoreBoundary(2, true), InfScoreBound.POSITIVE_INFINITY)
            Map.of("two", 2.0), // zpopmin(key8)
            Map.of("three", 3.0), // zpopmax(key8)
            0L, // zremrangebyrank(key8, 5, 10)
            0L, // zdiffstore(key8, new String[] {key8, key8})
            2L, // zadd(zSetKey2, Map.of("one", 1.0, "two", 2.0))
            new String[] {"one", "two"}, // zdiff(new String[] {zSetKey2, key8})
            Map.of("one", 1.0, "two", 2.0), // zdiffWithScores(new String[] {zSetKey2, key8})
            "0-1", // xadd(key9, Map.of("field1", "value1"),
            // StreamAddOptions.builder().id("0-1").build());
            "0-2", // xadd(key9, Map.of("field2", "value2"),
            // StreamAddOptions.builder().id("0-2").build());
            "0-3", // xadd(key9, Map.of("field3", "value3"),
            // StreamAddOptions.builder().id("0-3").build());
            OK,
            Map.of("timeout", "1000"),
            OK,
            "GLIDE", // echo
            0L, // rpushx(listKey3, new String[] { "_" })
            0L, // lpushx(listKey3, new String[] { "_" })
            3L, // lpush(listKey3, new String[] { value1, value2, value3})
            4L, // linsert(listKey3, AFTER, value2, value2)
            new String[] {listKey3, value3}, // blpop(new String[] { listKey3 }, 0.01)
            new String[] {listKey3, value1}, // brpop(new String[] { listKey3 }, 0.01);
            1L, // pfadd(hllKey1, new String[] {"a", "b", "c"})
            3L, // pfcount(new String[] { hllKey1, hllKey2 });;
            OK, // pfmerge(hllKey3, new String[] {hllKey1, hllKey2})
            3L, // pfcount(new String[] { hllKey3 })
*/
}
