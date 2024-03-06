// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package api

// RedisClusterClient is a client used for connection to cluster Redis servers.
type RedisClusterClient struct {
	baseClient
}

// CreateClusterClient creates a Redis client in cluster mode using the given [RedisClusterClientConfiguration].
func CreateClusterClient(config RedisClusterClientConfiguration) (*RedisClusterClient, error) {
	connPtr, err := createClient(&config)
	if err != nil {
		return nil, err
	}

	return &RedisClusterClient{baseClient{connPtr}}, nil
}
