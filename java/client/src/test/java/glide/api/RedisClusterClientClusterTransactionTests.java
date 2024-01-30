package glide.api;

import static glide.ProtobufArgumentMatchers.ProtobufRouteMatcher;
import static glide.ProtobufArgumentMatchers.ProtobufTransactionMatcher;
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

import glide.ProtobufArgumentMatchers;
import glide.api.models.ClusterTransaction;
import glide.api.models.ClusterValue;
import glide.api.models.commands.InfoOptions;
import glide.api.models.configuration.RequestRoutingConfiguration;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RedisClusterClientClusterTransactionTests {

    RedisClusterClient service;

    ConnectionManager connectionManager;

    CommandManager commandManager;

    @BeforeEach
    public void setUp() {
        connectionManager = mock(ConnectionManager.class);
        commandManager = mock(CommandManager.class);
        service = new RedisClusterClient(connectionManager, commandManager);
    }

    @SneakyThrows
    @Test
    public void transaction_success() {
        // setup
        String[] arg1 = new String[] {"GETSTRING", "one"};
        String[] arg2 = new String[] {"GETSTRING", "two"};
        String[] arg3 = new String[] {"GETSTRING", "two"};
        ClusterTransaction trans = new ClusterTransaction(Optional.empty());
        trans.customCommand(arg1).customCommand(arg2).customCommand(arg3);
        Object[] testObj = new Object[] {};
        CompletableFuture<ClusterValue<Object[]>> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(ClusterValue.of(testObj));

        when(commandManager.<ClusterValue<Object[]>>submitNewCommand(
                        argThat(
                                new ProtobufArgumentMatchers.ProtobufTransactionMatcher(
                                        List.of(
                                                Pair.of(CustomCommand, arg1),
                                                Pair.of(CustomCommand, arg2),
                                                Pair.of(CustomCommand, arg3)))),
                        any()))
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
        String[] arg1 = new String[] {"GETSTRING", "one"};
        String[] arg2 = new String[] {"GETSTRING", "two"};
        String[] arg3 = new String[] {"GETSTRING", "two"};
        RequestRoutingConfiguration.Route route = RequestRoutingConfiguration.SimpleRoute.RANDOM;
        ClusterTransaction trans = new ClusterTransaction(Optional.of(route));
        trans.customCommand(arg1).customCommand(arg2).customCommand(arg3);

        InterruptedException interruptedException = new InterruptedException();
        CompletableFuture<ClusterValue<Object[]>> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenThrow(interruptedException);

        when(commandManager.<ClusterValue<Object[]>>submitNewCommand(
                        and(
                                argThat(
                                        new ProtobufTransactionMatcher(
                                                List.of(
                                                        Pair.of(CustomCommand, arg1),
                                                        Pair.of(CustomCommand, arg2),
                                                        Pair.of(CustomCommand, arg3)))),
                                argThat(new ProtobufRouteMatcher(route))),
                        any()))
                .thenReturn(testResponse);

        // exercise
        InterruptedException exception =
                assertThrows(
                        InterruptedException.class,
                        () -> {
                            CompletableFuture response = service.exec(trans, route);
                            response.get();
                        });

        // verify
        assertEquals(interruptedException, exception);
    }

    @SneakyThrows
    @Test
    public void transaction_info_success() {
        // setup
        int slotId = 99;
        InfoOptions infoOptions =
                InfoOptions.builder()
                        .section(InfoOptions.Section.SERVER)
                        .section(InfoOptions.Section.ERRORSTATS)
                        .build();
        InfoOptions infoOptionsEverything =
                InfoOptions.builder().section(InfoOptions.Section.EVERYTHING).build();
        RequestRoutingConfiguration.Route route =
                new RequestRoutingConfiguration.SlotIdRoute(
                        slotId, RequestRoutingConfiguration.SlotType.PRIMARY);
        ClusterTransaction trans = new ClusterTransaction(Optional.of(route));
        trans.info().info(infoOptions).info(infoOptionsEverything);

        Map infoValue = Map.of("default", "value");
        Map infoOptionsValue =
                Map.of(
                        InfoOptions.Section.SERVER.toString(), "value",
                        InfoOptions.Section.ERRORSTATS.toString(), "value");
        Map infoOptionsEverythingValue = Map.of(InfoOptions.Section.EVERYTHING.toString(), "value");
        Object[] testObj = new Object[] {infoValue, infoOptionsValue, infoOptionsEverythingValue};

        CompletableFuture<ClusterValue<Object[]>> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(ClusterValue.of(testObj));
        when(commandManager.<ClusterValue<Object[]>>submitNewCommand(
                        and(
                                argThat(
                                        new ProtobufTransactionMatcher(
                                                List.of(
                                                        Pair.of(Info, new String[0]),
                                                        Pair.of(
                                                                Info,
                                                                new String[] {
                                                                    InfoOptions.Section.SERVER.toString(),
                                                                    InfoOptions.Section.ERRORSTATS.toString()
                                                                }),
                                                        Pair.of(
                                                                Info, new String[] {InfoOptions.Section.EVERYTHING.toString()})))),
                                argThat(new ProtobufRouteMatcher(route))),
                        any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<ClusterValue<Object[]>> response = service.exec(trans, route);

        // verify
        assertEquals(testResponse, response);
        ClusterValue<Object[]> clusterResponse = response.get();
        assertTrue(clusterResponse.hasSingleData());
        Object[] payload = clusterResponse.getSingleValue();
        assertEquals(testObj, payload);
    }
}
