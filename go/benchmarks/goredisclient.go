package benchmarks

import (
	"context"
	"crypto/tls"
	"fmt"
	"github.com/redis/go-redis/v9"
)

type GoRedisClient struct {
	coreClient redis.Cmdable
}

func (goRedisClient *GoRedisClient) ConnectToRedis(connectionSettings *ConnectionSettings) error {

	if connectionSettings.ClusterModeEnabled {
		clusterOptions := &redis.ClusterOptions{
			Addrs: []string{fmt.Sprintf("%s:%d", connectionSettings.Host, connectionSettings.Port)},
		}

		if connectionSettings.UseSsl {
			clusterOptions.TLSConfig = &tls.Config{MinVersion: tls.VersionTLS12}
		}

		goRedisClient.coreClient = redis.NewClusterClient(clusterOptions)
	} else {
		options := &redis.Options{
			Addr: fmt.Sprintf("%s:%d", connectionSettings.Host, connectionSettings.Port),
			DB:   0,
		}

		if connectionSettings.UseSsl {
			options.TLSConfig = &tls.Config{MinVersion: tls.VersionTLS12}
		}

		goRedisClient.coreClient = redis.NewClient(options)
	}

	return goRedisClient.coreClient.Ping(context.Background()).Err()
}

func (goRedisClient *GoRedisClient) Set(key string, value interface{}) error {
	return goRedisClient.coreClient.Set(context.Background(), key, value, 0).Err()
}

func (goRedisClient *GoRedisClient) Get(key string) (string, error) {
	value, err := goRedisClient.coreClient.Get(context.Background(), key).Result()
	if err != nil {
		return "", err
	}
	return value, nil
}

func (goRedisClient *GoRedisClient) CloseConnection() error {
	switch c := goRedisClient.coreClient.(type) {
	case *redis.Client:
		return c.Close()
	case *redis.ClusterClient:
		return c.Close()
	default:
		return fmt.Errorf("unsupported client type")
	}
}

func (goRedisClient *GoRedisClient) GetName() string {
	return "GoRedis"
}
