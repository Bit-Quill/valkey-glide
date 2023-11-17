package benchmarks

import (
	"github.com/aws/babushka/go/babushkaclient"
)

type GoBabushkaBenchmarkClient struct {
	coreClient babushkaclient.BabushkaRedisClient
}

func (goBabushkaBenchmarkClient *GoBabushkaBenchmarkClient) ConnectToRedis(connectionSettings *ConnectionSettings) error {
	err := goBabushkaBenchmarkClient.coreClient.ConnectToRedis(connectionSettings.Host, connectionSettings.Port, connectionSettings.UseSsl, connectionSettings.ClusterModeEnabled)
	if err != nil {
		return err
	}
	return nil
}

func (goBabushkaBenchmarkClient *GoBabushkaBenchmarkClient) Get(key string) (string, error) {
	return goBabushkaBenchmarkClient.coreClient.Get(key)
}

func (goBabushkaBenchmarkClient *GoBabushkaBenchmarkClient) Set(key string, value interface{}) error {
	return goBabushkaBenchmarkClient.coreClient.Set(key, value)
}

func (goBabushkaBenchmarkClient *GoBabushkaBenchmarkClient) CloseConnection() error {
	goBabushkaBenchmarkClient.coreClient.CloseConnection()
	return nil
}

func (goBabushkaBenchmarkClient *GoBabushkaBenchmarkClient) GetName() string {
	return "babushka"
}
