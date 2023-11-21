API Design

# Client Wrapper API design doc

## API requirements:
- The API will be thread-safe
- The API will accept as inputs all of RESP3 types.
- The API will attempt authentication, topology refreshes, reconnections, etc., automatically. In case of failures concrete errors will be returned to the user.

## Command Interface

### Unix Domain Socket solution
For clients based on Unix Domain Sockets (UDS), we will simply use the existing protobuf messages for creating a connection, sending requests, and receiving responses. Supported commands are enumerated in the [protobuf definition for requests](https://github.com/Bit-Quill/babushka/blob/main/babushka-core/src/protobuf/redis_request.proto) and we may add more in the future, although the `CustomCommand` request type is also adequate for all commands. As defined in the [protobuf definition for responses](https://github.com/Bit-Quill/babushka/blob/main/babushka-core/src/protobuf/response.proto), client wrappers will receive data as a pointer, which can be passed to Rust to marshal the data back into the wrapper language’s native data types.

### Raw FFI solution
For clients using a raw FFI solution, in Rust, we will expose a general command that is able to take any command and arguments as strings. 

We have 2 options for passing the command, arguments, and any additional configuration to the Rust core from the wrapper language:

#### Protobuf
The wrapper language will pass the commands, arguments, and configuration as protobuf messages using the same definitions as in the UDS solution.

Pros:
- We get to reuse the protobuf definitions, meaning fewer files to update if we make changes to the protobuf definitions
- May be simpler to implement compared to the C data types solution, since we do not need to define our own C data types

Cons:
- There is additional overhead from marshalling data to and from protobuf, which could impact performance significantly

#### C Data Types
The wrapper language will pass commands, arguments, and configuration as C data types.

Pros:
- No additional overhead from marshalling to and from protobuf, so this should perform better
- May be simpler to implement compared to protobuf solution, since it can be tricky to construct protobuf messages in a performant way and we have to add a varint to the messages as well 

Cons:
- Would add an additional file to maintain containing the C definitions (only one file though, since we could share between all raw FFI solutions), which we would need to update every time we want to update the existing protobuf definitions

We will be testing both approaches to see which is easier to implement, as well as the performance impact before deciding on a solution.

To marshal Redis data types back into the corresponding types for the wrapper language, we will convert them into appropriate C types, which can then be translated by the wrapper language into its native data types.

In our client wrappers, we will expose separate methods for each supported command. We do not need to validate data types for inputs, because Redis will do that for us.

## Supported Commands
We will be supporting all Redis commands. Commands with higher usage will be prioritized.

## Errors
ClosingError: Errors that report that the client has closed and is no longer usable.

RedisError: Errors that were reported during a request.

TimeoutError: Errors that are thrown when a request times out.

ExecAbortError: Errors that are thrown when a transaction is aborted.

ConnectionError: Errors that are thrown when a connection disconnects. These errors can be temporary, as the client will attempt to reconnect.

Errors returned are subject to change as we update the protobuf definitions.

