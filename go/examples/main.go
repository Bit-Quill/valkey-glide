// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package examples

import (
    "github.com/aws/glide-for-redis/go/glide/api"
    "log"
)

func main() {
    host := "localhost"
    port := 6379

    config := api.NewRedisClientConfiguration().
        WithAddress(&api.NodeAddress{host, port})

    client, err := api.CreateClient(config)
    if err != nil {
        log.Fatal("error connecting to database: ", err)
    }

    client.Close()
}
