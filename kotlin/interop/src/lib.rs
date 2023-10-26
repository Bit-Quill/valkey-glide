use std::fmt::Formatter;
use std::sync::{Arc, Mutex};
use redis::aio::MultiplexedConnection;
use redis::{ErrorKind, FromRedisValue, RedisResult, Value};
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
        let client = redis::Client::open(address)?;
        let runtime =
            Builder::new_multi_thread()
                .enable_all()
                .thread_name("Babushka kotlin thread")
                .build()?;

        let _runtime_handle = runtime.enter();

        let connection =
            runtime
                .block_on(client.get_multiplexed_async_connection())?;

        Ok(Arc::new(BabushkaClientData { runtime : Mutex::new(runtime), connection : Mutex::new(connection)} ))
    }

    pub fn set(&self, data: Arc<BabushkaClientData>, key: String, value: Option<String>) -> Result<(), BabushkaRedisError> {
        let res = data.runtime.lock().unwrap().block_on(async {
            data.connection.lock().unwrap()
                .set::<String, Option<String>, ()>(key, value).await
        });
        match res {
            Ok(_) => Ok(()),
            Err(err) => Err(convert_to_babushka_error(err))
        }
    }

    pub fn get(&self, data: Arc<BabushkaClientData>, key: String) -> Result<Option<String>, BabushkaRedisError> {
        let res = data.runtime.lock().unwrap().block_on(async {
            data.connection.lock().unwrap().get::<String, Option<String>>(key).await
        });
        match res {
            Ok(val) => Ok(val),
            Err(err) => Err(convert_to_babushka_error(err))
        }
    }
}
