use glide_core::client::Client as GlideClient;
/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */
use glide_core::connection_request;
use protobuf::Message;
use redis::RedisResult;
use std::{
    ffi::{c_void, CString},
    os::raw::c_char,
};
use tokio::runtime::Builder;
use tokio::runtime::Runtime;

pub type SuccessCallback =
    unsafe extern "C" fn(channel_address: usize, message: *const c_char) -> ();
pub type FailureCallback =
    unsafe extern "C" fn(channel_address: usize, err_message: *const c_char) -> ();

pub enum Level {
    Error = 0,
    Warn = 1,
    Info = 2,
    Debug = 3,
    Trace = 4,
}

#[repr(C)]
pub struct ConnectionResponse {
    conn_ptr: *const c_void,
    error: *const RedisErrorFFI,
}

#[repr(C)]
pub struct RedisErrorFFI {
    message: *const c_char,
    error_type: ErrorType,
}

#[repr(u32)]
pub enum ErrorType {
    ClosingError = 0,
    RequestError = 1,
    TimeoutError = 2,
    ExecAbortError = 3,
    ConnectionError = 4,
}

pub struct Client {
    client: GlideClient,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback, // TODO - add specific error codes
    runtime: Runtime,
}

fn create_client_internal(
    connection_request_bytes: &[u8],
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> RedisResult<Client> {
    let request =
        connection_request::ConnectionRequest::parse_from_bytes(connection_request_bytes).unwrap();
    let runtime = Builder::new_multi_thread()
        .enable_all()
        .thread_name("GLIDE for Redis Go thread")
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

/// Creates a new client to the given address. The success callback needs to copy the given string synchronously, since it will be dropped by Rust once the callback returns. All callbacks should be offloaded to separate threads in order not to exhaust the client's thread pool.
#[no_mangle]
pub unsafe extern "C" fn create_client(
    connection_request: *const u8,
    connection_request_len: usize,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> *const ConnectionResponse {
    let request_bytes =
        unsafe { std::slice::from_raw_parts(connection_request, connection_request_len) };
    let response = match create_client_internal(request_bytes, success_callback, failure_callback) {
        Err(err) => {
            let message_cstring = CString::new(err.to_string()).unwrap();
            ConnectionResponse {
                conn_ptr: std::ptr::null(),
                error: &RedisErrorFFI {
                    message: message_cstring.as_ptr(),
                    error_type: ErrorType::ConnectionError,
                },
            }
        }
        Ok(client) => ConnectionResponse {
            conn_ptr: Box::into_raw(Box::new(client)) as *const c_void,
            error: std::ptr::null(),
        },
    };
    Box::into_raw(Box::new(response))
}

#[no_mangle]
pub extern "C" fn close_client(client_ptr: *const c_void) {
    let client_ptr = unsafe { Box::from_raw(client_ptr as *mut Client) };
    let _runtime_handle = client_ptr.runtime.enter();
    drop(client_ptr);
}
