package api

import "github.com/aws/glide-for-redis/go/glide/protobuf"

// NodeAddress represents the host address and port of a node in the cluster.
type NodeAddress struct {
	Host string
	Port uint32
}

// RedisCredentials represents the credentials for connecting to a Redis server.
type RedisCredentials struct {
	// The username that will be used for authenticating connections to the Redis servers. If not
	// supplied, "default" will be used.
	Username string
	// The password that will be used for authenticating connections to the Redis servers.
	Password string
}

// ReadFrom represents the client's read from strategy.
type ReadFrom int

const (
	// Always get from primary, in order to get the freshest data.
	PRIMARY ReadFrom = 0
	// Spread the requests between all replicas in a round robin manner. If no replica is available,
	// route the requests to the primary.
	PREFER_REPLICA ReadFrom = 1
)

type baseClientConfiguration struct {
	addresses      []NodeAddress
	useTLS         bool
	credentials    *RedisCredentials
	readFrom       ReadFrom
	requestTimeout *uint32
}

func (config *baseClientConfiguration) toProtobufConnRequest() *protobuf.ConnectionRequest {
	request := protobuf.ConnectionRequest{}
	for _, address := range config.addresses {
		nodeAddress := &protobuf.NodeAddress{
			Host: address.Host,
			Port: address.Port,
		}
		request.Addresses = append(request.Addresses, nodeAddress)
	}

	if config.useTLS {
		request.TlsMode = protobuf.TlsMode_SecureTls
	} else {
		request.TlsMode = protobuf.TlsMode_NoTls
	}

	if config.credentials != nil {
		authInfo := protobuf.AuthenticationInfo{}
		if config.credentials.Username != "" {
			authInfo.Username = config.credentials.Username
		}
		authInfo.Password = config.credentials.Password
		request.AuthenticationInfo = &authInfo
	}

	request.ReadFrom = mapReadFrom(config.readFrom)
	if config.requestTimeout != nil {
		request.RequestTimeout = *config.requestTimeout
	}

	return &request
}

func mapReadFrom(readFrom ReadFrom) protobuf.ReadFrom {
	if readFrom == PREFER_REPLICA {
		return protobuf.ReadFrom_PreferReplica
	}

	return protobuf.ReadFrom_Primary
}

// BackoffStrategy represents the strategy used to determine how and when to reconnect, in case of
// connection failures. The time between attempts grows exponentially, to the formula
// rand(0 ... factor * (exponentBase ^ N)), where N is the number of failed attempts.
//
// Once the maximum value is reached, that will remain the time between retry attempts until a
// reconnect attempt is successful. The client will attempt to reconnect indefinitely.
type BackoffStrategy struct {
	// Number of retry attempts that the client should perform when disconnected from the server,
	// where the time between retries increases. Once the retries have reached the maximum value,
	// the time between retries will remain constant until a reconnect attempt is successful.
	NumOfRetries uint32
	// The multiplier that will be applied to the waiting time between each retry.
	Factor uint32
	// The exponent base configured for the strategy.
	ExponentBase uint32
}

// RedisClientConfiguration represents the configuration settings for a Standalone Redis client.
// baseClientConfiguration is an embedded struct that contains shared settings for standalone and
// cluster clients.
type RedisClientConfiguration struct {
	baseClientConfiguration
	reconnectStrategy *BackoffStrategy
	databaseId        *uint32
}

// NewRedisClientConfiguration returns a [RedisClientConfiguration] with default configuration
// settings. For further configuration, use the [RedisClientConfiguration] With* methods.
func NewRedisClientConfiguration() *RedisClientConfiguration {
	return &RedisClientConfiguration{
		baseClientConfiguration: baseClientConfiguration{readFrom: PRIMARY},
	}
}

func (config *RedisClientConfiguration) toProtobufConnRequest() *protobuf.ConnectionRequest {
	request := config.baseClientConfiguration.toProtobufConnRequest()
	request.ClusterModeEnabled = false
	if config.reconnectStrategy != nil {
		request.ConnectionRetryStrategy = &protobuf.ConnectionRetryStrategy{
			NumberOfRetries: config.reconnectStrategy.NumOfRetries,
			Factor:          config.reconnectStrategy.Factor,
			ExponentBase:    config.reconnectStrategy.ExponentBase,
		}
	}

	if config.databaseId != nil {
		request.DatabaseId = *config.databaseId
	}

	return request
}

// WithAddress adds an address for a known node in the cluster to this configuration's list of
// addresses. WithAddress can be called multiple times to add multiple addresses to the list. If the
// server is in cluster mode the list can be partial, as the client will attempt to map out the
// cluster and find all nodes. If the server is in standalone mode, only nodes whose addresses were
// provided will be used by the client. For example:
//
//	config := NewRedisClientConfiguration().
//	    WithAddress(&NodeAddress{
//	        Host: "sample-address-0001.use1.cache.amazonaws.com", Port: 6379}).
//	    WithAddress(&NodeAddress{
//	        Host: "sample-address-0002.use1.cache.amazonaws.com", Port: 6379})
func (config *RedisClientConfiguration) WithAddress(address *NodeAddress) *RedisClientConfiguration {
	config.addresses = append(config.addresses, *address)
	return config
}

