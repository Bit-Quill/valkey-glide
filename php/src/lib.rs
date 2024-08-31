use std::{
    ffi::{c_char, CString},
    sync::mpsc,
};

/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */
use glide_core::start_socket_listener as start_socket_listener_core;

#[no_mangle]
pub extern "C" fn start_socket_listener() -> *const c_char {
    let (tx, rx) = mpsc::channel::<Result<String, String>>();

    start_socket_listener_core(move |socket_path: Result<String, String>| {
        // Signals that thread has started
        let _ = tx.send(socket_path);
    });

    // Wait until the thread has started
    let socket_path = rx.recv();

    match socket_path {
        Ok(Ok(path)) => CString::into_raw(CString::new(path).unwrap()),
        //.map_err(|err| FFIError::Uds(err.to_string())),
        Ok(Err(_error_message)) => panic!("Error message"), //Err(FFIError::Uds(error_message)),
        Err(_error) => panic!("error recv"),                //Err(FFIError::Uds(error.to_string())),
    }
}
