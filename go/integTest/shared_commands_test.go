// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package integTest

import (
	"fmt"
	"github.com/stretchr/testify/assert"
	"testing"
)

func (suite *GlideTestSuite) TestCustomCommand() {
	clients := suite.getClients()

	for i, client := range clients {
		suite.T().Run(fmt.Sprintf("Testing [%v]", i), func(t *testing.T) {
			result, err := client.CustomCommand([]string{"ping"})

			assert.Nil(suite.T(), err)
			assert.Equal(suite.T(), "PONG", result)
		})
	}
}
