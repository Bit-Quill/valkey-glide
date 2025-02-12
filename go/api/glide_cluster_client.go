// Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0

package api

// #cgo LDFLAGS: -lglide_rs
// #cgo !windows LDFLAGS: -lm
// #cgo darwin LDFLAGS: -framework Security
// #cgo linux,amd64 LDFLAGS: -L${SRCDIR}/../rustbin/x86_64-unknown-linux-gnu
// #cgo linux,arm64 LDFLAGS: -L${SRCDIR}/../rustbin/aarch64-unknown-linux-gnu
// #cgo darwin,arm64 LDFLAGS: -L${SRCDIR}/../rustbin/aarch64-apple-darwin
// #cgo LDFLAGS: -L../target/release
// #include "../lib.h"
import "C"

import (
	"github.com/valkey-io/valkey-glide/go/api/config"
	"github.com/valkey-io/valkey-glide/go/api/options"
)

// GlideClusterClient interface compliance check.
var _ GlideClusterClientCommands = (*GlideClusterClient)(nil)

// GlideClusterClientCommands is a client used for connection in cluster mode.
type GlideClusterClientCommands interface {
	BaseClient
	GenericClusterCommands
	ServerManagementClusterCommands
	ConnectionManagementClusterCommands
}

// GlideClusterClient implements cluster mode operations by extending baseClient functionality.
type GlideClusterClient struct {
	*baseClient
}

// NewGlideClusterClient creates a [GlideClusterClientCommands] in cluster mode using the given
// [GlideClusterClientConfiguration].
func NewGlideClusterClient(config *GlideClusterClientConfiguration) (GlideClusterClientCommands, error) {
	client, err := createClient(config)
	if err != nil {
		return nil, err
	}

	return &GlideClusterClient{client}, nil
}

// CustomCommand executes a single command, specified by args, without checking inputs. Every part of the command,
// including the command name and subcommands, should be added as a separate value in args. The returning value depends on
// the executed
// command.
//
// The command will be routed automatically based on the passed command's default request policy.
//
// See [Valkey GLIDE Wiki] for details on the restrictions and limitations of the custom command API.
//
// This function should only be used for single-response commands. Commands that don't return complete response and awaits
// (such as SUBSCRIBE), or that return potentially more than a single response (such as XREAD), or that change the client's
// behavior (such as entering pub/sub mode on RESP2 connections) shouldn't be called using this function.
//
// Parameters:
//
//	args - Arguments for the custom command including the command name.
//
// Return value:
//
//	The returned value for the custom command.
//
// For example:
//
//	result, err := client.CustomCommand([]string{"ping"})
//	result.Value().(string): "PONG"
//
// [Valkey GLIDE Wiki]: https://github.com/valkey-io/valkey-glide/wiki/General-Concepts#custom-command
func (client *GlideClusterClient) CustomCommand(args []string) (ClusterValue[interface{}], error) {
	res, err := client.executeCommand(C.CustomCommand, args)
	if err != nil {
		return createEmptyClusterValue[interface{}](), err
	}
	data, err := handleInterfaceResponse(res)
	if err != nil {
		return createEmptyClusterValue[interface{}](), err
	}
	return createClusterValue[interface{}](data), nil
}

// Gets information and statistics about the server.
//
// The command will be routed to all primary nodes.
//
// See [valkey.io] for details.
//
// Return value:
//
//	A map where each address is the key and its corresponding node response is the information for the default sections.
//
// Example:
//
//	response, err := clusterClient.Info(opts)
//	if err != nil {
//		// handle error
//	}
//	for node, data := range response {
//		fmt.Printf("%s node returned %s\n", node, data)
//	}
//
// [valkey.io]: https://valkey.io/commands/info/
func (client *GlideClusterClient) Info() (map[string]string, error) {
	result, err := client.executeCommand(C.Info, []string{})
	if err != nil {
		return nil, err
	}

	return handleStringToStringMapResponse(result)
}

