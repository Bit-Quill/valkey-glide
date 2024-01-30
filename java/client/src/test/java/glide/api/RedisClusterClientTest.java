package glide.api;

import static glide.ProtobufArgumentMatchers.ProtobufRouteMatcher;
import static glide.ProtobufArgumentMatchers.ProtobufSingleCommandMatcher;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static redis_request.RedisRequestOuterClass.RequestType.CustomCommand;
import static redis_request.RedisRequestOuterClass.RequestType.Info;

import glide.api.models.ClusterValue;
import glide.api.models.commands.InfoOptions;
import glide.api.models.configuration.RequestRoutingConfiguration;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
        String[] arguments = new String[] {cmd, key};
        CompletableFuture<ClusterValue<Object>> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(ClusterValue.of(value));
        when(commandManager.<ClusterValue<Object>>submitNewCommand(
                        argThat(new ProtobufSingleCommandMatcher(CustomCommand, arguments)), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<ClusterValue<Object>> response = service.customCommand(arguments);

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
        String[] arguments = new String[] {cmd, key};
        CompletableFuture<ClusterValue<Object>> testResponse = mock(CompletableFuture.class);
        InterruptedException interruptedException = new InterruptedException();
        when(testResponse.get()).thenThrow(interruptedException);
        when(commandManager.<ClusterValue<Object>>submitNewCommand(
                        argThat(new ProtobufSingleCommandMatcher(CustomCommand, arguments)), any()))
                .thenReturn(testResponse);

        // exercise
        InterruptedException exception =
                assertThrows(
                        InterruptedException.class,
                        () -> {
                            CompletableFuture<ClusterValue<Object>> response = service.customCommand(arguments);
                            response.get();
                        });

        // verify
        assertEquals(interruptedException, exception);
    }

    @Test
    public void info_success() throws ExecutionException, InterruptedException {
        // setup

        CompletableFuture<ClusterValue<Map>> testResponse = mock(CompletableFuture.class);
        Map testPayload = new HashMap<String, String>();
        testPayload.put("key1", "value1");
        testPayload.put("key2", "value2");
        testPayload.put("key3", "value3");
        when(testResponse.get()).thenReturn(ClusterValue.of(testPayload));
        when(commandManager.<ClusterValue<Map>>submitNewCommand(
                        argThat(new ProtobufSingleCommandMatcher(Info, new String[0])), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<ClusterValue<Map>> response = service.info();

        // verify
        assertEquals(testResponse, response);
        ClusterValue<Map> clusterValue = response.get();
        assertTrue(clusterValue.hasMultiData());
        Map payload = clusterValue.getMultiValue();
        assertEquals(testPayload, payload);
    }

    @Test
    public void info_withRoute_success() throws ExecutionException, InterruptedException {
        // setup
        CompletableFuture<ClusterValue<Map>> testResponse = mock(CompletableFuture.class);
        Map testPayloadAddr1 = Map.of("key1", "value1", "key2", "value2");
        Map testPayloadAddr2 = Map.of("key3", "value3", "key4", "value4");
        Map<String, Map> testClusterValue =
                Map.of("addr1", testPayloadAddr1, "addr2", testPayloadAddr2);
        RequestRoutingConfiguration.Route route = RequestRoutingConfiguration.SimpleRoute.ALL_NODES;
        when(testResponse.get()).thenReturn(ClusterValue.of(testClusterValue));
        when(commandManager.<ClusterValue<Map>>submitNewCommand(
                        and(
                                argThat(new ProtobufRouteMatcher(route)),
                                argThat(new ProtobufSingleCommandMatcher(Info, new String[0]))),
                        any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<ClusterValue<Map>> response = service.info(route);

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
        String[] infoArguments = new String[] {"ALL", "DEFAULT"};
        CompletableFuture<ClusterValue<Map>> testResponse = mock(CompletableFuture.class);
        Map testPayloadAddr1 = Map.of("key1", "value1", "key2", "value2");
        Map testPayloadAddr2 = Map.of("key3", "value3", "key4", "value4");
        Map<String, Map> testClusterValue =
                Map.of("addr1", testPayloadAddr1, "addr2", testPayloadAddr2);
        when(testResponse.get()).thenReturn(ClusterValue.of(testClusterValue));
        RequestRoutingConfiguration.Route route = RequestRoutingConfiguration.SimpleRoute.ALL_PRIMARIES;
        when(commandManager.<ClusterValue<Map>>submitNewCommand(
                        and(
                                argThat(new ProtobufRouteMatcher(route)),
                                argThat(new ProtobufSingleCommandMatcher(Info, infoArguments))),
                        any()))
                .thenReturn(testResponse);

        // exercise
        InfoOptions options =
                InfoOptions.builder()
                        .section(InfoOptions.Section.ALL)
                        .section(InfoOptions.Section.DEFAULT)
                        .build();
        CompletableFuture<ClusterValue<Map>> response = service.info(options, route);

        // verify
        assertEquals(testResponse, response);
        ClusterValue<Map> clusterValue = response.get();
        assertTrue(clusterValue.hasMultiData());
        Map<String, Map> clusterMap = clusterValue.getMultiValue();
        assertEquals(testPayloadAddr1, clusterMap.get("addr1"));
        assertEquals(testPayloadAddr2, clusterMap.get("addr2"));
    }
}
