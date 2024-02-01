/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.managers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static glide.managers.RequestType.CUSTOM_COMMAND;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static redis_request.RedisRequestOuterClass.RequestType.CustomCommand;
import static response.ResponseOuterClass.RequestErrorType.UNRECOGNIZED;

import glide.api.models.configuration.RequestRoutingConfiguration.SimpleRoute;
import glide.api.models.configuration.RequestRoutingConfiguration.SlotIdRoute;
import glide.api.models.configuration.RequestRoutingConfiguration.SlotKeyRoute;
import glide.api.models.configuration.RequestRoutingConfiguration.SlotType;
import glide.api.models.Transaction;
import glide.api.models.configuration.RequestRoutingConfiguration.SimpleRoute;
import glide.api.models.configuration.RequestRoutingConfiguration.SlotIdRoute;
import glide.api.models.configuration.RequestRoutingConfiguration.SlotKeyRoute;
import glide.api.models.configuration.RequestRoutingConfiguration.SlotType;
import glide.api.models.exceptions.ClosingException;
import glide.api.models.exceptions.ConnectionException;
import glide.api.models.exceptions.ExecAbortException;
import glide.api.models.exceptions.RequestException;
import glide.api.models.exceptions.TimeoutException;
import glide.connectors.handlers.ChannelHandler;
import glide.managers.models.Command;
import java.util.Map;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import redis_request.RedisRequestOuterClass.RedisRequest;
import redis_request.RedisRequestOuterClass.SimpleRoutes;
import redis_request.RedisRequestOuterClass.SlotTypes;
import org.mockito.ArgumentCaptor;
import redis_request.RedisRequestOuterClass;
import redis_request.RedisRequestOuterClass.RedisRequest;
import response.ResponseOuterClass;
import response.ResponseOuterClass.RequestError;
import response.ResponseOuterClass.Response;

public class CommandManagerTest {

    ChannelHandler channelHandler;

    CommandManager service;

    @BeforeEach
    void init() {
        channelHandler = mock(ChannelHandler.class);
        service = new CommandManager(channelHandler);
    }

    @Test
    @SneakyThrows
    public void submitNewCommand_return_Object_result() {

        // setup
        long pointer = -1;
        Response respPointerResponse = Response.newBuilder().setRespPointer(pointer).build();
        Object respObject = mock(Object.class);

        CompletableFuture<Response> future = new CompletableFuture<>();
        future.complete(respPointerResponse);
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);

        // exercise
        CompletableFuture<Object> result =
                service.submitNewCommand(
                        CUSTOM_COMMAND,
                        new String[0],
                        Optional.empty(),
                        new BaseCommandResponseResolver((ptr) -> ptr == pointer ? respObject : null));
        Object respPointer = result.get();