// Gets information and statistics about the server.
//
// The command will be routed to all primary nodes, unless `route` in [ClusterInfoOptions] is provided.
//
// See [valkey.io] for details.
//
// Parameters:
//
//	options - Additional command parameters, see [ClusterInfoOptions] for more details.
//
// Return value:
//
//	When specifying a route other than a single node or when route is not given,
//	it returns a map where each address is the key and its corresponding node response is the value.
//	When a single node route is given, command returns a string containing the information for the sections requested.
//
// Example:
//
//	opts := options.ClusterInfoOptions{
//		InfoOptions: &options.InfoOptions{Sections: []options.Section{options.Server}},
//		RouteOption: &options.RouteOption{Route: config.RandomRoute},
//	}
//	response, err := clusterClient.InfoWithOptions(opts)
//	if err != nil {
//		// handle error
//	}
//	// Command sent to a single random node via RANDOM route, expecting SingleValue result as a `string`.
//	fmt.Println(response.SingleValue())
//
// [valkey.io]: https://valkey.io/commands/info/
func (client *GlideClusterClient) InfoWithOptions(options options.ClusterInfoOptions) (ClusterValue[string], error) {
	optionArgs, err := options.ToArgs()
	if err != nil {
		return createEmptyClusterValue[string](), err
	}
	if options.RouteOption == nil || options.RouteOption.Route == nil {
		response, err := client.executeCommand(C.Info, optionArgs)
		if err != nil {
			return createEmptyClusterValue[string](), err
		}
		data, err := handleStringToStringMapResponse(response)
		if err != nil {
			return createEmptyClusterValue[string](), err
		}
		return createClusterMultiValue[string](data), nil
	}
	response, err := client.executeCommandWithRoute(C.Info, optionArgs, options.Route)
	if err != nil {
		return createEmptyClusterValue[string](), err
	}
	if options.Route.IsMultiNode() {
		data, err := handleStringToStringMapResponse(response)
		if err != nil {
			return createEmptyClusterValue[string](), err
		}
		return createClusterMultiValue[string](data), nil
	}
	data, err := handleStringResponse(response)
	if err != nil {
		return createEmptyClusterValue[string](), err
	}
	return createClusterSingleValue[string](data), nil
}

// CustomCommandWithRoute executes a single command, specified by args, without checking inputs. Every part of the command,
// including the command name and subcommands, should be added as a separate value in args. The returning value depends on
// the executed command.
//
// See [Valkey GLIDE Wiki] for details on the restrictions and limitations of the custom command API.
//
// Parameters:
//
//	args  - Arguments for the custom command including the command name.
//	route - Specifies the routing configuration for the command. The client will route the
//	        command to the nodes defined by route.
//
// Return value:
//
//	The returning value depends on the executed command and route.
//
// For example:
//
//	route := config.SimpleNodeRoute(config.RandomRoute)
//	result, err := client.CustomCommandWithRoute([]string{"ping"}, route)
//	result.SingleValue().(string): "ping"
//
// [Valkey GLIDE Wiki]: https://github.com/valkey-io/valkey-glide/wiki/General-Concepts#custom-command
func (client *GlideClusterClient) CustomCommandWithRoute(
	args []string,
	route config.Route,
) (ClusterValue[interface{}], error) {
	res, err := client.executeCommandWithRoute(C.CustomCommand, args, route)
	if err != nil {
		return createEmptyClusterValue[interface{}](), err
	}
	data, err := handleInterfaceResponse(res)
	if err != nil {
		return createEmptyClusterValue[interface{}](), err
	}
	return createClusterValue[interface{}](data), nil
}

// Pings the server.
// The command will be routed to all primary nodes.
//
// Return value:
//
// Returns "PONG".
//
// For example:
//
//	result, err := clusterClient.Ping()
//	fmt.Println(result) // Output: PONG
//
// [valkey.io]: https://valkey.io/commands/ping/
func (client *GlideClusterClient) Ping() (string, error) {
	result, err := client.executeCommand(C.Ping, []string{})
	if err != nil {
		return defaultStringResponse, err
	}
	return handleStringResponse(result)
}

