package glide.api;

import static glide.api.models.configuration.Route.RouteType.ALL_NODES;
import static glide.api.models.configuration.Route.RouteType.ALL_PRIMARIES;
import static glide.api.models.configuration.Route.RouteType.RANDOM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glide.api.commands.Transaction;
import glide.api.models.ClusterValue;
import glide.api.models.commands.InfoOptions;
import glide.api.models.configuration.Route;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import glide.managers.models.Command;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RedisClusterClientTest {

    RedisClusterClient service;

    ConnectionManager connectionManager;

    CommandManager commandManager;

    @BeforeEach
    public void setUp() {
        connectionManager = mock(ConnectionManager.class);
        commandManager = mock(CommandManager.class);
        service = new RedisClusterClient(connectionManager, commandManager);
    }

    @Test
    public void customCommand_success() throws ExecutionException, InterruptedException {
        // setup
        String key = "testKey";
        String value = "testValue";
        String cmd = "GETSTRING";
        CompletableFuture<ClusterValue<Object>> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(ClusterValue.of(value));
        when(commandManager.<ClusterValue<Object>>submitNewCommand(any(), eq(Optional.empty()), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<ClusterValue<Object>> response =
                service.customCommand(new String[] {cmd, key});

        // verify
        assertEquals(testResponse, response);
        ClusterValue clusterValue = response.get();
        assertTrue(clusterValue.hasSingleData());
        String payload = (String) clusterValue.getSingleValue();
        assertEquals(value, payload);
    }

    @Test
    public void customCommand_interruptedException() throws ExecutionException, InterruptedException {
        // setup
        String key = "testKey";
        String cmd = "GETSTRING";
        CompletableFuture<ClusterValue<Object>> testResponse = mock(CompletableFuture.class);
        InterruptedException interruptedException = new InterruptedException();
        when(testResponse.get()).thenThrow(interruptedException);
        when(commandManager.<ClusterValue<Object>>submitNewCommand(any(), any(), any()))
                .thenReturn(testResponse);

        // exercise
        InterruptedException exception =
                assertThrows(
                        InterruptedException.class,
                        () -> {
                            CompletableFuture<ClusterValue<Object>> response =
                                    service.customCommand(new String[] {cmd, key});
                            response.get();
                        });

        // verify
        assertEquals(interruptedException, exception);
    }

    @Test
    public void info_success() throws ExecutionException, InterruptedException {
        // setup
        Command cmd = Command.builder().requestType(Command.RequestType.INFO).build();
        CompletableFuture<ClusterValue<Map>> testResponse = mock(CompletableFuture.class);
        Map testPayload = new HashMap<String, String>();
        testPayload.put("key1", "value1");
        testPayload.put("key2", "value2");
        testPayload.put("key3", "value3");
        when(testResponse.get()).thenReturn(ClusterValue.of(testPayload));
        when(commandManager.<ClusterValue<Map>>submitNewCommand(eq(cmd), eq(Optional.empty()), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<ClusterValue<Map>> response = service.info();

        // verify
        assertEquals(testResponse, response);
        ClusterValue<Map> clusterValue = response.get();
        assertTrue(clusterValue.hasSingleData());
        Map payload = (Map) clusterValue.getSingleValue();
        assertEquals(testPayload, payload);
    }

    @Test
    public void info_withRoute_success() throws ExecutionException, InterruptedException {
        // setup
        Command cmd = Command.builder().requestType(Command.RequestType.INFO).build();
        CompletableFuture<ClusterValue<Map>> testResponse = mock(CompletableFuture.class);
        Map testPayloadAddr1 = Map.of("key1", "value1", "key2", "value2");
        Map testPayloadAddr2 = Map.of("key3", "value3", "key4", "value4");
        Map<String, Map> testClusterValue =
                Map.of("addr1", testPayloadAddr1, "addr2", testPayloadAddr2);
        Route route = Route.builder(ALL_NODES).build();
        when(testResponse.get()).thenReturn(ClusterValue.of(testClusterValue));
        when(commandManager.<ClusterValue<Map>>submitNewCommand(eq(cmd), eq(Optional.of(route)), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<ClusterValue<Map>> response = service.info();

        // verify
        assertEquals(testResponse, response);
        ClusterValue<Map> clusterValue = response.get();
        assertTrue(clusterValue.hasMultiData());
        Map<String, Map> clusterMap = clusterValue.getMultiValue();
        assertEquals(testPayloadAddr1, clusterMap.get("addr1"));
        assertEquals(testPayloadAddr2, clusterMap.get("addr2"));
    }

    @Test
    public void infoWithRouteAndMultipleOptions_success()
            throws ExecutionException, InterruptedException {
        // setup
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.INFO)
                        .arguments(new String[] {"ALL", "DEFAULT"})
                        .build();
        CompletableFuture<ClusterValue<Map>> testResponse = mock(CompletableFuture.class);
        Map testPayloadAddr1 = Map.of("key1", "value1", "key2", "value2");
        Map testPayloadAddr2 = Map.of("key3", "value3", "key4", "value4");
        Map<String, Map> testClusterValue =
                Map.of("addr1", testPayloadAddr1, "addr2", testPayloadAddr2);
        when(testResponse.get()).thenReturn(ClusterValue.of(testClusterValue));
        Route route = Route.builder(ALL_PRIMARIES).build();
        when(commandManager.<ClusterValue<Map>>submitNewCommand(eq(cmd), eq(Optional.of(route)), any()))
                .thenReturn(testResponse);

        // exercise
        InfoOptions options =
                InfoOptions.builder()
                        .section(InfoOptions.Section.ALL)
                        .section(InfoOptions.Section.DEFAULT)
                        .build();
        CompletableFuture<ClusterValue<Map>> response = service.info(options);

        // verify
        assertEquals(testResponse, response);
        ClusterValue<Map> clusterValue = response.get();
        assertTrue(clusterValue.hasMultiData());
        Map<String, Map> clusterMap = clusterValue.getMultiValue();
        assertEquals(testPayloadAddr1, clusterMap.get("addr1"));
        assertEquals(testPayloadAddr2, clusterMap.get("addr2"));
    }

    @SneakyThrows
    @Test
    public void transaction_success() {
        // setup
        String cmd = "GETSTRING";
        Transaction trans =
                Transaction.builder()
                        .customCommand(new String[] {cmd, "one"})
                        .customCommand(new String[] {cmd, "two"})
                        .customCommand(new String[] {cmd, "three"})
                        .build();

        Object[] testObj = new Object[] {};
        CompletableFuture<ClusterValue<Object[]>> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(ClusterValue.of(testObj));
        // TODO update to expect the correct protobuf request
        when(commandManager.<ClusterValue<Object[]>>submitNewTransaction(
                        any(), eq(Optional.empty()), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<ClusterValue<Object[]>> response = service.exec(trans);

        // verify
        assertEquals(testResponse, response);
        ClusterValue<Object[]> clusterResponse = response.get();
        assertTrue(clusterResponse.hasSingleData());
        Object[] payload = clusterResponse.getSingleValue();
        assertEquals(testObj, payload);
    }

    @SneakyThrows
    @Test
    public void transaction_withRoute_interruptedException() {
        // setup
        String cmd = "GETSTRING";
        Transaction trans =
                Transaction.builder()
                        .customCommand(new String[] {cmd, "one"})
                        .customCommand(new String[] {cmd, "two"})
                        .customCommand(new String[] {cmd, "three"})
                        .build();

        InterruptedException interruptedException = new InterruptedException();
        CompletableFuture<ClusterValue<Object[]>> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenThrow(interruptedException);
        Route route = Route.builder(RANDOM).build();
        // TODO update to expect the correct protobuf request
        when(commandManager.<ClusterValue<Object[]>>submitNewTransaction(
                        any(), eq(Optional.empty()), any()))
                .thenReturn(testResponse);

        // exercise
        InterruptedException exception =
                assertThrows(
                        InterruptedException.class,
                        () -> {
                            CompletableFuture response = service.exec(trans);
                            response.get();
                        });

        // verify
        assertEquals(interruptedException, exception);
    }

    //    @SneakyThrows
    //    @Test
    //    public void transaction_info_success() {
    //        // setup
    //        InfoOptions infoOptions =
    // InfoOptions.builder().section(InfoOptions.Section.SERVER).section(InfoOptions.Section.ERRORSTATS).build();
    //        InfoOptions infoOptionsEverything =
    // InfoOptions.builder().section(InfoOptions.Section.EVERYTHING).build();
    //        Transaction trans =
    //            Transaction.builder()
    //                .info()
    //                .info(infoOptions)
    //                .info(infoOptionsEverything)
    //                .build();
    //
    //        Object[] testObj = new Object[] {null, value, null};
    //        CompletableFuture<Object[]> testResponse = mock(CompletableFuture.class);
    //        when(testResponse.get()).thenReturn(testObj);
    //        when(commandManager.<Object[]>submitNewTransaction(any(), eq(Optional.empty()),
    // any())).thenReturn(testResponse);
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

}
