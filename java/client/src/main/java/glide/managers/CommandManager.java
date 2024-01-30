package glide.managers;

import glide.api.BaseClient;
import glide.connectors.handlers.ChannelHandler;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import redis_request.RedisRequestOuterClass;
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
            RedisRequest.Builder command, RedisExceptionCheckedFunction<Response, T> responseHandler) {
        // write command request to channel
        // when complete, convert the response to our expected type T using the given responseHandler
        return channel.write(command, true).thenApplyAsync(responseHandler::apply);
    }

    public static RedisRequestOuterClass.RequestType mapRequestTypes(BaseClient.RequestType inType) {
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
