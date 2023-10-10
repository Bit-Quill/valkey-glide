package main

import (
	"flag"
	"fmt"
	"github.com/aws/babushka/go/benchmarks/goredis"
	"github.com/aws/babushka/go/benchmarks/utils"
	"os"
	"regexp"
	"strconv"
	"strings"
)

/*
Options represents the set of arguments passed into the program.
It's responsible for parsing and holding the raw values provided by the user.
If some arguments are not passed, Options will set default values for them.
*/
type Options struct {
	TLS             bool
	Host            string
	Port            int
	ResultsFile     string
	ClientCount     int
	Clients         string
	Configuration   string
	ConcurrentTasks string
	DataSize        int
}

type ClientNames struct {
	GoRedis      string
	GoRedisAsync string
	Babushka     string
	All          string
}

var ClientName = ClientNames{
	GoRedis:      "go-redis",
	GoRedisAsync: "go-redis-async",
	Babushka:     "babushka",
	All:          "all",
}

func main() {

	options := parseArguments()

	runConfiguration, err := verifyOptions(options)
	if err != nil {
		fmt.Println("Error verifying options: ", err)
		return
	}

	defer closeFileIfNotStdout(runConfiguration.ResultsFile)

	switch runConfiguration.Clients {
	case ClientName.GoRedis:
		fmt.Println("Not yet configured for go-redis benchmarking.")
		err = testClientSetGet(&goredisclient.GoRedisClient{}, runConfiguration, false)

	case ClientName.GoRedisAsync:
		fmt.Println("Not yet configured for go-redis_async benchmarking.")
		err = testClientSetGet(&goredisclient.GoRedisClient{}, runConfiguration, true)

	case ClientName.Babushka:
		fmt.Println("Not yet configured for babushka benchmarking.")

	case ClientName.All:
		fmt.Println("Not yet configured for babushka benchmarking.")
	}

	if err != nil {
		fmt.Println("Error running benchmarking: ", err)
		return
	}

}

func parseArguments() *Options {
	var options Options

	tls := flag.Bool("tls", false, "Use TLS (default: false)")
	host := flag.String("host", "localhost", "Host address")
	port := flag.Int("port", 6379, "Port number")
	resultsFile := flag.String("resultsFile", "", "Path to results file")
	clientCount := flag.Int("clientCount", 1, "Client Count")
	clients := flag.String("clients", "all", "One of: all|go-redis|go-redis-async|babushka")
	configuration := flag.String("configuration", "Release", "Configuration flag")
	concurrentTasks := flag.String("concurrentTasks", "[1 10 100]", "Number of concurrent tasks")
	dataSize := flag.Int("dataSize", 20, "Data block size")

	flag.Parse()

	options.TLS = *tls
	options.Host = *host
	options.Port = *port
	options.ResultsFile = *resultsFile
	options.ClientCount = *clientCount
	options.Clients = *clients
	options.Configuration = *configuration
	options.ConcurrentTasks = *concurrentTasks
	options.DataSize = *dataSize

	return &options
}

func verifyOptions(options *Options) (*utils.RunConfiguration, error) {
	var runConfiguration utils.RunConfiguration
	var err error

	if options.Configuration == "Release" || options.Configuration == "Debug" {
		runConfiguration.Configuration = options.Configuration
	} else {
		return nil, fmt.Errorf("invalid run configuration (Release|Debug)")
	}

	if options.ResultsFile == "" {
		runConfiguration.ResultsFile = os.Stdout
	} else {
		runConfiguration.ResultsFile, err = os.Create(options.ResultsFile)
		if err != nil {
			return nil, err
		}
	}

	runConfiguration.ConcurrentTasks, err = validateConcurrentTasks(options.ConcurrentTasks)
	if err != nil {
		return nil, err
	}

	switch {
	case strings.EqualFold(options.Clients, ClientName.GoRedis):
		runConfiguration.Clients = ClientName.GoRedis

	case strings.EqualFold(options.Clients, ClientName.GoRedisAsync):
		runConfiguration.Clients = ClientName.GoRedisAsync

	case strings.EqualFold(options.Clients, ClientName.Babushka):
		runConfiguration.Clients = ClientName.Babushka

	case strings.EqualFold(options.Clients, ClientName.All):
		runConfiguration.Clients = ClientName.All
	default:
		return nil, fmt.Errorf("invalid clients option: all|go-redis|babushka")
	}

	runConfiguration.Host = options.Host
	runConfiguration.Port = options.Port
	runConfiguration.ClientCount = options.ClientCount
	runConfiguration.TLS = options.TLS
	runConfiguration.DataSize = options.DataSize

	return &runConfiguration, nil
}

func testClientSetGet(client utils.Client, runConfiguration *utils.RunConfiguration, async bool) error {
	fmt.Printf("\n =====> %s <===== \n\n", runConfiguration.Clients)
	err := utils.MeasurePerformance(client, runConfiguration, async)
	if err != nil {
		return err
	}
	return nil
}

// Makes sure that concurrentTasks is in the format of 1 2 3 or [1 2 3]
func validateConcurrentTasks(concurrentTasks string) ([]int, error) {
	concurrentTasks = strings.Trim(strings.TrimSpace(concurrentTasks), "[]")

	if len(concurrentTasks) == 0 {
		return nil, fmt.Errorf("concurrent string is empty or contains only brackets")
	}

	matched, err := regexp.MatchString("^\\d+(\\s+\\d+)*$", concurrentTasks)
	if err != nil {
		return nil, err
	}
	if !matched {
		return nil, fmt.Errorf("invalid Concurrent Tasks")
	}

	splitTasks := strings.Split(concurrentTasks, " ")
	var concurrentTasksNumbersList []int
	for _, part := range splitTasks {
		num, err := strconv.Atoi(strings.TrimSpace(part))
		if err != nil {
			return nil, fmt.Errorf("invalid number format for concurrent tasks: %s", part)
		}
		concurrentTasksNumbersList = append(concurrentTasksNumbersList, num)
	}
	return concurrentTasksNumbersList, nil
}

func closeFileIfNotStdout(file *os.File) {
	if file != os.Stdout {
		err := file.Close()
		if err != nil {
			fmt.Println("Error closing the file:", err)
		}
	}
}
