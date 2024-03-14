// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package integTest

import (
	"github.com/aws/glide-for-redis/go/glide/api"
	"github.com/stretchr/testify/assert"
)

func (suite *GlideTestSuite) TestCustomCommand() {
	client := suite.createClient()

	result, err := client.CustomCommand([]string{"ping"})

	assert.Nil(suite.T(), err)
	assert.Equal(suite.T(), "PONG", result)

	client.Close()
}

func (suite *GlideTestSuite) TestInvalidCustomCommand() {
	client := suite.createClient()

	result, err := client.CustomCommand([]string{"pewpew"})

	assert.Nil(suite.T(), result)
	assert.NotNil(suite.T(), err)
	assert.IsType(suite.T(), &api.RequestError{}, err)

	client.Close()
}

func (suite *GlideTestSuite) TestInvalidCustomCommandArgs() {
	client := suite.createClient()

	result, err := client.CustomCommand([]string{"ping", "pang", "pong"})

	assert.Nil(suite.T(), result)
	assert.NotNil(suite.T(), err)
	assert.IsType(suite.T(), &api.RequestError{}, err)

	client.Close()
}
