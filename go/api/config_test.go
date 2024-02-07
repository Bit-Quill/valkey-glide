package api

import (
	"github.com/aws/glide-for-redis/go/glide/protobuf"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestConnectionRequestProtobufGeneration_defaultStandaloneConfig(t *testing.T) {
	config := NewRedisClientConfiguration()
	expected := &protobuf.ConnectionRequest{
		TlsMode:            protobuf.TlsMode_NoTls,
		ClusterModeEnabled: false,
		ReadFrom:           protobuf.ReadFrom_Primary,
	}

	result := config.toProtobufConnRequest()

	assert.Equal(t, expected, result)
}

func TestConnectionRequestProtobufGeneration_defaultClusterConfig(t *testing.T) {
	config := NewRedisClusterClientConfiguration()
	expected := &protobuf.ConnectionRequest{
		TlsMode:            protobuf.TlsMode_NoTls,
		ClusterModeEnabled: true,
		ReadFrom:           protobuf.ReadFrom_Primary,
	}

	result := config.toProtobufConnRequest()

	assert.Equal(t, expected, result)
}

func TestConnectionRequestProtobufGeneration_allFieldsSet(t *testing.T) {
	hosts := []string{"host1", "host2"}
	ports := []uint32{1234, 5678}
	username := "username"
	password := "password"
	var timeout uint32 = 3
	var retries, factor, base uint32 = 5, 10, 50
	var databaseId uint32 = 1

	config := NewRedisClientConfiguration().
		WithUseTLS(true).
		WithReadFrom(PREFER_REPLICA).
		WithCredentials(&RedisCredentials{username, password}).
		WithRequestTimeout(timeout).
		WithReconnectStrategy(&BackoffStrategy{retries, factor, base}).
		WithDatabaseId(databaseId)

	expected := &protobuf.ConnectionRequest{
		TlsMode:            protobuf.TlsMode_SecureTls,
		ReadFrom:           protobuf.ReadFrom_PreferReplica,
		ClusterModeEnabled: false,
		AuthenticationInfo: &protobuf.AuthenticationInfo{Username: username, Password: password},
		RequestTimeout:     timeout,
		ConnectionRetryStrategy: &protobuf.ConnectionRetryStrategy{
			NumberOfRetries: retries,
			Factor:          factor,
			ExponentBase:    base,
		},
		DatabaseId: databaseId,
	}

	assert.Equal(t, len(hosts), len(ports))
	for i := 0; i < len(hosts); i++ {
		config.WithAddress(&NodeAddress{hosts[i], ports[i]})
		expected.Addresses = append(
			expected.Addresses,
			&protobuf.NodeAddress{Host: hosts[i], Port: ports[i]},
		)
	}

	result := config.toProtobufConnRequest()

	assert.Equal(t, expected, result)
}