// WithUseTLS configures the TLS settings for this configuration. Set to true if communication with
// the cluster should use Transport Level Security. This setting should match the TLS configuration
// of the server/cluster, otherwise the connection attempt will fail.
func (config *RedisClientConfiguration) WithUseTLS(useTLS bool) *RedisClientConfiguration {
	config.useTLS = useTLS
	return config
}

// WithCredentials sets the credentials for the authentication process. If none are set, the client
// will not authenticate itself with the server.
func (config *RedisClientConfiguration) WithCredentials(credentials *RedisCredentials) *RedisClientConfiguration {
	config.credentials = credentials
	return config
}

// WithReadFrom sets the client's [ReadFrom] strategy. If not set, [PRIMARY] will be used.
func (config *RedisClientConfiguration) WithReadFrom(readFrom ReadFrom) *RedisClientConfiguration {
	config.readFrom = readFrom
	return config
}

// WithRequestTimeout sets the duration in milliseconds that the client should wait for a request to
// complete. This duration encompasses sending the request, awaiting for a response from the server,
// and any required reconnections or retries. If the specified timeout is exceeded for a pending
// request, it will result in a timeout error. If not set, a default value will be used.
func (config *RedisClientConfiguration) WithRequestTimeout(requestTimeout uint32) *RedisClientConfiguration {
	config.requestTimeout = &requestTimeout
	return config
}

// WithReconnectStrategy sets the [BackoffStrategy] used to determine how and when to reconnect, in
// case of connection failures. If not set, a default backoff strategy will be used.
func (config *RedisClientConfiguration) WithReconnectStrategy(strategy *BackoffStrategy) *RedisClientConfiguration {
	config.reconnectStrategy = strategy
	return config
}

// WithDatabaseId sets the index of the logical database to connect to.
func (config *RedisClientConfiguration) WithDatabaseId(id uint32) *RedisClientConfiguration {
	config.databaseId = &id
	return config
}

// RedisClusterClientConfiguration represents the configuration settings for a Cluster Redis client.
// Notes: Currently, the reconnection strategy in cluster mode is not configurable, and exponential
// backoff with fixed values is used.
type RedisClusterClientConfiguration struct {
	baseClientConfiguration
}

// NewRedisClusterClientConfiguration returns a [RedisClientConfiguration] with default
// configuration settings. For further configuration, use the [RedisClientConfiguration] With*
// methods.
func NewRedisClusterClientConfiguration() *RedisClusterClientConfiguration {
	return &RedisClusterClientConfiguration{
		baseClientConfiguration: baseClientConfiguration{},
	}
}

func (config *RedisClusterClientConfiguration) toProtobufConnRequest() *protobuf.ConnectionRequest {
	request := config.baseClientConfiguration.toProtobufConnRequest()
	request.ClusterModeEnabled = true
	return request
}

// WithAddress adds an address for a known node in the cluster to this configuration's list of
// addresses. WithAddress can be called multiple times to add multiple addresses to the list. If the
// server is in cluster mode the list can be partial, as the client will attempt to map out the
// cluster and find all nodes. If the server is in standalone mode, only nodes whose addresses were
// provided will be used by the client. For example:
//
//	config := NewRedisClusterClientConfiguration().
//	    WithAddress(&NodeAddress{
//	        Host: "sample-address-0001.use1.cache.amazonaws.com", Port: 6379}).
//	    WithAddress(&NodeAddress{
//	        Host: "sample-address-0002.use1.cache.amazonaws.com", Port: 6379})
func (config *RedisClusterClientConfiguration) WithAddress(address NodeAddress) *RedisClusterClientConfiguration {
	config.addresses = append(config.addresses, address)
	return config
}

// WithUseTLS configures the TLS settings for this configuration. Set to true if communication with
// the cluster should use Transport Level Security. This setting should match the TLS configuration
// of the server/cluster, otherwise the connection attempt will fail.
func (config *RedisClusterClientConfiguration) WithUseTLS(useTLS bool) *RedisClusterClientConfiguration {
	config.useTLS = useTLS
	return config
}

// WithCredentials sets the credentials for the authentication process. If none are set, the client
// will not authenticate itself with the server.
func (config *RedisClusterClientConfiguration) WithCredentials(credentials *RedisCredentials) *RedisClusterClientConfiguration {
	config.credentials = credentials
	return config
}

// WithReadFrom sets the client's [ReadFrom] strategy. If not set, [PRIMARY] will be used.
func (config *RedisClusterClientConfiguration) WithReadFrom(readFrom ReadFrom) *RedisClusterClientConfiguration {
	config.readFrom = readFrom
	return config
}

// WithRequestTimeout sets the duration in milliseconds that the client should wait for a request to
// complete. This duration encompasses sending the request, awaiting for a response from the server,
// and any required reconnections or retries. If the specified timeout is exceeded for a pending
// request, it will result in a timeout error. If not set, a default value will be used.
func (config *RedisClusterClientConfiguration) WithRequestTimeout(requestTimeout uint32) *RedisClusterClientConfiguration {
	config.requestTimeout = &requestTimeout
	return config
}
