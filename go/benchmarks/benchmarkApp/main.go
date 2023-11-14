package main

import (
	"flag"
	"fmt"
	"github.com/aws/babushka/go/benchmarks"
	"github.com/aws/babushka/go/benchmarks/babushkaclient"
	"os"
	"regexp"
	"strconv"
	"strings"
)

/*
options represents the set of arguments passed into the program.
It's responsible for parsing and holding the raw values provided by the user.
If some arguments are not passed, options will set default values for them.
*/
type options struct {
	tls                bool
	clusterModeEnabled bool
	host               string
	port               int
	resultsFile        string
	clientCount        string
	clientNames        string
	configuration      string
	concurrentTasks    string
	dataSize           string
}

/*
runConfiguration takes the parsed options and matches them,
ensuring their validity and converting them to the appropriate types
for the program's execution requirements.
*/
type runConfiguration struct {
	tls                bool
	clusterModeEnabled bool
	host               string
	port               int
	resultsFile        *os.File
	clientCount        []int
	clientNames        []string
	configuration      string
	concurrentTasks    []int
	dataSize           []int
}

type clientNameOptions struct {
	goRedis    string
	goBabushka string
	all        string
}

var clientNameOpts = clientNameOptions{
	goRedis:    "go-redis",
	goBabushka: "go-babushka",
	all:        "all",
}

func main() {
	opts := parseArguments()

	runConfig, err := verifyOptions(opts)
	if err != nil {
		fmt.Println("Error verifying options: ", err)
		return
	}

	defer closeFileIfNotStdout(runConfig.resultsFile)

	err = runBenchmarks(runConfig)
	if err != nil {
		fmt.Println("Error running benchmarking: ", err)
		return
	}

}

func parseArguments() *options {
	var opts options

	tls := flag.Bool("tls", false, "Use TLS (default: false)")
	clusterModeEnabled := flag.Bool("clusterModeEnabled", false, "Use ClusterModeEnabled (default: false)")
	host := flag.String("host", "localhost", "Host address")
	port := flag.Int("port", 6379, "Port number")
	resultsFile := flag.String("resultsFile", "", "Path to results file")
	clientCount := flag.String("clientCount", "[1]", "Client Count")
	clientNames := flag.String("clients", "all", "One of: all|go-redis|go-babushka")
	configuration := flag.String("configuration", "Release", "Configuration flag")
	concurrentTasks := flag.String("concurrentTasks", "[1 10 100]", "Number of concurrent tasks")
	dataSize := flag.String("dataSize", "[100 4000]", "Data block size")

	flag.Parse()

	opts.tls = *tls
	opts.clusterModeEnabled = *clusterModeEnabled
	opts.host = *host
	opts.port = *port
	opts.resultsFile = *resultsFile
	opts.clientCount = *clientCount
	opts.clientNames = *clientNames
	opts.configuration = *configuration
	opts.concurrentTasks = *concurrentTasks
	opts.dataSize = *dataSize

	return &opts
}

func verifyOptions(opts *options) (*runConfiguration, error) {
	var runConfig runConfiguration
	var err error

	if opts.configuration == "Release" || opts.configuration == "Debug" {
		runConfig.configuration = opts.configuration
	} else {
		return nil, fmt.Errorf("invalid run configuration (Release|Debug)")
	}

	if opts.resultsFile == "" {
		runConfig.resultsFile = os.Stdout
	} else {
		runConfig.resultsFile, err = os.Create(opts.resultsFile)
		if err != nil {
			return nil, err
		}
	}

	runConfig.concurrentTasks, err = validateArgumentListFormat(opts.concurrentTasks)
	if err != nil {
		return nil, fmt.Errorf("invalid concurrent tasks: %v", err)
	}

	runConfig.dataSize, err = validateArgumentListFormat(opts.dataSize)
	if err != nil {
		return nil, fmt.Errorf("invalid data size: %v", err)
	}

	runConfig.clientCount, err = validateArgumentListFormat(opts.clientCount)
	if err != nil {
		return nil, fmt.Errorf("invalid client count: %v", err)
	}

	switch {
	case strings.EqualFold(opts.clientNames, clientNameOpts.goRedis):
		runConfig.clientNames = append(runConfig.clientNames, clientNameOpts.goRedis)

	case strings.EqualFold(opts.clientNames, clientNameOpts.goBabushka):
		runConfig.clientNames = append(runConfig.clientNames, clientNameOpts.goBabushka)

	case strings.EqualFold(opts.clientNames, clientNameOpts.all):
		runConfig.clientNames = append(runConfig.clientNames, clientNameOpts.goRedis, clientNameOpts.goBabushka)
	default:
		return nil, fmt.Errorf("invalid clients option: all|go-redis|go-babushka")
	}

	runConfig.host = opts.host
	runConfig.port = opts.port
	runConfig.tls = opts.tls
	runConfig.clusterModeEnabled = opts.clusterModeEnabled

	return &runConfig, nil
}

