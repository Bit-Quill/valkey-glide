Java API Design

# Java API Design documentation

## Overview

This document is available to demonstrate the high-level and detailed design elements of the Java-Wrapper client library
interface. Specifically, it demonstrates how requests are received from the user, and responses with typing are delivered
back to the user. 

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