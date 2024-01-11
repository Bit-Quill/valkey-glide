package glide.managers;

import glide.api.models.configuration.Route;
import glide.connectors.handlers.CallbackDispatcher;
import glide.connectors.handlers.ChannelHandler;
import glide.managers.models.Command;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import redis_request.RedisRequestOuterClass;
import redis_request.RedisRequestOuterClass.RequestType;
import redis_request.RedisRequestOuterClass.Command.ArgsArray;
import redis_request.RedisRequestOuterClass.RedisRequest;
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
     * @param responseHandler The handler for the response object
     * @return A result promise of type T
     */
    public <T> CompletableFuture<T> submitNewCommand(
            Command command, RedisExceptionCheckedFunction<Response, T> responseHandler) {
        // write command request to channel
        // when complete, convert the response to our expected type T using the given responseHandler
        return channel
                .write(prepareRedisRequest(command.getRequestType(), command.getArguments()), true)
                .thenApplyAsync(responseHandler::apply);
    }

    /**
     * Build a command and send.
     *
     * @param command The command to execute
     * @param route The routing options
     * @param responseHandler The handler for the response object
     * @return A result promise of type T
     */
    public <T> CompletableFuture<T> submitNewCommand(
            Command command, Route route, RedisExceptionCheckedFunction<Response, T> responseHandler) {
        // write command request to channel
        // when complete, convert the response to our expected type T using the given responseHandler
        return channel
                .write(prepareRedisRequest(command.getRequestType(), command.getArguments(), route), true)
                .thenApplyAsync(responseHandler::apply);
    }

    /**
     * Build a protobuf command/transaction request object.<br>
     * Used by {@link CommandManager}.
     *
     * @param command - Redis command
     * @param args - Redis command arguments as string array
     * @return An uncompleted request. {@link CallbackDispatcher} is responsible to complete it by
     *     adding a callback id.
     */
    private RedisRequest.Builder prepareRedisRequest(Command.RequestType command, String[] args) {
        ArgsArray.Builder commandArgs = ArgsArray.newBuilder();
        for (var arg : args) {
            commandArgs.addArgs(arg);
        }

        return RedisRequest.newBuilder()
                .setSingleCommand(
                        RedisRequestOuterClass.Command.newBuilder()
                                .setRequestType(mapRequestTypes(command))
                                .setArgsArray(commandArgs.build())
                                .build());
    }

    private RequestType mapRequestTypes(Command.RequestType inType) {
      switch (inType) {
        case CUSTOM_COMMAND:
          return RequestType.CustomCommand;
      }
      throw new RuntimeException("Unsupported request type");
    }

    /**
     * Build a protobuf command/transaction request object with routing options.<br>
     * Used by {@link CommandManager}.
     *
     * @return An uncompleted request. {@link CallbackDispatcher} is responsible to complete it by
     *     adding a callback id.
     */
    private RedisRequest.Builder prepareRedisRequest(
        Command.RequestType command, String[] args, Route route) {
        RedisRequest.Builder builder = prepareRedisRequest(command, args);

        switch (route.getRouteType()) {
            case RANDOM:
            case ALL_NODES:
            case ALL_PRIMARIES:
                builder.setRoute(
                        RedisRequestOuterClass.Routes.newBuilder()
                                .setSimpleRoutes(getSimpleRoutes(route.getRouteType()))
                                .build());
                break;
            case PRIMARY_SLOT_KEY:
            case REPLICA_SLOT_KEY:
                builder.setRoute(
                        RedisRequestOuterClass.Routes.newBuilder()
                                .setSlotKeyRoute(
                                        RedisRequestOuterClass.SlotKeyRoute.newBuilder()
                                                .setSlotKey(route.getSlotKey())
                                                .setSlotType(getSlotTypes(route.getRouteType()))));
                break;
            case PRIMARY_SLOT_ID:
            case REPLICA_SLOT_ID:
                builder.setRoute(
                        RedisRequestOuterClass.Routes.newBuilder()
                                .setSlotIdRoute(
                                        RedisRequestOuterClass.SlotIdRoute.newBuilder()
                                                .setSlotId(route.getSlotId())
                                                .setSlotType(getSlotTypes(route.getRouteType()))));
        }
        return builder;
    }

    private RedisRequestOuterClass.SimpleRoutes getSimpleRoutes(Route.RouteType routeType) {
        switch (routeType) {
            case RANDOM:
                return RedisRequestOuterClass.SimpleRoutes.Random;
            case ALL_NODES:
                return RedisRequestOuterClass.SimpleRoutes.AllNodes;
            case ALL_PRIMARIES:
                return RedisRequestOuterClass.SimpleRoutes.AllPrimaries;
        }
        throw new IllegalStateException("Unreachable code");
    }

    private RedisRequestOuterClass.SlotTypes getSlotTypes(Route.RouteType routeType) {
        switch (routeType) {
            case PRIMARY_SLOT_ID:
            case PRIMARY_SLOT_KEY:
                return RedisRequestOuterClass.SlotTypes.Primary;
            case REPLICA_SLOT_ID:
            case REPLICA_SLOT_KEY:
                return RedisRequestOuterClass.SlotTypes.Replica;
        }
        throw new IllegalStateException("Unreachable code");
    }
}
