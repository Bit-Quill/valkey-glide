/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models;

import static glide.api.models.commands.SetOptions.RETURN_OLD_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static redis_request.RedisRequestOuterClass.RequestType.GetString;
import static redis_request.RedisRequestOuterClass.RequestType.Info;
import static redis_request.RedisRequestOuterClass.RequestType.Ping;
import static redis_request.RedisRequestOuterClass.RequestType.SetString;
import static redis_request.RedisRequestOuterClass.RequestType.Zadd;
import static redis_request.RedisRequestOuterClass.RequestType.Zcard;
import static redis_request.RedisRequestOuterClass.RequestType.Zrem;

import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.SetOptions;
import glide.api.models.commands.ZaddOptions;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import redis_request.RedisRequestOuterClass.Command;
import redis_request.RedisRequestOuterClass.Command.ArgsArray;
import redis_request.RedisRequestOuterClass.RequestType;

public class TransactionTests {
    @Test
    public void transaction_builds_protobuf_request() {
        Transaction transaction = new Transaction();

        List<Pair<RequestType, ArgsArray>> results = new LinkedList<>();

        transaction.get("key");
        results.add(Pair.of(GetString, ArgsArray.newBuilder().addArgs("key").build()));

        transaction.set("key", "value");
        results.add(Pair.of(SetString, ArgsArray.newBuilder().addArgs("key").addArgs("value").build()));

        transaction.set("key", "value", SetOptions.builder().returnOldValue(true).build());
        results.add(
                Pair.of(
                        SetString,
                        ArgsArray.newBuilder()
                                .addArgs("key")
                                .addArgs("value")
                                .addArgs(RETURN_OLD_VALUE)
                                .build()));

        transaction.ping();
        results.add(Pair.of(Ping, ArgsArray.newBuilder().build()));

        transaction.ping("KING PONG");
        results.add(Pair.of(Ping, ArgsArray.newBuilder().addArgs("KING PONG").build()));

        transaction.info();
        results.add(Pair.of(Info, ArgsArray.newBuilder().build()));

        transaction.info(InfoOptions.builder().section(InfoOptions.Section.EVERYTHING).build());
        results.add(
                Pair.of(
                        Info,
                        ArgsArray.newBuilder().addArgs(InfoOptions.Section.EVERYTHING.toString()).build()));

        Map<String, Double> membersScores = new HashMap<String, Double>();
        membersScores.put("member1", 1.0d);
        membersScores.put("member2", 2.0d);
        transaction.zadd(
                "key",
                membersScores,
                ZaddOptions.builder()
                        .updateOptions(ZaddOptions.UpdateOptions.SCORE_LESS_THAN_CURRENT)
                        .build(),
                true);
        results.add(
                Pair.of(
                        Zadd,
                        ArgsArray.newBuilder()
                                .addArgs("key")
                                .addArgs("LT")
                                .addArgs("CH")
                                .addArgs("2.0")
                                .addArgs("member2")
                                .addArgs("1.0")
                                .addArgs("member1")
                                .build()));

        transaction.zaddIncr(
                "key",
                "member1",
                3.0,
                ZaddOptions.builder()
                        .updateOptions(ZaddOptions.UpdateOptions.SCORE_LESS_THAN_CURRENT)
                        .build());
        results.add(
                Pair.of(
                        Zadd,
                        ArgsArray.newBuilder()
                                .addArgs("key")
                                .addArgs("LT")
                                .addArgs("INCR")
                                .addArgs("3.0")
                                .addArgs("member1")
                                .build()));

        transaction.zrem("key", new String[] {"member1", "member2"});
        results.add(
                Pair.of(
                        Zrem,
                        ArgsArray.newBuilder().addArgs("key").addArgs("member1").addArgs("member2").build()));

        transaction.zcard("key");
        results.add(Pair.of(Zcard, ArgsArray.newBuilder().addArgs("key").build()));

        var protobufTransaction = transaction.getProtobufTransaction().build();

        for (int idx = 0; idx < protobufTransaction.getCommandsCount(); idx++) {
            Command protobuf = protobufTransaction.getCommands(idx);

            assertEquals(results.get(idx).getLeft(), protobuf.getRequestType());
            assertEquals(
                    results.get(idx).getRight().getArgsCount(), protobuf.getArgsArray().getArgsCount());
            assertEquals(results.get(idx).getRight(), protobuf.getArgsArray());
        }
    }
}
