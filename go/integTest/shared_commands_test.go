// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package integTest

import (
	"fmt"
	"strings"

	"github.com/aws/glide-for-redis/go/glide/api"
	"github.com/stretchr/testify/assert"
)

func (suite *GlideTestSuite) TestCustomCommandPing() {
	suite.runWithDefaultClients(func(client api.BaseClient) {
		result, err := client.CustomCommand([]string{"PING"})

		assert.Nil(suite.T(), err)
		assert.Equal(suite.T(), "PONG", result)
	})
}

func (suite *GlideTestSuite) TestCustomCommandInfo() {
	suite.runWithDefaultClients(func(client api.BaseClient) {
		result, err := client.CustomCommand([]string{"INFO"})

		assert.Nil(suite.T(), err)
		assert.IsType(suite.T(), "", result)
		strResult := result.(string)
		assert.True(suite.T(), strings.Contains(strResult, "# Stats"))
	})
}

func (suite *GlideTestSuite) TestCustomCommandClientInfo() {
	clientName := "TEST_CLIENT_NAME"
	config := api.NewRedisClientConfiguration().
		WithAddress(&api.NodeAddress{Port: suite.standalonePorts[0]}).
		WithClientName(clientName)
	clusterConfig := api.NewRedisClusterClientConfiguration().
		WithAddress(&api.NodeAddress{Port: suite.clusterPorts[0]}).
		WithClientName(clientName)
	clients := []api.BaseClient{suite.client(config), suite.clusterClient(clusterConfig)}

	suite.runWithClients(clients, func(client api.BaseClient) {
		result, err := client.CustomCommand([]string{"CLIENT", "INFO"})

		assert.Nil(suite.T(), err)
		assert.IsType(suite.T(), "", result)
		strResult := result.(string)
		assert.True(suite.T(), strings.Contains(strResult, fmt.Sprintf("name=%s", clientName)))
	})
}

func (suite *GlideTestSuite) TestCustomCommand_invalidCommand() {
	suite.runWithDefaultClients(func(client api.BaseClient) {
		result, err := client.CustomCommand([]string{"pewpew"})

		assert.Nil(suite.T(), result)
		assert.NotNil(suite.T(), err)
		assert.IsType(suite.T(), &api.RequestError{}, err)
	})
}

func (suite *GlideTestSuite) TestCustomCommand_invalidArgs() {
	suite.runWithDefaultClients(func(client api.BaseClient) {
		result, err := client.CustomCommand([]string{"ping", "pang", "pong"})

		assert.Nil(suite.T(), result)
		assert.NotNil(suite.T(), err)
		assert.IsType(suite.T(), &api.RequestError{}, err)
	})
}

func (suite *GlideTestSuite) TestCommandOnClosedClient() {
	suite.runWithDefaultClients(func(client api.BaseClient) {
		client.Close()
		result, err := client.CustomCommand([]string{"ping"})

		assert.Nil(suite.T(), result)
		assert.NotNil(suite.T(), err)
		assert.IsType(suite.T(), &api.DisconnectError{}, err)
	})
}
