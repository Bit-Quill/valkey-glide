use std::os::raw::c_char;
use std::ffi::{CStr,CString};
use std::str;
use std::mem;
use std::ptr::null;

/*
use std::sync::{Mutex};
use redis::aio::MultiplexedConnection;
use redis::{Client, ErrorKind, FromRedisValue, RedisResult, Value};
use redis::{AsyncCommands, RedisError};
use tokio::runtime::Builder;
use tokio::runtime::Runtime;
*/
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
        value_type: ResultType::Str as u8,
        //*
        value: BabushkaValue {
            str: str_to_ptr("ololo"),
            num: 0
        }
        // */
    }
}

#[no_mangle]
pub extern fn static_function2_1() -> BabushkaResult {
    //BabushkaResult::from_err(str_to_ptr("oh no"))
    BabushkaResult {
        error: str_to_ptr("oh no"),
        value_type: ResultType::Err as u8,
        value: Default::default()
    }
}

#[no_mangle]
pub extern fn static_function2_2() -> BabushkaResult {
    //BabushkaResult::from_int(100500)
    BabushkaResult {
        error: null(),
        value_type: ResultType::Int as u8,
        //*
        value: BabushkaValue {
            str: null(),
            num: 100500
        }
        // */
    }
}

#[no_mangle]
pub extern fn static_function2_3() -> BabushkaResult {
    //BabushkaResult::from_ok()
    BabushkaResult {
        error: null(),
        value_type: ResultType::Ok as u8,
        value: Default::default()
    }
}

#[no_mangle]
pub extern fn static_function2_4() -> BabushkaResult {
    BabushkaResult {
        error: null(),
        value_type: ResultType::Nil as u8,
        value: Default::default()
    }
}

