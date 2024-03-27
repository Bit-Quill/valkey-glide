// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package internal

import (
	"github.com/aws/glide-for-redis/go/glide/api"
)

type GlideBenchmarkClient struct {
	client api.GlideClient
}

func (glideBenchmarkClient *GlideBenchmarkClient) ConnectToRedis(connectionSettings *ConnectionSettings) error {
	if connectionSettings.ClusterModeEnabled {
		config := api.NewRedisClusterClientConfiguration().
			WithAddress(&api.NodeAddress{Host: connectionSettings.Host, Port: connectionSettings.Port}).
			WithUseTLS(connectionSettings.UseTLS)
		glideClient, err := api.NewRedisClusterClient(config)
		if err != nil {
			return err
		}

		glideBenchmarkClient.client = glideClient
		return nil
	} else {
		config := api.NewRedisClientConfiguration().
			WithAddress(&api.NodeAddress{Host: connectionSettings.Host, Port: connectionSettings.Port}).
			WithUseTLS(connectionSettings.UseTLS)
		glideClient, err := api.NewRedisClient(config)
		if err != nil {
			return err
		}

		glideBenchmarkClient.client = glideClient
		return nil
	}
}

func (glideBenchmarkClient *GlideBenchmarkClient) Get(key string) (string, error) {
	return glideBenchmarkClient.client.Get(key)
}

func (glideBenchmarkClient *GlideBenchmarkClient) Set(key string, value string) (string, error) {
	return glideBenchmarkClient.client.Set(key, value)
}

func (glideBenchmarkClient *GlideBenchmarkClient) CloseConnection() error {
	glideBenchmarkClient.client.Close()
	return nil
}

func (glideBenchmarkClient *GlideBenchmarkClient) GetName() string {
	return "glide"
}
