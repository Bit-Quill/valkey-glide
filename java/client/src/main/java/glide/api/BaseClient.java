package glide.api;

import glide.api.commands.BaseCommands;
import glide.api.commands.Transaction;
import glide.api.models.exceptions.RedisException;
import glide.ffi.resolvers.RedisValueResolver;
import glide.managers.BaseCommandResponseResolver;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import glide.managers.models.Command;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.AllArgsConstructor;
import response.ResponseOuterClass.Response;

/** Base Client class for Redis */
@AllArgsConstructor
public abstract class BaseClient implements AutoCloseable, BaseCommands {

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

    /**
     * Extracts the response from the Protobuf response and either throws an exception or returns the
     * appropriate response as an Object
     *
     * @param response Redis protobuf message
     * @return Response Object
     */
    protected static Object handleObjectResponse(Response response) {
        // convert protobuf response into Object and then Object into T
        return new BaseCommandResponseResolver(RedisValueResolver::valueFromPointer).apply(response);
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
     * the value as an Object[].
     *
     * @param response Redis protobuf message
     * @return Response as an Object[]
     */
    protected static Object[] handleArrayResponse(Response response) {
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

    @Override
    public CompletableFuture<Object> customCommand(String[] args) {
        Command command =
                Command.builder().requestType(Command.RequestType.CUSTOM_COMMAND).arguments(args).build();
        return commandManager.submitNewCommand(command, BaseClient::handleObjectResponse);
    }

    /**
     * Execute a transaction by processing the queued commands.
     *
     * @see <a href="https://redis.io/topics/Transactions/">redis.io</a> for details on Redis
     *     Transactions.
     * @param transaction - A {@link Transaction} object containing a list of commands to be executed.
     * @return A list of results corresponding to the execution of each command in the transaction.
     *     <ul>
     *       <li>If a command returns a value, it will be included in the list. If a command doesn't
     *           return a value, the list entry will be null.
     *       <li>If the transaction failed due to a WATCH command, `exec` will return `null`.
     *     </ul>
     */
    public CompletableFuture<Object[]> exec(Transaction transaction) {
        return commandManager.submitNewTransaction(transaction, BaseClient::handleArrayResponse);
    }
}
