// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package integTest

import (
	"fmt"
	"github.com/stretchr/testify/suite"
	"log"
	"os"
	"os/exec"
	"strconv"
	"strings"
	"testing"
)

type GlideTestSuite struct {
	suite.Suite
	standalonePorts []int
	clusterPorts    []int
	redisVersion    string
}

func (suite *GlideTestSuite) SetupSuite() {
	// Stop cluster in case previous test run was interrupted or crashed and didn't stop.
	// If an error occurs, we ignore it in case the servers actually were stopped before running this.
	_ = exec.Command("python3", "../../utils/cluster_manager.py", "stop", "--prefix", "redis-cluster", "--keep-folder").Run()

	// Delete dirs if stop failed due to https://github.com/aws/glide-for-redis/issues/849
	err := os.RemoveAll("../../utils/clusters")
	if err != nil && !os.IsNotExist(err) {
		log.Fatal(err)
	}

	// Start standalone Redis instance
	output, err := exec.Command("python3", "../../utils/cluster_manager.py", "start", "-r", "0").CombinedOutput()
	if err != nil {
		suite.T().Fatal(fmt.Sprint(err) + ": " + string(output))
	}

	suite.standalonePorts = extractPorts(suite, string(output))
	suite.T().Logf("Standalone ports = %s", fmt.Sprint(suite.standalonePorts))

	// Start Redis cluster
	output, err = exec.Command("python3", "../../utils/cluster_manager.py", "start", "--cluster-mode").CombinedOutput()
	if err != nil {
		suite.T().Fatal(fmt.Sprint(err) + ": " + string(output))
	}

	suite.clusterPorts = extractPorts(suite, string(output))
	suite.T().Logf("Cluster ports = %s", fmt.Sprint(suite.clusterPorts))

	// Get Redis version
	output, err = exec.Command("redis-server", "-v").CombinedOutput()
	if err != nil {
		suite.T().Fatal(fmt.Sprint(err) + ": " + string(output))
	}

	suite.redisVersion = extractRedisVersion(string(output))
	suite.T().Logf("Redis version = %s", suite.redisVersion)
}

func extractPorts(suite *GlideTestSuite, output string) []int {
	var ports []int
	for _, line := range strings.Split(output, "\n") {
		if !strings.HasPrefix(line, "CLUSTER_NODES=") {
			continue
		}

		addresses := strings.Split(line, "=")[1]
		addressList := strings.Split(addresses, ",")
		for _, address := range addressList {
			portString := strings.Split(address, ":")[1]
			port, err := strconv.Atoi(portString)
			if err != nil {
				suite.T().Fatalf("Failed to parse port from cluster_manager.py output: %s", err.Error())
			}

			ports = append(ports, port)
		}
	}

	return ports
}

func extractRedisVersion(output string) string {
	// Expected line format:
	// Redis server v=7.2.3 sha=00000000:0 malloc=jemalloc-5.3.0 bits=64 build=7504b1fedf883f2
	versionSection := strings.Split(output, " ")[2]
	return strings.Split(versionSection, "=")[1]
}

func TestGlideTestSuite(t *testing.T) {
	suite.Run(t, new(GlideTestSuite))
}

func (suite *GlideTestSuite) TearDownSuite() {
	output, err := exec.Command("python3", "../../utils/cluster_manager.py", "stop", "--prefix", "redis-cluster", "--keep-folder").CombinedOutput()
	if err != nil {
		suite.T().Fatal(fmt.Sprint(err) + ": " + string(output))
	}
}
