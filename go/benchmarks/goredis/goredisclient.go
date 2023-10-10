package goredisclient

import (
	"context"
	"crypto/tls"
	"fmt"
	"github.com/aws/babushka/go/benchmarks/utils"
	"github.com/redis/go-redis/v9"
)

type GoRedisClient struct {
	coreClient *redis.Client
}

func (goRedisClient *GoRedisClient) ConnectToRedis(ctx context.Context, connectionSettings *utils.ConnectionSettings) error {
	options := &redis.Options{
		Addr: fmt.Sprintf("%s:%d", connectionSettings.Host, connectionSettings.Port),
		DB:   0,
	}

	if connectionSettings.UseSsl {
		options.TLSConfig = &tls.Config{MinVersion: tls.VersionTLS12}
	}

	goRedisClient.coreClient = redis.NewClient(options)

	return goRedisClient.coreClient.Ping(ctx).Err()
}

func (goRedisClient *GoRedisClient) Set(ctx context.Context, key string, value interface{}) error {
	err := goRedisClient.coreClient.Set(ctx, key, value, 0).Err()
	return err
}

func (goRedisClient *GoRedisClient) Get(ctx context.Context, key string) (string, error) {
	value, err := goRedisClient.coreClient.Get(ctx, key).Result()
	if err != nil {
		return "", err
	}
	return value, nil
}

func (goRedisClient *GoRedisClient) CloseConnection() error {
	return goRedisClient.coreClient.Close()
}

func (goRedisClient *GoRedisClient) GetName() string {
	return "GoRedis"
}
