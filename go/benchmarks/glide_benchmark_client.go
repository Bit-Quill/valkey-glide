// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package main

import (
	"github.com/aws/glide-for-redis/go/glide/api"
)

type glideBenchmarkClient struct {
	client api.GlideClient
}

func (glideBenchmarkClient *glideBenchmarkClient) connect(connectionSettings *connectionSettings) error {
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

func (glideBenchmarkClient *glideBenchmarkClient) get(key string) (string, error) {
	return glideBenchmarkClient.client.Get(key)
}

func (glideBenchmarkClient *glideBenchmarkClient) set(key string, value string) (string, error) {
	return glideBenchmarkClient.client.Set(key, value)
}

func (glideBenchmarkClient *glideBenchmarkClient) close() error {
	glideBenchmarkClient.client.Close()
	return nil
}

func (glideBenchmarkClient *glideBenchmarkClient) getName() string {
	return "glide"
}
