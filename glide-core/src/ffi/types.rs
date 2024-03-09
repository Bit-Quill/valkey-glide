/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */

use std::ffi::{c_char, c_void};
use tokio::runtime::Runtime;
use crate::errors::RequestErrorType;

// Cannot use glide_core::redis_request::RequestType, because it is not FFI safe
#[repr(u32)]
pub enum RequestType {
    // copied from redis_request.proto
    InvalidRequest = 0,
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
    HSetNX = 82,
    SIsMember = 83,
    Hvals = 84,
    PTTL = 85,
    ZRemRangeByRank = 86,
    Persist = 87,
}

// TODO use pub(create) for types below?

/// Success callback that is called when a Redis command succeeds.
///
/// # Safety
///
/// TODO - copy, ptr
// TODO: Change message type when implementing command logic
pub type SuccessCallback = unsafe extern "C" fn(
    callback_index: usize,
    message: *const c_char
) -> ();

/// Failure callback that is called when a Redis command fails.
///
/// # Safety
///
/// TODO - copy, ptr
pub type FailureCallback = unsafe extern "C" fn(
    callback_index: usize,
    error_message: *const c_char,
    error_type: RequestErrorType,
) -> ();

/// The connection response.
///
/// It contains either a connection or an error. It is represented as a struct instead of an enum for ease of use in the wrapper language.
///
/// This struct should be freed using both `free_connection_response` and `free_error` to avoid memory leaks.
#[repr(C)]
pub struct ConnectionResponse {
    pub conn_ptr: *const c_void,
    pub error_message: *const c_char,
    pub error_type: RequestErrorType,
}

/// The glide client.
pub struct Client {
    pub client: crate::client::Client,
    pub success_callback: SuccessCallback,
    pub failure_callback: FailureCallback,
    pub runtime: Runtime,
}
