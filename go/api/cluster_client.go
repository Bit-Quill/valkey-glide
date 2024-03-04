// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package api

type RedisClusterClient struct {
	baseClient
}

func CreateClusterClient(converter connectionRequestConverter) (*RedisClusterClient, error) {
	connPtr, err := createClient(converter)
	if err != nil {
		return nil, err
	}

	return &RedisClusterClient{baseClient{connPtr}}, nil
}

func (client *RedisClusterClient) Close() {
	client.baseClient.close()
}
