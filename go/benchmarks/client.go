package benchmarks

type Client interface {
	ConnectToRedis(connectionSettings *ConnectionSettings) error
	Set(key string, value interface{}) error
	Get(key string) (string, error)
	CloseConnection() error
	GetName() string
}
