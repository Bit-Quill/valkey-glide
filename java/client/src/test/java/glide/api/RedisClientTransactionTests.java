package glide.api;

// import static glide.ProtobufArgumentMatchers.ProtobufTransactionMatcher;
// import static glide.api.models.commands.SetOptions.CONDITIONAL_SET_ONLY_IF_EXISTS;
// import static glide.api.models.commands.SetOptions.ConditionalSet.ONLY_IF_EXISTS;
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNull;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.argThat;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;
// import static redis_request.RedisRequestOuterClass.RequestType.CustomCommand;
// import static redis_request.RedisRequestOuterClass.RequestType.GetString;
// import static redis_request.RedisRequestOuterClass.RequestType.Info;
// import static redis_request.RedisRequestOuterClass.RequestType.Ping;
// import static redis_request.RedisRequestOuterClass.RequestType.SetString;
//
// import glide.api.models.Transaction;
// import glide.api.models.commands.InfoOptions;
// import glide.api.models.commands.SetOptions;
// import glide.managers.CommandManager;
// import glide.managers.ConnectionManager;
// import java.util.List;
// import java.util.Map;
// import java.util.concurrent.CompletableFuture;
// import lombok.SneakyThrows;
// import org.apache.commons.lang3.tuple.Pair;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;

public class RedisClientTransactionTests {
    //
    //    RedisClient service;
    //
    //    ConnectionManager connectionManager;
    //
    //    CommandManager commandManager;
    //
    //    @BeforeEach
    //    public void setUp() {
    //        connectionManager = mock(ConnectionManager.class);
    //        commandManager = mock(CommandManager.class);
    //        service = new RedisClient(connectionManager, commandManager);
    //    }
    //
    //    @SneakyThrows
    //    @Test
    //    public void transaction_success() {
    //        // setup
    //        String[] arg1 = new String[] {"GETSTRING", "one"};
    //        String[] arg2 = new String[] {"GETSTRING", "two"};
    //        String[] arg3 = new String[] {"GETSTRING", "two"};
    //        Transaction trans = new Transaction();
    //        trans.customCommand(arg1).customCommand(arg2).customCommand(arg3);
    //
    //        Object[] testObj = new Object[] {};
    //        CompletableFuture<Object[]> testResponse = mock(CompletableFuture.class);
    //        when(testResponse.get()).thenReturn(testObj);
    //        when(commandManager.<Object[]>submitNewCommand(
    //                        argThat(
    //                                new ProtobufTransactionMatcher(
    //                                        List.of(
    //                                                Pair.of(CustomCommand, arg1),
    //                                                Pair.of(CustomCommand, arg2),
    //                                                Pair.of(CustomCommand, arg3)))),
    //                        any()))
    //                .thenReturn(testResponse);
    //
    //        // exercise
    //        CompletableFuture<Object[]> response = service.exec(trans);
    //        Object[] payload = response.get();
    //
    //        // verify
    //        assertEquals(testObj, payload);
    //    }
    //
    //    @SneakyThrows
    //    @Test
    //    public void transaction_interruptedException() {
    //        // setup
    //        String[] arg1 = new String[] {"GETSTRING", "one"};
    //        String[] arg2 = new String[] {"GETSTRING", "two"};
    //        String[] arg3 = new String[] {"GETSTRING", "two"};
    //        Transaction trans = new Transaction();
    //        trans.customCommand(arg1).customCommand(arg2).customCommand(arg3);
    //
    //        InterruptedException interruptedException = new InterruptedException();
    //        CompletableFuture<Object[]> testResponse = mock(CompletableFuture.class);
    //        when(testResponse.get()).thenThrow(interruptedException);
    //        when(commandManager.<Object[]>submitNewCommand(
    //                        argThat(
    //                                new ProtobufTransactionMatcher(
    //                                        List.of(
    //                                                Pair.of(CustomCommand, arg1),
    //                                                Pair.of(CustomCommand, arg2),
    //                                                Pair.of(CustomCommand, arg3)))),
    //                        any()))
    //                .thenReturn(testResponse);
    //
    //        // exercise
    //        InterruptedException exception =
    //                assertThrows(
    //                        InterruptedException.class,
    //                        () -> {
    //                            CompletableFuture response = service.exec(trans);
    //                            response.get();
    //                        });
    //
    //        // verify
    //        assertEquals(interruptedException, exception);
    //    }
    //
    //    @SneakyThrows
    //    @Test
    //    public void transaction_getSet_success() {
    //        // setup
    //        String key = "testKey";
    //        String value = "testValue";
    //        SetOptions setOptions = SetOptions.builder().conditionalSet(ONLY_IF_EXISTS).build();
    //        Transaction trans = new Transaction();
    //        trans.set(key, value).get(key).set(key, value, setOptions);
    //
    //        Object[] testObj = new Object[] {null, value, null};
    //        CompletableFuture<Object[]> testResponse = mock(CompletableFuture.class);
    //        when(testResponse.get()).thenReturn(testObj);
    //        when(commandManager.<Object[]>submitNewCommand(
    //                        argThat(
    //                                new ProtobufTransactionMatcher(
    //                                        List.of(
    //                                                Pair.of(SetString, new String[] {key, value}),
    //                                                Pair.of(GetString, new String[] {key}),
    //                                                Pair.of(
    //                                                        SetString,
    //                                                        new String[] {key, value,
    // CONDITIONAL_SET_ONLY_IF_EXISTS})))),
    //                        any()))
    //                .thenReturn(testResponse);
    //
    //        // exercise
    //        CompletableFuture<Object[]> response = service.exec(trans);
    //        Object[] payload = response.get();
    //
    //        // verify
    //        assertNull(payload[0]);
    //        assertEquals(value, payload[1]);
    //        assertNull(payload[2]);
    //    }
    //
    //    @SneakyThrows
    //    @Test
    //    public void transaction_pingPong_success() {
    //        // setup
    //        String msg = "PONGPONG";
    //        Transaction trans = new Transaction();
    //        trans.ping().ping(msg).ping();
    //
    //        Object[] testObj = new Object[] {null, msg, null};
    //        CompletableFuture<Object[]> testResponse = mock(CompletableFuture.class);
    //        when(testResponse.get()).thenReturn(testObj);
    //        // TODO update to expect the correct protobuf request
    //        when(commandManager.<Object[]>submitNewCommand(
    //                        argThat(
    //                                new ProtobufTransactionMatcher(
    //                                        List.of(
    //                                                Pair.of(Ping, new String[0]),
    //                                                Pair.of(Ping, new String[] {msg}),
    //                                                Pair.of(Ping, new String[0])))),
    //                        any()))
    //                .thenReturn(testResponse);
    //
    //        // exercise
    //        CompletableFuture<Object[]> response = service.exec(trans);
    //        Object[] payload = response.get();
    //
    //        // verify
    //        assertNull(payload[0]);
    //        assertEquals(msg, payload[1]);
    //        assertNull(payload[2]);
    //    }
    //
    //    @SneakyThrows
    //    @Test
    //    public void transaction_info_success() {
    //        // setup
    //        InfoOptions infoOptions =
    //                InfoOptions.builder()
    //                        .section(InfoOptions.Section.SERVER)
    //                        .section(InfoOptions.Section.ERRORSTATS)
    //                        .build();
    //        InfoOptions infoOptionsEverything =
    //                InfoOptions.builder().section(InfoOptions.Section.EVERYTHING).build();
    //        Transaction trans = new Transaction();
    //        trans.info().info(infoOptions).info(infoOptionsEverything);
    //
    //        Map infoValue = Map.of("default", "value");
    //        Map infoOptionsValue =
    //                Map.of(
    //                        InfoOptions.Section.SERVER.toString(), "value",
    //                        InfoOptions.Section.ERRORSTATS.toString(), "value");
    //        Map infoOptionsEverythingValue = Map.of(InfoOptions.Section.EVERYTHING.toString(),
    // "value");
    //
    //        Object[] testObj = new Object[] {infoValue, infoOptionsValue,
    // infoOptionsEverythingValue};
    //        CompletableFuture<Object[]> testResponse = mock(CompletableFuture.class);
    //        when(testResponse.get()).thenReturn(testObj);
    //        when(commandManager.<Object[]>submitNewCommand(
    //                        argThat(
    //                                new ProtobufTransactionMatcher(
    //                                        List.of(
    //                                                Pair.of(Info, new String[0]),
    //                                                Pair.of(
    //                                                        Info,
    //                                                        new String[] {
    //
    // InfoOptions.Section.SERVER.toString(),
    //
    // InfoOptions.Section.ERRORSTATS.toString()
    //                                                        }),
    //                                                Pair.of(Info, new String[]
    // {InfoOptions.Section.EVERYTHING.toString()})))),
    //                        any()))
    //                .thenReturn(testResponse);
    //
    //        // exercise
    //        CompletableFuture<Object[]> response = service.exec(trans);
    //        Object[] payload = response.get();
    //
    //        // verify
    //        assertEquals(infoValue, payload[0]);
    //        assertEquals(infoOptionsValue, payload[1]);
    //        assertEquals(infoOptionsEverythingValue, payload[2]);
    //    }
}
