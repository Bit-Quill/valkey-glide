package glide.api;

import static glide.ffi.resolvers.SocketListenerResolver.getSocket;
import static glide.managers.CommandManager.mapRequestTypes;

import glide.api.commands.ConnectionCommands;
import glide.api.commands.StringCommands;
import glide.api.models.BaseTransaction;
import glide.api.models.commands.SetOptions;
import glide.api.models.configuration.RequestRoutingConfiguration;
import glide.api.models.exceptions.RedisException;
import glide.connectors.handlers.CallbackDispatcher;
import glide.connectors.handlers.ChannelHandler;
import glide.ffi.resolvers.RedisValueResolver;
import glide.managers.BaseCommandResponseResolver;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import response.ResponseOuterClass.Response;

/** Base Client class for Redis */
@AllArgsConstructor
public abstract class BaseClient implements AutoCloseable, StringCommands, ConnectionCommands {

    protected final ConnectionManager connectionManager;
    protected final CommandManager commandManager;

    /**
     * Closes this resource, relinquishing any underlying resources. This method is invoked
     * automatically on objects managed by the try-with-resources statement.
     *
     * <p>see: <a
     * href="https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html#close--">AutoCloseable::close()</a>
     */
    @Override
    public void close() throws ExecutionException {
        try {
            connectionManager.closeConnection().get();
        } catch (InterruptedException e) {
            // suppressing the interrupted exception - it is already suppressed in the future
            throw new RuntimeException(e);
        }
    }

    protected static ChannelHandler buildChannelHandler() throws InterruptedException {
        CallbackDispatcher callbackDispatcher = new CallbackDispatcher();
        return new ChannelHandler(callbackDispatcher, getSocket());
    }

    protected static ConnectionManager buildConnectionManager(ChannelHandler channelHandler) {
        return new ConnectionManager(channelHandler);
    }

    protected static CommandManager buildCommandManager(ChannelHandler channelHandler) {
        return new CommandManager(channelHandler);
    }

    /**
     * Extracts the response from the Protobuf response and either throws an exception or returns the
     * appropriate response as an Object
     *
     * @param response Redis protobuf message
     * @return Response Object
     */
    protected Object handleObjectResponse(Response response) {
        // convert protobuf response into Object and then Object into T
        return new BaseCommandResponseResolver(RedisValueResolver::valueFromPointer).apply(response);
    }

    /**
     * Check for errors in the Response and return null Throws an error if an unexpected value is
     * returned
     *
     * @return null if the response is empty
     */
    protected Void handleVoidResponse(Response response) {
        Object value = handleObjectResponse(response);
        if (value == null) {
            return null;
        }
        throw new RedisException(
                "Unexpected return type from Redis: got "
                        + value.getClass().getSimpleName()
                        + " expected null");
    }

    /**
     * Extracts the response value from the Redis response and either throws an exception or returns
     * the value as a String.
     *
     * @param response Redis protobuf message
     * @return Response as a String
     */
    protected String handleStringResponse(Response response) {
        Object value = handleObjectResponse(response);
        if (value instanceof String) {
            return (String) value;
        }
        throw new RedisException(
                "Unexpected return type from Redis: got "
                        + value.getClass().getSimpleName()
                        + " expected String");
    }

    /**
     * Extracts the response value from the Redis response and either throws an exception or returns
     * the value as an Object[].
     *
     * @param response Redis protobuf message
     * @return Response as an Object[]
     */
    protected Object[] handleArrayResponse(Response response) {
        Object value = handleObjectResponse(response);
        if (value instanceof Object[]) {
            return (Object[]) value;
        }
        throw new RedisException(
                "Unexpected return type from Redis: got "
                        + value.getClass().getSimpleName()
                        + " expected Object[]");
    }

    /**
     * Extracts the response value from the Redis response and either throws an exception or returns
     * the * value as a HashMap
     *
     * @param response Redis protobuf message
     * @return Response as a String
     */
    protected HashMap<String, Object> handleMapResponse(Response response) {
        Object value = handleObjectResponse(response);
        if (value instanceof HashMap) {
            return (HashMap<String, Object>) value;
        }
        throw new RedisException(
                "Unexpected return type from Redis: got "
                        + value.getClass().getSimpleName()
                        + " expected HashMap");
    }

    @Override
    public CompletableFuture<String> ping() {
        return commandManager.submitNewCommand(
                prepareRedisRequest(RequestType.PING, new String[0], Optional.empty()),
                this::handleStringResponse);
    }

    @Override
    public CompletableFuture<String> ping(String msg) {
        return commandManager.submitNewCommand(
                prepareRedisRequest(RequestType.PING, new String[] {msg}, Optional.empty()),
                this::handleStringResponse);
    }

