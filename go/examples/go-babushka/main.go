package main

import (
	"fmt"
	"github.com/aws/babushka/go/babushkaclient"
	"github.com/aws/babushka/go/benchmarks"
)

func main() {
	fmt.Println("Starting go-babushka client...")
	client := babushkaclient.BabushkaRedisClient{}
	connectionSettings := benchmarks.NewConnectionSettings("localhost", 6379, false, false)

	err := client.ConnectToRedis(connectionSettings)
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

	err = client.CloseConnection()
	if err != nil {
		fmt.Println("Error closing the client:", err)
		return
	}

	fmt.Println("Disconnected from Redis")
}
