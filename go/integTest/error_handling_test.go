// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package integTest

import (
	"github.com/aws/glide-for-redis/go/glide/api"
	"github.com/stretchr/testify/assert"
)

func (suite *GlideTestSuite) TestConnectWithInvalidAddress() {
	config := api.NewRedisClientConfiguration().
		WithAddress(&api.NodeAddress{Host: "invalid-host"})
	client, err := api.CreateClient(config)

	assert.Nil(suite.T(), client)
	assert.NotNil(suite.T(), err)
	assert.IsType(suite.T(), &api.DisconnectError{}, err)
}
