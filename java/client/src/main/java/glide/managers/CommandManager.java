package glide.managers;

import glide.api.commands.Transaction;
import glide.api.models.configuration.Route;
import glide.connectors.handlers.CallbackDispatcher;
import glide.connectors.handlers.ChannelHandler;
import glide.managers.models.Command;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import redis_request.RedisRequestOuterClass;
import redis_request.RedisRequestOuterClass.Command.ArgsArray;
import redis_request.RedisRequestOuterClass.RedisRequest;
import redis_request.RedisRequestOuterClass.RequestType;
import redis_request.RedisRequestOuterClass.SimpleRoutes;
import redis_request.RedisRequestOuterClass.SlotIdRoute;
import redis_request.RedisRequestOuterClass.SlotKeyRoute;
import redis_request.RedisRequestOuterClass.SlotTypes;
import response.ResponseOuterClass.Response;

/**
 * Service responsible for submitting command requests to a socket channel handler and unpack
 * responses from the same socket channel handler.
 */
@RequiredArgsConstructor
public class CommandManager {

    /** UDS connection representation. */
    private final ChannelHandler channel;

    /**
     * Build a command and send.
     *
     * @param command The command to execute
     * @param route The routing options
     * @param responseHandler The handler for the response object
     * @return A result promise of type T
     */
    public <T> CompletableFuture<T> submitNewCommand(
            Command command,
            Optional<Route> route,
            RedisExceptionCheckedFunction<Response, T> responseHandler) {
        // write command request to channel
        // when complete, convert the response to our expected type T using the given responseHandler
        return channel
                .write(prepareRedisRequest(command, route), true)
                .thenApplyAsync(responseHandler::apply);
    }

    public <T> CompletableFuture<T> submitNewTransaction(
            Transaction transaction,
            Optional<Route> route,
            RedisExceptionCheckedFunction<Response, T> responseHandler) {
        // write command request to channel
        // when complete, convert the response to our expected type T using the given responseHandler
        return channel
                .write(prepareRedisTransaction(transaction, route), true)
                .thenApplyAsync(responseHandler::apply);
    }

    private RequestType mapRequestTypes(Command.RequestType inType) {
        switch (inType) {
            case CUSTOM_COMMAND:
                return RequestType.CustomCommand;
            case PING:
                return RequestType.Ping;
            case INFO:
                return RequestType.Info;
            case GET_STRING:
                return RequestType.GetString;
            case SET_STRING:
                return RequestType.SetString;
        }
        throw new RuntimeException("Unsupported request type");
    }

    /**
     * Build a protobuf command request object with routing options.<br>
     * Used by {@link CommandManager}.
     *
     * @param command Redis command type
     * @param route Command routing parameters
     * @return An uncompleted request. {@link CallbackDispatcher} is responsible to complete it by
     *     adding a callback id.
     */
    private RedisRequest.Builder prepareRedisRequest(Command command, Optional<Route> route) {
        ArgsArray.Builder commandArgs = ArgsArray.newBuilder();
        for (var arg : command.getArguments()) {
            commandArgs.addArgs(arg);
        }

        RedisRequest.Builder builder =
                RedisRequest.newBuilder().setSingleCommand(prepareCommand(command));

        if (route.isEmpty()) {
            return builder;
        }

        switch (route.get().getRouteType()) {
            case RANDOM:
            case ALL_NODES:
            case ALL_PRIMARIES:
                builder.setRoute(
                        RedisRequestOuterClass.Routes.newBuilder()
                                .setSimpleRoutes(getSimpleRoutes(route.get().getRouteType()))
                                .build());
                break;
            case PRIMARY_SLOT_KEY:
            case REPLICA_SLOT_KEY:
                builder.setRoute(
                        RedisRequestOuterClass.Routes.newBuilder()
                                .setSlotKeyRoute(
                                        SlotKeyRoute.newBuilder()
                                                .setSlotKey(route.get().getSlotKey())
                                                .setSlotType(getSlotTypes(route.get().getRouteType()))));
                break;
            case PRIMARY_SLOT_ID:
            case REPLICA_SLOT_ID:
                builder.setRoute(
                        RedisRequestOuterClass.Routes.newBuilder()
                                .setSlotIdRoute(
                                        SlotIdRoute.newBuilder()
                                                .setSlotId(route.get().getSlotId())
                                                .setSlotType(getSlotTypes(route.get().getRouteType()))));
        }
        return builder;
    }

