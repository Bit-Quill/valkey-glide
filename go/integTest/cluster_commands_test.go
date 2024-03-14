// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package integTest

import "github.com/stretchr/testify/assert"

func (suite *GlideTestSuite) TestClusterCustomCommand() {
	client := suite.createClusterClient()

	result, err := client.CustomCommand([]string{"ping"})

	assert.Nil(suite.T(), err)
	assert.Equal(suite.T(), "PONG", result)

	client.Close()
}
