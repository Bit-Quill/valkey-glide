Java API Design

# Java API Design documentation

## Overview

This document is available to demonstrate the high-level and detailed design elements of the Java-Wrapper client library
interface. Specifically, it demonstrates how requests are received from the user, and responses with typing are delivered
back to the user. 

# Use Cases

### Case 1: Connect to RedisClient

```java
// create a client configuration for a standalone client and connect
babushka.client.api.RedisClientConfiguration configuration =
    babushka.client.api.RedisClientConfiguration.builder()
    .address(babushka.client.api.Addresses.builder()
        .host(host)
        .port(port)
        .build())
    .useTLS(true)
    .build();

// connect to Redis
CompletableFuture<babushka.client.api.RedisClient> redisClientConnection =
    babushka.client.api.RedisClient.CreateClient(configuration);

// resolve the Future and get a RedisClient
babushka.client.api.RedisClient redisClient = redisClientConnection.get();
```

### Case 2: Connection to RedisClient fails with ConnectionException
```java
// create a client configuration for a standalone client - check connection
CompletableFuture<babushka.client.api.RedisClient> redisClientConnection =
    babushka.client.api.RedisClient.CreateClient(configuration);

// resolve the Future and get a RedisClient that is not connected
try{
    babushka.client.api.RedisClient redisClient=redisClientConnection.get();
} catch (babushka.client.api.model.exceptions.RedisException redisException){
    if (redisException instanceOf babushka.client.api.model.exceptions.ConnectionException) {
        throw new RuntimeException("Failed to connect to Redis: " + redisException.getMessage());
    }
}
```

### Case 3: Try RedisClient with resource 
```java
try (RedisClient redisClient = RedisClient.CreateClient(configuration).get()) {
    // use the RedisClient
} catch (babushka.client.api.model.exceptions.RedisException redisException) {
    throw new RuntimeException("RedisClient failed with: " + redisException.getMessage());
}
```

### Case 4: Connect to RedisClusterClient
```java
// create a client configuration for a standalone client and connect
babushka.client.api.RedisClusterClientConfiguration configuration =
    babushka.client.api.RedisClusterClientConfiguration.builder()
    .address(babushka.client.api.Addresses.builder()
        .host(address_one)
        .port(port_one)
        .build())
    .address(babushka.client.api.Addresses.builder()
        .host(address_two)
        .port(port_two)
        .build())
    .useTLS(true)
    .build();

// connect to Redis
CompletableFuture<babushka.client.api.RedisClusterClient> redisClusterClientConnection =
    babushka.client.api.RedisClusterClient.CreateClient(configuration);

// resolve the Future and get a RedisClusterClient
babushka.client.api.RedisClusterClient redisClusterClient = redisClusterClientConnection.get();
```

### Case 5: Connect to RedisClient and receive RESP2 responses (Future)
```java
// create a client configuration for a standalone client and connect
babushka.client.api.RedisClientConfiguration configuration =
    babushka.client.api.RedisClientConfiguration.builder()
    .address(babushka.client.api.Addresses.builder()
        .host(host)
        .port(port)
        .build())
    .useTLS(true)
    .useRESP2(true)
    .build();

// connect to Redis
CompletableFuture<babushka.client.api.RedisClient> redisClientConnection =
    babushka.client.api.RedisClient.CreateClient(configuration);

// resolve the Future and get a RedisClient
babushka.client.api.RedisClient redisClient = redisClientConnection.get();
```

### Case 6: Get(key) from connected RedisClient
```java
CompletableFuture<String> getRequest = redisClient.get("apples");
String getValueStr = getRequest.get();
```

### Case 7: Set(key, value) from connected RedisClient
```java
CompletableFuture<Void> setRequest = redisClient.set("apples", "oranges");
setRequest.get(); // returns null when complete

SetOptions setOptions = SetOptions.builder()
    .returnOldValue(true) // returns a String
    .build();
CompletableFuture<String> setRequest = redisClient.set("apples", "oranges", setOptions);
String oldValue = setRequest.get(); // returns a string unless .returnOldValue() is not true
```

