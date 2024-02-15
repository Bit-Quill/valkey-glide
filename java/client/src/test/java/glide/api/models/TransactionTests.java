/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models;

import static glide.api.models.commands.SetOptions.RETURN_OLD_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static redis_request.RedisRequestOuterClass.RequestType.GetString;
import static redis_request.RedisRequestOuterClass.RequestType.Info;
import static redis_request.RedisRequestOuterClass.RequestType.LLen;
import static redis_request.RedisRequestOuterClass.RequestType.LPop;
import static redis_request.RedisRequestOuterClass.RequestType.LPush;
import static redis_request.RedisRequestOuterClass.RequestType.LRange;
import static redis_request.RedisRequestOuterClass.RequestType.LRem;
import static redis_request.RedisRequestOuterClass.RequestType.LTrim;
import static redis_request.RedisRequestOuterClass.RequestType.Ping;
import static redis_request.RedisRequestOuterClass.RequestType.SetString;

import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.SetOptions;
import java.util.LinkedList;
import java.util.List;
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

        transaction.lpush("key", new String[] {"element1", "element2"});
        results.add(
                Pair.of(
                        LPush,
                        ArgsArray.newBuilder().addArgs("key").addArgs("element1").addArgs("element2").build()));

        transaction.lpop("key");
        results.add(Pair.of(LPop, ArgsArray.newBuilder().addArgs("key").build()));

        transaction.lpopCount("key", 2);
        results.add(Pair.of(LPop, ArgsArray.newBuilder().addArgs("key").addArgs("2").build()));

        transaction.lrange("key", 1, 2);
        results.add(
                Pair.of(LRange, ArgsArray.newBuilder().addArgs("key").addArgs("1").addArgs("2").build()));

        transaction.llen("key");
        results.add(Pair.of(LLen, ArgsArray.newBuilder().addArgs("key").build()));

        transaction.ltrim("key", 1, 2);
        results.add(
                Pair.of(LTrim, ArgsArray.newBuilder().addArgs("key").addArgs("1").addArgs("2").build()));

        transaction.lrem("key", 1, "element");
        results.add(
                Pair.of(
                        LRem, ArgsArray.newBuilder().addArgs("key").addArgs("1").addArgs("element").build()));

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
