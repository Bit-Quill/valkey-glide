/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */

use std::ffi::CStr;
use std::os::raw::c_char;
use logger_core::Level;

#[no_mangle]
#[allow(improper_ctypes_definitions)]
/// # Safety
/// Unsafe function because creating string from pointer
pub unsafe extern "C" fn log(
    log_level: Level,
    log_identifier: *const c_char,
    message: *const c_char,
) {
    // TODO return error or ignore?
    unsafe {
        logger_core::log(
            log_level.into(),
            CStr::from_ptr(log_identifier)
                .to_str()
                .expect("Can not read log_identifier argument."),
            CStr::from_ptr(message)
                .to_str()
                .expect("Can not read message argument."),
        );
    }
}

#[no_mangle]
#[allow(improper_ctypes_definitions)]
/// # Safety
/// Unsafe function because creating string from pointer
// TODO support optional level
pub unsafe extern "C" fn init_logging(level: Level, file_name: *const c_char) -> Level {
    let file_name_as_str = if file_name.is_null() {
        None
    } else {
        Some(
            unsafe { CStr::from_ptr(file_name) }
                .to_str()
                // TODO return an error?
                .expect("Can not read string argument."),
        )
    };

    logger_core::init(Some(level), file_name_as_str)
}
