package glide.managers;

import static org.mockito.Mockito.mock;

import glide.connectors.handlers.ChannelHandler;
import org.junit.jupiter.api.BeforeEach;

public class CommandManagerTest {

    ChannelHandler channelHandler;

    CommandManager service;

    @BeforeEach
    void init() {
        channelHandler = mock(ChannelHandler.class);
        service = new CommandManager(channelHandler);
    }

    //    @Test
    //    public void submitNewCommand_returnObjectResult()
    //            throws ExecutionException, InterruptedException {
    //
    //        // setup
    //        long pointer = -1;
    //        Response respPointerResponse = Response.newBuilder().setRespPointer(pointer).build();
    //        Object respObject = mock(Object.class);
    //
    //        CompletableFuture<Response> future = new CompletableFuture<>();
    //        future.complete(respPointerResponse);
    //        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
    //
    //        // exercise
    //        CompletableFuture result =
    //                service.submitNewCommand(
    //                        command, new BaseCommandResponseResolver((ptr) -> ptr == pointer ?
    // respObject : null));
    //        Object respPointer = result.get();
    //
    //        // verify
    //        assertEquals(respObject, respPointer);
    //    }
    //
    //    @Test
    //    public void submitNewCommand_returnNullResult() throws ExecutionException,
    // InterruptedException {
    //        // setup
    //        Response respPointerResponse = Response.newBuilder().build();
    //        CompletableFuture<Response> future = new CompletableFuture<>();
    //        future.complete(respPointerResponse);
    //        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
    //
    //        // exercise
    //        CompletableFuture result =
    //                service.submitNewCommand(
    //                        command, new BaseCommandResponseResolver((p) -> new
    // RuntimeException("")));
    //        Object respPointer = result.get();
    //
    //        // verify
    //        assertNull(respPointer);
    //    }
    //
    //    @Test
    //    public void submitNewCommand_returnStringResult()
    //            throws ExecutionException, InterruptedException {
    //
    //        // setup
    //        long pointer = 123;
    //        String testString = "TEST STRING";
    //
    //        Response respPointerResponse = Response.newBuilder().setRespPointer(pointer).build();
    //
    //        CompletableFuture<Response> future = new CompletableFuture<>();
    //        future.complete(respPointerResponse);
    //        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
    //
    //        // exercise
    //        CompletableFuture result =
    //                service.submitNewCommand(
    //                        command, new BaseCommandResponseResolver((p) -> p == pointer ?
    // testString : null));
    //        Object respPointer = result.get();
    //
    //        // verify
    //        assertTrue(respPointer instanceof String);
    //        assertEquals(testString, respPointer);
    //    }
    //
    //    @Test
    //    public void submitNewCommand_throwClosingException() {
    //
    //        // setup
    //        String errorMsg = "Closing";
    //
    //        Response closingErrorResponse = Response.newBuilder().setClosingError(errorMsg).build();
    //
    //        CompletableFuture<Response> future = new CompletableFuture<>();
    //        future.complete(closingErrorResponse);
    //        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
    //
    //        // exercise
    //        ExecutionException e =
    //                assertThrows(
    //                        ExecutionException.class,
    //                        () -> {
    //                            CompletableFuture result =
    //                                    service.submitNewCommand(
    //                                            command, new BaseCommandResponseResolver((ptr) ->
    // new Object()));
    //                            result.get();
    //                        });
    //
    //        // verify
    //        assertTrue(e.getCause() instanceof ClosingException);
    //        assertEquals(errorMsg, e.getCause().getMessage());
    //    }
    //
    //    @ParameterizedTest
    //    @EnumSource(ResponseOuterClass.RequestErrorType.class) // six numbers
    //    public void BaseCommandResponseResolver_handles_all_errors(
    //            ResponseOuterClass.RequestErrorType requestErrorType) {
    //        if (requestErrorType == UNRECOGNIZED) {
    //            return;
    //        }
    //        Response errorResponse =
    //                Response.newBuilder()
    //                        .setRequestError(
    //                                RequestError.newBuilder()
    //                                        .setTypeValue(requestErrorType.getNumber())
    //                                        .setMessage(requestErrorType.toString())
    //                                        .build())
    //                        .build();
    //
    //        CompletableFuture<Response> future = new CompletableFuture<>();
    //        future.complete(errorResponse);
    //        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
    //
    //        ExecutionException executionException =
    //                assertThrows(
    //                        ExecutionException.class,
    //                        () -> {
    //                            CompletableFuture result =
    //                                    service.submitNewCommand(command, new
    // BaseCommandResponseResolver((ptr) -> null));
    //                            result.get();
    //                        });
    //
    //        // verify
    //        switch (requestErrorType) {
    //            case Unspecified:
    //                // only Unspecified errors return a RequestException
    //                assertTrue(executionException.getCause() instanceof RequestException);
    //                break;
    //            case ExecAbort:
    //                assertTrue(executionException.getCause() instanceof ExecAbortException);
    //                break;
    //            case Timeout:
    //                assertTrue(executionException.getCause() instanceof TimeoutException);
    //                break;
    //            case Disconnect:
    //                assertTrue(executionException.getCause() instanceof ConnectionException);
    //                break;
    //            default:
    //                fail("Unexpected protobuf error type");
    //        }
    //        assertEquals(requestErrorType.toString(), executionException.getCause().getMessage());
    //    }
    //
    //    @ParameterizedTest
    //    @EnumSource(value = SimpleRoute.class)
    //    public void prepare_request_with_simple_routes(SimpleRoute routeType) {
    //        CompletableFuture<Response> future = new CompletableFuture<>();
    //        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
    //        var command =
    //
    // Command.builder().requestType(Command.RequestType.CUSTOM_COMMAND).route(routeType).build();
    //
    //        ArgumentCaptor<RedisRequest.Builder> captor =
    //                ArgumentCaptor.forClass(RedisRequest.Builder.class);
    //        service.submitNewCommand(command, r -> null);
    //        verify(channelHandler).write(captor.capture(), anyBoolean());
    //        var requestBuilder = captor.getValue();
    //
    //        assertAll(
    //                () -> assertTrue(requestBuilder.hasRoute()),
    //                () -> assertTrue(requestBuilder.getRoute().hasSimpleRoutes()),
    //                () ->
    //                        assertEquals(
    //                                routeType.getProtobufMapping(),
    // requestBuilder.getRoute().getSimpleRoutes()),
    //                () -> assertFalse(requestBuilder.getRoute().hasSlotIdRoute()),
    //                () -> assertFalse(requestBuilder.getRoute().hasSlotKeyRoute()));
    //    }
    //
    //    @ParameterizedTest
    //    @EnumSource(value = SlotType.class)
    //    public void prepare_request_with_slot_id_routes(SlotType slotType) {
    //        CompletableFuture<Response> future = new CompletableFuture<>();
    //        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
    //        var command =
    //                Command.builder()
    //                        .requestType(Command.RequestType.CUSTOM_COMMAND)
    //                        .route(new SlotIdRoute(42, slotType))
    //                        .build();
    //
    //        ArgumentCaptor<RedisRequest.Builder> captor =
    //                ArgumentCaptor.forClass(RedisRequest.Builder.class);
    //
    //        service.submitNewCommand(command, r -> null);
    //        verify(channelHandler).write(captor.capture(), anyBoolean());
    //        var requestBuilder = captor.getValue();
    //
    //        assertAll(
    //                () -> assertTrue(requestBuilder.hasRoute()),
    //                () -> assertTrue(requestBuilder.getRoute().hasSlotIdRoute()),
    //                () ->
    //                        assertEquals(
    //                                slotType.getSlotTypes(),
    // requestBuilder.getRoute().getSlotIdRoute().getSlotType()),
    //                () -> assertEquals(42, requestBuilder.getRoute().getSlotIdRoute().getSlotId()),
    //                () -> assertFalse(requestBuilder.getRoute().hasSimpleRoutes()),
    //                () -> assertFalse(requestBuilder.getRoute().hasSlotKeyRoute()));
    //    }
    //
    //    @ParameterizedTest
    //    @EnumSource(value = SlotType.class)
    //    public void prepare_request_with_slot_key_routes(SlotType slotType) {
    //        CompletableFuture<Response> future = new CompletableFuture<>();
    //        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
    //        var command =
    //                Command.builder()
    //                        .requestType(Command.RequestType.CUSTOM_COMMAND)
    //                        .route(new SlotKeyRoute("TEST", slotType))
    //                        .build();
    //
    //        ArgumentCaptor<RedisRequest.Builder> captor =
    //                ArgumentCaptor.forClass(RedisRequest.Builder.class);
    //
    //        service.submitNewCommand(command, r -> null);
    //        verify(channelHandler).write(captor.capture(), anyBoolean());
    //        var requestBuilder = captor.getValue();
    //
    //        assertAll(
    //                () -> assertTrue(requestBuilder.hasRoute()),
    //                () -> assertTrue(requestBuilder.getRoute().hasSlotKeyRoute()),
    //                () ->
    //                        assertEquals(
    //                                slotType.getSlotTypes(),
    // requestBuilder.getRoute().getSlotKeyRoute().getSlotType()),
    //                () -> assertEquals("TEST",
    // requestBuilder.getRoute().getSlotKeyRoute().getSlotKey()),
    //                () -> assertFalse(requestBuilder.getRoute().hasSimpleRoutes()),
    //                () -> assertFalse(requestBuilder.getRoute().hasSlotIdRoute()));
    //    }
    //
    //    @Test
    //    public void prepare_request_with_unknown_route_type() {
    //        CompletableFuture<Response> future = new CompletableFuture<>();
    //        when(channelHandler.write(any(), anyBoolean())).thenReturn(future);
    //        var command =
    //                Command.builder()
    //                        .requestType(Command.RequestType.CUSTOM_COMMAND)
    //                        .route(() -> false)
    //                        .build();
    //
    //        var exception =
    //                assertThrows(
    //                        IllegalArgumentException.class, () -> service.submitNewCommand(command,
    // r -> null));
    //        assertEquals("Unknown type of route", exception.getMessage());
    //    }
    //
    //    @SneakyThrows
    //    @ParameterizedTest
    //    @EnumSource(Command.RequestType.class)
    //    public void submitNewCommand_covers_all_mapRequestTypes(Command.RequestType requestType) {
    //
    //        // setup
    //        RequestType protobufRequestType;
    //        switch (requestType) {
    //            case CUSTOM_COMMAND:
    //                protobufRequestType = RequestType.CustomCommand;
    //                break;
    //            case PING:
    //                protobufRequestType = RequestType.Ping;
    //                break;
    //            case INFO:
    //                protobufRequestType = RequestType.Info;
    //                break;
    //            case GET_STRING:
    //                protobufRequestType = RequestType.GetString;
    //                break;
    //            case SET_STRING:
    //                protobufRequestType = RequestType.SetString;
    //                break;
    //            default:
    //                // not implemented
    //                return;
    //        }
    //
    //        Command command = Command.builder().requestType(requestType).arguments(new String[]
    // {}).build();
    //
    //        //        RedisRequest.Builder request =
    //        //                prepareProtoBufRequest(
    //        //                                protobufRequestType,
    //        // RedisRequestOuterClass.Command.ArgsArray.newBuilder().build());
    //        ArgumentCaptor<RedisRequest.Builder> captor =
    //            ArgumentCaptor.forClass(RedisRequest.Builder.class);
    //
    //        CompletableFuture<Response> testFuture = new CompletableFuture<>();
    //        testFuture.complete(Response.newBuilder().setRespPointer(-1L).build());
    //
    //        when(channelHandler.write(captor.capture(), anyBoolean())).thenReturn(testFuture);
    //        Object testResult = new Object();
    //
    //        // exercise
    //        CompletableFuture future =
    //            service.submitNewCommand(
    //                command, Optional.empty(), new BaseCommandResponseResolver((r) -> testResult));
    //        Object result = future.get();
    //
    //        // verify
    //        assertEquals(testResult, result);
    //    }
    //
    //    private RedisRequest.Builder prepareProtoBufRequest(
    //        RequestType requestType, RedisRequestOuterClass.Command.ArgsArray argsArray) {
    //        return RedisRequest.newBuilder()
    //            .setSingleCommand(
    //                RedisRequestOuterClass.Command.newBuilder()
    //                    .setRequestType(requestType)
    //                    .setArgsArray(argsArray)
    //                    .build());
    //    }
}
