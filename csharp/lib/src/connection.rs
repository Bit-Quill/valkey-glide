/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */
use std::ffi::c_char;

#[repr(C)]
pub struct ConnectionConfig {
    pub address_count: usize,
    /// Pointer to an array.
    pub addresses: *const *const NodeAddress,
    pub tls_mode: TlsMode,
    pub cluster_mode: bool,
    pub request_timeout: u32,
    pub read_from: ReadFrom,
    pub connection_retry_strategy: ConnectionRetryStrategy,
    pub authentication_info: AuthenticationInfo,
    pub database_id: u32,
    pub protocol: ProtocolVersion,
    pub client_name: *const c_char,
}

#[repr(C)]
pub struct NodeAddress {
    pub host: *const c_char,
    pub port: u16,
}

#[repr(C)]
pub enum TlsMode {
    NoTls = 0,
    SecureTls = 1,
    InsecureTls = 2,
}

#[repr(C)]
pub enum ReadFrom {
    Primary = 0,
    PreferReplica = 1,
    LowestLatency = 2,
    AZAffinity = 3,
}

#[repr(C)]
pub struct ConnectionRetryStrategy {
    pub number_of_retries: u32,
    pub factor: u32,
    pub exponent_base: u32,
}

#[repr(C)]
pub struct AuthenticationInfo {
    pub username: *const c_char,
    pub password: *const c_char,
}

#[repr(C)]
pub enum ProtocolVersion {
    RESP3 = 0,
    RESP2 = 1,
}