/// Convert a native string to a Rust string
fn to_string(pointer: * const c_char) -> String {
    let slice = unsafe { CStr::from_ptr(pointer).to_bytes() };
    str::from_utf8(slice).unwrap().to_string()
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

// redis::Value
#[repr(C)]
pub enum ResultType {
    Str = 0, // * const c_char
    Int = 1, // i64
    Nil = 2,
    Data = 3, // Vec<u8>
    Bulk = 4, // Vec<Value>
    Ok = 5,
    Err = 6
}

#[repr(C)]
pub struct BabushkaResult {
    pub error: * const c_char,
    pub value_type: u8, //ResultType,
    value: BabushkaValue,
}

//impl BabushkaResult {
/*
    pub fn from_str(str: * const c_char) -> Self {
        Self {
            error: null(),
            value_type: ResultType::Str,
            value: BabushkaValue {
                str: str,
                num: 0,
//                data: Vec::new(),
//                bulk: Vec::new(),
            }
        }
    }

    pub fn from_int(int: i64) -> Self {
        Self {
            error: null(),
            value_type: ResultType::Int,
            value: BabushkaValue {
                str: null(),
                num: int,
//                data: Vec::new(),
//                bulk: Vec::new(),
            }
        }
    }

    pub fn from_data(data: Vec<u8>) -> Self {
        Self {
            error: null(),
            value_type: ResultType::Data,
            value: BabushkaValue {
                str: null(),
                num: 0,
//                data: data,
//                bulk: Vec::new(),
            }
        }
    }

    pub fn from_bulk(bulk: Vec<BabushkaValue>) -> Self {
        Self {
            error: null(),
            value_type: ResultType::Bulk,
            value: BabushkaValue {
                str: null(),
                num: 0,
//                data: Vec::new(),
//                bulk: bulk,
            }
        }
    }

    pub fn from_err(err: * const c_char) -> Self {
        Self {
            error: err,
            value_type: ResultType::Err,
            value: BabushkaValue::default()
        }
    }

    pub fn from_nil() -> Self {
        Self {
            error: null(),
            value_type: ResultType::Nil,
            value: BabushkaValue::default()
        }
    }

    pub fn from_ok() -> Self {
        Self {
            error: null(),
            value_type: ResultType::Ok,
            value: BabushkaValue::default()
        }
    }
*/

/*
    #[no_mangle]
    pub extern fn get_str(&self) -> * const c_char {
        self.value.str
    }

    #[no_mangle]
    pub extern fn get_int(&self) -> i64 {
        self.value.num
    }
*/
    // TODO other types
//}

#[repr(C)]
pub struct BabushkaValue {
    str: * const c_char,
    num: i64,
//    data: Vec<u8>,
//    bulk: Vec<BabushkaValue>,
}

impl Default for BabushkaValue {
    fn default() -> Self {
        Self {
            str: null(),
            num: 0,
//            data: Vec::default(),
//            bulk: Vec::default()
        }
    }
}


/*
#[repr(C)]
pub struct BabushkaClient {
    runtime: Option<Mutex<Runtime>>,
    connection: Option<Mutex<MultiplexedConnection>>,
}

impl BabushkaClient {
    /*
    fn new() -> Self {
        Self {
            runtime: None,
            connection: None
        }
    }*/

    #[no_mangle]
    pub extern fn class_function(&self) -> String {
        "hello from rust -> class function".into()
    }

    #[no_mangle]
    pub extern fn connect(&mut self, address: String) -> Result<(), String> {
        //self.client = Some(redis::Client::open(address)?);
        let client_res = redis::Client::open(address);
        let client : Client;

        match client_res {
            Ok(c) => client = c,
            Err(err) => return Err(err.to_string())
        }

        let runtime_res =
            Builder::new_multi_thread()
                .enable_all()
                .thread_name("Babushka java thread")
                .build();

        let runtime : Runtime;

        match runtime_res {
            Ok(rt) => runtime = rt,
            Err(err) => return Err(err.to_string())
        }

        let _runtime_handle = runtime.enter();

        let connection_res = runtime
            .block_on(client.get_multiplexed_async_connection());

        let connection : MultiplexedConnection;

        match connection_res {
            Ok(c) => connection = c,
            Err(err) => return Err(err.to_string())
        }

        self.runtime = Some(Mutex::new(runtime));
        self.connection = Some(Mutex::new(connection));

        Ok(())
    }

    /*
        pub fn disconnect(&mut self) -> Result<(), RedisError> {

        }
    */
    // TODO support any type for value
    #[no_mangle]
    pub extern fn set(&mut self, key: String, value: String) -> Result<(), String> {
        //self.runtime.spawn(async move {
        self.connection.as_mut().unwrap().lock().unwrap()
            //.set/*::<String, String, RV>*/(key, value);
            .set::<String, String, ()>(key, value); // TODO try RedisValue
        //});
        Ok(())
    }

    #[no_mangle]
    pub extern fn set2(&mut self, key: String, value: Option<String>) -> Result<(), String> {
        //self.runtime.spawn(async move {
        //let val: Value = value.map_or_else(|| { Value::Nil }, |s| { Value::Status(s) });
        self.connection.as_mut().unwrap().lock().unwrap()
            //.set/*::<String, String, RV>*/(key, value);
            .set::<Option<String>, Option<String>, ()>(Some(key), value); // TODO try RedisValue
        //});
        Ok(())
    }

    // TODO support any type for value
    // TODO support null value (Option<...>)
    // TODO handle (nil)
    // TODO handle other types
    #[no_mangle]
    pub extern fn get(&mut self, key: String) -> Result<String, String> {
        let res = self.runtime.as_mut().unwrap().lock().unwrap().block_on(async {
            self.connection.as_mut().unwrap().lock().unwrap().get::<String, String>(key).await
        });
        match res {
            Ok(val) => Ok(val),
            Err(err) => Err(err.to_string())
        }
    }

    #[no_mangle]
    pub extern fn get2(&mut self, key: String) -> Result<String, String> {
        let res = self.runtime.as_mut().unwrap().lock().unwrap().block_on(async {
            self.connection.as_mut().unwrap().lock().unwrap().get::<String, Value>(key).await
        });
        match res {
            Ok(val) => Ok(String::from_redis_value(&val).unwrap()),
            Err(err) => Err(err.to_string())
        }
    }

    #[no_mangle]
    pub extern fn get3(&mut self, key: String) -> Result<Option<String>, String> {
        let res = self.runtime.as_mut().unwrap().lock().unwrap().block_on(async {
            self.connection.as_mut().unwrap().lock().unwrap().get::<String, Option<String>>(key).await
        });
        match res {
            Ok(val) => Ok(val),
            Err(err) => Err(err.to_string())
        }
    }

    #[no_mangle]
    pub extern fn get4(&mut self, key: String) -> Result<* const String, String> {
        let res = self.runtime.as_mut().unwrap().lock().unwrap().block_on(async {
            self.connection.as_mut().unwrap().lock().unwrap().get::<String, Option<String>>(key).await
        });
        match res {
            Ok(val) => {
                let value: Option<* const String> = val.map(|val| {
                    &val as *const String
                });
                Ok(value.unwrap_or(std::ptr::null()))
            },
            Err(err) => Err(err.to_string())
        }
    }
}
*/