### Case 8: Get(key) from a disconnected RedisClient
Throw a babushka.api.models.exceptions.ConnectionException if the RedisClient is closed/disconnected
```java
try {
    CompletableFuture<String> getRequest = redisClient.get("apples");
    String getValueStr = getRequest.get();
} catch (babushka.client.api.model.exceptions.RedisException redisException) {
    // handle RedisException
    throw new RuntimeException("RedisClient get failed with: " + redisException.getMessage());
}
```

### Case 9: Send customCommand to RedisClient and receive a RedisFuture (CompleteableFuture wrapper)
```java
// returns an Object: custom command requests don't have an associated return type
RedisFuture<Object> customCommandRequest = redisClient.customCommand(StringCommands.GETSTRING, "apples");
Object objectResponse = customCommandRequest.get();
if (customCommandRequest.isDone()) {
  switch(customCommandRequest.getValueType()) {
    STRING:
      String stringResponse = customCommandRequest.getString();
      break;
    DEFAULT: 
      throw new RuntimeException("Unexpected value type returned");
  }  
}
```

### Case 10: Send transaction to RedisClient
```java
// submit three commands in a single transaction to Redis
Command getApplesRequest = Command.builder()
    .requestType(GETSTRING)
    .arguments(new String[]{apples"})
    .build();
Command getPearsRequest = Command.builder()
    .requestType(GETSTRING)
    .arguments(new String[]{"pears"})
    .build();
Command setCherriesRequest = Command.builder()
    .requestType(SETSTRING)
    .arguments(new String[]{"cherries", "Bing"})
    .build();

Transaction transaction = Transaction.builder()
    .command(getAppleRequest)
    .command(getPearRequest)
    .command(setCherryRequest)
    .build();

CompletableFuture<List<Object>> transactionRequest = redisClient.exec(transaction);
List<Object> transactionResponse = transactionRequest.get();

// TODO: verify that if we use the RedisFuture wrapper, can we receive the typing for each object 
```

### Case 11: Send get request to a RedisClusterClient with one address
// TODO

### Case 12: Send get request to a RedisClusterClient with multiple addresses
// TODO

### Case 13: Request is interrupted
```java
CompletableFuture<String> getRequest = redisClient.get("apples");
try{
    RedisStringResponse getResponse = getRequest.get(); // throws InterruptedException
} catch (InterruptedException interruptedException) {
    throw new RuntimeException("RedisClient was interrupted: " + interruptedException.getMessage());
}
```

### Case 14: Request timesout
```java
CompletableFuture<String> getRequest = redisClient.get("apples");
try{
    RedisStringResponse getResponse = getRequest.get(); // throws TimeoutException
} catch (babushka.client.api.model.exceptions.RedisException redisException) {
    if (redisException instanceOf babushka.client.api.model.exceptions.TimeoutException) {
        throw new RuntimeException("RedisClient timedout: " + redisException.getMessage());
    }
}
```

# High-Level Architecture

## Presentation

![Architecture Overview](img/design-java-api-high-level.svg)

## Responsibilities

At a high-level the Java wrapper client has 3 layers:
1. The API layer that is exposed to the user
2. The service layer that deals with data mapping between the client models and data access models
3. The data access layer that is responsible for sending and receiving data from the Redis service

# API Detailed Design

## Presentation

![API Design](img/design-java-api-detailed-level.svg)

## Responsibilities

1. A client library that can receive Redis service configuration, and connect to a standalone and clustered Redis service
2. Once connected, the client library can send single command requests to the Redis service
3. Once connected, the client library can send transactional/multi-command request to the Redis service 
4. Success and Error feedback is returned to the user
5. Route descriptions are returned from cluster Redis services
6. The payload data in either RESP2 or RESP3 format is returned with the response

# Response and Payload typing

## Presentation

![API Request and Response typing](img/design-java-api-sequence-datatypes.svg)

## Responsibilities

1. Data typing and verification is performed for known commands  
2. Data is returned as a payload in the RedisResponse object on a success response
3. If no data payload is requested, the service returns an OK constant response
4. Otherwise, the service will cast to the specified type on a one-for-one mapping based on the command
5. If the casting fails, the Java-wrapper will report an Error