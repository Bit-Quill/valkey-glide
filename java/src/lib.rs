use jni::objects::{JClass, JObject, JStaticFieldID, JString};
use jni::JNIEnv;
use jni::sys::{jclass, jfieldID, jint, jlong, jstring};
//use log::error;
use log::{debug, error};

use redis::{AsyncCommands, Client, RedisResult, Value};
use redis::aio::MultiplexedConnection;
use tokio::runtime::{Builder, Runtime};
use std::os::raw::c_char;
use std::ffi::{CStr,CString};
use std::str;
use std::mem;
use std::ptr::null;
use std::sync::{Mutex};



#[no_mangle]
pub extern fn static_function_which_throws() -> BabushkaResultStr {
    BabushkaResultStr { error: str_to_ptr(""), result: str_to_ptr("hello from rust -> static function which throws") }
}

#[no_mangle]
pub extern fn static_function4() -> BabushkaResultStr {
    BabushkaResultStr { error: str_to_ptr("pewpew"), result: null() }
}

#[no_mangle]
pub extern fn static_function2_0() -> BabushkaResult {
    //BabushkaResult::from_str(str_to_ptr("ololo"))
    BabushkaResult {
        error: null(),
        value_type: ResultType::Str as u32,
        str: str_to_ptr("ololo"),
        num: 0
    }
}

#[no_mangle]
pub extern fn static_function2_1() -> BabushkaResult {
    //BabushkaResult::from_err(str_to_ptr("oh no"))
    BabushkaResult {
        error: str_to_ptr("oh no"),
        value_type: ResultType::Err as u32,
        str: null(),
        num: 0
    }
}

#[no_mangle]
pub extern fn static_function2_2() -> BabushkaResult {
    //BabushkaResult::from_int(100500)
    BabushkaResult {
        error: null(),
        value_type: ResultType::Int as u32,
        str: null(),
        num: 100500
    }
}

#[no_mangle]
pub extern fn static_function2_3() -> BabushkaResult {
    //BabushkaResult::from_ok()
    BabushkaResult {
        error: null(),
        value_type: ResultType::Ok as u32,
        str: null(),
        num: 0
    }
}

#[no_mangle]
pub extern fn static_function2_4() -> BabushkaResult {
    BabushkaResult {
        error: null(),
        value_type: ResultType::Nil as u32,
        str: null(),
        num: 0
    }
}

/// Convert a native string to a Rust string
fn ptr_to_string(pointer: * const c_char) -> String {
    let slice = unsafe { CStr::from_ptr(pointer).to_bytes() };
    str::from_utf8(slice).unwrap().to_string()
}

/// Convert a native string to a Rust string
fn ptr_to_str(pointer: * const c_char) -> &'static str {
    let slice = unsafe { CStr::from_ptr(pointer).to_bytes() };
    str::from_utf8(slice).unwrap()
}

/// Convert a Rust string to a native string
fn string_to_ptr(string: String) -> * const c_char {
    let cs = CString::new(string.as_bytes()).unwrap();
    let ptr = cs.as_ptr();
    // Tell Rust not to clean up the string while we still have a pointer to it.
    // Otherwise, we'll get a segfault.
    mem::forget(cs);
    ptr
}

/// Convert a Rust string to a native string
fn str_to_ptr(string: &str) -> * const c_char {
    let cs = CString::new(string.as_bytes()).unwrap();
    let ptr = cs.as_ptr();
    // Tell Rust not to clean up the string while we still have a pointer to it.
    // Otherwise, we'll get a segfault.
    mem::forget(cs);
    ptr
}

#[repr(C)]
pub struct BabushkaResultStr {
    pub error: * const c_char,
    pub result: * const c_char
}

use num_derive::FromPrimitive;
use num_traits::FromPrimitive;

// redis::Value
#[derive(FromPrimitive)]
pub enum ResultType {
    Str = 0, // * const c_char
    Int = 1, // i64
    Nil = 2,
    Data = 3, // Vec<u8>
    Bulk = 4, // Vec<Value>
    Ok = 5,
    Err = 6
}

