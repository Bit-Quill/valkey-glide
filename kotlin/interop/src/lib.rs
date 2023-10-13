uniffi::include_scaffolding!("babushka");

#[derive(Debug, thiserror::Error)]
pub enum BabushkaError {
    #[error("...")]
    Oops
}

pub fn static_function_which_throws() -> Result<String, BabushkaError> {
    Ok("hello from rust -> static function which throws".into())
}

pub fn static_function() -> String {
    "hello from rust -> static function".into()
}

pub struct BabushkaClient {
}

impl BabushkaClient {
    pub fn new() -> Self {
        BabushkaClient { }
    }

    pub fn class_function(&self) -> String {
       "hello from rust -> class function".into()
    }
}
