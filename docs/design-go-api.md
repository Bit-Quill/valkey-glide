# Go API Design documentation

## Overview

This document presents the high-level user API for the Go-Wrapper client library. Specifically, it demonstrates how the user connects to Redis, executes requests, receives responses, and checks for errors.

## Requirements

- The minimum supported Go version will be 1.18. This version was chosen because it added support for generics, including type constraints and type sets
- The API will be thread-safe.
- The API will accept as inputs all of [RESP2 types](https://github.com/redis/redis-specifications/blob/master/protocol/RESP2.md). We plan to add support for RESP3 types when they are available.
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

### Case 2: Connection to Redis fails with ConnectionError
```go
var client *glide.RedisClient
var err error
client, err := glide.CreateClient(config)

// User can check specifically for a ConnectionError:
if err != nil {
    connErr, isConnError := err.(glide.ConnectionError)
    if isConnError {  
        log.Fatal("Failed to connect to Redis: " + connErr.Error())
    }
}

// Or user can simply log the error message:
if err != nil {
    log.Fatal("Redis client failed with: " + err.Error())
}
```

### Case 3: Connect to Redis with deferred cleanup
```go
var client *glide.RedisClient
var err error
client, err = glide.CreateClient(config)
if err != nil {
    log.Fatal("Redis client failed with: " + err.Error())
}

defer client.Close()

// continue using client...
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
Return a glide.ConnectionError if the RedisClient is closed/disconnected
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

firstResponse := result[0]
secondResponse := result[1]
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

### Case 11: Send Get request to a RedisClusterClient with multiple addresses
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

result, err := client.GetWithRoutes("apples", glide.NewRoute(glide.AllNodes))
```

### Case 12: Request times out, user retries up to a predetermined limit
```go
numRetries := 3

for {
    attempts := 0
    result, err := client.Get("apples")

    if err == nil {
        break
    }

    timeoutErr, isTimeoutErr := err.(glide.TimeoutError)
    if isTimeoutErr {
        fmt.Println("RedisClient Get encountered a TimeoutError")
    } else {
        log.Fatal("RedisClient Get failed with: " + err.Error())
    } 

    attempts += 1
    if attempts == numRetries {
        log.Fatal("RedisClient Get request hit the retry limit")
    }
}
```

### Case 13: Get(key) encounters a ClosingError
```go
result, err := client.Get("apples")  // ClosingError is detected internally, client will internally close and perform any necessary cleanup steps
if err != nil {
    closingErr, isClosingErr := err.(glide.ClosingError)
    if isClosingErr {  
        log.Fatal("RedisClient get failed with: " + closingErr.Error())
    }
}
```

# API Design

## Presentation

![API Design](img/design-go-api.svg)
