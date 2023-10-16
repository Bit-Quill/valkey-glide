use std::fmt::Formatter;
use std::sync::{Arc, Mutex};
use redis::aio::MultiplexedConnection;
use redis::{ErrorKind, FromRedisValue, Value};
use redis::{AsyncCommands, RedisError};
use tokio::runtime::Builder;
use tokio::runtime::Runtime;

uniffi::include_scaffolding!("babushka");

#[derive(Debug, thiserror::Error)]
pub enum BabushkaError {
    #[error("...")]
    Oops,
}


#[derive(Debug, thiserror::Error)]
pub enum BabushkaRedisError {
    ResponseError,
    AuthenticationFailed,
    TypeError,
    ExecAbortError,
    BusyLoadingError,
    NoScriptError,
    InvalidClientConfig,
    Moved,
    Ask,
    TryAgain,
    ClusterDown,
    CrossSlot,
    MasterDown,
    IoError,
    ClientError,
    ExtensionError,
    ReadOnly,
    MasterNameNotFoundBySentinel,
    NoValidReplicasFoundBySentinel,
    EmptySentinelList,
    NotBusy
}

impl std::fmt::Display for BabushkaRedisError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{:?}", self)
    }
}

fn convert_to_babushka_error(err: RedisError) -> BabushkaRedisError {
    match err.kind() {
        ErrorKind::ResponseError => BabushkaRedisError::ResponseError,
        ErrorKind::AuthenticationFailed => BabushkaRedisError::AuthenticationFailed,
        ErrorKind::TypeError => BabushkaRedisError::TypeError,
        ErrorKind::ExecAbortError => BabushkaRedisError::ExecAbortError,
        ErrorKind::BusyLoadingError => BabushkaRedisError::BusyLoadingError,
        ErrorKind::NoScriptError => BabushkaRedisError::NoScriptError,
        ErrorKind::InvalidClientConfig => BabushkaRedisError::InvalidClientConfig,
        ErrorKind::Moved => BabushkaRedisError::Moved,
        ErrorKind::Ask => BabushkaRedisError::Ask,
        ErrorKind::TryAgain => BabushkaRedisError::TryAgain,
        ErrorKind::ClusterDown => BabushkaRedisError::ClusterDown,
        ErrorKind::CrossSlot => BabushkaRedisError::CrossSlot,
        ErrorKind::MasterDown => BabushkaRedisError::MasterDown,
        ErrorKind::IoError => BabushkaRedisError::IoError,
        ErrorKind::ClientError => BabushkaRedisError::ClientError,
        ErrorKind::ExtensionError => BabushkaRedisError::ExtensionError,
        ErrorKind::ReadOnly => BabushkaRedisError::ReadOnly,
        ErrorKind::MasterNameNotFoundBySentinel => BabushkaRedisError::MasterNameNotFoundBySentinel,
        ErrorKind::NoValidReplicasFoundBySentinel => BabushkaRedisError::NoValidReplicasFoundBySentinel,
        ErrorKind::EmptySentinelList => BabushkaRedisError::EmptySentinelList,
        ErrorKind::NotBusy => BabushkaRedisError::NotBusy,
        //ErrorKind::Serialize => {}
//        _ => BabushkaRedisError::__
        _ => todo!()
    }
}

/*
pub struct FfiConverterTypeRedisError {
}

unsafe impl FfiConverter<> for FfiConverterTypeRedisError {
    type RustType = RedisError;
    type FfiType = ErrorKind;

    fn lower(obj: Self::RustType) -> Self::FfiType {
        obj.kind()
    }

    fn try_lift(v: Self::FfiType) -> uniffi::Result<Self::RustType> {
        Err(Error::msg(v))
    }

    fn write(obj: Self::RustType, buf: &mut Vec<u8>) {
        buf.write(obj.category().as_bytes()).expect("oops");
    }

    fn try_read(buf: &mut &[u8]) -> uniffi::Result<Self::RustType> {
        Ok(RedisError::from((ExtensionError, String::from_byte_vec(buf))))
    }
}
*/

pub fn static_function_which_throws() -> Result<String, BabushkaError> {
    Ok("hello from rust -> static function which throws".into())
}

pub fn static_function() -> String {
    "hello from rust -> static function".into()
}