fn BabushkaResult_from_error<'local>(env: &mut JNIEnv<'local>, error: String) -> JObject<'local> {
    let res = env.new_object(
        "javababushka/benchmarks/babushka/Jni$BabushkaResult",
        "(Ljava/lang/String;ILjava/lang/String;J)V",
        &[
            (&env.new_string(error.clone()).unwrap()).into(),
            jint::from(ResultType::Err as i32).into(),
            (&JObject::null()).into(),
            0i64.into(),
        ]);//.unwrap()
    match res {
        Ok(res) => return res,
        Err(err) => error!("Failed to create `BabushkaResult` from error {}: {}", error, err)
    }
    JObject::null()
}

fn BabushkaResult_from_ok<'local>(env: &mut JNIEnv<'local>) -> JObject<'local> {
    let res = env.new_object(
        "javababushka/benchmarks/babushka/Jni$BabushkaResult",
        "(Ljava/lang/String;ILjava/lang/String;J)V",
        &[
            (&JObject::null()).into(),
            jint::from(ResultType::Ok as i32).into(),
            (&JObject::null()).into(),
            0i64.into(),
        ]);//.unwrap()
    match res {
        Ok(res) => return res,
        Err(err) => error!("Failed to create `BabushkaResult` from ok: {}", err)
    }
    JObject::null()
}

fn BabushkaResult_from_string<'local>(env: &mut JNIEnv<'local>, string: String) -> JObject<'local> {
    let res = env.new_object(
        "javababushka/benchmarks/babushka/Jni$BabushkaResult",
        "(Ljava/lang/String;ILjava/lang/String;J)V",
        &[
            (&JObject::null()).into(),
            jint::from(ResultType::Str as i32).into(),
            (&env.new_string(string.clone()).unwrap()).into(),
            0i64.into(),
        ]);//.unwrap()
    match res {
        Ok(res) => return res,
        Err(err) => error!("Failed to create `BabushkaResult` from string {}: {}", string, err)
    }
    JObject::null()
}

fn BabushkaResult_from_empty_string<'local>(env: &mut JNIEnv<'local>) -> JObject<'local> {
    let res = env.new_object(
        "javababushka/benchmarks/babushka/Jni$BabushkaResult",
        "(Ljava/lang/String;ILjava/lang/String;J)V",
        &[
            (&JObject::null()).into(),
            jint::from(ResultType::Str as i32).into(),
            (&JObject::null()).into(),
            0i64.into(),
        ]);//.unwrap()
    match res {
        Ok(res) => return res,
        Err(err) => error!("Failed to create `BabushkaResult` from empty string: {}", err)
    }
    JObject::null()
}

#[repr(C)]
pub struct BabushkaResult {
    pub error: * const c_char,
    pub value_type: u32, //ResultType,
    pub str: * const c_char,
    pub num: i64,
}

impl BabushkaResult {
    pub fn from_str(str: String) -> Self {
        Self {
            error: null(),
            value_type: ResultType::Str as u32,
            str: string_to_ptr(str),
            num: 0,
        }
    }

    pub fn from_empty_str() -> Self {
        Self {
            error: null(),
            value_type: ResultType::Str as u32,
            str: null(),
            num: 0,
        }
    }

    pub fn from_int(int: i64) -> Self {
        Self {
            error: null(),
            value_type: ResultType::Int as u32,
            str: null(),
            num: int,
        }
    }

    pub fn from_err(err: String) -> Self {
        Self {
            error: string_to_ptr(err),
            value_type: ResultType::Err as u32,
            str: null(),
            num: 0,
        }
    }

    pub fn from_nil() -> Self {
        Self {
            error: null(),
            value_type: ResultType::Nil as u32,
            str: null(),
            num: 0,
        }
    }

    pub fn from_ok() -> Self {
        Self {
            error: null(),
            value_type: ResultType::Ok as u32,
            str: null(),
            num: 0,
        }
    }

    pub fn get_type(&self) -> ResultType {
        // unsafe { std::mem::transmute(self.value_type as u32) };
        ResultType::from_u32(self.value_type).unwrap()
    }

    pub fn get_err(&self) -> String {
        ptr_to_string(self.error)
    }

    pub fn get_str(&self) -> String {
        ptr_to_string(self.str)
    }

    pub fn get_int(&self) -> i64 {
        self.num
    }
    // TODO other types
}


