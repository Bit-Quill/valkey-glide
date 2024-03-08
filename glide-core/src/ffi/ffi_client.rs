/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */

use std::ffi::{c_char, c_void, CStr, CString};
use std::slice::from_raw_parts;
use std::str::Utf8Error;
use redis::{Cmd, cmd, FromRedisValue, RedisResult, Value};
use tokio::runtime::Builder;
// TODO
//   #[cfg(feature = "socket-layer")]
//   from https://github.com/aws/glide-for-redis/pull/1088
use protobuf::Message;
use crate::client::Client as GlideClient;
use crate::errors::{error_message, error_type, RequestErrorType};
use crate::ffi::configuration::{ConnectionConfig, NodeAddress, ProtocolVersion, ReadFrom, TlsMode};
use crate::connection_request;
use crate::ffi::types::{Client, ConnectionResponse, FailureCallback, RequestType, SuccessCallback};

struct CreateClientError {
    message: String,
    error_type: RequestErrorType,
}

/// Convert raw C string to a rust string.
///
/// # Safety
///
/// * `ptr` must be able to be safely casted to a valid `CStr` via `CStr::from_ptr`. See the safety documentation of [`std::ffi_client::CStr::from_ptr`](https://doc.rust-lang.org/std/ffi/struct.CStr.html#method.from_ptr).
unsafe fn ptr_to_str(ptr: *const c_char) -> String {
    if ptr as i64 != 0 {
        unsafe { CStr::from_ptr(ptr) }.to_str().unwrap().into()
    } else {
        "".into()
    }
}

/// Convert raw array pointer to a vector of [`NodeAddress`](NodeAddress)es.
///
/// # Safety
///
/// * `len` must not be greater than `isize::MAX`. See the safety documentation of [`std::slice::from_raw_parts`](https://doc.rust-lang.org/std/slice/fn.from_raw_parts.html).
/// * `data` must not be null.
/// * `data` must point to `len` consecutive properly initialized [`NodeAddress`](NodeAddress) structs.
/// * Each [`NodeAddress`](NodeAddress) dereferenced by `data` must contain a valid string pointer. See the safety documentation of [`ptr_to_str`](ptr_to_str).
#[allow(rustdoc::redundant_explicit_links)]
unsafe fn node_addresses_to_proto(
    data: *const *const NodeAddress,
    len: usize,
) -> Vec<connection_request::NodeAddress> {
    unsafe { from_raw_parts(data as *mut NodeAddress, len) }
        .iter()
        .map(|addr| {
            let mut address_info = connection_request::NodeAddress::new();
            address_info.host = unsafe { ptr_to_str(addr.host) }.into();
            address_info.port = addr.port as u32;
            address_info
        })
        .collect()
}

/// Convert connection configuration to a corresponding protobuf object.
///
/// # Safety
///
/// * `config` must not be null.
/// * `config` must be a valid pointer to a [`ConnectionConfig`](ConnectionConfig) struct.
/// * Dereferenced [`ConnectionConfig`](ConnectionConfig) struct and all nested structs must contain valid pointers. See the safety documentation of [`node_addresses_to_proto`](node_addresses_to_proto) and [`ptr_to_str`](ptr_to_str).
#[allow(rustdoc::redundant_explicit_links)]
// TODO return Result<...>
unsafe fn create_connection_request_using_config(
    config: *const ConnectionConfig,
) -> connection_request::ConnectionRequest {
    let mut connection_request = connection_request::ConnectionRequest::new();

    let config_ref = unsafe { &*config };

    connection_request.addresses =
        unsafe { node_addresses_to_proto(config_ref.addresses, config_ref.address_count) };

    connection_request.tls_mode = match config_ref.tls_mode {
        TlsMode::Secure => connection_request::TlsMode::SecureTls,
        TlsMode::Insecure => connection_request::TlsMode::InsecureTls,
        TlsMode::NoTls => connection_request::TlsMode::NoTls,
    }
        .into();
    connection_request.cluster_mode_enabled = config_ref.cluster_mode;
    connection_request.request_timeout = config_ref.request_timeout;

    connection_request.read_from = match config_ref.read_from {
        ReadFrom::AZAffinity => connection_request::ReadFrom::AZAffinity,
        ReadFrom::PreferReplica => connection_request::ReadFrom::PreferReplica,
        ReadFrom::Primary => connection_request::ReadFrom::Primary,
        ReadFrom::LowestLatency => connection_request::ReadFrom::LowestLatency,
    }
        .into();

    let mut retry_strategy = connection_request::ConnectionRetryStrategy::new();
    retry_strategy.number_of_retries = config_ref.connection_retry_strategy.number_of_retries;
    retry_strategy.factor = config_ref.connection_retry_strategy.factor;
    retry_strategy.exponent_base = config_ref.connection_retry_strategy.exponent_base;
    connection_request.connection_retry_strategy = Some(retry_strategy).into();

    let mut auth_info = connection_request::AuthenticationInfo::new();
    auth_info.username = unsafe { ptr_to_str(config_ref.authentication_info.username) }.into();
    auth_info.password = unsafe { ptr_to_str(config_ref.authentication_info.password) }.into();
    connection_request.authentication_info = Some(auth_info).into();

    connection_request.database_id = config_ref.database_id;
    connection_request.protocol = match config_ref.protocol {
        ProtocolVersion::RESP2 => connection_request::ProtocolVersion::RESP2,
        ProtocolVersion::RESP3 => connection_request::ProtocolVersion::RESP3,
    }
        .into();

    connection_request.client_name = unsafe { ptr_to_str(config_ref.client_name) }.into();

    connection_request
}

