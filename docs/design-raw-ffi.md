# Babushka Socket Listener

## Sequence Diagram - Unix Domain Socket Manager

**Summary**: The Babushka "UDS" solution uses a socket manager to redis-client worker threads, and UDS to manage the communication
between the wrapper and redis-client threads.  This works well because we allow the socket to manage the communication. This 
results in simple/fast communication.  But the worry is that the unix sockets can become a bottleneck for data-intensive communication; 
ie read/write buffer operations become the bottleneck.  

_Observation_: We noticed that we are creating a fixed number of Babushka/Redis client connections based on the number of CPUs available. 
It would be better to configure this thread count, or default it to the number of CPUs available.  

```mermaid
sequenceDiagram

participant Wrapper as Client-Wrapper
participant ffi as Babushka FFI
participant manager as Babushka impl
participant worker as Tokio Worker
participant SocketListener as Socket Listener
participant Socket as Unix Domain Socket
participant Client as Redis

activate Wrapper
activate Socket
activate Client
Wrapper ->>+ ffi: connect_to_redis
ffi ->>+ manager: start_socket_listener(init_callback)
    manager ->> worker: Create Tokio::Runtime (count: CPUs)
        activate worker
        worker ->> SocketListener: listen_on_socket(init_callback)
        SocketListener ->> SocketListener: loop: listen_on_client_stream
            activate SocketListener
    manager -->> ffi: socket_path
ffi -->>- Wrapper: socket_path
      SocketListener -->> Client: BabushkaClient::new
      SocketListener -->> Socket: UnixStreamListener::new 
Wrapper ->> Socket: write buffer (socket_path)
Socket -->> Wrapper: 
    SocketListener ->> SocketListener: handle_request
    SocketListener ->> Socket: read_values_loop(client_listener, client)
        Socket -->> SocketListener: 
    SocketListener ->> Client: send(request)
    Client -->> SocketListener: ClientUsageResult
    SocketListener ->> Socket: write_result
        Socket -->> SocketListener: 
Wrapper ->> Socket: read buffer (socket_path)
    Socket -->> Wrapper: 
Wrapper ->> Wrapper: Result 
Wrapper ->> ffi: close_connection
    ffi ->> manager: close_connection
        manager ->>- worker: close
            worker ->>SocketListener: close
                deactivate SocketListener
            deactivate worker
deactivate Wrapper
deactivate Client
deactivate Socket
```

## Elements
* **Wrapper**: Our Babushka wrapper that exposes a client API (java, python, node, etc)
* **Babushka FFI**: Foreign Function Interface definitions from our wrapper to our Rust Babushka-Core
* **Babushka impl**: public interface layer and thread manager
* **Tokio Worker**: Tokio worker threads (number of CPUs) 
* **SocketListener**: listens for work from the Socket, and handles commands
* **Unix Domain Socket**: Unix Domain Socket to handle communication
* **Redis**: Our data store

## (Current) Raw-FFI Benchmark Test

**Summary**: We copied the C# benchmarking implementation, and discovered that it wasn't using the Babushka/Redis client 
at all, but instead spawning a single worker thread to connect to Redis using a general Rust Redis client.

```mermaid
sequenceDiagram

participant Wrapper as Client-Wrapper
participant ffi as Babushka FFI
participant worker as Tokio Worker
participant Client as Redis

activate Wrapper
activate Client
Wrapper ->>+ ffi: create_connection
    ffi ->>+ worker: Create Tokio::Runtime (count: 1)
        worker ->> Client: new Redis::Client
    ffi -->> Wrapper: Connection
Wrapper ->> ffi: command (GET/SET)
    ffi ->>+ worker: Runtime::spawn
        worker ->> Client: Connection::clone(command)
        Client -->> worker: Result
    worker -->> ffi: success_callback
ffi ->> Wrapper: async Result
deactivate Wrapper
deactivate Client
```

## Sequence Diagram - Managed Raw-FFI Client

**Summary**: Following the socket listener/manager solution, we can create a [event manager](https://en.wikipedia.org/wiki/Reactor_pattern)
on the Rust side that will spawn worker threads available to execute event commands on-demand. FFI calls will petition the 
worker thread manager for work request. 

_Expectation_: According to Shachar, it is our understanding that having a Tokio thead manager on the Rust side, and an event
manager on the Wrapper-side will create a lot of busy-waiting between the two thread managers. 

_Observation_: Go routines seems to have a decent solution using channels.  Instead of waiting, we can close the threads
on the wrapper since, and (awaken) push the threads back to the channel once the Tokio threads are completed. 

```mermaid
sequenceDiagram

participant Wrapper as Client-Wrapper
participant ffi as Babushka FFI
participant manager as Babushka impl
participant worker as Tokio Worker
participant RuntimeManager as Runtime Manager
participant Client as Redis

activate Wrapper
activate Client
Wrapper ->>+ ffi: connect_to_redis
ffi ->>+ manager: start_thread_manager(init_callback)
    manager ->> worker: Create Tokio::Runtime (count: CPUs)
        activate worker
      worker ->> RuntimeManager: wait_for_work(init_callback)
          RuntimeManager ->> RuntimeManager: loop: wait
            activate RuntimeManager
    manager -->> ffi: callback
ffi -->>- Wrapper: callback
      RuntimeManager -->> Client: BabushkaClient::new

Wrapper ->> ffi: command: get(key)
    ffi ->> manager: command: get(key)
        manager ->> worker: command: get(key)
            worker ->> Client: send(command)
            Client -->> worker: Result
        worker -->> ffi: success_callback
ffi ->> Wrapper: async Result

Wrapper ->> ffi: close_connection
    ffi ->> manager: close_connection
        manager ->>- worker: close
            worker ->>RuntimeManager: close
                deactivate RuntimeManager
            deactivate worker
deactivate Wrapper
deactivate Client
```