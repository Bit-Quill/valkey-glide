package glide.api.models;

import static glide.api.models.commands.SetOptions.RETURN_OLD_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static redis_request.RedisRequestOuterClass.RequestType.GetString;
import static redis_request.RedisRequestOuterClass.RequestType.Info;
import static redis_request.RedisRequestOuterClass.RequestType.Ping;
import static redis_request.RedisRequestOuterClass.RequestType.SetString;

import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.SetOptions;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import redis_request.RedisRequestOuterClass;

public class ClusterTransactionTests {
    @Test
    public void transaction_builds_protobuf_request() {

        ClusterTransaction transaction = new ClusterTransaction();

        List<Pair<RedisRequestOuterClass.RequestType, RedisRequestOuterClass.Command.ArgsArray>>
                results = new LinkedList<>();

        transaction.get("key");
        results.add(
                Pair.of(
                        GetString,
                        RedisRequestOuterClass.Command.ArgsArray.newBuilder().addArgs("key").build()));

        transaction.set("key", "value");
        results.add(
                Pair.of(
                        SetString,
                        RedisRequestOuterClass.Command.ArgsArray.newBuilder()
                                .addArgs("key")
                                .addArgs("value")
                                .build()));

        transaction.set("key", "value", SetOptions.builder().returnOldValue(true).build());
        results.add(
                Pair.of(
                        SetString,
                        RedisRequestOuterClass.Command.ArgsArray.newBuilder()
                                .addArgs("key")
                                .addArgs("value")
                                .addArgs(RETURN_OLD_VALUE)
                                .build()));

        transaction.ping();
        results.add(Pair.of(Ping, RedisRequestOuterClass.Command.ArgsArray.newBuilder().build()));

        transaction.ping("KING PONG");
        results.add(
                Pair.of(
                        Ping,
                        RedisRequestOuterClass.Command.ArgsArray.newBuilder().addArgs("KING PONG").build()));

        transaction.info();
        results.add(Pair.of(Info, RedisRequestOuterClass.Command.ArgsArray.newBuilder().build()));

        transaction.info(InfoOptions.builder().section(InfoOptions.Section.EVERYTHING).build());
        results.add(
                Pair.of(
                        Info,
                        RedisRequestOuterClass.Command.ArgsArray.newBuilder()
                                .addArgs(InfoOptions.Section.EVERYTHING.toString())
                                .build()));

        var protobufTransaction = transaction.getTransactionBuilder().build();

        for (int idx = 0; idx < protobufTransaction.getCommandsCount(); idx++) {
            RedisRequestOuterClass.Command protobuf = protobufTransaction.getCommands(idx);

            assertEquals(results.get(idx).getLeft(), protobuf.getRequestType());
            assertEquals(
                    results.get(idx).getRight().getArgsCount(), protobuf.getArgsArray().getArgsCount());
            assertEquals(results.get(idx).getRight(), protobuf.getArgsArray());
        }
    }
}