    /**
     * Build a protobuf transaction request object with routing options.<br>
     * Used by {@link CommandManager}.
     *
     * @param transaction Redis command type
     * @param route Command routing parameters
     * @return An uncompleted request. {@link CallbackDispatcher} is responsible to complete it by
     *     adding a callback id.
     */
    private RedisRequest.Builder prepareRedisTransaction(
            Transaction transaction, Optional<Route> route) {

        RedisRequestOuterClass.Transaction.Builder transactionBuilder =
                RedisRequestOuterClass.Transaction.newBuilder();
        transaction.getCommands().stream()
                .map(command -> prepareCommand(command))
                .forEach(transactionBuilder::addCommands);

        RedisRequest.Builder builder =
                RedisRequest.newBuilder().setTransaction(transactionBuilder.build());

        if (route.isEmpty()) {
            return builder;
        }

        switch (route.get().getRouteType()) {
            case RANDOM:
            case ALL_NODES:
            case ALL_PRIMARIES:
                builder.setRoute(
                        RedisRequestOuterClass.Routes.newBuilder()
                                .setSimpleRoutes(getSimpleRoutes(route.get().getRouteType()))
                                .build());
                break;
            case PRIMARY_SLOT_KEY:
            case REPLICA_SLOT_KEY:
                builder.setRoute(
                        RedisRequestOuterClass.Routes.newBuilder()
                                .setSlotKeyRoute(
                                        SlotKeyRoute.newBuilder()
                                                .setSlotKey(route.get().getSlotKey())
                                                .setSlotType(getSlotTypes(route.get().getRouteType()))));
                break;
            case PRIMARY_SLOT_ID:
            case REPLICA_SLOT_ID:
                builder.setRoute(
                        RedisRequestOuterClass.Routes.newBuilder()
                                .setSlotIdRoute(
                                        SlotIdRoute.newBuilder()
                                                .setSlotId(route.get().getSlotId())
                                                .setSlotType(getSlotTypes(route.get().getRouteType()))));
        }
        return builder;
    }

    private RedisRequestOuterClass.Command prepareCommand(Command command) {

        RedisRequestOuterClass.Command.ArgsArray.Builder commandArgs =
                RedisRequestOuterClass.Command.ArgsArray.newBuilder();
        for (var arg : command.getArguments()) {
            commandArgs.addArgs(arg);
        }

        return RedisRequestOuterClass.Command.newBuilder()
                .setRequestType(mapRequestTypes(command.getRequestType()))
                .setArgsArray(commandArgs.build())
                .build();
    }

    private SimpleRoutes getSimpleRoutes(Route.RouteType routeType) {
        switch (routeType) {
            case RANDOM:
                return SimpleRoutes.Random;
            case ALL_NODES:
                return SimpleRoutes.AllNodes;
            case ALL_PRIMARIES:
                return SimpleRoutes.AllPrimaries;
        }
        throw new IllegalStateException("Unreachable code");
    }

    private SlotTypes getSlotTypes(Route.RouteType routeType) {
        switch (routeType) {
            case PRIMARY_SLOT_ID:
            case PRIMARY_SLOT_KEY:
                return SlotTypes.Primary;
            case REPLICA_SLOT_ID:
            case REPLICA_SLOT_KEY:
                return SlotTypes.Replica;
        }
        throw new IllegalStateException("Unreachable code");
    }
}