// Pings the server.
// The command will be routed to all primary nodes, unless `Route` is provided in `pingOptions`.
//
// Parameters:
//
//	pingOptions - The [ClusterPingOptions] type.
//
// Return value:
//
//	Returns the copy of message.
//
// For example:
//
//	route := options.RouteOption{config.RandomRoute}
//	opts  := options.ClusterPingOptions{ &options.PingOptions{ "Hello" }, &route }
//	result, err := clusterClient.PingWithOptions(opts)
//	fmt.Println(result) // Output: Hello
//
// [valkey.io]: https://valkey.io/commands/ping/
func (client *GlideClusterClient) PingWithOptions(pingOptions options.ClusterPingOptions) (string, error) {
	args, err := pingOptions.ToArgs()
	if err != nil {
		return defaultStringResponse, err
	}
	if pingOptions.RouteOption == nil || pingOptions.RouteOption.Route == nil {
		response, err := client.executeCommand(C.Ping, args)
		if err != nil {
			return defaultStringResponse, err
		}
		return handleStringResponse(response)
	}

	response, err := client.executeCommandWithRoute(C.Ping, args, pingOptions.Route)
	if err != nil {
		return defaultStringResponse, err
	}

	return handleStringResponse(response)
}

// Returns the server time.
// The command will be routed to a random node, unless Route in opts is provided.
//
// See [valkey.io] for details.
//
// Parameters:
//
//	options - The [RouteOption] type.
//
// Return value:
//
// The current server time as a String array with two elements: A UNIX TIME and the amount
// of microseconds already elapsed in the current second.
// The returned array is in a [UNIX TIME, Microseconds already elapsed] format.
//
// Example:
//
//	opts := options.RouteOption{config.RandomRoute}
//	response := client.TimeWithOptions(opts)
//	fmt.Println(response.SingleValue()) // Output: [1737994354 547816]
//
// [valkey.io]: https://valkey.io/commands/time/
func (client *GlideClusterClient) TimeWithOptions(opts options.RouteOption) (ClusterValue[[]string], error) {
	result, err := client.executeCommandWithRoute(C.Time, []string{}, opts.Route)
	if err != nil {
		return createEmptyClusterValue[[]string](), err
	}
	return handleTimeClusterResponse(result)
}

// Returns the number of keys in the database.
//
// Parameters:
//
//	options - The [RouteOption] type.
//
// Return value:
//
//	The number of keys in the database.
//
// Example:
//
//	options := options.RouteOption{config.RandomRoute}
//	result, err := client.DBSizeWithOption(options)
//	if err != nil {
//	  // handle error
//	}
//	fmt.Println(result) // Output: 1
//
// [valkey.io]: https://valkey.io/commands/dbsize/
func (client *GlideClusterClient) DBSizeWithOptions(opts options.RouteOption) (int64, error) {
	result, err := client.executeCommandWithRoute(C.DBSize, []string{}, opts.Route)
	if err != nil {
		return defaultIntResponse, err
	}
	return handleIntResponse(result)
}

// Echo the provided message back.
// The command will be routed a random node, unless `Route` in `echoOptions` is provided.
//
// Parameters:
//
//	echoOptions - The [ClusterEchoOptions] type.
//
// Return value:
//
//	A map where each address is the key and its corresponding node response is the information for the default sections.
//
// Example:
//
//	options := options.ClusterEchoOptions{&options.EchoOptions{"Hi"}, &options.RouteOption{config.AllNodes}}
//	response, err := clusterClient.EchoWithOptions(options)
//	if err != nil {
//		// handle error
//	}
//	for node, data := range response {
//		fmt.Printf("%s node returned %s\n", node, data)
//	}
//
// [valkey.io]: https://valkey.io/commands/echo/
func (client *GlideClusterClient) EchoWithOptions(echoOptions options.ClusterEchoOptions) (ClusterValue[string], error) {
	args, err := echoOptions.ToArgs()
	if err != nil {
		return createEmptyClusterValue[string](), err
	}
	var route config.Route
	if echoOptions.RouteOption != nil && echoOptions.RouteOption.Route != nil {
		route = echoOptions.RouteOption.Route
	}
	response, err := client.executeCommandWithRoute(C.Echo, args, route)
	if err != nil {
		return createEmptyClusterValue[string](), err
	}
	if echoOptions.RouteOption != nil && echoOptions.RouteOption.Route != nil &&
		(echoOptions.RouteOption.Route).IsMultiNode() {
		data, err := handleStringToStringMapResponse(response)
		if err != nil {
			return createEmptyClusterValue[string](), err
		}
		return createClusterMultiValue[string](data), nil
	}
	data, err := handleStringResponse(response)
	if err != nil {
		return createEmptyClusterValue[string](), err
	}
	return createClusterSingleValue[string](data), nil
}
