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

enum RequestType {
  CustomCommand = 1,
  GetString = 2,
  SetString = 3,
  Ping = 4,
  Info = 5,
  Del = 6,
  Select = 7,
  ConfigGet = 8,
  ConfigSet = 9,
  ConfigResetStat = 10,
  ConfigRewrite = 11,
  ClientGetName = 12,
  ClientGetRedir = 13,
  ClientId = 14,
  ClientInfo = 15,
  ClientKill = 16,
  ClientList = 17,
  ClientNoEvict = 18,
  ClientNoTouch = 19,
  ClientPause = 20,
  ClientReply = 21,
  ClientSetInfo = 22,
  ClientSetName = 23,
  ClientUnblock = 24,
  ClientUnpause = 25,
  Expire = 26,
  HashSet = 27,
  HashGet = 28,
  HashDel = 29,
  HashExists = 30,
  MGet = 31,
  MSet = 32,
  Incr = 33,
  IncrBy = 34,
  Decr = 35,
  IncrByFloat = 36,
  DecrBy = 37,
  HashGetAll = 38,
  HashMSet = 39,
  HashMGet = 40,
  HashIncrBy = 41,
  HashIncrByFloat = 42,
  LPush = 43,
  LPop = 44,
  RPush = 45,
  RPop = 46,
  LLen = 47,
  LRem = 48,
  LRange = 49,
  LTrim = 50,
  SAdd = 51,
  SRem = 52,
  SMembers = 53,
  SCard = 54,
  PExpireAt = 55,
  PExpire = 56,
  ExpireAt = 57,
  Exists = 58,
  Unlink = 59,
  TTL = 60,
  Zadd = 61,
  Zrem = 62,
  Zrange = 63,
  Zcard = 64,
  Zcount = 65,
  ZIncrBy = 66,
  ZScore = 67,
  Type = 68,
  HLen = 69,
  Echo = 70,
  ZPopMin = 71,
  Strlen = 72,
  Lindex = 73,
  ZPopMax = 74,
  XRead = 75,
  XAdd = 76,
  XReadGroup = 77,
  XAck = 78,
  XTrim = 79,
  XGroupCreate = 80,
  XGroupDestroy = 81,
};
typedef uint32_t RequestType;

typedef struct Level Level;

typedef struct Option_Level Option_Level;

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
struct ConnectionResponse create_client(const char *host,
                                        uint32_t port,
                                        bool use_tls,
                                        SuccessCallback success_callback,
                                        FailureCallback failure_callback);

void close_client(const void *client_ptr);

void command(const void *client_ptr,
             uintptr_t channel,
             RequestType command_type,
             uintptr_t arg_count,
             const char *const *args);

/**
 * # Safety
 * Unsafe function because creating string from pointer
 */
void log(struct Level log_level, const char *log_identifier, const char *message);

/**
 * # Safety
 * Unsafe function because creating string from pointer
 */
struct Level init(struct Option_Level level, const char *file_name);
