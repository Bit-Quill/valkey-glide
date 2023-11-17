use babushka::client::Client as BabushkaClient;
use babushka::connection_request;
use babushka::connection_request::AddressInfo;
use redis::{Cmd, FromRedisValue, RedisResult};
use std::{
    ffi::{c_void, CStr, CString},
    os::raw::c_char,
};
use tokio::runtime::Builder;
use tokio::runtime::Runtime;

pub type SuccessCallback = unsafe extern "C" fn(message: *const c_char, channel_address: usize) -> ();
pub type FailureCallback = unsafe extern "C" fn(err_message: *const c_char, channel_address: usize) -> ();



pub struct Connection {
    connection: BabushkaClient,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
    runtime: Runtime,
}

fn create_connection_request(
    host: String,
    port: u32,
    use_tls: bool,
    use_cluster_mode: bool,
) -> connection_request::ConnectionRequest {
    let mut address_info = AddressInfo::new();
    address_info.host = host.to_string().into();
    address_info.port = port;
    let addresses_info = vec![address_info];
    let mut connection_request = connection_request::ConnectionRequest::new();
    connection_request.addresses = addresses_info;
    connection_request.cluster_mode_enabled = use_cluster_mode;
    connection_request.tls_mode = if use_tls {
        connection_request::TlsMode::InsecureTls
    } else {
        connection_request::TlsMode::NoTls
    }
        .into();

    connection_request
}

