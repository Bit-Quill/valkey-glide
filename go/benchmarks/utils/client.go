package utils

import (
	"context"
)

type Client interface {
	ConnectToRedis(ctx context.Context, connectionSettings *ConnectionSettings) error
	Set(ctx context.Context, key string, value interface{}) error
	Get(ctx context.Context, key string) (string, error)
	CloseConnection() error
	GetName() string
}
