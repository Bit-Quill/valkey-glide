/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */
pub mod connection;
use connection::{ConnectionConfig, NodeAddress, ProtocolVersion, ReadFrom, TlsMode};

use glide_core::client::Client as GlideClient;
use glide_core::connection_request;
use redis::{Cmd, FromRedisValue, RedisResult};
use std::{
    ffi::{c_void, CStr, CString},
    os::raw::c_char,
};
use tokio::runtime::{Builder, Runtime};

pub enum Level {
    Error = 0,
    Warn = 1,
    Info = 2,
    Debug = 3,
    Trace = 4,
}

pub struct Client {
    client: GlideClient,
    success_callback: unsafe extern "C" fn(usize, *const c_char) -> (),
    failure_callback: unsafe extern "C" fn(usize) -> (), // TODO - add specific error codes
    runtime: Runtime,
}

/// Convert raw C string to a rust string.
///
/// # Safety
///
/// * `ptr` must be able to be safely casted to a valid `CString` via `CString::from_raw` if `ptr` is not null. See the safety documentation of [`std::ffi::CString::from_raw`](https://doc.rust-lang.org/std/ffi/struct.CString.html#method.from_raw).
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
    unsafe { std::slice::from_raw_parts(data as *mut NodeAddress, len) }
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
/// * `config` must be a valid pointer to a [`ConnectionConfig`](ConnectionConfig) struct. See the safety documentation of [`std::ptr`](https://doc.rust-lang.org/std/ptr/index.html#safety).
/// * Dereferenced [`ConnectionConfig`](ConnectionConfig) struct and all nested structs must contain valid pointers. See the safety documentation of [`node_addresses_to_proto`](node_addresses_to_proto) and [`ptr_to_str`](ptr_to_str).
#[allow(rustdoc::redundant_explicit_links)]
unsafe fn create_connection_request(
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

/// # Safety
///
/// * `config` must be a valid `ConnectionConfig` pointer. See the safety documentation of [`create_connection_request`](create_connection_request).
#[allow(rustdoc::redundant_explicit_links)]
unsafe fn create_client_internal(
    config: *const ConnectionConfig,
    success_callback: unsafe extern "C" fn(usize, *const c_char) -> (),
    failure_callback: unsafe extern "C" fn(usize) -> (),
) -> RedisResult<Client> {
    let request = unsafe { create_connection_request(config) };
    let runtime = Builder::new_multi_thread()
        .enable_all()
        .thread_name("GLIDE for Redis C# thread")
        .build()?;
    let _runtime_handle = runtime.enter();
    let client = runtime.block_on(GlideClient::new(request)).unwrap(); // TODO - handle errors.
    Ok(Client {
        client,
        success_callback,
        failure_callback,
        runtime,
    })
}

/// Creates a new client to the configuration. The success callback needs to copy the given string synchronously, since it will be dropped by Rust once the callback returns. All callbacks should be offloaded to separate threads in order not to exhaust the client's thread pool.
///
/// # Safety
///
/// * `config` must be a valid `ConnectionConfig` pointer. See the safety documentation of [`create_client_internal`](create_client_internal).
#[allow(rustdoc::redundant_explicit_links)]
#[allow(rustdoc::private_intra_doc_links)]
#[no_mangle]
pub unsafe extern "C" fn create_client(
    config: *const ConnectionConfig,
    success_callback: unsafe extern "C" fn(usize, *const c_char) -> (),
    failure_callback: unsafe extern "C" fn(usize) -> (),
) -> *const c_void {
    match unsafe { create_client_internal(config, success_callback, failure_callback) } {
        Err(_) => std::ptr::null(), // TODO - log errors
        Ok(client) => Box::into_raw(Box::new(client)) as *const c_void,
    }
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

/// Execute `SET` command. See [`redis.io`](https://redis.io/commands/set/) for details.
///
/// # Safety
///
/// * `client_ptr` must not be null.
/// * `client_ptr` must be able to be safely casted to a valid `Box<Client>` via `Box::from_raw`. See the safety documentation of [`std::boxed::Box::from_raw`](https://doc.rust-lang.org/std/boxed/struct.Box.html#method.from_raw).
/// * `key` and `value` must not be null.
/// * `key` and `value` must be able to be safely casted to a valid `CString` via `CString::from_raw`. See the safety documentation of [`std::ffi::CString::from_raw`](https://doc.rust-lang.org/std/ffi/struct.CString.html#method.from_raw).
/// * `key` and `value` must be kept valid until the callback is called.
#[no_mangle]
pub extern "C" fn set(
    client_ptr: *const c_void,
    callback_index: usize,
    key: *const c_char,
    value: *const c_char,
) {
    let client = unsafe { Box::leak(Box::from_raw(client_ptr as *mut Client)) };
    // The safety of this needs to be ensured by the calling code. Cannot dispose of the pointer before all operations have completed.
    let ptr_address = client_ptr as usize;

    let key_cstring = unsafe { CStr::from_ptr(key as *mut c_char) };
    let value_cstring = unsafe { CStr::from_ptr(value as *mut c_char) };
    let mut client_clone = client.client.clone();
    client.runtime.spawn(async move {
        let key_bytes = key_cstring.to_bytes();
        let value_bytes = value_cstring.to_bytes();
        let mut cmd = Cmd::new();
        cmd.arg("SET").arg(key_bytes).arg(value_bytes);
        let result = client_clone.send_command(&cmd, None).await;
        unsafe {
            let client = Box::leak(Box::from_raw(ptr_address as *mut Client));
            match result {
                Ok(_) => (client.success_callback)(callback_index, std::ptr::null()), // TODO - should return "OK" string.
                Err(_) => (client.failure_callback)(callback_index), // TODO - report errors
            };
        }
    });
}

/// Execute `GET` command. See [`redis.io`](https://redis.io/commands/get/) for details.
///
/// # Safety
///
/// * `client_ptr` must not be null.
/// * `client_ptr` must be able to be safely casted to a valid `Box<Client>` via `Box::from_raw`. See the safety documentation of [`std::boxed::Box::from_raw`](https://doc.rust-lang.org/std/boxed/struct.Box.html#method.from_raw).
/// * `key` must not be null.
/// * `key` must be able to be safely casted to a valid `CString` via `CString::from_raw`. See the safety documentation of [`std::ffi::CString::from_raw`](https://doc.rust-lang.org/std/ffi/struct.CString.html#method.from_raw).
/// * `key` must be kept valid until the callback is called.
/// * If the callback is called with a string pointer, the pointer must be used synchronously, because the string will be dropped after the callback.
#[no_mangle]
pub extern "C" fn get(client_ptr: *const c_void, callback_index: usize, key: *const c_char) {
    let client = unsafe { Box::leak(Box::from_raw(client_ptr as *mut Client)) };
    // The safety of this needs to be ensured by the calling code. Cannot dispose of the pointer before all operations have completed.
    let ptr_address = client_ptr as usize;

    let key_cstring = unsafe { CStr::from_ptr(key as *mut c_char) };
    let mut client_clone = client.client.clone();
    client.runtime.spawn(async move {
        let key_bytes = key_cstring.to_bytes();
        let mut cmd = Cmd::new();
        cmd.arg("GET").arg(key_bytes);
        let result = client_clone.send_command(&cmd, None).await;
        let client = unsafe { Box::leak(Box::from_raw(ptr_address as *mut Client)) };
        let value = match result {
            Ok(value) => value,
            Err(_) => {
                unsafe { (client.failure_callback)(callback_index) }; // TODO - report errors,
                return;
            }
        };
        let result = Option::<CString>::from_owned_redis_value(value);

        unsafe {
            match result {
                Ok(None) => (client.success_callback)(callback_index, std::ptr::null()),
                Ok(Some(c_str)) => (client.success_callback)(callback_index, c_str.as_ptr()),
                Err(_) => (client.failure_callback)(callback_index), // TODO - report errors
            };
        }
    });
}

impl From<logger_core::Level> for Level {
    fn from(level: logger_core::Level) -> Self {
        match level {
            logger_core::Level::Error => Level::Error,
            logger_core::Level::Warn => Level::Warn,
            logger_core::Level::Info => Level::Info,
            logger_core::Level::Debug => Level::Debug,
            logger_core::Level::Trace => Level::Trace,
        }
    }
}

impl From<Level> for logger_core::Level {
    fn from(level: Level) -> logger_core::Level {
        match level {
            Level::Error => logger_core::Level::Error,
            Level::Warn => logger_core::Level::Warn,
            Level::Info => logger_core::Level::Info,
            Level::Debug => logger_core::Level::Debug,
            Level::Trace => logger_core::Level::Trace,
        }
    }
}

/// # Safety
///
/// * `message` must not be null.
/// * `message` must be able to be safely casted to a valid `CString` via `CString::from_raw`. See the safety documentation of [`std::ffi::CString::from_raw`](https://doc.rust-lang.org/std/ffi/struct.CString.html#method.from_raw).
#[no_mangle]
#[allow(improper_ctypes_definitions)]
pub unsafe extern "C" fn log(
    log_level: Level,
    log_identifier: *const c_char,
    message: *const c_char,
) {
    unsafe {
        logger_core::log(
            log_level.into(),
            CStr::from_ptr(log_identifier)
                .to_str()
                .expect("Can not read log_identifier argument."),
            CStr::from_ptr(message)
                .to_str()
                .expect("Can not read message argument."),
        );
    }
}

/// # Safety
///
/// * `file_name` must not be null.
/// * `file_name` must be able to be safely casted to a valid `CString` via `CString::from_raw`. See the safety documentation of [`std::ffi::CString::from_raw`](https://doc.rust-lang.org/std/ffi/struct.CString.html#method.from_raw).
#[no_mangle]
#[allow(improper_ctypes_definitions)]
pub unsafe extern "C" fn init(level: Option<Level>, file_name: *const c_char) -> Level {
    let file_name_as_str;
    unsafe {
        file_name_as_str = if file_name.is_null() {
            None
        } else {
            Some(
                CStr::from_ptr(file_name)
                    .to_str()
                    .expect("Can not read string argument."),
            )
        };

        let logger_level = logger_core::init(level.map(|level| level.into()), file_name_as_str);
        logger_level.into()
    }
}
