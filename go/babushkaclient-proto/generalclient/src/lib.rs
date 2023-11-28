use babushka::client::{Client as BabushkaClient, Client};
use babushka::connection_request::{ConnectionRequest};
use babushka::redis_request::redis_request::Command as EnumCommand;
use babushka::redis_request::{command, Command, RedisRequest, RequestType};
use protobuf::Message;
use redis::{cmd, Value};
use redis::{Cmd, FromRedisValue, RedisResult, RedisError};
use std::ptr::null;
use std::{
    ffi::{c_void, CString},
    os::raw::c_char,
};
use tokio::runtime::Builder;
use tokio::runtime::Runtime;
pub type SuccessCallback =
    unsafe extern "C" fn(message: *const c_char, channel_address: usize) -> ();
pub type FailureCallback =
    unsafe extern "C" fn(err_message: *const c_char, channel_address: usize) -> ();

pub struct Connection {
    connection: BabushkaClient,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
    runtime: Runtime,
}
//TODO error handling needed for unwraps
fn create_connection_internal(
    ptr_to_connection_request: *const u8,
    size_of_connection_request: usize,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> RedisResult<Connection> {
    let request = get_proto_request::<ConnectionRequest>(
        ptr_to_connection_request,
        size_of_connection_request,
    );
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
pub extern "C" fn create_connection_proto(
    ptr_to_connection_request: *const u8,
    size_of_connection_request: usize,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> *const c_void {
    println!("In Create Connection for Proto");
    match create_connection_internal(
        ptr_to_connection_request,
        size_of_connection_request,
        success_callback,
        failure_callback,
    ) {
        Err(_) => std::ptr::null(),
        Ok(connection) => Box::into_raw(Box::new(connection)) as *const c_void,
    }
}

#[no_mangle]
pub extern "C" fn close_connection_proto(connection_ptr: *const c_void) {
    let connection_ptr = unsafe { Box::from_raw(connection_ptr as *mut Connection) };
    let _runtime_handle = connection_ptr.runtime.enter();
    drop(connection_ptr);
}

#[no_mangle]
pub extern "C" fn execute_command_proto(
    connection_ptr: *const c_void,
    ptr_to_connection_request: *const u8,
    size_of_connection_request: usize,
    channel: usize
) {
    let connection = unsafe { Box::leak(Box::from_raw(connection_ptr as *mut Connection)) };
    // The safety of this needs to be ensured by the calling code. Cannot dispose of the pointer before all operations have completed.
    let ptr_address = connection_ptr as usize;
    let mut connection_clone = connection.connection.clone();
    let proto_request = get_proto_request::<RedisRequest>(ptr_to_connection_request, size_of_connection_request);
    connection.runtime.spawn(async move {
        let get_command_from_proto = build_command_from_proto(&proto_request, &mut connection_clone).await;
        let connection = unsafe { Box::leak(Box::from_raw(ptr_address as *mut Connection)) };
        let value = match get_command_from_proto {
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

fn get_proto_request<T: Message>(
    ptr_to_connection_request: *const u8,
    size_of_connection_request: usize,
) -> T {
    let bytes = unsafe {
        std::slice::from_raw_parts(ptr_to_connection_request, size_of_connection_request)
    };
    T::parse_from_bytes(bytes).unwrap()
}
async fn build_command_from_proto(proto_request: &RedisRequest, connection_clone: &mut Client) -> RedisResult<Value> {
    match &proto_request.command {
        Some(action) => match action {
            EnumCommand::SingleCommand(command) => {
                match get_redis_command(&command) {
                    Ok(cmd) => {
                        connection_clone.req_packed_command(&cmd, None).await
                    }
                    Err(_) => panic!("Error in proto request command"),
                }
            }
            EnumCommand::Transaction(transaction) => {
                panic!("Transaction commands not yet supported")
            }
            _ => todo!()
        },
        None => {
            panic!("Error in proto request command")
        }
    }
}

fn get_redis_command(command: &Command) -> Result<Cmd, &str> {
    let Some(mut cmd) = get_command(command) else {
        todo!()
    };

    match &command.args {
        Some(command::Args::ArgsArray(args_vec)) => {
            for arg in args_vec.args.iter() {
                cmd.arg(arg.as_bytes());
            }
        }
        Some(command::Args::ArgsVecPointer(pointer)) => {
            let res = *unsafe { Box::from_raw(*pointer as *mut Vec<String>) };
            for arg in res {
                cmd.arg(arg.as_bytes());
            }
        }
        Some(_) => {
            todo!()
        }
        None => {
            todo!()
        }
    };

    Ok(cmd)
}

fn get_command(request: &Command) -> Option<Cmd> {
    let request_enum = request
        .request_type
        .enum_value_or(RequestType::InvalidRequest);
    match request_enum {
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
    }
}
fn get_two_word_command(first: &str, second: &str) -> Cmd {
    let mut cmd = cmd(first);
    cmd.arg(second);
    cmd
}
