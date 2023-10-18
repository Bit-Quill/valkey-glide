use babushka::start_socket_listener;

use jni::objects::{JClass, JObject};
use jni::JNIEnv;
use jni::sys::jlong;
use std::sync::mpsc;

use redis::Value;

fn redis_value_to_java<'local>(mut env: JNIEnv<'local>, val: Value) -> JObject<'local> {
    match val {
        Value::Nil => JObject::null(),
        Value::Status(str) => JObject::from(env.new_string(str).unwrap()),
        Value::Okay => JObject::from(env.new_string("OK").unwrap()),
        Value::Int(num) => env.new_object("java/lang/Integer", "(I)Ljava/lang/Integer;", &[num.into()]).unwrap(),
        Value::Data(data) => match std::str::from_utf8(data.as_ref()) {
            Ok(val) => JObject::from(env.new_string(val).unwrap()),
            Err(_err) => {
                let _ = env.throw("Error decoding Unicode data");
                JObject::null()
            },
        },
        Value::Bulk(_bulk) => {
            let _ = env.throw("Not implemented");
            JObject::null()
            /*
            let elements: &PyList = PyList::new(
                py,
                bulk.into_iter()
                    .map(|item| redis_value_to_py(py, item).unwrap()),
            );
            Ok(elements.into_py(py))
            */
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_javababushka_client_RedisClient_valueFromPointer<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    pointer: jlong
) -> JObject<'local> {
    let value = unsafe { Box::from_raw(pointer as *mut Value) };
    redis_value_to_java(env, *value)
}

#[no_mangle]
pub extern "system" fn Java_javababushka_client_RedisClient_startSocketListenerExternal<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    callback: JObject<'local>
) {
    let jvm = env.get_java_vm().unwrap();

    let callback = env.new_global_ref(callback).unwrap();

    let (tx, rx) = mpsc::channel();
    start_socket_listener(move |socket_path| {
        // Signals that thread has started
        tx.send(()).unwrap();
        let mut env = jvm.attach_current_thread().unwrap();
        match socket_path {
            Ok(path) => {
                let path = env.new_string(path).unwrap();
                let _ = env.call_method(callback, "initCallback", "(Ljava/lang/String;Ljava/lang/String;)V", &[(&JObject::from(path)).into(), (&JObject::null()).into()]);
            },
            Err(error_message) => {
                let error_message = env.new_string(error_message).unwrap();
                let _ = env.call_method(callback, "initCallback", "(Ljava/lang/String;Ljava/lang/String;)V", &[(&JObject::null()).into(), (&JObject::from(error_message)).into()]);
            }
        }
    });
    // Wait until the thread has started
    rx.recv().unwrap();
}
