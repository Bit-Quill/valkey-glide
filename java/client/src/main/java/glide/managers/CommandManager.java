package glide.managers;

import glide.api.models.BaseTransaction;
import glide.api.models.configuration.RequestRoutingConfiguration;
import glide.connectors.handlers.CallbackDispatcher;
import glide.connectors.handlers.ChannelHandler;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import redis_request.RedisRequestOuterClass;
import redis_request.RedisRequestOuterClass.Command;
import redis_request.RedisRequestOuterClass.Command.ArgsArray;
import redis_request.RedisRequestOuterClass.RedisRequest;
import redis_request.RedisRequestOuterClass.Routes;
import redis_request.RedisRequestOuterClass.SlotIdRoute;
import redis_request.RedisRequestOuterClass.SlotKeyRoute;
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
     * @param requestType Redis command type
     * @param arguments Redis command arguments
     * @param route Command routing parameters
     * @param responseHandler The handler for the response object
     * @return A result promise of type T
     */
    public <T> CompletableFuture<T> submitNewCommand(
            RequestType requestType,
            String[] arguments,
            Optional<RequestRoutingConfiguration.Route> route,
            RedisExceptionCheckedFunction<Response, T> responseHandler) {

        RedisRequest.Builder command = prepareRedisRequest(requestType, arguments, route);
        return submitNewCommand(command, responseHandler);
    }

    /**
     * Build a command and send.
     *
     * @param requestType Redis command type
     * @param arguments Redis command arguments
     * @param responseHandler The handler for the response object
     * @return A result promise of type T
     */
    public <T> CompletableFuture<T> submitNewCommand(
            RequestType requestType,
            String[] arguments,
            RedisExceptionCheckedFunction<Response, T> responseHandler) {

        return submitNewCommand(requestType, arguments, Optional.empty(), responseHandler);
    }

    /**
     * Build a Transaction and send.
     *
     * @param transaction Redis Transaction request with multiple commands
     * @param route Command routing parameters
     * @param responseHandler The handler for the response object
     * @return A result promise of type T
     */
    public <T> CompletableFuture<T> submitNewCommand(
            BaseTransaction transaction,
            Optional<RequestRoutingConfiguration.Route> route,
            RedisExceptionCheckedFunction<Response, T> responseHandler) {

        RedisRequest.Builder command = prepareRedisRequest(transaction, route);
        return submitNewCommand(command, responseHandler);
    }

    /**
     * Take a redis request and send to channel.
     *
     * @param command The Redis command request as a builder to execute
     * @param responseHandler The handler for the response object
     * @return A result promise of type T
     */
    protected <T> CompletableFuture<T> submitNewCommand(
            RedisRequest.Builder command, RedisExceptionCheckedFunction<Response, T> responseHandler) {
        // write command request to channel
        // when complete, convert the response to our expected type T using the given responseHandler
        return channel.write(command, true).thenApplyAsync(responseHandler::apply);
    }

    /**
     * Build a protobuf command request object with routing options.<br>
     *
     * @param requestType Redis command type
     * @param arguments Redis command arguments
     * @param route Command routing parameters
     * @return An uncompleted request. {@link CallbackDispatcher} is responsible to complete it by
     *     adding a callback id.
     */
    protected RedisRequest.Builder prepareRedisRequest(
            RequestType requestType,
            String[] arguments,
            Optional<RequestRoutingConfiguration.Route> route) {
        ArgsArray.Builder commandArgs = ArgsArray.newBuilder();
        for (var arg : arguments) {
            commandArgs.addArgs(arg);
        }

        var builder =
                RedisRequest.newBuilder()
                        .setSingleCommand(
                                Command.newBuilder()
                                        .setRequestType(mapRequestTypes(requestType))
                                        .setArgsArray(commandArgs.build())
                                        .build());

        if (route.isEmpty()) {
            return builder;
        }

        if (route.get() instanceof RequestRoutingConfiguration.SimpleRoute) {
            builder.setRoute(
                    Routes.newBuilder()
                            .setSimpleRoutes(
                                    ((RequestRoutingConfiguration.SimpleRoute) route.get()).getProtobufMapping())
                            .build());
        } else if (route.get() instanceof RequestRoutingConfiguration.SlotIdRoute) {
            builder.setRoute(
                    Routes.newBuilder()
                            .setSlotIdRoute(
                                    SlotIdRoute.newBuilder()
                                            .setSlotId(
                                                    ((RequestRoutingConfiguration.SlotIdRoute) route.get()).getSlotId())
                                            .setSlotType(
                                                    ((RequestRoutingConfiguration.SlotIdRoute) route.get())
                                                            .getSlotType()
                                                            .getSlotTypes())));
        } else if (route.get() instanceof RequestRoutingConfiguration.SlotKeyRoute) {
            builder.setRoute(
                    Routes.newBuilder()
                            .setSlotKeyRoute(
                                    SlotKeyRoute.newBuilder()
                                            .setSlotKey(
                                                    ((RequestRoutingConfiguration.SlotKeyRoute) route.get()).getSlotKey())
                                            .setSlotType(
                                                    ((RequestRoutingConfiguration.SlotKeyRoute) route.get())
                                                            .getSlotType()
                                                            .getSlotTypes())));
        } else {
            throw new IllegalArgumentException("Unknown type of route");
        }
        return builder;
    }

    protected RedisRequest.Builder prepareRedisRequest(
            BaseTransaction transaction, Optional<RequestRoutingConfiguration.Route> route) {

        var builder =
                RedisRequest.newBuilder().setTransaction(transaction.getTransactionBuilder().build());

        if (route.isEmpty()) {
            return builder;
        }

        if (route.get() instanceof RequestRoutingConfiguration.SimpleRoute) {
            builder.setRoute(
                    Routes.newBuilder()
                            .setSimpleRoutes(
                                    ((RequestRoutingConfiguration.SimpleRoute) route.get()).getProtobufMapping())
                            .build());
        } else if (route.get() instanceof RequestRoutingConfiguration.SlotIdRoute) {
            builder.setRoute(
                    Routes.newBuilder()
                            .setSlotIdRoute(
                                    SlotIdRoute.newBuilder()
                                            .setSlotId(
                                                    ((RequestRoutingConfiguration.SlotIdRoute) route.get()).getSlotId())
                                            .setSlotType(
                                                    ((RequestRoutingConfiguration.SlotIdRoute) route.get())
                                                            .getSlotType()
                                                            .getSlotTypes())));
        } else if (route.get() instanceof RequestRoutingConfiguration.SlotKeyRoute) {
            builder.setRoute(
                    Routes.newBuilder()
                            .setSlotKeyRoute(
                                    SlotKeyRoute.newBuilder()
                                            .setSlotKey(
                                                    ((RequestRoutingConfiguration.SlotKeyRoute) route.get()).getSlotKey())
                                            .setSlotType(
                                                    ((RequestRoutingConfiguration.SlotKeyRoute) route.get())
                                                            .getSlotType()
                                                            .getSlotTypes())));
        } else {
            throw new IllegalArgumentException("Unknown type of route");
        }
        return builder;
    }

    public enum RequestType {
        /** Call a custom command with list of string arguments */
        CUSTOM_COMMAND,
        /**
         * Ping the Redis server.
         *
         * @see <hred=https://redis.io/commands/ping/>command reference</a>
         */
        PING,
        /**
         * Get information and statistics about the Redis server.
         *
         * @see <hred=https://redis.io/commands/info/>command reference</a>
         */
        INFO,
        /**
         * Get the value of key.
         *
         * @see: <href=https://redis.io/commands/get/>command reference</a>
         */
        GET_STRING,
        /**
         * Set key to hold the string value.
         *
         * @see: <href=https://redis.io/commands/set/>command reference</a>
         */
        SET_STRING,
    }

    public static RedisRequestOuterClass.RequestType mapRequestTypes(RequestType inType) {
        switch (inType) {
            case CUSTOM_COMMAND:
                return RedisRequestOuterClass.RequestType.CustomCommand;
            case PING:
                return RedisRequestOuterClass.RequestType.Ping;
            case INFO:
                return RedisRequestOuterClass.RequestType.Info;
            case GET_STRING:
                return RedisRequestOuterClass.RequestType.GetString;
            case SET_STRING:
                return RedisRequestOuterClass.RequestType.SetString;
        }
        throw new RuntimeException("Unsupported request type");
    }
}