    @Override
    public CompletableFuture<String> get(String key) {
        return commandManager.submitNewCommand(
                prepareRedisRequest(RequestType.GET_STRING, new String[] {key}, Optional.empty()),
                this::handleStringResponse);
    }

    @Override
    public CompletableFuture<Void> set(String key, String value) {
        return commandManager.submitNewCommand(
                prepareRedisRequest(RequestType.SET_STRING, new String[] {key, value}, Optional.empty()),
                this::handleVoidResponse);
    }

    @Override
    public CompletableFuture<String> set(String key, String value, SetOptions options) {
        String[] arguments = ArrayUtils.addAll(new String[] {key, value}, options.toArgs());
        return commandManager.submitNewCommand(
                prepareRedisRequest(RequestType.SET_STRING, arguments, Optional.empty()),
                this::handleStringResponse);
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
    protected redis_request.RedisRequestOuterClass.RedisRequest.Builder prepareRedisRequest(
            RequestType requestType,
            String[] arguments,
            Optional<RequestRoutingConfiguration.Route> route) {
        redis_request.RedisRequestOuterClass.Command.ArgsArray.Builder commandArgs =
                redis_request.RedisRequestOuterClass.Command.ArgsArray.newBuilder();
        for (var arg : arguments) {
            commandArgs.addArgs(arg);
        }

        var builder =
                redis_request.RedisRequestOuterClass.RedisRequest.newBuilder()
                        .setSingleCommand(
                                redis_request.RedisRequestOuterClass.Command.newBuilder()
                                        .setRequestType(mapRequestTypes(requestType))
                                        .setArgsArray(commandArgs.build())
                                        .build());

        if (route.isEmpty()) {
            return builder;
        }

        if (route.get() instanceof RequestRoutingConfiguration.SimpleRoute) {
            builder.setRoute(
                    redis_request.RedisRequestOuterClass.Routes.newBuilder()
                            .setSimpleRoutes(
                                    ((RequestRoutingConfiguration.SimpleRoute) route.get()).getProtobufMapping())
                            .build());
        } else if (route.get() instanceof RequestRoutingConfiguration.SlotIdRoute) {
            builder.setRoute(
                    redis_request.RedisRequestOuterClass.Routes.newBuilder()
                            .setSlotIdRoute(
                                    redis_request.RedisRequestOuterClass.SlotIdRoute.newBuilder()
                                            .setSlotId(
                                                    ((RequestRoutingConfiguration.SlotIdRoute) route.get()).getSlotId())
                                            .setSlotType(
                                                    ((RequestRoutingConfiguration.SlotIdRoute) route.get())
                                                            .getSlotType()
                                                            .getSlotTypes())));
        } else if (route.get() instanceof RequestRoutingConfiguration.SlotKeyRoute) {
            builder.setRoute(
                    redis_request.RedisRequestOuterClass.Routes.newBuilder()
                            .setSlotKeyRoute(
                                    redis_request.RedisRequestOuterClass.SlotKeyRoute.newBuilder()
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

    protected redis_request.RedisRequestOuterClass.RedisRequest.Builder prepareRedisRequest(
            BaseTransaction transaction, Optional<RequestRoutingConfiguration.Route> route) {

        var builder =
                redis_request.RedisRequestOuterClass.RedisRequest.newBuilder()
                        .setTransaction(transaction.getTransactionBuilder().build());

        if (route.isEmpty()) {
            return builder;
        }

        if (route.get() instanceof RequestRoutingConfiguration.SimpleRoute) {
            builder.setRoute(
                    redis_request.RedisRequestOuterClass.Routes.newBuilder()
                            .setSimpleRoutes(
                                    ((RequestRoutingConfiguration.SimpleRoute) route.get()).getProtobufMapping())
                            .build());
        } else if (route.get() instanceof RequestRoutingConfiguration.SlotIdRoute) {
            builder.setRoute(
                    redis_request.RedisRequestOuterClass.Routes.newBuilder()
                            .setSlotIdRoute(
                                    redis_request.RedisRequestOuterClass.SlotIdRoute.newBuilder()
                                            .setSlotId(
                                                    ((RequestRoutingConfiguration.SlotIdRoute) route.get()).getSlotId())
                                            .setSlotType(
                                                    ((RequestRoutingConfiguration.SlotIdRoute) route.get())
                                                            .getSlotType()
                                                            .getSlotTypes())));
        } else if (route.get() instanceof RequestRoutingConfiguration.SlotKeyRoute) {
            builder.setRoute(
                    redis_request.RedisRequestOuterClass.Routes.newBuilder()
                            .setSlotKeyRoute(
                                    redis_request.RedisRequestOuterClass.SlotKeyRoute.newBuilder()
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
}
