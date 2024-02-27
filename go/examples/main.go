package main

import (
	"fmt"
	"github.com/aws/glide-for-redis/go/glide/glide"
)

func main() {
	fmt.Println("Starting go-glide client...")
	client := glide.GlideRedisClient{}

	err := client.ConnectToRedis("localhost", 6379, false, false)
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
