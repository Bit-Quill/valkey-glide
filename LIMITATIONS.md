## Known Limitations

### Java Client Limitations

- Limited JDK support outside of JDK 11 and above. Unable to build libraries on JDK 21.

### Glide Core Limitations

- Single slot transactions:  Transaction commands must be commited to a single slot, and multiple commands that require different slots will fail. See [#199](https://github.com/valkey-io/valkey-glide/issues/199).
- Function Load and server propagation:
   - When loading functions to the server, glide will propagate the function library to existing cluster nodes only. Any new cluster nodes may not receive function libraries automatically.
server nodes will not receive new libraries automatically.
- Pub/Sub subscription commands: Subscribe commands require a new client and configuration. See: [PubSub documentation](https://github.com/valkey-io/valkey-glide/wiki/General-Concepts#glide-pubsub-feature) for more information.
- SCAN on cluster mode: Due to cluster layout, a scan request on a cluster of nodes cannot return a simple cursor. see: [SCAN documentation](https://github.com/valkey-io/valkey-glide/wiki/General-Concepts#cluster-scan) for more information.
- Large script requests are limited by the size of protobuf messages. see: [#1756](https://github.com/valkey-io/valkey-glide/issues/1756) for more information.
- FCALL commands ignores the is_readonly_cmd flag and sends requests to replicas. see: [#1569](https://github.com/valkey-io/valkey-glide/issues/1569) for more information.
