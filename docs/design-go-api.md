# Go API Design documentation

## Overview

This document presents the high-level user API for the Go-Wrapper client library. Specifically, it demonstrates how the user connects to Redis, executes requests, receives responses, and checks for errors.

# Use Cases

### Case 1: Create Redis client and connect

```go
var config *StandaloneClientConfiguration = glide.config.standalone.NewClientConfiguration().
		WithAddress(glide.config.AddressInfo{Host: host, Port: port}).
		WithUseTLS(true)

// Create a client and connect
var client *RedisClient
var err error
client, err = glide.client.standalone.CreateClient(config)
```

### Case 2: Connection to Redis fails with ConnectionError
```go
var client *RedisClient
var err error
client, err := glide.client.standalone.CreateClient(config)

// User can check specifically for a ConnectionError:
if err != nil {
    if connErr, ok := err.(glide.errors.ConnectionError); ok {  
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
var client *RedisClient
var err error
client, err := glide.client.standalone.CreateClient(config)
if err != nil {
    log.Fatal("Redis client failed with: " + err.Error())
}

defer client.Close()

// continue using client...
```

### Case 4: Connect to Redis cluster
```go
var config *StandaloneClientConfiguration
config = glide.config.cluster.NewClientConfiguration().
		WithAddresses([]AddressInfo{
			{Host: host1, Port: port1},
			{Host: host2, Port: port2},
		}).
		WithUseTLS(true)

var client *RedisClusterClient
var err error
client, err := glide.client.cluster.CreateClient(config)
```

### Case 5: Get(key) from connected RedisClient
```go
result, err := client.Get("apples")
```

### Case 6: Set(key, value) from connected RedisClient
```go
// Without setOptions
err = client.Set("apples", "oranges")

// With setOptions
var setOptions *SetOptions
setOptions = glide.commands.options.NewSetOptions().
    WithReturnOldValue(true)
oldValue, err := client.SetWithOptions("apples", "oranges", setOptions)
```

### Case 7: Get(key) from a disconnected RedisClient
Return a glide.errors.ConnectionError if the RedisClient is closed/disconnected
```go
result, err := client.Get("apples")
if err != nil {
    if connErr, ok := err.(glide.errors.ConnectionError); ok {  
        log.Fatal("RedisClient get failed with: " + connErr.Error())
    }
}
```

### Case 8: Send customCommand to RedisClient
```go
var result interface{}
var err error

result, err = client.CustomCommand([]{"GET", "apples"})
if err != nil {
	log.Fatal("RedisClient failed to execute custom command with: " + err.Error())
}

var strResult string
var ok bool
if strResult, ok = result.(string); !ok {
	log.Fatal("Expected result to be of type string but the actual type was: " + reflect.TypeOf(result))
}
```

### Case 9: Send transaction to RedisClient
```go
transaction := glide.commands.NewTransaction()
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

### Case 10: Send get request to a RedisClusterClient with one address
```go
var config *ClusterClientConfiguration
config = glide.config.cluster.NewClientConfiguration().
		WithAddresses([]glide.config.AddressInfo{{Host: host, Port: port}}).
		WithUseTLS(true)

// Create a client and connect
var client *RedisClusterClient
var err error
client, err := glide.client.cluster.CreateClient(config)
if err != nil {
    log.Fatal("Redis client failed with: " + err.Error())
}

result, err := client.Get("apples")
```

### Case 11: Send get request to a RedisClusterClient with multiple addresses
```go
var config *ClusterClientConfiguration
config := glide.config.cluster.NewClientConfiguration().
		WithAddresses([]AddressInfo{
			{Host: host1, Port: port1},
			{Host: host2, Port: port2},
		}).
		WithUseTLS(true)

// Create a client and connect
var client *RedisClusterClient
var err error
client, err := glide.client.cluster.CreateClient(config)
if err != nil {
    log.Fatal("Redis client failed with: " + err.Error())
}

result, err := client.Get("apples")
```

### Case 12: Request times out
```go
result, err := client.Get("apples")
if err != nil {
    if connErr, ok := err.(glide.errors.TimeoutError); ok {  
        log.Fatal("RedisClient get failed with: " + connErr.Error())
    }
}
```
