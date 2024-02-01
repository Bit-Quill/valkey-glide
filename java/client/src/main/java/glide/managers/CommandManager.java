package glide.managers;

import glide.api.models.BaseTransaction;
import glide.api.models.configuration.RequestRoutingConfiguration;
import glide.api.models.configuration.RequestRoutingConfiguration.Route;
import glide.api.models.configuration.RequestRoutingConfiguration.SimpleRoute;
import glide.connectors.handlers.CallbackDispatcher;
import glide.api.models.configuration.RequestRoutingConfiguration.Route;
import glide.api.models.configuration.RequestRoutingConfiguration.SimpleRoute;
import glide.api.models.configuration.RequestRoutingConfiguration.SlotIdRoute;
import glide.api.models.configuration.RequestRoutingConfiguration.SlotKeyRoute;
import glide.api.models.exceptions.ClosingException;
import glide.connectors.handlers.CallbackDispatcher;
import glide.connectors.handlers.ChannelHandler;
import java.util.Optional;
import glide.managers.models.Command;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import redis_request.RedisRequestOuterClass.Command;
import redis_request.RedisRequestOuterClass.Command.ArgsArray;
import redis_request.RedisRequestOuterClass.RedisRequest;
import redis_request.RedisRequestOuterClass.Routes;
import redis_request.RedisRequestOuterClass.SlotIdRoute;
import redis_request.RedisRequestOuterClass.SlotKeyRoute;
import redis_request.RedisRequestOuterClass;
import redis_request.RedisRequestOuterClass.Command.ArgsArray;
import redis_request.RedisRequestOuterClass.RedisRequest;
import redis_request.RedisRequestOuterClass.RequestType;
import redis_request.RedisRequestOuterClass.Routes;
import redis_request.RedisRequestOuterClass.SimpleRoutes;
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
     * @param requestType Redis command type
     * @param arguments Redis command arguments
     * @param route Command routing parameters
     * @param responseHandler The handler for the response object
     * @return A result promise of type T
     */
    public <T> CompletableFuture<T> submitNewCommand(
            RequestType requestType,
            String[] arguments,
            Optional<Route> route,
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
            Optional<Route> route,
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
        return channel
                .write(
                        prepareRedisRequest(
                                command.getRequestType(),
                                command.getArguments(),
                                Optional.ofNullable(command.getRoute())),
                        true)
                .exceptionally(this::exceptionHandler)
                .thenApplyAsync(responseHandler::apply);
    }

    /**
     * Exception handler for future pipeline.
     *
     * @param e An exception thrown in the pipeline before
     * @return Nothing, it rethrows the exception
     */
    private Response exceptionHandler(Throwable e) {
        if (e instanceof ClosingException) {
            channel.close();
        }
        if (e instanceof RuntimeException) {
            // RedisException also goes here
            throw (RuntimeException) e;
        }
        throw new RuntimeException(e);
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
            RequestType requestType, String[] arguments, Optional<Route> route) {
        ArgsArray.Builder commandArgs = ArgsArray.newBuilder();
        for (var arg : arguments) {
            commandArgs.addArgs(arg);
        }

        var builder =
                RedisRequest.newBuilder()
                        .setSingleCommand(
                                Command.newBuilder()
                                        .setRequestType(requestType.getProtobufMapping())
                                        .setArgsArray(commandArgs.build())
                                        .build());

        if (route.isEmpty()) {
            return builder;
        }

        if (route.get() instanceof SimpleRoute) {
            builder.setRoute(
                    Routes.newBuilder()
                            .setSimpleRoutes(((SimpleRoute) route.get()).getProtobufMapping())
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
            BaseTransaction transaction, Optional<Route> route) {

        var builder =
                RedisRequest.newBuilder().setTransaction(transaction.getTransactionBuilder().build());

        if (route.isEmpty()) {
            return builder;
        }

        if (route.get() instanceof SimpleRoute) {
            builder.setRoute(
                    Routes.newBuilder()
                            .setSimpleRoutes(((SimpleRoute) route.get()).getProtobufMapping())
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
}
