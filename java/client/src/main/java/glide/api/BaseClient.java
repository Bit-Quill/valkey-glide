/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api;

import static glide.ffi.resolvers.SocketListenerResolver.getSocket;
import static redis_request.RedisRequestOuterClass.RequestType.Decr;
import static redis_request.RedisRequestOuterClass.RequestType.DecrBy;
import static redis_request.RedisRequestOuterClass.RequestType.GetString;
import static redis_request.RedisRequestOuterClass.RequestType.Incr;
import static redis_request.RedisRequestOuterClass.RequestType.IncrBy;
import static redis_request.RedisRequestOuterClass.RequestType.IncrByFloat;
import static redis_request.RedisRequestOuterClass.RequestType.MGet;
import static redis_request.RedisRequestOuterClass.RequestType.MSet;
import static redis_request.RedisRequestOuterClass.RequestType.Ping;
import static redis_request.RedisRequestOuterClass.RequestType.SetString;

import glide.api.commands.ConnectionManagementCommands;
import glide.api.commands.StringCommands;
import glide.api.models.commands.SetOptions;
import glide.api.models.configuration.BaseClientConfiguration;
import glide.api.models.exceptions.RedisException;
import glide.connectors.handlers.CallbackDispatcher;
import glide.connectors.handlers.ChannelHandler;
import glide.ffi.resolvers.RedisValueResolver;
import glide.managers.BaseCommandResponseResolver;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import response.ResponseOuterClass.Response;