#[repr(C)]
pub struct BabushkaClient {
    runtime: Option<Mutex<Runtime>>,
    connection: Option<Mutex<MultiplexedConnection>>,
    data: i32
}

#[link_section = ".init_array"]
pub static INITIALIZE: extern "C" fn() = rust_ctor;

#[no_mangle]
pub extern "C" fn rust_ctor() {
    env_logger::init();
}

#[no_mangle]
pub extern "system" fn Java_javababushka_benchmarks_babushka_Jni_init_1client<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    data: jint
) -> jlong {
    let p = Box::<BabushkaClient>::into_raw(Box::new(
        BabushkaClient {
            runtime: None,
            connection: None,
            data
        }));
    jlong::from(p as i64)
}

#[no_mangle]
pub extern fn init_client0(data: i32) -> u64 {
    let p = Box::<BabushkaClient>::into_raw(Box::new(
    BabushkaClient {
        runtime: None,
        connection: None,
        data
    }));
    p as u64
    //p.into();
}

#[no_mangle]
pub extern "system" fn Java_javababushka_benchmarks_babushka_Jni_test<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    pointer: jlong,
    data: jstring
) -> JObject<'local> {

    let mut babushka = unsafe { Box::from_raw(pointer as *mut BabushkaClient) };
    babushka.data += 15;

    let err = JObject::null();
    let res_type = jint::from(ResultType::Ok as i32);
    let str = env.new_string(format!("{} {:?}", babushka.data, data)).unwrap();
    let num = Box::<BabushkaClient>::into_raw(babushka) as i64;

    let res = env.new_object(
        "javababushka/benchmarks/babushka/Jni$BabushkaResult",
        "(Ljava/lang/String;ILjava/lang/String;J)V",
        &[
            (&err).into(),
            (res_type).into(),
            (&str).into(),
            (num).into(),
        ]);//.unwrap()
    match res {
        Ok(res) => return res,
        Err(err) => error!("{}", err)
    }
    JObject::null()
}


#[no_mangle]
pub extern "system" fn Java_javababushka_benchmarks_babushka_Jni_connect<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    pointer: jlong,
    address: JString
) -> JObject<'local> {
    let addr : String = env.get_string(&address).unwrap().into();

    let mut babushka = unsafe { Box::from_raw(pointer as *mut BabushkaClient) };
    let client_res = redis::Client::open(addr);

    let client : Client;

    match client_res {
        Ok(c) => client = c,
        Err(err) => return BabushkaResult_from_error(&mut env, err.to_string())
    }

    let runtime_res =
        Builder::new_multi_thread()
            .enable_all()
            .thread_name("Babushka java thread")
            .build();

    let runtime : Runtime;

    match runtime_res {
        Ok(rt) => runtime = rt,
        Err(err) => return BabushkaResult_from_error(&mut env, err.to_string())
    }

    let _runtime_handle = runtime.enter();

    let connection_res = runtime
        .block_on(client.get_multiplexed_async_connection());

    let connection : MultiplexedConnection;

    match connection_res {
        Ok(c) => connection = c,
        Err(err) => return BabushkaResult_from_error(&mut env, err.to_string())
    }

    babushka.runtime = Some(Mutex::new(runtime));
    babushka.connection = Some(Mutex::new(connection));

    Box::<BabushkaClient>::into_raw(babushka);
    BabushkaResult_from_ok(&mut env)
}


#[no_mangle]
pub extern "system" fn Java_javababushka_benchmarks_babushka_Jni_set<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    pointer: jlong,
    key: jstring,
    value: jstring
) -> JObject<'local> {
    let key_str : String = env.get_string(&unsafe { JString::from_raw(key) }).unwrap().into();
    let value_str : String = env.get_string(&unsafe { JString::from_raw(value) }).unwrap().into();

    let mut babushka = unsafe { Box::from_raw(pointer as *mut BabushkaClient) };
    let res = babushka.runtime.as_mut().unwrap().lock().unwrap().block_on(async {
        babushka.connection.as_mut().unwrap().lock().unwrap()
            .set::<String, String, ()>(key_str, value_str).await
    });
    Box::<BabushkaClient>::into_raw(babushka);
    match res {
        Ok(_) => BabushkaResult_from_ok(&mut env),
        Err(err) => BabushkaResult_from_error(&mut env, err.to_string())
    }
}