fn create_connection_internal(
    host: *const c_char,
    port: u32,
    use_tls: bool,
    use_cluster_mode: bool,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> RedisResult<Connection> {
    let host_cstring = unsafe { CStr::from_ptr(host as *mut c_char) };
    let host_string = host_cstring.to_str()?.to_string();
    let request = create_connection_request(host_string, port, use_tls, use_cluster_mode);
    let runtime = Builder::new_multi_thread()
        .enable_all()
        .thread_name("Babushka go thread")
        .build()?;
    let _runtime_handle = runtime.enter();
    let connection = runtime.block_on(BabushkaClient::new(request)).unwrap();
    Ok(Connection {
        connection,
        success_callback,
        failure_callback,
        runtime,
    })
}

/// Creates a new connection to the given address. The success callback needs to copy the given string synchronously, since it will be dropped by Rust once the callback returns. All callbacks should be offloaded to separate threads in order not to exhaust the connection's thread pool.
#[no_mangle]
pub extern "C" fn create_connection(
    host: *const c_char,
    port: u32,
    use_tls: bool,
    use_cluster_mode: bool,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> *const c_void {
    match create_connection_internal(host, port, use_tls, use_cluster_mode, success_callback, failure_callback) {
        Err(_) => std::ptr::null(),
        Ok(connection) => Box::into_raw(Box::new(connection)) as *const c_void,
    }
}

#[no_mangle]
pub extern "C" fn close_connection(connection_ptr: *const c_void) {
    let connection_ptr = unsafe { Box::from_raw(connection_ptr as *mut Connection) };
    let _runtime_handle = connection_ptr.runtime.enter();
    drop(connection_ptr);
}

/// Expects that key and value will be kept valid until the callback is called.
#[no_mangle]
pub extern "C" fn set(
    connection_ptr: *const c_void,
    key: *const c_char,
    value: *const c_char,
    channel: usize
) {
    let connection = unsafe { Box::leak(Box::from_raw(connection_ptr as *mut Connection)) };
    // The safety of this needs to be ensured by the calling code. Cannot dispose of the pointer before all operations have completed.
    let ptr_address = connection_ptr as usize;

    let key_cstring = unsafe { CStr::from_ptr(key as *mut c_char) };
    let value_cstring = unsafe { CStr::from_ptr(value as *mut c_char) };
    let mut connection_clone = connection.connection.clone();
    connection.runtime.spawn(async move {
        let key_bytes = key_cstring.to_bytes();
        let value_bytes = value_cstring.to_bytes();
        let mut cmd = Cmd::new();
        cmd.arg("SET").arg(key_bytes).arg(value_bytes);
        let result = connection_clone.req_packed_command(&cmd, None).await;
        unsafe {
            let client = Box::leak(Box::from_raw(ptr_address as *mut Connection));
            match result {
                Ok(_) => (client.success_callback)(std::ptr::null(), channel),
                Err(err) => {
                    let c_err_str = CString::new(err.to_string()).expect("CString::new failed");
                    (client.failure_callback)(c_err_str.as_ptr(), channel)
                }
            };
        }
    });
}

/// Expects that key will be kept valid until the callback is called. If the callback is called with a string pointer, the pointer must
/// be used synchronously, because the string will be dropped after the callback.
#[no_mangle]
pub extern "C" fn get(connection_ptr: *const c_void, key: *const c_char, channel: usize) {
    let connection = unsafe { Box::leak(Box::from_raw(connection_ptr as *mut Connection)) };
    // The safety of this needs to be ensured by the calling code. Cannot dispose of the pointer before all operations have completed.
    let ptr_address = connection_ptr as usize;

    let key_cstring = unsafe { CStr::from_ptr(key as *mut c_char) };
    let mut connection_clone = connection.connection.clone();
    connection.runtime.spawn(async move {
        let key_bytes = key_cstring.to_bytes();
        let mut cmd = Cmd::new();
        cmd.arg("GET").arg(key_bytes);
        let result = connection_clone.req_packed_command(&cmd, None).await;
        let connection = unsafe { Box::leak(Box::from_raw(ptr_address as *mut Connection)) };
        let value = match result {
            Ok(value) => value,
            Err(err) => {
                let c_err_str = CString::new(err.to_string()).expect("CString::new failed");
                unsafe { (connection.failure_callback)(c_err_str.as_ptr(), channel) };
                return;
            }
        };
        let result = Option::<CString>::from_redis_value(&value);

        unsafe {
            match result {
                Ok(None) => (connection.success_callback)(std::ptr::null(), channel),
                Ok(Some(c_str)) => (connection.success_callback)(c_str.as_ptr(), channel),
                Err(err) =>{
                    let c_err_str = CString::new(err.to_string()).expect("CString::new failed");
                    (connection.failure_callback)(c_err_str.as_ptr(), channel)
                }
            };
        }
    });
}

/// Expects that key and value will be kept valid until the callback is called.
#[no_mangle]
pub extern "C" fn ping(
    connection_ptr: *const c_void,
    channel: usize
) {
    let connection = unsafe { Box::leak(Box::from_raw(connection_ptr as *mut Connection)) };
    // The safety of this needs to be ensured by the calling code. Cannot dispose of the pointer before all operations have completed.
    let ptr_address = connection_ptr as usize;

    let mut connection_clone = connection.connection.clone();
    connection.runtime.spawn(async move {
        let mut cmd = Cmd::new();
        cmd.arg("PING");
        let result = connection_clone.req_packed_command(&cmd, None).await;
        let connection = unsafe { Box::leak(Box::from_raw(ptr_address as *mut Connection)) };
        let value = match result {
            Ok(value) => value,
            Err(err) => {
                let c_err_str = CString::new(err.to_string()).expect("CString::new failed");
                unsafe { (connection.failure_callback)(c_err_str.as_ptr(), channel) };
                return;
            }
        };
        let result = Option::<CString>::from_redis_value(&value);
        unsafe {
            match result {
                Ok(None) => (connection.success_callback)(std::ptr::null(), channel),
                Ok(Some(c_str)) => (connection.success_callback)(c_str.as_ptr(), channel),
                Err(err) => {
                    let c_err_str = CString::new(err.to_string()).expect("CString::new failed");
                    (connection.failure_callback)(c_err_str.as_ptr(), channel)
                }
            };
        }
    });
}

#[no_mangle]
pub extern "C" fn info(
    connection_ptr: *const c_void,
    channel: usize
) {
    let connection = unsafe { Box::leak(Box::from_raw(connection_ptr as *mut Connection)) };
    // The safety of this needs to be ensured by the calling code. Cannot dispose of the pointer before all operations have completed.
    let ptr_address = connection_ptr as usize;

    let mut connection_clone = connection.connection.clone();
    connection.runtime.spawn(async move {
        let mut cmd = Cmd::new();
        cmd.arg("INFO");
        let result = connection_clone.req_packed_command(&cmd, None).await;
        let connection = unsafe { Box::leak(Box::from_raw(ptr_address as *mut Connection)) };
        let value = match result {
            Ok(value) => value,
            Err(err) => {
                let c_err_str = CString::new(err.to_string()).expect("CString::new failed");
                unsafe { (connection.failure_callback)(c_err_str.as_ptr(), channel) };
                return;
            }
        };
        let result = Option::<CString>::from_redis_value(&value);
        unsafe {
            match result {
                Ok(None) => (connection.success_callback)(std::ptr::null(), channel),
                Ok(Some(c_str)) => (connection.success_callback)(c_str.as_ptr(), channel),
                Err(err) => {
                    let c_err_str = CString::new(err.to_string()).expect("CString::new failed");
                    (connection.failure_callback)(c_err_str.as_ptr(), channel)
                }
            };
        }
    });
}