fn create_connection_request_using_bytes(connection_request_bytes: &[u8])
    -> Result<connection_request::ConnectionRequest, CreateClientError> {
    connection_request::ConnectionRequest::parse_from_bytes(connection_request_bytes)
        .map_err(|err| CreateClientError {
            message: err.to_string(),
            error_type: RequestErrorType::Unspecified,
        })
}

/// # Safety
///
/// * `config` must be a valid [`ConnectionConfig`](ConnectionConfig) pointer. See the safety documentation of [`create_connection_request`](create_connection_request_using_config).
#[allow(rustdoc::redundant_explicit_links)]
unsafe fn create_client_using_config_internal(
    config: *const ConnectionConfig,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> Result<Client, CreateClientError> {
    let request = unsafe { create_connection_request_using_config(config) };
    create_client_using_connection_request(request, success_callback, failure_callback)
}

fn create_client_using_protobuf_internal(
    connection_request_bytes: &[u8],
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> Result<Client, CreateClientError> {
    let request = create_connection_request_using_bytes(connection_request_bytes)?;
    create_client_using_connection_request(request, success_callback, failure_callback)
}

// TODO PR #1088
fn create_client_using_connection_request(
    connection_request: connection_request::ConnectionRequest,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> Result<Client, CreateClientError> {
    let runtime = Builder::new_multi_thread()
        .enable_all()
        .thread_name("GLIDE for Redis Go thread")
        .build()
        .map_err(|err| {
            let redis_error = err.into();
            CreateClientError {
                message: error_message(&redis_error),
                error_type: error_type(&redis_error),
            }
        })?;
    let _runtime_handle = runtime.enter();
    let client = runtime
        .block_on(GlideClient::new(connection_request))
        .map_err(|err| CreateClientError {
            message: err.to_string(),
            error_type: RequestErrorType::Disconnect,
        })?;
    Ok(Client {
        client,
        success_callback,
        failure_callback,
        runtime,
    })
}

fn create_client_result_to_connection_response(result: Result<Client, CreateClientError>) -> ConnectionResponse {
    match result {
        Err(err) => ConnectionResponse {
            conn_ptr: std::ptr::null(),
            error_message: CString::into_raw(CString::new(err.message).unwrap()),
            error_type: err.error_type,
        },
        Ok(client) => ConnectionResponse {
            conn_ptr: Box::into_raw(Box::new(client)) as *const c_void,
            error_message: std::ptr::null(),
            error_type: RequestErrorType::Unspecified,
        }
    }
}

/// Creates a new client with the given configuration. The success callback needs to copy the given string synchronously, since it will be dropped by Rust once the callback returns. All callbacks should be offloaded to separate threads in order not to exhaust the client's thread pool.
///
/// # Safety
///
/// * `config` must be a valid [`ConnectionConfig`](ConnectionConfig) pointer. See the safety documentation of [`create_client_internal`](create_client_using_config_internal).
#[allow(rustdoc::redundant_explicit_links)]
#[allow(rustdoc::private_intra_doc_links)]
#[no_mangle]
pub unsafe extern "C" fn create_client_using_config(
    config: *const ConnectionConfig,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> *const ConnectionResponse {
    let result = unsafe { create_client_using_config_internal(config, success_callback, failure_callback) };
    let response = create_client_result_to_connection_response(result);
    Box::into_raw(Box::new(response))
}

/// Creates a new client with the given configuration. The success callback needs to copy the given string synchronously, since it will be dropped by Rust once the callback returns. All callbacks should be offloaded to separate threads in order not to exhaust the client's thread pool.
///
/// The returned `ConnectionResponse` should be manually freed by calling `free_connection_response`, otherwise a memory leak will occur. It should be freed whether or not an error occurs.
///
/// # Safety
///
/// * `connection_request_bytes` must point to `connection_request_len` consecutive properly initialized bytes.
/// * `connection_request_len` must not be greater than `isize::MAX`. See the safety documentation of [`std::slice::from_raw_parts`](https://doc.rust-lang.org/std/slice/fn.from_raw_parts.html).
#[no_mangle]
pub unsafe extern "C" fn create_client_using_protobuf(
    connection_request_bytes: *const u8,
    connection_request_len: usize,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> *const ConnectionResponse {
    let request_bytes = unsafe { from_raw_parts(connection_request_bytes, connection_request_len) };
    let result = create_client_using_protobuf_internal(request_bytes, success_callback, failure_callback);
    let response = create_client_result_to_connection_response(result);
    Box::into_raw(Box::new(response))
}

/// Closes the given client, deallocating it from the heap.
///
/// # Safety
///
/// * `client_ptr` must not be null.
/// * `client_ptr` must be able to be safely casted to a valid `Box<Client>` via `Box::from_raw`. See the safety documentation of [`std::boxed::Box::from_raw`](https://doc.rust-lang.org/std/boxed/struct.Box.html#method.from_raw).
#[no_mangle]
pub unsafe extern "C" fn close_client(client_ptr: *const c_void) {
    let client_ptr = unsafe { Box::from_raw(client_ptr as *mut Client) };
    let _runtime_handle = client_ptr.runtime.enter();
    drop(client_ptr);
}

// copied from glide_core::socket_listener::get_command
pub fn get_command(request_type: RequestType) -> Option<Cmd> {
    match request_type {
        RequestType::InvalidRequest => None,
        RequestType::CustomCommand => Some(Cmd::new()),
        RequestType::GetString => Some(cmd("GET")),
        RequestType::SetString => Some(cmd("SET")),
        RequestType::Ping => Some(cmd("PING")),
        RequestType::Info => Some(cmd("INFO")),
        RequestType::Del => Some(cmd("DEL")),
        RequestType::Select => Some(cmd("SELECT")),
        RequestType::ConfigGet => Some(get_two_word_command("CONFIG", "GET")),
        RequestType::ConfigSet => Some(get_two_word_command("CONFIG", "SET")),
        RequestType::ConfigResetStat => Some(get_two_word_command("CONFIG", "RESETSTAT")),
        RequestType::ConfigRewrite => Some(get_two_word_command("CONFIG", "REWRITE")),
        RequestType::ClientGetName => Some(get_two_word_command("CLIENT", "GETNAME")),
        RequestType::ClientGetRedir => Some(get_two_word_command("CLIENT", "GETREDIR")),
        RequestType::ClientId => Some(get_two_word_command("CLIENT", "ID")),
        RequestType::ClientInfo => Some(get_two_word_command("CLIENT", "INFO")),
        RequestType::ClientKill => Some(get_two_word_command("CLIENT", "KILL")),
        RequestType::ClientList => Some(get_two_word_command("CLIENT", "LIST")),
        RequestType::ClientNoEvict => Some(get_two_word_command("CLIENT", "NO-EVICT")),
        RequestType::ClientNoTouch => Some(get_two_word_command("CLIENT", "NO-TOUCH")),
        RequestType::ClientPause => Some(get_two_word_command("CLIENT", "PAUSE")),
        RequestType::ClientReply => Some(get_two_word_command("CLIENT", "REPLY")),
        RequestType::ClientSetInfo => Some(get_two_word_command("CLIENT", "SETINFO")),
        RequestType::ClientSetName => Some(get_two_word_command("CLIENT", "SETNAME")),
        RequestType::ClientUnblock => Some(get_two_word_command("CLIENT", "UNBLOCK")),
        RequestType::ClientUnpause => Some(get_two_word_command("CLIENT", "UNPAUSE")),
        RequestType::Expire => Some(cmd("EXPIRE")),
        RequestType::HashSet => Some(cmd("HSET")),
        RequestType::HashGet => Some(cmd("HGET")),
        RequestType::HashDel => Some(cmd("HDEL")),
        RequestType::HashExists => Some(cmd("HEXISTS")),
        RequestType::MSet => Some(cmd("MSET")),
        RequestType::MGet => Some(cmd("MGET")),
        RequestType::Incr => Some(cmd("INCR")),
        RequestType::IncrBy => Some(cmd("INCRBY")),
        RequestType::IncrByFloat => Some(cmd("INCRBYFLOAT")),
        RequestType::Decr => Some(cmd("DECR")),
        RequestType::DecrBy => Some(cmd("DECRBY")),
        RequestType::HashGetAll => Some(cmd("HGETALL")),
        RequestType::HashMSet => Some(cmd("HMSET")),
        RequestType::HashMGet => Some(cmd("HMGET")),
        RequestType::HashIncrBy => Some(cmd("HINCRBY")),
        RequestType::HashIncrByFloat => Some(cmd("HINCRBYFLOAT")),
        RequestType::LPush => Some(cmd("LPUSH")),
        RequestType::LPop => Some(cmd("LPOP")),
        RequestType::RPush => Some(cmd("RPUSH")),
        RequestType::RPop => Some(cmd("RPOP")),
        RequestType::LLen => Some(cmd("LLEN")),
        RequestType::LRem => Some(cmd("LREM")),
        RequestType::LRange => Some(cmd("LRANGE")),
        RequestType::LTrim => Some(cmd("LTRIM")),
        RequestType::SAdd => Some(cmd("SADD")),
        RequestType::SRem => Some(cmd("SREM")),
        RequestType::SMembers => Some(cmd("SMEMBERS")),
        RequestType::SCard => Some(cmd("SCARD")),
        RequestType::PExpireAt => Some(cmd("PEXPIREAT")),
        RequestType::PExpire => Some(cmd("PEXPIRE")),
        RequestType::ExpireAt => Some(cmd("EXPIREAT")),
        RequestType::Exists => Some(cmd("EXISTS")),
        RequestType::Unlink => Some(cmd("UNLINK")),
        RequestType::TTL => Some(cmd("TTL")),
        RequestType::Zadd => Some(cmd("ZADD")),
        RequestType::Zrem => Some(cmd("ZREM")),
        RequestType::Zrange => Some(cmd("ZRANGE")),
        RequestType::Zcard => Some(cmd("ZCARD")),
        RequestType::Zcount => Some(cmd("ZCOUNT")),
        RequestType::ZIncrBy => Some(cmd("ZINCRBY")),
        RequestType::ZScore => Some(cmd("ZSCORE")),
        RequestType::Type => Some(cmd("TYPE")),
        RequestType::HLen => Some(cmd("HLEN")),
        RequestType::Echo => Some(cmd("ECHO")),
        RequestType::ZPopMin => Some(cmd("ZPOPMIN")),
        RequestType::Strlen => Some(cmd("STRLEN")),
        RequestType::Lindex => Some(cmd("LINDEX")),
        RequestType::ZPopMax => Some(cmd("ZPOPMAX")),
        RequestType::XAck => Some(cmd("XACK")),
        RequestType::XAdd => Some(cmd("XADD")),
        RequestType::XReadGroup => Some(cmd("XREADGROUP")),
        RequestType::XRead => Some(cmd("XREAD")),
        RequestType::XGroupCreate => Some(get_two_word_command("XGROUP", "CREATE")),
        RequestType::XGroupDestroy => Some(get_two_word_command("XGROUP", "DESTROY")),
        RequestType::XTrim => Some(cmd("XTRIM")),
        RequestType::HSetNX => Some(cmd("HSETNX")),
        RequestType::SIsMember => Some(cmd("SISMEMBER")),
        RequestType::Hvals => Some(cmd("HVALS")),
        RequestType::PTTL => Some(cmd("PTTL")),
        RequestType::ZRemRangeByRank => Some(cmd("ZREMRANGEBYRANK")),
        RequestType::Persist => Some(cmd("PERSIST")),
        _ => None,
    }
}

// copied from glide_core::socket_listener::get_two_word_command
fn get_two_word_command(first: &str, second: &str) -> Cmd {
    let mut cmd = cmd(first);
    cmd.arg(second);
    cmd
}

unsafe fn convert_double_pointer_to_vec(
    data: *const *const c_char,
    len: usize,
) -> Result<Vec<String>, Utf8Error> {
    unsafe { from_raw_parts(data, len) }
        .iter()
        .map(|arg| unsafe { CStr::from_ptr(*arg) }.to_str().map(ToString::to_string))
        .collect()
}

unsafe fn submit_error_into_callback(
    client: &mut Client,
    callback_index: usize,
    error_message: String,
    error_type: RequestErrorType,
) -> () {
    logger_core::log_debug("command error", format!("callback: {}, error: {}", callback_index, error_message));
    let c_err_str = CString::new(error_message).unwrap();
    unsafe { (client.failure_callback)(callback_index, c_err_str.as_ptr(), error_type) };
}

/// Execute an arbitrary command. See [`redis.io`](https://redis.io/commands/) for details.
///
/// # Safety
///
/// * `client_ptr` must not be null.
/// * `client_ptr` must be able to be safely casted to a valid `Box<Client>` via `Box::from_raw`. See the safety documentation of [`std::boxed::Box::from_raw`](https://doc.rust-lang.org/std/boxed/struct.Box.html#method.from_raw).
/// * `key` and `value` must not be null.
/// * `key` and `value` must be able to be safely casted to a valid `CStr` via `CStr::from_ptr`. See the safety documentation of [`std::ffi_client::CStr::from_ptr`](https://doc.rust-lang.org/std/ffi/struct.CStr.html#method.from_ptr).
/// * `key` and `value` must be kept valid until the callback is called.
/// * `arg_count` must not be greater than `isize::MAX`. See the safety documentation of [`std::slice::from_raw_parts`](https://doc.rust-lang.org/std/slice/fn.from_raw_parts.html).
/// * `args` must not be null.
/// * `args` must point to `arg_count` consecutive properly initialized null-terminated C strings.
/// TODO ref other unsafe fns
#[no_mangle]
pub unsafe extern "C" fn command(
    client_ptr: *const c_void,
    callback_index: usize,
    command_type: RequestType,
    arg_count: usize,
    args: *const *const c_char
) {
    let client = unsafe { Box::leak(Box::from_raw(client_ptr as *mut Client)) };
    // The safety of this needs to be ensured by the calling code. Cannot dispose of the pointer before all operations have completed.
    let ptr_address = client_ptr as usize;

    let arg_vec = match unsafe { convert_double_pointer_to_vec(args, arg_count) } {
        Ok(vec) => vec,
        Err(_) => {
            unsafe { submit_error_into_callback(client, callback_index, "Failed to read arguments".into(), RequestErrorType::Unspecified)}
            return;
        }
    };

    let mut client_clone = client.client.clone();
    client.runtime.spawn(async move {
        let client = unsafe { Box::leak(Box::from_raw(ptr_address as *mut Client)) };
        let mut cmd = match get_command(command_type) {
            Some(cmd) => cmd,
            None => {
                unsafe { submit_error_into_callback(client, callback_index, "Failed to read command type".into(), RequestErrorType::Unspecified)}
                return;
            }
        };
        cmd.arg(arg_vec);
        let result = client_clone.send_command(&cmd, None).await;

        let value = match result {
            Ok(value) => value,
            Err(err) => {
                unsafe { submit_error_into_callback(client, callback_index, error_message(&err), error_type(&err))}
                return;
            }
        };

        let result : RedisResult<Option<CString>> = match value {
            Value::Nil => Ok(None),
            Value::Int(num) => Ok(Some(CString::new(format!("{}", num)).unwrap())),
            Value::SimpleString(_) | Value::BulkString(_) => Option::<CString>::from_owned_redis_value(value),
            Value::Okay => Ok(Some(CString::new("OK").unwrap())),
            Value::Double(num) => Ok(Some(CString::new(format!("{}", num)).unwrap())),
            Value::Boolean(bool) => Ok(Some(CString::new(format!("{}", bool)).unwrap())),
            other => {
                // TODO maybe use _ instead of other
                unsafe { submit_error_into_callback(client, callback_index, format!("Failed to process response of type {:?}", other), RequestErrorType::Unspecified)}
                return;
            }
        };

        unsafe {
            match result {
                Ok(None) => (client.success_callback)(callback_index, std::ptr::null()),
                Ok(Some(c_str)) => (client.success_callback)(callback_index, c_str.as_ptr()),
                Err(err) => submit_error_into_callback(client, callback_index, error_message(&err), error_type(&err))
            };
        }
    });
}
