// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package integTest

import (
	"github.com/aws/glide-for-redis/go/glide/api"
	"github.com/stretchr/testify/assert"
)

func (suite *GlideTestSuite) TestInvalidCustomCommand() {
	client := suite.createClient()

	result, err := client.CustomCommand([]string{"pewpew"})

	assert.Nil(suite.T(), result)
	assert.NotNil(suite.T(), err)
	assert.IsType(suite.T(), &api.RequestError{}, err)
}

func (suite *GlideTestSuite) TestInvalidCustomCommandArgs() {
	client := suite.createClient()

	result, err := client.CustomCommand([]string{"ping", "pang", "pong"})

	assert.Nil(suite.T(), result)
	assert.NotNil(suite.T(), err)
	assert.IsType(suite.T(), &api.RequestError{}, err)
}
