/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */
use std::ffi::c_char;

/// A mirror of `ConnectionRequest` from [`connection_request.proto`](https://github.com/aws/glide-for-redis/blob/main/glide-core/src/protobuf/connection_request.proto).
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

/// A mirror of `NodeAddress` from [`connection_request.proto`](https://github.com/aws/glide-for-redis/blob/main/glide-core/src/protobuf/connection_request.proto).
/// Represents the address and port of a node in the cluster.
#[repr(C)]
pub struct NodeAddress {
    pub host: *const c_char,
    pub port: u16,
}

/// A mirror of `TlsMode` from [`connection_request.proto`](https://github.com/aws/glide-for-redis/blob/main/glide-core/src/protobuf/connection_request.proto).
#[repr(C)]
pub enum TlsMode {
    NoTls = 0,
    Secure = 1,
    Insecure = 2,
}

/// A mirror of `ReadFrom` from [`connection_request.proto`](https://github.com/aws/glide-for-redis/blob/main/glide-core/src/protobuf/connection_request.proto).
/// Represents the client's read from strategy.
#[repr(C)]
pub enum ReadFrom {
    /// Always get from primary, in order to get the freshest data.
    Primary = 0,
    /// Spread the requests between all replicas in a round-robin manner. If no replica is available, route the requests to the primary.
    PreferReplica = 1,
    LowestLatency = 2,
    AZAffinity = 3,
}

/// A mirror of `ConnectionRetryStrategy` from [`connection_request.proto`](https://github.com/aws/glide-for-redis/blob/main/glide-core/src/protobuf/connection_request.proto).
/// Represents the strategy used to determine how and when to reconnect, in case of connection failures.
/// The time between attempts grows exponentially, to the formula
/// ```
/// rand(0 ... factor * (exponentBase ^ N))
/// ```
/// where `N` is the number of failed attempts.
///
/// Once the maximum value is reached, that will remain the time between retry attempts until a
/// reconnect attempt is successful. The client will attempt to reconnect indefinitely.
#[repr(C)]
pub struct ConnectionRetryStrategy {
    /// Number of retry attempts that the client should perform when disconnected from the server,
    /// where the time between retries increases. Once the retries have reached the maximum value, the
    /// time between retries will remain constant until a reconnect attempt is successful.
    pub number_of_retries: u32,
    /// The multiplier that will be applied to the waiting time between each retry.
    pub factor: u32,
    /// The exponent base configured for the strategy.
    pub exponent_base: u32,
}

/// A mirror of `AuthenticationInfo` from [`connection_request.proto`](https://github.com/aws/glide-for-redis/blob/main/glide-core/src/protobuf/connection_request.proto).
#[repr(C)]
pub struct AuthenticationInfo {
    pub username: *const c_char,
    pub password: *const c_char,
}

/// A mirror of `ProtocolVersion` from [`connection_request.proto`](https://github.com/aws/glide-for-redis/blob/main/glide-core/src/protobuf/connection_request.proto).
/// Represents the communication protocol with the server.
#[repr(C)]
pub enum ProtocolVersion {
    /// Use RESP3 to communicate with the server nodes.
    RESP3 = 0,
    /// Use RESP2 to communicate with the server nodes.
    RESP2 = 1,
}
