# Go API Design documentation

## Overview

This document presents the high-level user API for the Go-Wrapper client library. Specifically, it demonstrates how the user connects to Redis, executes requests, receives responses, and checks for errors.

# Use Cases

### Case 1: Connect to RedisClient

```go
config := glide.config.NewClientConfiguration().
		WithAddress(glide.config.AddressInfo{Host: host, Port: port}).
		WithUseTLS(true)

// Create a client and connect
client, err := glide.client.CreateRedisClient(config)
```

### Case 2: Connection to RedisClient fails with ConnectionError
```go
client, err := glide.client.CreateRedisClient(config)

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

### Case 3: Connect to RedisClient with deferred cleanup
```go
client, err := glide.client.CreateRedisClient(config)
if err != nil {
    log.Fatal("Redis client failed with: " + err.Error())
}

defer client.Close()

// continue using client...
```

### Case 4: Connect to RedisClusterClient
```go
// TODO: the Python client auto-discovers all addresses, do we need to do the same?
config := glide.config.NewClusterClientConfiguration().
		WithAddresses([]AddressInfo{
			{Host: host1, Port: port1},
			{Host: host2, Port: port2},
		}).
		WithUseTLS(true)

client, err := glide.client.CreateRedisClusterClient(config)
```

### Case 5: Connect to RedisClient and receive RESP2 responses
```go
config := glide.config.NewClientConfiguration().
		WithAddresses([]glide.config.AddressInfo{{Host: host, Port: port}}).
		WithUseTLS(true).
		WithUseRESP2(true)  // TODO: this field does not exist in the Python client, do we need it?

// Create a client and connect
client, err := glide.client.CreateRedisClient(config)
if err != nil {
    log.Fatal("Redis client failed with: " + err.Error())
}

result, err := client.Get("apples")
```

### Case 6: Get(key) from connected RedisClient
```go
result, err := client.Get("apples")
```

### Case 7: Set(key, value) from connected RedisClient
```go
// Without setOptions
err = client.Set("apples", "oranges")

// With setOptions
setOptions := glide.commands.options.NewSetOptions().
    WithReturnOldValue(true)
oldValue, err := client.SetWithOptions("apples", "oranges", setOptions)
```

### Case 8: Get(key) from a disconnected RedisClient
Return a glide.errors.ConnectionError if the RedisClient is closed/disconnected
```go
result, err := client.Get("apples")
if err != nil {
    if connErr, ok := err.(glide.errors.ConnectionError); ok {  
        log.Fatal("RedisClient get failed with: " + connErr.Error())
    }
}
```

### Case 9: Send customCommand to RedisClient
```go
result, err := client.CustomCommand([]{"GET", "apples"})
```

### Case 10: Send transaction to RedisClient
```go
transaction := glide.commands.NewTransaction()
transaction.Get("apples")
transaction.Get("pears")
transaction.Set("cherries", "Bing")

result, err := client.Exec(transaction)
if err != nil {
    log.Fatal("Redis client transaction failed with: " + err.Error())
}

firstResponse := result[0]
secondResponse := result[1]
thirdResponse := result[2]  // evaluates to nil
```

### Case 11: Send get request to a RedisClusterClient with one address
```go
config := glide.config.NewClusterClientConfiguration().
		WithAddresses([]glide.config.AddressInfo{{Host: host, Port: port}}).
		WithUseTLS(true)

// Create a client and connect
client, err := glide.client.CreateRedisCluserClient(config)
if err != nil {
    log.Fatal("Redis client failed with: " + err.Error())
}

result, err := client.Get("apples")
```

### Case 12: Send get request to a RedisClusterClient with multiple addresses
```go
config := glide.config.NewClusterClientConfiguration().
		WithAddresses([]AddressInfo{
			{Host: host1, Port: port1},
			{Host: host2, Port: port2},
		}).
		WithUseTLS(true)

// Create a client and connect
client, err := glide.client.CreateRedisClusterClient(config)
if err != nil {
    log.Fatal("Redis client failed with: " + err.Error())
}

result, err := client.Get("apples")
```

### Case 13: Request is interrupted
```go
// TODO: is this case still applicable in Go?
result, err := client.Get("apples")
if err != nil {
    if connErr, ok := err.(glide.errors.InterruptedError); ok {  
        log.Fatal("RedisClient get failed with: " + connErr.Error())
    }
}
```

### Case 14: Request timesout
```go
// TODO: is this case still applicable in Go?
result, err := client.Get("apples")
if err != nil {
    if connErr, ok := err.(glide.errors.TimeoutError); ok {  
        log.Fatal("RedisClient get failed with: " + connErr.Error())
    }
}
```
