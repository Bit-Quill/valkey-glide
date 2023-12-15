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
    .useTls(true)
    .build();

// connect to Redis
CompletableFuture<babushka.client.api.RedisClient> redisClientConnection =
    babushka.client.api.RedisClient.CreateClient(configuration);

// resolve the Future and get a RedisClient
babushka.client.api.RedisClient redisClient = redisClientConnection.get();
redisClient.isConnected(); // true
```

### Case 2: Connection to RedisClient fails
```java
// create a client configuration for a standalone client - check connection
CompletableFuture<babushka.client.api.RedisClient> redisClientConnection =
    babushka.client.api.RedisClient.CreateClient(configuration);

// resolve the Future and get a RedisClient that is not connected
babushka.client.api.RedisClient redisClient = redisClientConnection.get();
if (!redisClient.isConnected()) {
    throw new RuntimeException("Failed to connect to Redis: " + redisClient.getConnectionError().getMessage());
}
```

### Case 3: Disconnect from RedisClient
TODO: confirm that close should be an async call
```java
CompletableFuture<babushka.client.api.RedisClient> redisClientCloseConnection = redisClient.close();
redisClient = redisClientCloseConnection.get();
redisClient.isConnected(); // false
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
    .useTls(true)
    .build();

// connect to Redis
CompletableFuture<babushka.client.api.RedisClusterClient> redisClusterClientConnection =
    babushka.client.api.RedisClusterClient.CreateClient(configuration);

// resolve the Future and get a RedisClusterClient
babushka.client.api.RedisClusterClient redisClusterClient = redisClusterClientConnection.get();
redisClusterClient.isConnected(); // true
```

### Case 5: Connect to RedisClient and receive RESP2 responses
To confirm: send `.resp2(true)` in the configuration

### Case ___: Get(key) from connected RedisClient
```java
CompletableFuture<RedisStringResponse> getRequest = redisClient.get("apples");
RedisStringResponse getResponse = getRequest.get();
getResponse.isSuccessful(); // true
String getValue = getResponse.getValue();
```

### Case ___: Set(key, value) from connected RedisClient
```java
CompletableFuture<RedisVoidResponse> setRequest = redisClient.set("apples", "oranges");
RedisVoidResponse setResponse = setRequest.get();
setResponse.isSuccessful(); // true
getResponse.isOk(); // true
getResponse.toString(); // "Ok"
```

### Case ___: Get(key) from a disconnected RedisClient
Throw a babushka.api.models.exceptions.ConnectionException if the RedisClient is closed/disconnected

### Case ___: Send customCommand to RedisClient
```java

CompletableFuture<RedisBaseResponse> customCommandRequest = redisClient.customCommand(StringCommands.GETSTRING, "apples");
RedisBaseResponse<Object> customCommandResponse = customCommandRequest.get();
if (customCommandResponse.isSuccessful()) {
  switch(customCommandResponse.getValueType()) {
    STRING:
      String customCommandValue = (String) customCommandResponse.getValue();
      break;
    DEFAULT: 
      throw new RuntimeException("Unexpected value type returned");
  }  
}
```
### Case ___: Send transaction to RedisClient
TODO

### Case ___: Send get request to a RedisClusterClient with one address
TODO

### Case ___: Send get request to a RedisClusterClient with multiple addresses
TODO

### Case ___: Request is interrupted
```java
CompletableFuture<RedisStringResponse> getRequest = redisClient.get("apples");
try{
    RedisStringResponse getResponse=getRequest.get(); // throws InterruptedException
} catch (InterruptedException interruptedException) {
    redisClient.isConnected(); // false  
}
```

### Case ___: Request timesout
```java
CompletableFuture<RedisStringResponse> getRequest = redisClient.get("apples");
RedisStringResponse getResponse = getRequest.get(); // times out
if (getResponse.isError()) {
    if (getResponse.getErrorType() == TIMEOUT_ERROR) {
      babushka.client.api.exceptions.TimeoutException timeoutException = getResponse.getTimeoutException();
      System.err.out("Timeout Exception: " + timeoutException.getMessage());
      throw timeoutException;
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