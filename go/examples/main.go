// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package examples

import (
	"fmt"
	"github.com/aws/glide-for-redis/go/glide/api"
)

func main() {
	fmt.Println("Starting go-glide client...")
	config := api.NewRedisClientConfiguration()

	client, err := api.CreateClient(config)
	if err != nil {
		panic(err)
	}

	//err = client.Set("FOO", "BAR")
	//if err != nil {
	//	panic(err)
	//}
	//fmt.Println("SET FOO : BAR")
	//
	//val, err := client.Get("FOO")
	//if err != nil {
	//	panic(err)
	//}
	//fmt.Println("GET FOO :", val)

	client.Close()

	fmt.Println("Disconnected from Redis")
}
