/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api;

import glide.api.models.exceptions.RedisException;
import glide.ffi.resolvers.RedisValueResolver;
import glide.managers.BaseCommandResponseResolver;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import response.ResponseOuterClass.Response;

/** Base Client class for Redis */
@AllArgsConstructor
public abstract class BaseClient implements AutoCloseable {

    protected final ConnectionManager connectionManager;
    protected final CommandManager commandManager;

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
     * Async request for an async (non-blocking) Redis client.
     *
     * @param config Redis client Configuration
     * @param constructor Redis client constructor reference
     * @param <T> Client type
     * @return a Future to connect and return a RedisClient
     */
    protected static <T> CompletableFuture<T> CreateClient(
            BaseClientConfiguration config,
            BiFunction<ConnectionManager, CommandManager, T> constructor) {
        try {
            ChannelHandler channelHandler = buildChannelHandler();
            ConnectionManager connectionManager = buildConnectionManager(channelHandler);
            CommandManager commandManager = buildCommandManager(channelHandler);
            // TODO: Support exception throwing, including interrupted exceptions
            return connectionManager
                    .connectToRedis(config)
                    .thenApply(ignore -> constructor.apply(connectionManager, commandManager));
        } catch (InterruptedException e) {
            // Something bad happened while we were establishing netty connection to UDS
            var future = new CompletableFuture<T>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Check for errors in the Response and return null Throws an error if an unexpected value is
     * returned
     *
     * @return null if the response is empty
     */
    protected static Void handleVoidResponse(Response response) {
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
    protected static String handleStringResponse(Response response) {
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
     * the * value as a HashMap
     *
     * @param response Redis protobuf message
     * @return Response as a String
     */
    protected static HashMap<String, Object> handleMapResponse(Response response) {
        Object value = handleObjectResponse(response);
        if (value instanceof HashMap) {
            return (HashMap<String, Object>) value;
        }
        throw new RedisException(
                "Unexpected return type from Redis: got "
                        + value.getClass().getSimpleName()
                        + " expected HashMap");
    }

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
}
