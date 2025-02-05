package api

import (
	"fmt"
)

// getExampleGlideClient returns a GlideClient instance for testing purposes.
// This function is used in the examples of the GlideClient methods.
func getExampleGlideClient() *GlideClient {
	config := NewGlideClientConfiguration().
		WithAddress(new(NodeAddress)) // use default address

	client, err := NewGlideClient(config)
	if err != nil {
		fmt.Println("error connecting to database: ", err)
	}

	_, err = client.CustomCommand([]string{"FLUSHALL"}) // todo: replace with client.FlushAll() when implemented
	if err != nil {
		fmt.Println("error flushing database: ", err)
	}

	return client.(*GlideClient)
}

// getExampleGlideClusterClient returns a GlideClusterClient instance for testing purposes.
// This function is used in the examples of the GlideClusterClient methods.
func getExampleGlideClusterClient() *GlideClusterClient {
	nodeAddress := NodeAddress{"localhost", 7000}
	config := NewGlideClusterClientConfiguration().
		WithAddress(&nodeAddress) // use default address

	client, err := NewGlideClusterClient(config)
	if err != nil {
		fmt.Println("error connecting to database: ", err)
	}

	_, err = client.CustomCommand([]string{"FLUSHALL"}) // todo: replace with client.FlushAll() when implemented
	if err != nil {
		fmt.Println("error flushing database: ", err)
	}

	return client.(*GlideClusterClient)
}
