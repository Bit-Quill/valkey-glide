package glide.managers;

import glide.api.models.exceptions.RedisException;
import lombok.AllArgsConstructor;
import response.ResponseOuterClass.Response;

/**
 * Response resolver responsible for evaluating the Redis response object with a success or failure.
 */
@AllArgsConstructor
public class BaseCommandResponseResolver
        implements RedisExceptionCheckedFunction<Response, Object> {

    private RedisExceptionCheckedFunction<Long, Object> respPointerResolver;

    /**
     * Extracts value from the RESP pointer.
     *
     * @return A generic Object with the Response or null if the response is empty
     */
    public Object apply(Response response) throws RedisException {
        // Note: errors are already handled before in CallbackDispatcher
        if (response.hasConstantResponse()) {
            // Return "OK"
            return response.getConstantResponse().toString();
        }
        if (response.hasRespPointer()) {
            // Return the shared value - which may be a null value
            return respPointerResolver.apply(response.getRespPointer());
        }
        // if no response payload is provided, assume null
        return null;
    }
}
