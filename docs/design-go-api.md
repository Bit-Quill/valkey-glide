# Go API Design documentation

## Overview

This document presents the high-level user API for the Go-Wrapper client library. Specifically, it demonstrates how the user connects to Redis, executes requests, receives responses, and checks for errors.

## Requirements

- The minimum supported Go version will be 1.18. This version introduces support for generics, including type constraints and type sets.
- The API will be thread-safe.
- The API will accept as inputs all [RESP3 types](https://github.com/redis/redis-specifications/blob/master/protocol/RESP3.md).
- The API will attempt authentication, topology refreshes, reconnections, etc., automatically. In case of failures concrete errors will be returned to the user.

# Use Cases

### Case 1: Create Redis client and connect

```go
var config *StandaloneClientConfiguration = glide.NewClientConfiguration()
    WithAddress(glide.AddressInfo{Host: host, Port: port}).
    WithUseTLS(true)

// Create a client and connect
var client *glide.RedisClient
var err error
client, err = glide.CreateClient(config)
```

### Case 2: Connection to Redis fails with ClosingError
```go
var client *glide.RedisClient
var err error
client, err := glide.CreateClient(config)

// User can check specifically for a ClosingError:
if err != nil {
    closingErr, isClosingErr := err.(glide.ClosingError)
    if isClosingErr {  
        log.Fatal("Failed to connect to Redis: " + closingErr.Error())
    }
}
```

### Case 3: Connect to Redis with deferred cleanup
```go
func connectAndGet(key string) string {
    var client *glide.RedisClient
    var err error
    client, err = glide.CreateClient(config)
    if err != nil {
        log.Fatal("Redis client failed with: " + err.Error())
    }

    // client.Close() is executed when the function exits.
    // The client is available until the end of the function.
    defer client.Close()
    
    result, err := client.Get(key)
    if err != nil {
        // If we enter this branch, client.Close() will be executed after logging this message.
        log.Fatal("Redis Get failed with: " + err.Error())
    }

    // client.Close() will be executed when the result is returned.
    return result
}
```

### Case 4: Connect to Redis cluster
```go
var config *glide.ClusterClientConfiguration
config = glide.NewClusterClientConfiguration().
    WithAddress(glide.AddressInfo{Host: host1, Port: port1}).
    WithAddress(glide.AddressInfo{Host: host2, Port: port2}).
    WithUseTLS(true)

var client *glide.RedisClusterClient
var err error
client, err = glide.CreateClusterClient(config)
```

### Case 5: Get(key) from connected RedisClient
```go
result, err := client.Get("apples")
fmt.Println("The value associated with 'apples' is: " + result)
```

### Case 6: Set(key, value) from connected RedisClient
```go
// Without setOptions
err := client.Set("apples", "oranges")

// With setOptions
var setOptions *glide.SetOptions
setOptions = glide.NewSetOptions().
    WithReturnOldValue(true)
oldValue, err := client.SetWithOptions("apples", "oranges", setOptions)
```

### Case 7: Get(key) from a disconnected RedisClient
Return a glide.ConnectionError if the RedisClient fails to connect to Redis
```go
result, err := client.Get("apples")
if err != nil {
    connErr, isConnErr := err.(glide.ConnectionError)
    if isConnErr {  
        log.Fatal("RedisClient get failed with: " + connErr.Error())
    }
}
```

### Case 8: Send custom command to RedisClient
```go
var result interface{}
var err error

result, err = client.CustomCommand([]{"GET", "apples"})
if err != nil {
    log.Fatal("RedisClient failed to execute custom command with: " + err.Error())
}

strResult, isString := result.(string)
if !isString {
    log.Fatal("Expected result to be of type string but the actual type was: " + reflect.TypeOf(result))
}
```

### Case 9: Send transaction to RedisClient
```go
transaction := glide.NewTransaction()
transaction.Get("apples")
transaction.Get("pears")
transaction.Set("cherries", "Bing")

var result []interface{}
var err error
result, err = client.Exec(transaction)
if err != nil {
    log.Fatal("Redis client transaction failed with: " + err.Error())
}

firstResponse := result[0]  // evaluates to a string
secondResponse := result[1]  // evaluates to a string
thirdResponse := result[2]  // evaluates to nil
```

### Case 10: Send Get request to a RedisClusterClient with one address
```go
var config *glide.ClusterClientConfiguration
config = glide.NewClusterClientConfiguration().
    WithAddress(glide.AddressInfo{Host: host, Port: port}).
    WithUseTLS(true)

// Create a client and connect
var client *glide.RedisClusterClient
var err error
client, err = glide.CreateClusterClient(config)
if err != nil {
    log.Fatal("Redis client failed with: " + err.Error())
}

result, err := client.Get("apples")
```

### Case 11: Send Ping request to a RedisClusterClient with multiple addresses
```go
var config *glide.ClusterClientConfiguration
config = glide.NewClusterClientConfiguration().
    WithAddress(glide.AddressInfo{Host: host1, Port: port1}).
    WithAddress(glide.AddressInfo{Host: host2, Port: port2}).
    WithUseTLS(true)

// Create a client and connect
var client *glide.RedisClusterClient
var err error
client, err = glide.CreateClusterClient(config)
if err != nil {
    log.Fatal("Redis client failed with: " + err.Error())
}

// Without message or route
result, err := client.Ping()

// With message
result, err := client.PingWithMessage("Ping received")

// With route
result, err := client.PingWithRoute(glide.NewRoute(glide.AllNodes))

// With message and route
result, err := client.PingWithMessageAndRoute("Ping received", glide.NewRoute(glide.AllNodes))
```

### Case 12: Get(key) encounters a TimeoutError
```go
result, err := client.Get("apples")
if err != nil {
    timeoutErr, isTimeoutErr := err.(glide.TimeoutError)
    if isTimeoutErr {  
        // Handle error as desired
    }
}
```

### Case 13: Get(key) encounters a ConnectionError
```go
result, err := client.Get("apples")
if err != nil {
    connErr, isConnErr := err.(glide.ConnectionError)
    if isConnErr {  
        // Handle error as desired
    }
}
```

# API Design

## Presentation

![API Design](img/design-go-api.svg)
