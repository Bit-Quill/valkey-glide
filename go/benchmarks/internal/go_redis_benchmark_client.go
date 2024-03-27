// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package internal

import (
    "context"
    "crypto/tls"
    "errors"
    "fmt"
    "github.com/redis/go-redis/v9"
)

type GoRedisBenchmarkClient struct {
    client redis.Cmdable
}

func (goRedisClient *GoRedisBenchmarkClient) ConnectToRedis(connectionSettings *ConnectionSettings) error {

    if connectionSettings.ClusterModeEnabled {
        clusterOptions := &redis.ClusterOptions{
            Addrs: []string{fmt.Sprintf("%s:%d", connectionSettings.Host, connectionSettings.Port)},
        }

        if connectionSettings.UseTLS {
            clusterOptions.TLSConfig = &tls.Config{MinVersion: tls.VersionTLS12}
        }

        goRedisClient.client = redis.NewClusterClient(clusterOptions)
    } else {
        options := &redis.Options{
            Addr: fmt.Sprintf("%s:%d", connectionSettings.Host, connectionSettings.Port),
            DB:   0,
        }

        if connectionSettings.UseTLS {
            options.TLSConfig = &tls.Config{MinVersion: tls.VersionTLS12}
        }

        goRedisClient.client = redis.NewClient(options)
    }

    return goRedisClient.client.Ping(context.Background()).Err()
}

func (goRedisClient *GoRedisBenchmarkClient) Set(key string, value string) (string, error) {
    return goRedisClient.client.Set(context.Background(), key, value, 0).Result()
}

func (goRedisClient *GoRedisBenchmarkClient) Get(key string) (string, error) {
    value, err := goRedisClient.client.Get(context.Background(), key).Result()
    if err != nil && !errors.Is(err, redis.Nil) {
        return "", err
    }

    return value, nil
}

func (goRedisClient *GoRedisBenchmarkClient) CloseConnection() error {
    switch c := goRedisClient.client.(type) {
    case *redis.Client:
        return c.Close()
    case *redis.ClusterClient:
        return c.Close()
    default:
        return fmt.Errorf("unsupported client type")
    }
}

func (goRedisClient *GoRedisBenchmarkClient) GetName() string {
    return "go-redis"
}
