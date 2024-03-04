#include <stdarg.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>

enum ErrorType {
  ClosingError = 0,
  RequestError = 1,
  TimeoutError = 2,
  ExecAbortError = 3,
  ConnectionError = 4,
};
typedef uint32_t ErrorType;

typedef struct RedisErrorFFI {
  const char *message;
  ErrorType error_type;
} RedisErrorFFI;

typedef struct ConnectionResponse {
  const void *conn_ptr;
  const struct RedisErrorFFI *error;
} ConnectionResponse;

typedef void (*SuccessCallback)(uintptr_t channel_address, const char *message);

typedef void (*FailureCallback)(uintptr_t channel_address, const char *err_message);

/**
 * Creates a new client to the given address. The success callback needs to copy the given string synchronously, since it will be dropped by Rust once the callback returns. All callbacks should be offloaded to separate threads in order not to exhaust the client's thread pool.
 */
const struct ConnectionResponse *create_client(const uint8_t *connection_request,
                                               uintptr_t request_len,
                                               SuccessCallback success_callback,
                                               FailureCallback failure_callback);

void close_client(const void *client_ptr);
