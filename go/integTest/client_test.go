// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package integTest

import (
	"github.com/aws/glide-for-redis/go/glide/api"
	"github.com/stretchr/testify/assert"
)

func (suite *GlideTestSuite) TestStandaloneConnect() {
	config := api.NewRedisClientConfiguration().
		WithAddress(&api.NodeAddress{Port: suite.standalonePorts[0]})
	client, err := api.CreateClient(config)

	assert.Nil(suite.T(), err)
	assert.NotNil(suite.T(), client)

	client.Close()
}

func (suite *GlideTestSuite) TestClusterConnect() {
	config := api.NewRedisClientConfiguration()
	for _, port := range suite.clusterPorts {
		config.WithAddress(&api.NodeAddress{Port: port})
	}

	client, err := api.CreateClient(config)

	assert.Nil(suite.T(), err)
	assert.NotNil(suite.T(), client)

	client.Close()
}
