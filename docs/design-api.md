API Design

# Client Wrapper API design doc

## API requirements:
- The API will be thread-safe.
- The API will accept as inputs all of [RESP2 types](https://github.com/redis/redis-specifications/blob/master/protocol/RESP2.md). We plan to add support for RESP3 types when they are available.
- The API will attempt authentication, topology refreshes, reconnections, etc., automatically. In case of failures concrete errors will be returned to the user.

## Command Interface

### Unix Domain Socket solution
For clients based on Unix Domain Sockets (UDS), we will simply use the existing protobuf messages for creating a connection, sending requests, and receiving responses. Supported commands are enumerated in the [protobuf definition for requests](../babushka-core/src/protobuf/redis_request.proto) and we may add more in the future, although the `CustomCommand` request type is also adequate for all commands. As defined in the [protobuf definition for responses](../babushka-core/src/protobuf/response.proto), client wrappers will receive data as a pointer, which can be passed to Rust to marshal the data back into the wrapper language’s native data types.

Transactions will be handled by adding a list of `Command`s to the protobuf request. The response will be a `redis::Value::Bulk`, which should be handled in the same Rust function that marshals the data back into the wrapper language's native data types. This is handled by storing the results in a collection type native to the wrapper language.

When running Redis in Cluster Mode, several routing options will be provided. These are all specified in the protobuf request. The various options are detaield below in the ["Routing Options" section](#routing-options). We will also provide a separate client for handling Cluster Mode responses, which will convert the list of values and nodes into a map, as is done in existing client wrappers.

### Raw FFI solution
For clients using a raw FFI solution, in Rust, we will expose a general command that is able to take any command and arguments as strings.

Like in the UDS solution, we will support a separate client for Cluster Mode.

We have 2 options for passing the command, arguments, and any additional configuration to the Rust core from the wrapper language:

#### Protobuf
The wrapper language will pass the commands, arguments, and configuration as protobuf messages using the same definitions as in the UDS solution.

Transactions will be handled by adding a list of `Command`s to the protobuf request. The response will be a `redis::Value::Bulk`, which can be marshalled into a C array of values before being passed from Rust to the wrapper language. The wrapper language is responsible for converting the array of results to its own native collection type.

Cluster Mode support is the same here as in the UDS solution detailed above.

Pros:
- We get to reuse the protobuf definitions, meaning fewer files to update if we make changes to the protobuf definitions
- May be simpler to implement compared to the C data types solution, since we do not need to define our own C data types

Cons:
- There is additional overhead from marshalling data to and from protobuf, which could impact performance significantly

#### C Data Types
The wrapper language will pass commands, arguments, and configuration as C data types. 

Transactions will be handled by passing a C array of an array of arguments to Rust from the wrapper language. The response will be a `redis::Value::Bulk`, which can be marshalled in the same way as explained in the protobuf solution.

For Cluster Mode support, [routing options](#routing-options) will be defined as C enums and structs. Like in the protobuf solution, we will provide a separate client for handling Cluster Mode responses, which will convert the list of values and nodes into a map.

Pros:
- No additional overhead from marshalling to and from protobuf, so this should perform better
- May be simpler to implement compared to protobuf solution, since it can be tricky to construct protobuf messages in a performant way and we have to add a varint to the messages as well 

Cons:
- Would add an additional file to maintain containing the C definitions (only one file though, since we could share between all raw FFI solutions), which we would need to update every time we want to update the existing protobuf definitions

We will be testing both approaches to see which is easier to implement, as well as the performance impact before deciding on a solution.

To marshal Redis data types back into the corresponding types for the wrapper language, we will convert them into appropriate C types, which can then be translated by the wrapper language into its native data types. Here is what a Redis result might look like:
```
typedef struct redisValue {
    enum {NIL, INT, DATA, STATUS, BULK, OKAY} kind;
    union Payload {
        long intValue;
        unsigned char *dataValue;
        char *statusValue;
        struct redisValue *bulkValue;
    } payload;
} RedisValue
```

## Routing Options
We will be supporting routing Redis requests to all nodes, all primary nodes, or a random node. For more specific routing to a node, we will also allow sending a request to a primary or replica node with a specified hash slot or key. When the wrapper given a key route, the key is passed to the Rust core, which will find the corresponding hash slot for it.

## Supported Commands
We will be supporting all Redis commands. Commands with higher usage will be prioritized, as determined by usage numbers from AWS ElastiCache usage logs.

Two different methods of sending commands will be supported:

### Custom Command
We will expose an `executeRaw` method that does no validation of the input types or command on the client side, leaving it up to Redis to reject the command should it be malformed. This gives the user the flexibility to send any type of command they want, including ones not officially supported yet.

For example, if a user wants to implement support for the Redis ZADD command in Java, their implementation might look something like this:
```java
public Long zadd(K key, double score, V member) throws RequestException {
    string[] args = { key.toString(), score.toString(), member.toString() };
    return (Long) executeRaw(args);
}
```

where `executeRaw` has the following signature:
```java
public Object executeRaw(string[] args) throws RequestException
```

### Explicitly Supported Command
We will expose separate methods for each supported command. There will be a separate version of each method for transactions, as well as another version for Cluster Mode clients. For statically typed languages, we will leverage the compiler of the wrapper language to validate the types of the command arguments as much as possible. Since wrappers should be as lightweight as possible, we will be performing very few to no checks for proper typing for non-statically typed languages.

## Errors
ClosingError: Errors that report that the client has closed and is no longer usable.

RedisError: Errors that were reported during a request.

TimeoutError: Errors that are thrown when a request times out.

ExecAbortError: Errors that are thrown when a transaction is aborted.

ConnectionError: Errors that are thrown when a connection disconnects. These errors can be temporary, as the client will attempt to reconnect.

Errors returned are subject to change as we update the protobuf definitions.

## Java Specific Details
We will be using the UDS solution for communication between the wrapper and the Rust core. This thin layer is implemented using the [jni-rs library](https://github.com/jni-rs/jni-rs) to start the socket listener and marshal Redis values into native Java data types.

Errors in Rust are represented as Algebraic Data Types, which are not supported in Java by default (at least not in the versions of Java we want to support). Instead, we utilise the [jni-rs library](https://github.com/jni-rs/jni-rs) to throw Java `Exception`s where we receive errors from Redis.

## Golang Specific Details
We will be using a raw FFI solution for communication between the wrapper and the Rust core. TODO: Add more details here