#[no_mangle]
pub extern "system" fn Java_javababushka_benchmarks_babushka_Jni_get<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    pointer: jlong,
    key: jstring
) -> JObject<'local> {
    let key_str : String = env.get_string(&unsafe { JString::from_raw(key) }).unwrap().into();

    let mut babushka = unsafe { Box::from_raw(pointer as *mut BabushkaClient) };

    let res = babushka.runtime.as_mut().unwrap().lock().unwrap().block_on(async {
        babushka.connection.as_mut().unwrap().lock().unwrap().get::<String, Option<String>>(key_str).await
    });
    Box::<BabushkaClient>::into_raw(babushka);
    match res {
        Ok(Some(val)) => BabushkaResult_from_string(&mut env, val),
        Ok(None) => BabushkaResult_from_empty_string(&mut env),
        Err(err) => BabushkaResult_from_error(&mut env, err.to_string())
    }
}

#[no_mangle]
pub extern fn test0(ptr: u64, address: * const c_char) -> BabushkaResult {
    let mut babushka = unsafe { Box::from_raw(ptr as *mut BabushkaClient) };
    babushka.data += 15;
    BabushkaResult {
        error: null(),
        value_type: ResultType::Ok as u32,
        str: string_to_ptr(format!("{} {}", babushka.data, ptr_to_string(address))),
        num: Box::<BabushkaClient>::into_raw(babushka) as i64,
    }
}

//*
#[no_mangle]
pub extern fn connect0(ptr: u64, address: * const c_char) -> BabushkaResult {
    let mut babushka = unsafe { Box::from_raw(ptr as *mut BabushkaClient) };
    let client_res = redis::Client::open(ptr_to_string(address));
    let client : Client;

    match client_res {
        Ok(c) => client = c,
        Err(err) => return BabushkaResult::from_err(err.to_string())
    }

    let runtime_res =
        Builder::new_multi_thread()
            .enable_all()
            .thread_name("Babushka java thread")
            .build();

    let runtime : Runtime;

    match runtime_res {
        Ok(rt) => runtime = rt,
        Err(err) => return BabushkaResult::from_err(err.to_string())
    }

    let _runtime_handle = runtime.enter();

    let connection_res = runtime
        .block_on(client.get_multiplexed_async_connection());

    let connection : MultiplexedConnection;

    match connection_res {
        Ok(c) => connection = c,
        Err(err) => return BabushkaResult::from_err(err.to_string())
    }

    babushka.runtime = Some(Mutex::new(runtime));
    babushka.connection = Some(Mutex::new(connection));

    Box::<BabushkaClient>::into_raw(babushka);
    BabushkaResult::from_ok()
}

#[no_mangle]
pub extern fn set0(ptr: u64, key: * const c_char, value: * const c_char) -> BabushkaResult {
    let mut babushka = unsafe { Box::from_raw(ptr as *mut BabushkaClient) };
    let res = babushka.runtime.as_mut().unwrap().lock().unwrap().block_on(async {
    babushka.connection.as_mut().unwrap().lock().unwrap()
        .set::<String, String, ()>(ptr_to_string(key), ptr_to_string(value)).await
    });
    Box::<BabushkaClient>::into_raw(babushka);
    match res {
        Ok(_) => BabushkaResult::from_ok(),
        Err(err) => BabushkaResult::from_err(err.to_string())
    }
}

#[no_mangle]
pub extern fn get0(ptr: u64, key: * const c_char) -> BabushkaResult {
    let mut babushka = unsafe { Box::from_raw(ptr as *mut BabushkaClient) };
    let res = babushka.runtime.as_mut().unwrap().lock().unwrap().block_on(async {
        babushka.connection.as_mut().unwrap().lock().unwrap().get::<String, Option<String>>(ptr_to_string(key)).await
    });
    Box::<BabushkaClient>::into_raw(babushka);
    match res {
        Ok(Some(val)) => BabushkaResult::from_str(val),
        Ok(None) => BabushkaResult::from_empty_str(),
        Err(err) => BabushkaResult::from_err(err.to_string())
    }
}