pub struct BabushkaClient {
}

pub struct BabushkaClientData {
    runtime: Mutex<Runtime>,
    connection: Mutex<MultiplexedConnection>,
}

impl BabushkaClient {
    pub fn new() -> Self {
        Self { }
    }

    pub fn class_function(&self) -> String {
        "hello from rust -> class function".into()
    }

    pub fn connect(&self, address: String) -> Result<Arc<BabushkaClientData>, BabushkaRedisError> {
        match self.inner_connect(address) {
            Ok(data) => Ok(data),
            Err(err) => Err(convert_to_babushka_error(err))
        }
    }

    fn inner_connect(&self, address: String) -> Result<Arc<BabushkaClientData>, RedisError> {
        //self.client = Some(redis::Client::open(address)?);
        let client = redis::Client::open(address)?;
        let runtime =
            Builder::new_multi_thread()
                .enable_all()
                .thread_name("Babushka kotlin thread")
                .build()?;

        //let _runtime_handle = self.runtime.unwrap().enter();
        let _runtime_handle = runtime.enter();

        let connection =
            runtime
                .block_on(client.get_multiplexed_async_connection())?;

        Ok(Arc::new(BabushkaClientData { runtime : Mutex::new(runtime), connection : Mutex::new(connection)} ))
    }

    /*
        pub fn disconnect(&mut self) -> Result<(), RedisError> {

        }
    */
    // TODO support any type for value
    pub fn set(&self, data: Arc<BabushkaClientData>, key: String, value: String) -> Result<(), BabushkaRedisError> {
        //self.runtime.spawn(async move {
        data.connection.lock().unwrap()
            //.set/*::<String, String, RV>*/(key, value);
            .set::<String, String, ()>(key, value); // TODO try RedisValue
        //});
        Ok(())
    }

    pub fn set2(&self, data: Arc<BabushkaClientData>, key: String, value: Option<String>) -> Result<(), BabushkaRedisError> {
        //self.runtime.spawn(async move {
        //let val: Value = value.map_or_else(|| { Value::Nil }, |s| { Value::Status(s) });
        data.connection.lock().unwrap()
            //.set/*::<String, String, RV>*/(key, value);
            .set::<Option<String>, Option<String>, ()>(Some(key), value); // TODO try RedisValue
        //});
        Ok(())
    }

    // TODO support any type for value
    // TODO support null value (Option<...>)
    // TODO handle (nil)
    // TODO handle other types
    pub fn get(&self, data: Arc<BabushkaClientData>, key: String) -> Result<String, BabushkaRedisError> {
        let res = data.runtime.lock().unwrap().block_on(async {
            data.connection.lock().unwrap().get::<String, String>(key).await
        });
        match res {
            Ok(val) => Ok(val),
            Err(err) => Err(convert_to_babushka_error(err))
        }
    }

    pub fn get2(&self, data: Arc<BabushkaClientData>, key: String) -> Result<String, BabushkaRedisError> {
        let res = data.runtime.lock().unwrap().block_on(async {
            data.connection.lock().unwrap().get::<String, Value>(key).await
        });
        match res {
            Ok(val) => Ok(String::from_redis_value(&val).unwrap()),
            Err(err) => Err(convert_to_babushka_error(err))
        }
    }

    pub fn get3(&self, data: Arc<BabushkaClientData>, key: String) -> Result<Option<String>, BabushkaRedisError> {
        let res = data.runtime.lock().unwrap().block_on(async {
            data.connection.lock().unwrap().get::<String, Option<String>>(key).await
        });
        match res {
            Ok(val) => Ok(val),
            Err(err) => Err(convert_to_babushka_error(err))
        }
    }

    pub fn get4(&self, data: Arc<BabushkaClientData>, key: String) -> Result<* const String, BabushkaRedisError> {
        let res = data.runtime.lock().unwrap().block_on(async {
            data.connection.lock().unwrap().get::<String, Option<String>>(key).await
        });
        match res {
            Ok(val) => {
                let value: Option<*const String> = val.map(|val| {
                    &val as *const String
                });
                Ok(value.unwrap_or(std::ptr::null()))
            },
            Err(err) => Err(convert_to_babushka_error(err))
        }
    }
}