/** Base Client class for Redis */
@AllArgsConstructor
public abstract class BaseClient
        implements AutoCloseable, StringCommands, ConnectionManagementCommands {

    public static final String OK = "OK";

    protected final ConnectionManager connectionManager;
    protected final CommandManager commandManager;

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
     * Closes this resource, relinquishing any underlying resources. This method is invoked
     * automatically on objects managed by the try-with-resources statement.
     *
     * @see <a
     *     href="https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html#close--">AutoCloseable::close()</a>
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
     * appropriate response as an <code>Object</code>.
     *
     * @param response Redis protobuf message
     * @return Response <code>Object</code>
     */
    protected Object handleObjectResponse(Response response) {
        // convert protobuf response into Object
        return new BaseCommandResponseResolver(RedisValueResolver::valueFromPointer).apply(response);
    }

    /**
     * Extracts the response value from the Redis response and either throws an exception or returns
     * the value as a <code>String</code>.
     *
     * @param response Redis protobuf message
     * @return Response as a <code>String</code>
     * @throws RedisException if there's a type mismatch
     */
    protected String handleStringResponse(Response response) {
        Object value = handleObjectResponse(response);
        if (value instanceof String || value == null) {
            return (String) value;
        }
        throw new RedisException(
                "Unexpected return type from Redis: got "
                        + value.getClass().getSimpleName()
                        + " expected String");
    }

    /**
     * Extracts the response value from the Redis response and either throws an exception or returns
     * the value as an <code>Long</code>.
     *
     * @param response Redis protobuf message
     * @return Response as an <code>Long</code>
     */
    protected Long handleLongResponse(Response response) {
        Object value = handleObjectResponse(response);
        if (value instanceof Long) {
            return (Long) value;
        }
        throw new RedisException(
                "Unexpected return type from Redis: got "
                        + value.getClass().getSimpleName()
                        + " expected Long");
    }

    /**
     * Extracts the response value from the Redis response and either throws an exception or returns
     * the value as an <code>Double</code>.
     *
     * @param response Redis protobuf message
     * @return Response as an <code>Double</code>
     */
    protected Double handleDoubleResponse(Response response) {
        Object value = handleObjectResponse(response);
        if (value instanceof Double) {
            return (Double) value;
        }
        throw new RedisException(
                "Unexpected return type from Redis: got "
                        + value.getClass().getSimpleName()
                        + " expected Double");
    }

    /**
     * Extracts the response value from the Redis response and either throws an exception or returns
     * the value as an <code>Object[]</code>.
     *
     * @param response Redis protobuf message
     * @return Response as an <code>Object[]</code>
     * @throws RedisException if there's a type mismatch
     */
    protected Object[] handleArrayResponse(Response response) {
        Object value = handleObjectResponse(response);
        if (value instanceof Object[]) {
            return (Object[]) value;
        }
        String className = (value == null) ? "null" : value.getClass().getSimpleName();
        throw new RedisException(
                "Unexpected return type from Redis: got " + className + " expected Object[]");
    }

    /**
     * Extracts the response value from the Redis response and either throws an exception or returns
     * the value as a <code>Map</code>.
     *
     * @param response Redis protobuf message
     * @return Response as a <code>Map</code>
     * @throws RedisException if there's a type mismatch
     */
    @SuppressWarnings("unchecked")
    protected Map<Object, Object> handleMapResponse(Response response) {
        Object value = handleObjectResponse(response);
        if (value instanceof Map) {
            return (Map<Object, Object>) value;
        }
        String className = (value == null) ? "null" : value.getClass().getSimpleName();
        throw new RedisException(
                "Unexpected return type from Redis: got " + className + " expected Map");
    }

    @Override
    public CompletableFuture<String> ping() {
        return commandManager.submitNewCommand(Ping, new String[0], this::handleStringResponse);
    }

    @Override
    public CompletableFuture<String> ping(String msg) {
        return commandManager.submitNewCommand(Ping, new String[] {msg}, this::handleStringResponse);
    }

    @Override
    public CompletableFuture<String> get(String key) {
        return commandManager.submitNewCommand(
                GetString, new String[] {key}, this::handleStringResponse);
    }

    @Override
    public CompletableFuture<String> set(String key, String value) {
        return commandManager.submitNewCommand(
                SetString, new String[] {key, value}, this::handleStringResponse);
    }

    @Override
    public CompletableFuture<String> set(String key, String value, SetOptions options) {
        String[] arguments = ArrayUtils.addAll(new String[] {key, value}, options.toArgs());
        return commandManager.submitNewCommand(SetString, arguments, this::handleStringResponse);
    }

    @Override
    public CompletableFuture<Long> decr(String key) {
        return commandManager.submitNewCommand(Decr, new String[] {key}, this::handleLongResponse);
    }

    @Override
    public CompletableFuture<Long> decrBy(String key, long amount) {
        return commandManager.submitNewCommand(
                DecrBy, new String[] {key, Long.toString(amount)}, this::handleLongResponse);
    }

    @Override
    public CompletableFuture<Long> incr(String key) {
        return commandManager.submitNewCommand(Incr, new String[] {key}, this::handleLongResponse);
    }

    @Override
    public CompletableFuture<Long> incrBy(String key, long amount) {
        return commandManager.submitNewCommand(
                IncrBy, new String[] {key, Long.toString(amount)}, this::handleLongResponse);
    }

    @Override
    public CompletableFuture<Double> incrByFloat(String key, double amount) {
        return commandManager.submitNewCommand(
                IncrByFloat, new String[] {key, Double.toString(amount)}, this::handleDoubleResponse);
    }

    @Override
    public CompletableFuture<String[]> mget(String[] keys) {
        return commandManager
                .submitNewCommand(MGet, keys, this::handleArrayResponse)
                .thenApply(
                        objectsArray ->
                                Arrays.stream(objectsArray).map(object -> (String) object).toArray(String[]::new));
    }

    @Override
    public CompletableFuture<String> mset(HashMap<String, String> keyValueMap) {
        String[] args = new String[keyValueMap.size() * 2];

        int i = 0;
        for (Map.Entry<String, String> entry : keyValueMap.entrySet()) {
            args[i++] = entry.getKey();
            args[i++] = entry.getValue();
        }

        return commandManager.submitNewCommand(MSet, args, this::handleStringResponse);
    }
}