        // verify
        assertEquals(respObject, respPointer);
    }

    @Test
    @SneakyThrows
    public void submitNewCommand_return_Null_result() {
        // setup
        Response respPointerResponse = Response.newBuilder().build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        future.complete(respPointerResponse);
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);

        // exercise
        CompletableFuture<Object> result =
                service.submitNewCommand(
                        CUSTOM_COMMAND,
                        new String[0],
                        Optional.empty(),
                        new BaseCommandResponseResolver((p) -> new RuntimeException("")));
        Object respPointer = result.get();

        // verify
        assertNull(respPointer);
    }

    @Test
    @SneakyThrows
    public void submitNewCommand_return_String_result() {

        // setup
        long pointer = 123;
        String testString = "TEST STRING";

        Response respPointerResponse = Response.newBuilder().setRespPointer(pointer).build();

        CompletableFuture<Response> future = new CompletableFuture<>();
        future.complete(respPointerResponse);
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);

        // exercise
        CompletableFuture<Object> result =
                service.submitNewCommand(
                        CUSTOM_COMMAND,
                        new String[0],
                        Optional.empty(),
                        new BaseCommandResponseResolver((p) -> p == pointer ? testString : null));
        Object respPointer = result.get();

        // verify
        assertTrue(respPointer instanceof String);
        assertEquals(testString, respPointer);
    }

    @Test
    public void submitNewCommand_throws_ClosingException() {

        // setup
        String errorMsg = "Closing";

        Response closingErrorResponse = Response.newBuilder().setClosingError(errorMsg).build();

        CompletableFuture<Response> future = new CompletableFuture<>();
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
        var command =
                Command.builder().requestType(Command.RequestType.CUSTOM_COMMAND).route(routeType).build();

        ArgumentCaptor<RedisRequest.Builder> captor =
                ArgumentCaptor.forClass(RedisRequest.Builder.class);

        var protobufToClientRouteMapping =
                Map.of(
                        SimpleRoutes.AllNodes, SimpleRoute.ALL_NODES,
                        SimpleRoutes.AllPrimaries, SimpleRoute.ALL_PRIMARIES,
                        SimpleRoutes.Random, SimpleRoute.RANDOM);

        service.submitNewCommand(command, r -> null);
        verify(channelHandler).write(captor.capture(), anyBoolean());
        var requestBuilder = captor.getValue();

        // exercise
        ExecutionException e =
                assertThrows(
                        ExecutionException.class,
                        () -> {
                            CompletableFuture result =
                                    service.submitNewCommand(
                                            CUSTOM_COMMAND,
                                            new String[0],
                                            Optional.empty(),
                                            new BaseCommandResponseResolver((ptr) -> new Object()));
                            result.get();
                        });

    @ParameterizedTest
    @EnumSource(value = SlotType.class)
    public void prepare_request_with_slot_id_routes(SlotType slotType) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
        var command =
                Command.builder()
                        .requestType(Command.RequestType.CUSTOM_COMMAND)
                        .route(new SlotIdRoute(42, slotType))
                        .build();

        ArgumentCaptor<RedisRequest.Builder> captor =
                ArgumentCaptor.forClass(RedisRequest.Builder.class);

        service.submitNewCommand(command, r -> null);
        verify(channelHandler).write(captor.capture(), anyBoolean());
        var requestBuilder = captor.getValue();

        var protobufToClientRouteMapping =
                Map.of(
                        SlotTypes.Primary, SlotType.PRIMARY,
                        SlotTypes.Replica, SlotType.REPLICA);

        assertAll(
                () -> assertTrue(requestBuilder.hasRoute()),
                () -> assertTrue(requestBuilder.getRoute().hasSlotIdRoute()),
                () ->
                        assertEquals(
                                slotType,
                                protobufToClientRouteMapping.get(
                                        requestBuilder.getRoute().getSlotIdRoute().getSlotType())),
                () -> assertEquals(42, requestBuilder.getRoute().getSlotIdRoute().getSlotId()),
                () -> assertFalse(requestBuilder.getRoute().hasSimpleRoutes()),
                () -> assertFalse(requestBuilder.getRoute().hasSlotKeyRoute()));
    }

    @ParameterizedTest
    @EnumSource(value = SlotType.class)
    public void prepare_request_with_slot_key_routes(SlotType slotType) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
        var command =
                Command.builder()
                        .requestType(Command.RequestType.CUSTOM_COMMAND)
                        .route(new SlotKeyRoute("TEST", slotType))
                        .build();

        ArgumentCaptor<RedisRequest.Builder> captor =
                ArgumentCaptor.forClass(RedisRequest.Builder.class);

        service.submitNewCommand(command, r -> null);
        verify(channelHandler).write(captor.capture(), anyBoolean());
        var requestBuilder = captor.getValue();

        var protobufToClientRouteMapping =
                Map.of(
                        SlotTypes.Primary, SlotType.PRIMARY,
                        SlotTypes.Replica, SlotType.REPLICA);

        assertAll(
                () -> assertTrue(requestBuilder.hasRoute()),
                () -> assertTrue(requestBuilder.getRoute().hasSlotKeyRoute()),
                () ->
                        assertEquals(
                                slotType,
                                protobufToClientRouteMapping.get(
                                        requestBuilder.getRoute().getSlotKeyRoute().getSlotType())),
                () -> assertEquals("TEST", requestBuilder.getRoute().getSlotKeyRoute().getSlotKey()),
                () -> assertFalse(requestBuilder.getRoute().hasSimpleRoutes()),
                () -> assertFalse(requestBuilder.getRoute().hasSlotIdRoute()));
    }

    @Test
    public void prepare_request_with_unknown_route_type() {
        CompletableFuture<Response> future = new CompletableFuture<>();
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
        var command =
                Command.builder()
                        .requestType(Command.RequestType.CUSTOM_COMMAND)
                        .route(() -> false)
                        .build();

        var exception =
                assertThrows(
                        ExecutionException.class,
                        () -> {
                            CompletableFuture result =
                                    service.submitNewCommand(
                                            CUSTOM_COMMAND,
                                            new String[0],
                                            Optional.empty(),
                                            new BaseCommandResponseResolver((ptr) -> null));
                            result.get();
                        });

        // verify
        switch (requestErrorType) {
            case Unspecified:
                // only Unspecified errors return a RequestException
                assertTrue(executionException.getCause() instanceof RequestException);
                break;
            case ExecAbort:
                assertTrue(executionException.getCause() instanceof ExecAbortException);
                break;
            case Timeout:
                assertTrue(executionException.getCause() instanceof TimeoutException);
                break;
            case Disconnect:
                assertTrue(executionException.getCause() instanceof ConnectionException);
                break;
            default:
                fail("Unexpected protobuf error type");
        }
        assertEquals(requestErrorType.toString(), executionException.getCause().getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = SimpleRoute.class)
    public void prepare_request_with_simple_routes(SimpleRoute routeType) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);

        ArgumentCaptor<RedisRequest.Builder> captor =
                ArgumentCaptor.forClass(RedisRequest.Builder.class);

        service.submitNewCommand(CUSTOM_COMMAND, new String[0], Optional.of(routeType), r -> null);
        verify(channelHandler).write(captor.capture(), anyBoolean());
        var requestBuilder = captor.getValue();

        assertAll(
                () -> assertTrue(requestBuilder.hasRoute()),
                () -> assertTrue(requestBuilder.getRoute().hasSimpleRoutes()),
                () ->
                        assertEquals(
                                routeType.getProtobufMapping(), requestBuilder.getRoute().getSimpleRoutes()),
                () -> assertFalse(requestBuilder.getRoute().hasSlotIdRoute()),
                () -> assertFalse(requestBuilder.getRoute().hasSlotKeyRoute()));
    }

    @ParameterizedTest
    @EnumSource(value = SlotType.class)
    public void prepare_request_with_slot_id_routes(SlotType slotType) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);

        ArgumentCaptor<RedisRequest.Builder> captor =
                ArgumentCaptor.forClass(RedisRequest.Builder.class);

        service.submitNewCommand(
                CUSTOM_COMMAND, new String[0], Optional.of(new SlotIdRoute(42, slotType)), r -> null);
        verify(channelHandler).write(captor.capture(), anyBoolean());
        var requestBuilder = captor.getValue();

        assertAll(
                () -> assertTrue(requestBuilder.hasRoute()),
                () -> assertTrue(requestBuilder.getRoute().hasSlotIdRoute()),
                () ->
                        assertEquals(
                                slotType.getSlotTypes(), requestBuilder.getRoute().getSlotIdRoute().getSlotType()),
                () -> assertEquals(42, requestBuilder.getRoute().getSlotIdRoute().getSlotId()),
                () -> assertFalse(requestBuilder.getRoute().hasSimpleRoutes()),
                () -> assertFalse(requestBuilder.getRoute().hasSlotKeyRoute()));
    }

    @ParameterizedTest
    @EnumSource(value = SlotType.class)
    public void prepare_request_with_slot_key_routes(SlotType slotType) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);

        ArgumentCaptor<RedisRequest.Builder> captor =
                ArgumentCaptor.forClass(RedisRequest.Builder.class);

        service.submitNewCommand(
                CUSTOM_COMMAND, new String[0], Optional.of(new SlotKeyRoute("TEST", slotType)), r -> null);
        verify(channelHandler).write(captor.capture(), anyBoolean());
        var requestBuilder = captor.getValue();

        assertAll(
                () -> assertTrue(requestBuilder.hasRoute()),
                () -> assertTrue(requestBuilder.getRoute().hasSlotKeyRoute()),
                () ->
                        assertEquals(
                                slotType.getSlotTypes(), requestBuilder.getRoute().getSlotKeyRoute().getSlotType()),
                () -> assertEquals("TEST", requestBuilder.getRoute().getSlotKeyRoute().getSlotKey()),
                () -> assertFalse(requestBuilder.getRoute().hasSimpleRoutes()),
                () -> assertFalse(requestBuilder.getRoute().hasSlotIdRoute()));
    }

    @Test
    public void prepare_request_with_unknown_route_type() {
        CompletableFuture<Response> future = new CompletableFuture<>();
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);

        var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.submitNewCommand(
                                        CUSTOM_COMMAND, new String[0], Optional.of(() -> false), r -> null));
        assertEquals("Unknown type of route", exception.getMessage());
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(RequestType.class)
    public void submitNewCommand_covers_all_mapRequestTypes(RequestType requestType) {
        // setup
        RedisRequestOuterClass.RequestType protobufRequestType = requestType.getProtobufMapping();

        ArgumentCaptor<RedisRequest.Builder> captor =
                ArgumentCaptor.forClass(RedisRequest.Builder.class);

        CompletableFuture<Response> testFuture = new CompletableFuture<>();
        testFuture.complete(Response.newBuilder().setRespPointer(-1L).build());

        when(channelHandler.write(captor.capture(), anyBoolean())).thenReturn(testFuture);
        Object testResult = new Object();

        // exercise
        CompletableFuture future =
                service.submitNewCommand(
                        requestType,
                        new String[0],
                        Optional.empty(),
                        new BaseCommandResponseResolver((r) -> testResult));
        Object result = future.get();
        var requestBuilder = captor.getValue();

        // verify
        assertEquals(testResult, result);
        assertTrue(requestBuilder.hasSingleCommand());
        assertEquals(protobufRequestType, requestBuilder.getSingleCommand().getRequestType());
    }

    @SneakyThrows
    @Test
    public void submitNewCommand_with_Transaction_sends_protobuf_request() {
        // setup
        String[] arg1 = new String[] {"GETSTRING", "one"};
        String[] arg2 = new String[] {"GETSTRING", "two"};
        String[] arg3 = new String[] {"GETSTRING", "three"};
        Transaction trans = new Transaction();
        trans.customCommand(arg1).customCommand(arg2).customCommand(arg3);

        CompletableFuture<Response> future = new CompletableFuture<>();
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);

        ArgumentCaptor<RedisRequest.Builder> captor =
                ArgumentCaptor.forClass(RedisRequest.Builder.class);

        // exercise
        service.submitNewCommand(trans, Optional.empty(), r -> null);

        // verify
        verify(channelHandler).write(captor.capture(), anyBoolean());
        var requestBuilder = captor.getValue();

        // verify
        assertTrue(requestBuilder.hasTransaction());
        assertEquals(3, requestBuilder.getTransaction().getCommandsCount());

        LinkedList<String> resultPayloads = new LinkedList();
        resultPayloads.add("one");
        resultPayloads.add("two");
        resultPayloads.add("three");
        for (RedisRequestOuterClass.Command command :
                requestBuilder.getTransaction().getCommandsList()) {
            assertEquals(CustomCommand, command.getRequestType());
            assertEquals("GETSTRING", command.getArgsArray().getArgs(0));
            assertEquals(resultPayloads.pop(), command.getArgsArray().getArgs(1));
        }
    }

    @SneakyThrows
    @Test
    public void submitNewCommand_with_Transaction_throws_interruptedException() {
        // setup
        String[] arg1 = new String[] {"GETSTRING", "one"};
        String[] arg2 = new String[] {"GETSTRING", "two"};
        String[] arg3 = new String[] {"GETSTRING", "two"};
        Transaction trans = new Transaction();
        trans.customCommand(arg1).customCommand(arg2).customCommand(arg3);

        CompletableFuture<Response> future = new CompletableFuture<>();
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
        InterruptedException interruptedException = new InterruptedException();
        future.completeExceptionally(interruptedException);

        ArgumentCaptor<RedisRequest.Builder> captor =
                ArgumentCaptor.forClass(RedisRequest.Builder.class);

        // exercise
        ExecutionException exception =
                assertThrows(
                        ExecutionException.class,
                        () -> {
                            CompletableFuture response =
                                    service.submitNewCommand(trans, Optional.empty(), r -> null);
                            response.get();
                        });
        verify(channelHandler).write(captor.capture(), anyBoolean());
        var requestBuilder = captor.getValue();

        // verify
        assertTrue(requestBuilder.hasTransaction());
        assertEquals(3, requestBuilder.getTransaction().getCommandsCount());
        assertEquals(interruptedException, exception.getCause());
    }

    @ParameterizedTest
    @EnumSource(value = SimpleRoute.class)
    public void submitNewCommand_with_Transaction_with_route_sends_protobuf_request(
            SimpleRoute routeType) {

        String[] arg1 = new String[] {"GETSTRING", "one"};
        String[] arg2 = new String[] {"GETSTRING", "two"};
        String[] arg3 = new String[] {"GETSTRING", "two"};
        Transaction trans = new Transaction();
        trans.customCommand(arg1).customCommand(arg2).customCommand(arg3);

        CompletableFuture<Response> future = new CompletableFuture<>();
        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);

        ArgumentCaptor<RedisRequest.Builder> captor =
                ArgumentCaptor.forClass(RedisRequest.Builder.class);

        service.submitNewCommand(trans, Optional.of(routeType), r -> null);
        verify(channelHandler).write(captor.capture(), anyBoolean());
        var requestBuilder = captor.getValue();

        assertAll(
                () -> assertTrue(requestBuilder.hasRoute()),
                () -> assertTrue(requestBuilder.getRoute().hasSimpleRoutes()),
                () ->
                        assertEquals(
                                routeType.getProtobufMapping(), requestBuilder.getRoute().getSimpleRoutes()),
                () -> assertFalse(requestBuilder.getRoute().hasSlotIdRoute()),
                () -> assertFalse(requestBuilder.getRoute().hasSlotKeyRoute()));
    }
}
