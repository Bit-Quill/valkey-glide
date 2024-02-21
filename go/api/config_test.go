/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */

package api

import (
	"testing"

	"github.com/aws/glide-for-redis/go/glide/protobuf"
	"github.com/stretchr/testify/assert"
)

func TestConnectionRequestProtobufGeneration_defaultStandaloneConfig_withConstructor(t *testing.T) {
	config := NewRedisClientConfiguration()
	expected := &protobuf.ConnectionRequest{
		TlsMode:            protobuf.TlsMode_NoTls,
		ClusterModeEnabled: false,
		ReadFrom:           protobuf.ReadFrom_Primary,
	}

	result, err := config.toProtobufConnRequest()

	assert.Nil(t, err)
	assert.Equal(t, expected, result)
}

func TestConnectionRequestProtobufGeneration_defaultStandaloneConfig_withoutConstructor(
	t *testing.T,
) {
	config := RedisClientConfiguration{}
	expected := &protobuf.ConnectionRequest{
		TlsMode:            protobuf.TlsMode_NoTls,
		ClusterModeEnabled: false,
		ReadFrom:           protobuf.ReadFrom_Primary,
	}

	result, err := config.toProtobufConnRequest()

	assert.Nil(t, err)
	assert.Equal(t, expected, result)
}

func TestConnectionRequestProtobufGeneration_defaultClusterConfig_withConstructor(t *testing.T) {
	config := NewRedisClusterClientConfiguration()
	expected := &protobuf.ConnectionRequest{
		TlsMode:            protobuf.TlsMode_NoTls,
		ClusterModeEnabled: true,
		ReadFrom:           protobuf.ReadFrom_Primary,
	}

	result, err := config.toProtobufConnRequest()

	assert.Nil(t, err)
	assert.Equal(t, expected, result)
}

func TestConnectionRequestProtobufGeneration_defaultClusterConfig_withoutConstructor(t *testing.T) {
	config := RedisClusterClientConfiguration{}
	expected := &protobuf.ConnectionRequest{
		TlsMode:            protobuf.TlsMode_NoTls,
		ClusterModeEnabled: true,
		ReadFrom:           protobuf.ReadFrom_Primary,
	}

	result, err := config.toProtobufConnRequest()

	assert.Nil(t, err)
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
		WithReadFrom(PreferReplica).
		WithCredentials(&RedisCredentials{&username, &password}).
		WithRequestTimeout(timeout).
		WithReconnectStrategy(&BackoffStrategy{&retries, &factor, &base}).
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
		config.WithAddress(&NodeAddress{&hosts[i], &ports[i]})
		expected.Addresses = append(
			expected.Addresses,
			&protobuf.NodeAddress{Host: hosts[i], Port: ports[i]},
		)
	}

	result, err := config.toProtobufConnRequest()

	assert.Nil(t, err)
	assert.Equal(t, expected, result)
}

func TestConnectionRequestProtobufGeneration_invalidCredentials(t *testing.T) {
	username := "username"
	config := NewRedisClientConfiguration().
		WithCredentials(&RedisCredentials{Username: &username})

	result, err := config.toProtobufConnRequest()

	assert.Nil(t, result)
	assert.NotNil(t, err)
	assert.IsType(t, &RedisError{}, err)
}

func TestConnectionRequestProtobufGeneration_invalidBackoffStrategy(t *testing.T) {
	numRetries := uint32(1)
	factor := uint32(1)
	exponentBase := uint32(1)

	missingNumOfRetries := BackoffStrategy{
		Factor:       &factor,
		ExponentBase: &exponentBase,
	}

	missingFactor := BackoffStrategy{
		NumOfRetries: &numRetries,
		ExponentBase: &exponentBase,
	}

	missingExponentBase := BackoffStrategy{
		NumOfRetries: &numRetries,
		Factor:       &factor,
	}

	invalidStrategies := [3]BackoffStrategy{missingNumOfRetries, missingFactor, missingExponentBase}
	for _, strategy := range invalidStrategies {
		config := NewRedisClientConfiguration().WithReconnectStrategy(&strategy)

		result, err := config.toProtobufConnRequest()

		assert.Nil(t, result)
		assert.NotNil(t, err)
		assert.IsType(t, &RedisError{}, err)
	}
}
