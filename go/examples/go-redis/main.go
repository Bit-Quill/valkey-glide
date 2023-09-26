package main

import (
	"context"
	"fmt"
	"github.com/redis/go-redis/v9"
)

func main() {
	fmt.Println("Starting go-redis client...")
	client := redis.NewClient(&redis.Options{
		Addr:     "localhost:6379",
		Password: "", // no password set
		DB:       0,  // use default DB
	})

	fmt.Println("Client PING")
	ctx := context.Background()

	pong, err := client.Ping(ctx).Result()
	if err != nil {
		panic(err)
	}

	if pong == "PONG" {
		fmt.Println("Server PONG")
	}

	err = client.Set(ctx, "FOO", "BAR", 0).Err()
	if err != nil {
		panic(err)
	}
	fmt.Println("SET FOO : BAR")

	val, err := client.Get(ctx, "FOO").Result()
	if err != nil {
		panic(err)
	}
	fmt.Println("GET FOO : ", val)
}
