use jni::objects::{JClass, JObject, JStaticFieldID, JString};
use jni::JNIEnv;
use jni::sys::{jclass, jfieldID, jint, jlong, jstring};
use std::sync::{mpsc, Mutex};
//use log::error;
use log::{debug, error};

use redis::{AsyncCommands, Client, RedisResult, Value};
use redis::aio::MultiplexedConnection;
use tokio::runtime::{Builder, Runtime};


// redis::Value
//#[derive(FromPrimitive)]
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
pub struct BabushkaClient {
    runtime: Option<Mutex<Runtime>>,
    connection: Option<Mutex<MultiplexedConnection>>,
    //runtime: Option<Mutex<BabushkaValue>>,
    //connection: Option<Mutex<BabushkaValue>>,
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
pub extern "system" fn Java_javababushka_benchmarks_babushka_Jni_test<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    pointer: jlong,
    data: jstring
) -> JObject<'local> {

    let mut babushka = unsafe { Box::from_raw(pointer as *mut BabushkaClient) };
    babushka.data += 15;

/*
    // ENUM:
    let clSTATUS: JClass   = env.find_class("javababushka/benchmarks/babushka/Jni$ResultType")?;
    let fidONE: JStaticFieldID   = env.get_static_field_id(clSTATUS , "ONE", "LMyClass$STATUS;")?;
    let STATUS_ONE = env.get_static_field(clSTATUS, fidONE)?;
*/
    let err = JObject::null();
    //let res_type = env.new_object("java/lang/Integer", "(I)V", &[(ResultType::Ok as i32).into()]).unwrap();
    let res_type = jint::from(ResultType::Ok as i32);
    let str = env.new_string(format!("{} {:?}", babushka.data, data)).unwrap();
    //let num = env.new_object("java/lang/Long", "(J)V", &[(Box::<BabushkaClient>::into_raw(babushka) as i64).into()]).unwrap();
    let num = Box::<BabushkaClient>::into_raw(babushka) as i64;

    let res = env.new_object(
        "javababushka/benchmarks/babushka/Jni$BabushkaResult",
        "(Ljava/lang/String;ILjava/lang/String;J)V",
        &[
            (&err).into(),
            //(&res_type).into(),
            (res_type).into(),
            (&str).into(),
            //(&num).into(),
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
    //address: jstring
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