func runBenchmarks(runConfig *runConfiguration) error {
	fmt.Printf("\n =====> %s <===== \n\n", runConfig.clientNames)

	connectionSettings := benchmarks.NewConnectionSettings(
		runConfig.host,
		runConfig.port,
		runConfig.tls,
		runConfig.clusterModeEnabled,
	)

	err := executeBenchmarks(runConfig, connectionSettings)
	if err != nil {
		return err
	}

	if runConfig.resultsFile != os.Stdout {
		return benchmarks.ProcessResults(runConfig.resultsFile)
	}

	return nil
}

func executeBenchmarks(runConfig *runConfiguration, connectionSettings *benchmarks.ConnectionSettings) error {
	for _, clientName := range runConfig.clientNames {
		for _, dataSize := range runConfig.dataSize {
			for _, concurrentTasks := range runConfig.concurrentTasks {
				for _, clientCount := range runConfig.clientCount {
					err := runSingleBenchmark(runConfig.resultsFile, clientName, dataSize, concurrentTasks, clientCount, connectionSettings)
					if err != nil {
						return err
					}
				}
			}
		}
	}
	return nil
}

func runSingleBenchmark(resultsFile *os.File, clientName string, dataSize, concurrentTasks, clientCount int, connectionSettings *benchmarks.ConnectionSettings) error {
	fmt.Printf("Starting %s data size: %d concurrency: %d client count: %d\n", clientName, dataSize, concurrentTasks, clientCount)

	clients, err := createClients(clientCount, clientName, connectionSettings)
	if err != nil {
		return err
	}

	tps, latencyResults := benchmarks.MeasurePerformance(clients, concurrentTasks, dataSize)
	benchmarkConfig := benchmarks.NewBenchmarkConfig(
		clientName,
		concurrentTasks,
		dataSize,
		clientCount,
		connectionSettings.ClusterModeEnabled,
	)

	if resultsFile == os.Stdout {
		benchmarks.PrintResultsStdOut(benchmarkConfig, latencyResults, tps, resultsFile)
	} else {
		benchmarks.AddResultsJsonFormat(benchmarkConfig, latencyResults, tps)
	}

	return closeClients(clients)
}

func createClients(clientCount int, clientType string, connectionSettings *benchmarks.ConnectionSettings) ([]benchmarks.Client, error) {
	var clients []benchmarks.Client

	for clientNum := 0; clientNum < clientCount; clientNum++ {
		var client benchmarks.Client
		switch clientType {
		case clientNameOpts.goRedis:
			client = &benchmarks.GoRedisClient{}
		case clientNameOpts.goBabushka:
			client = &babushkaclient.BabushkaRedisClient{}
		}
		err := client.ConnectToRedis(connectionSettings)
		if err != nil {
			return nil, err
		}
		clients = append(clients, client)
	}
	return clients, nil
}

func closeClients(clients []benchmarks.Client) error {
	for _, client := range clients {
		err := client.CloseConnection()
		if err != nil {
			return err
		}
	}
	return nil
}

// Makes sure that arguments are in the format of 1 2 3 or [1 2 3]
func validateArgumentListFormat(arg string) ([]int, error) {
	arg = strings.Trim(strings.TrimSpace(arg), "[]")

	if len(arg) == 0 {
		return nil, fmt.Errorf("argument is empty or contains only brackets")
	}

	matched, err := regexp.MatchString("^\\d+(\\s+\\d+)*$", arg)
	if err != nil {
		return nil, err
	}
	if !matched {
		return nil, fmt.Errorf("wrong format for argument")
	}

	splitArgs := strings.Split(arg, " ")
	var argNumbersList []int
	for _, part := range splitArgs {
		var num int
		num, err = strconv.Atoi(strings.TrimSpace(part))
		if err != nil {
			return nil, fmt.Errorf("wrong number format for argument: %s", part)
		}
		argNumbersList = append(argNumbersList, num)
	}
	return argNumbersList, nil
}

func closeFileIfNotStdout(file *os.File) {
	if file != os.Stdout {
		err := file.Close()
		if err != nil {
			fmt.Println("Error closing the file:", err)
		}
	}
}
