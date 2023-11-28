package benchmarks

import (
	"fmt"
	configproto "github.com/aws/babushka/go/babushkaclient-proto"
	"github.com/aws/babushka/go/babushkaclient-proto/generalclient"
)

type GoProtobufBenchmarkClient struct {
	coreClient generalclient.BaseClient
}

func (goProtobufBenchmarkClient *GoProtobufBenchmarkClient) ConnectToRedis(connectionSettings *ConnectionSettings) error {
	clientConfig := configproto.ClientConfiguration{}

	addressInfo := []configproto.AddressInfo{
		{Host: connectionSettings.Host, Port: uint32(connectionSettings.Port)},
	}
	clientConfig.Addresses = addressInfo
	clientConfig.UseTLS = connectionSettings.UseSsl

	goProtobufBenchmarkClient.coreClient.Config = clientConfig

	err := goProtobufBenchmarkClient.coreClient.ConnectToRedis(connectionSettings.ClusterModeEnabled)
	if err != nil {
		return err
	}
	return nil
}

func (goProtobufBenchmarkClient *GoProtobufBenchmarkClient) Get(key string) (string, error) {
	return goProtobufBenchmarkClient.coreClient.Get(key)
}

func (goProtobufBenchmarkClient *GoProtobufBenchmarkClient) Set(key string, value interface{}) error {
	str := fmt.Sprintf("%v", value)
	return goProtobufBenchmarkClient.coreClient.Set(key, str)
}

func (goProtobufBenchmarkClient *GoProtobufBenchmarkClient) CloseConnection() error {
	goProtobufBenchmarkClient.coreClient.CloseConnection()
	return nil
}

func (goProtobufBenchmarkClient *GoProtobufBenchmarkClient) GetName() string {
	return "babushka"
}
