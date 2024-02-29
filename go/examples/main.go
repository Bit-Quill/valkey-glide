/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */
package main

import (
	"fmt"
	"github.com/aws/glide-for-redis/go/glide/glide"
	"github.com/aws/glide-for-redis/go/glide/protobuf"
)

func main() {
	fmt.Println("Starting go-glide client...")
	client := glide.GlideRedisClient{}

	request := &protobuf.ConnectionRequest{
		TlsMode:            protobuf.TlsMode_NoTls,
		ClusterModeEnabled: false,
		ReadFrom:           protobuf.ReadFrom_Primary,
	}
        request.Addresses = append(
		request.Addresses,
		&protobuf.NodeAddress{Host: "localhost", Port: uint32(6379)},
	)
	err := client.ConnectToRedis(request)
	if err != nil {
		return
	}

	err = client.Set("FOO", "BAR")
	if err != nil {
		panic(err)
	}
	fmt.Println("SET FOO : BAR")

	val, err := client.Get("FOO")
	if err != nil {
		panic(err)
	}
	fmt.Println("GET FOO :", val)

	client.CloseClient()

	fmt.Println("Disconnected from Redis")
}
