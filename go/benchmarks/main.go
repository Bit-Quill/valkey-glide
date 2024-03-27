// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"math"
	"math/rand"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/aws/glide-for-redis/go/glide/benchmarks/internal"
)

type options struct {
	resultsFile        string
	dataSize           string
	concurrentTasks    string
	clients            string
	host               string
	port               int
	clientCount        string
	tls                bool
	clusterModeEnabled bool
	minimal            bool
}

type runConfiguration struct {
	resultsFile        *os.File
	dataSize           []int
	concurrentTasks    []int
	clientNames        []string
	host               string
	port               int
	clientCount        []int
	tls                bool
	clusterModeEnabled bool
	minimal            bool
}

const (
	goRedis = "go-redis"
	glide   = "glide"
	all     = "all"
)

func main() {
	opts := parseArguments()

	runConfig, err := verifyOptions(opts)
	if err != nil {
		log.Fatal("Error verifying options:", err)
		return
	}

	if runConfig.resultsFile != os.Stdout {
		defer closeFile(runConfig.resultsFile)
	}

	err = runBenchmarks(runConfig)
	if err != nil {
		log.Fatal("Error running benchmarking:", err)
	}
}

func closeFile(file *os.File) {
	err := file.Close()
	if err != nil {
		log.Fatal("Error closing the file:", err)
	}
}

func parseArguments() *options {
	resultsFile := flag.String("resultsFile", "", "Result filepath")
	dataSize := flag.String("dataSize", "[100 4000]", "Data block size")
	concurrentTasks := flag.String("concurrentTasks", "[100 1000]", "Number of concurrent tasks")
	clientNames := flag.String("clients", "all", "One of: all|go-redis|glide")
	host := flag.String("host", "localhost", "Hostname")
	port := flag.Int("port", 6379, "Port number")
	clientCount := flag.String("clientCount", "[1]", "Number of clients to run")
	tls := flag.Bool("tls", false, "Use TLS")
	clusterModeEnabled := flag.Bool("clusterModeEnabled", false, "Is cluster mode enabled")
	minimal := flag.Bool("minimal", false, "Run benchmark in minimal mode")

	flag.Parse()

	return &options{
		resultsFile:        *resultsFile,
		dataSize:           *dataSize,
		concurrentTasks:    *concurrentTasks,
		clients:            *clientNames,
		host:               *host,
		port:               *port,
		clientCount:        *clientCount,
		tls:                *tls,
		clusterModeEnabled: *clusterModeEnabled,
		minimal:            *minimal,
	}
}

func verifyOptions(opts *options) (*runConfiguration, error) {
	var runConfig runConfiguration
	var err error

	if opts.resultsFile == "" {
		runConfig.resultsFile = os.Stdout
	} else {
		err = os.MkdirAll(filepath.Dir(opts.resultsFile), os.ModePerm)
		if err != nil {
			return nil, err
		}

		runConfig.resultsFile, err = os.Create(opts.resultsFile)
		if err != nil {
			return nil, err
		}
	}

	runConfig.concurrentTasks, err = parseOptionsIntList(opts.concurrentTasks)
	if err != nil {
		return nil, fmt.Errorf("invalid concurrentTasks option: %v", err)
	}

	runConfig.dataSize, err = parseOptionsIntList(opts.dataSize)
	if err != nil {
		return nil, fmt.Errorf("invalid dataSize option: %v", err)
	}

	runConfig.clientCount, err = parseOptionsIntList(opts.clientCount)
	if err != nil {
		return nil, fmt.Errorf("invalid clientCount option: %v", err)
	}

	switch {
	case strings.EqualFold(opts.clients, goRedis):
		runConfig.clientNames = append(runConfig.clientNames, goRedis)

	case strings.EqualFold(opts.clients, glide):
		runConfig.clientNames = append(runConfig.clientNames, glide)

	case strings.EqualFold(opts.clients, all):
		runConfig.clientNames = append(runConfig.clientNames, goRedis, glide)
	default:
		return nil, fmt.Errorf("invalid clients option, should be one of: all|go-redis|glide")
	}

	runConfig.host = opts.host
	runConfig.port = opts.port
	runConfig.tls = opts.tls
	runConfig.clusterModeEnabled = opts.clusterModeEnabled
	runConfig.minimal = opts.minimal

	return &runConfig, nil
}

func parseOptionsIntList(listAsString string) ([]int, error) {
	listAsString = strings.Trim(strings.TrimSpace(listAsString), "[]")
	if len(listAsString) == 0 {
		return nil, fmt.Errorf("option is empty or contains only brackets")
	}

	matched, err := regexp.MatchString("^\\d+(\\s+\\d+)*$", listAsString)
	if err != nil {
		return nil, err
	}

	if !matched {
		return nil, fmt.Errorf("wrong format for option")
	}

	stringList := strings.Split(listAsString, " ")
	var intList []int
	for _, intString := range stringList {
		num, err := strconv.Atoi(strings.TrimSpace(intString))
		if err != nil {
			return nil, fmt.Errorf("wrong number format for option: %s", intString)
		}

		intList = append(intList, num)
	}

	return intList, nil
}

func runBenchmarks(runConfig *runConfiguration) error {
	connSettings := &internal.ConnectionSettings{
		Host:               runConfig.host,
		Port:               runConfig.port,
		UseTLS:             runConfig.tls,
		ClusterModeEnabled: runConfig.clusterModeEnabled,
	}

	err := executeBenchmarks(runConfig, connSettings)
	if err != nil {
		return err
	}

	if runConfig.resultsFile != os.Stdout {
		return processResults(runConfig.resultsFile)
	}

	return nil
}

type benchmarkConfig struct {
	ClientName         string
	NumConcurrentTasks int
	ClientCount        int
	DataSize           int
	Minimal            bool
	ConnectionSettings *internal.ConnectionSettings
	ResultsFile        *os.File
}

func executeBenchmarks(runConfig *runConfiguration, connectionSettings *internal.ConnectionSettings) error {
	for _, clientName := range runConfig.clientNames {
		for _, numConcurrentTasks := range runConfig.concurrentTasks {
			for _, clientCount := range runConfig.clientCount {
				for _, dataSize := range runConfig.dataSize {
					benchmarkConfig := &benchmarkConfig{
						ClientName:         clientName,
						NumConcurrentTasks: numConcurrentTasks,
						ClientCount:        dataSize,
						DataSize:           clientCount,
						Minimal:            runConfig.minimal,
						ConnectionSettings: connectionSettings,
						ResultsFile:        runConfig.resultsFile,
					}

					err := runSingleBenchmark(benchmarkConfig)
					if err != nil {
						return err
					}
				}
			}
		}

		fmt.Println()
	}

	return nil
}

func runSingleBenchmark(config *benchmarkConfig) error {
	fmt.Printf("Running benchmarking for %s client:\n", config.ClientName)
	fmt.Printf("\n =====> %s <===== %d clients %d concurrent %d data size \n\n", config.ClientName, config.ClientCount, config.NumConcurrentTasks, config.DataSize)

	clients, err := createClients(config)
	if err != nil {
		return err
	}

	benchmarkResult := measureBenchmark(clients, config)
	if config.ResultsFile != os.Stdout {
		addResultsJsonFormat(config, benchmarkResult)
	}

	printResults(benchmarkResult)
	return closeClients(clients)
}

func createClients(config *benchmarkConfig) ([]benchmarkClient, error) {
	var clients []benchmarkClient

	for clientNum := 0; clientNum < config.ClientCount; clientNum++ {
		var client benchmarkClient
		switch config.ClientName {
		case goRedis:
			client = &internal.GoRedisBenchmarkClient{}
		case glide:
			client = &internal.GlideBenchmarkClient{}
		}

		err := client.ConnectToRedis(config.ConnectionSettings)
		if err != nil {
			return nil, err
		}

		clients = append(clients, client)
	}

	return clients, nil
}

func closeClients(clients []benchmarkClient) error {
	for _, client := range clients {
		err := client.CloseConnection()
		if err != nil {
			return err
		}
	}

	return nil
}

// benchmarks package

var jsonResults []map[string]interface{}

func processResults(file *os.File) error {
	encoder := json.NewEncoder(file)
	err := encoder.Encode(jsonResults)
	if err != nil {
		return fmt.Errorf("error encoding JSON: %v", err)
	}

	return nil
}

type benchmarkClient interface {
	ConnectToRedis(connectionSettings *internal.ConnectionSettings) error
	Set(key string, value string) (string, error)
	Get(key string) (string, error)
	CloseConnection() error
	GetName() string
}

type benchmarkResults struct {
	iterationsPerTask int
	durationNano      time.Duration
	tps               float64
	latencyStats      map[string]*latencyStats
}

func measureBenchmark(clients []benchmarkClient, config *benchmarkConfig) *benchmarkResults {
	var iterationsPerTask int
	if config.Minimal {
		iterationsPerTask = 1000
	} else {
		iterationsPerTask = int(math.Min(math.Max(1e5, float64(config.NumConcurrentTasks*1e4)), 1e7))
	}

	actions := getActions(config.DataSize)
	duration, latencies := runBenchmark(iterationsPerTask, config.NumConcurrentTasks, actions, clients)
	tps := float64(len(latencies)) / duration.Seconds()
	stats := getLatencyStats(latencies)
	return &benchmarkResults{
		iterationsPerTask: iterationsPerTask,
		durationNano:      duration,
		tps:               tps,
		latencyStats:      stats,
	}
}

type operations func(client benchmarkClient) (string, error)

const (
	getExisting    = "get_existing"
	getNonExisting = "get_non_existing"
	set            = "set"
)

func getActions(dataSize int) map[string]operations {
	actions := map[string]operations{
		getExisting: func(client benchmarkClient) (string, error) {
			return client.Get(keyFromExistingKeyspace())
		},
		getNonExisting: func(client benchmarkClient) (string, error) {
			return client.Get(keyFromNewKeyspace())
		},
		set: func(client benchmarkClient) (string, error) {
			return client.Set(keyFromExistingKeyspace(), strings.Repeat("0", dataSize))
		},
	}

	return actions
}

const sizeNewKeyspace = 3750000
const sizeExistingKeyspace = 3000000

func keyFromExistingKeyspace() string {
	localRand := rand.New(rand.NewSource(time.Now().UnixNano()))
	return fmt.Sprint(math.Floor(localRand.Float64()*float64(sizeExistingKeyspace)) + 1)
}

func keyFromNewKeyspace() string {
	localRand := rand.New(rand.NewSource(time.Now().UnixNano()))
	totalRange := sizeNewKeyspace - sizeExistingKeyspace
	return fmt.Sprint(math.Floor(localRand.Float64()*float64(totalRange) + sizeExistingKeyspace + 1))
}

type actionLatency struct {
	action  string
	latency time.Duration
}

func runBenchmark(iterationsPerTask int, concurrentTasks int, actions map[string]operations, clients []benchmarkClient) (totalDuration time.Duration, latencies map[string][]time.Duration) {
	latencies = map[string][]time.Duration{
		getExisting:    {},
		getNonExisting: {},
		set:            {},
	}

	start := time.Now()
	numResults := concurrentTasks * iterationsPerTask
	results := make(chan actionLatency, numResults)
	for i := 0; i < concurrentTasks; i++ {
		go runTask(results, iterationsPerTask, actions, clients)
	}

	for i := 0; i < numResults; i++ {
		result := <-results
		latencies[result.action] = append(latencies[result.action], result.latency)
	}

	return time.Since(start), latencies
}

func runTask(results chan<- actionLatency, iterations int, actions map[string]operations, clients []benchmarkClient) {
	for i := 0; i < iterations; i++ {
		clientIndex := i % len(clients)
		action := randomAction()
		operation := actions[action]
		latency := measureOperation(operation, clients[clientIndex])
		results <- actionLatency{action: action, latency: latency}
	}
}

func measureOperation(operation operations, client benchmarkClient) time.Duration {
	start := time.Now()
	operation(client)
	return time.Since(start)
}

const probGet = 0.8
const probGetExistingKey = 0.8

func randomAction() string {
	localRand := rand.New(rand.NewSource(time.Now().UnixNano()))
	if localRand.Float64() > probGet {
		return set
	}

	if localRand.Float64() > probGetExistingKey {
		return getNonExisting
	}

	return getExisting
}

type latencyStats struct {
	avgLatency   time.Duration
	p50Latency   time.Duration
	p90Latency   time.Duration
	p99Latency   time.Duration
	stdDeviation time.Duration
	numRequests  int
}

func getLatencyStats(actionLatencies map[string][]time.Duration) map[string]*latencyStats {
	results := make(map[string]*latencyStats)

	for action, latencies := range actionLatencies {
		sort.Slice(latencies, func(i, j int) bool {
			return latencies[i] < latencies[j]
		})

		results[action] = &latencyStats{
			avgLatency:   average(latencies),
			p50Latency:   percentile(latencies, 50),
			p90Latency:   percentile(latencies, 90),
			p99Latency:   percentile(latencies, 99),
			stdDeviation: standardDeviation(latencies),
			numRequests:  len(latencies),
		}
	}

	return results
}

func average(observations []time.Duration) time.Duration {
	var sumNano int64 = 0
	for _, observation := range observations {
		sumNano += observation.Nanoseconds()
	}

	avgNano := sumNano / int64(len(observations))
	return time.Duration(avgNano)
}

func percentile(observations []time.Duration, p float64) time.Duration {
	N := float64(len(observations))
	n := (N-1)*p/100 + 1

	if n == 1.0 {
		return observations[0]
	} else if n == N {
		return observations[int(N)-1]
	}

	k := int(n)
	d := n - float64(k)
	interpolatedValue := float64(observations[k-1]) + d*(float64(observations[k])-float64(observations[k-1]))
	return time.Duration(int64(math.Round(interpolatedValue)))
}

func standardDeviation(observations []time.Duration) time.Duration {
	var sum, mean, sd float64
	lengthNumbers := len(observations)

	for i := 0; i < lengthNumbers; i++ {
		sum += float64(observations[i])
	}

	mean = sum / float64(lengthNumbers)

	for j := 0; j < lengthNumbers; j++ {
		sd += math.Pow(float64(observations[j])-mean, 2)
	}

	sd = math.Sqrt(sd / float64(lengthNumbers))
	return time.Duration(sd)
}

func printResults(results *benchmarkResults) {
	durationSec := float64(results.durationNano) / 1e9
	fmt.Printf("Runtime (sec): %.3f\n", durationSec)
	fmt.Printf("Iterations: %d\n", results.iterationsPerTask)
	fmt.Printf("TPS: %d\n", int(results.tps))

	var totalRequests int
	for action, latencyStat := range results.latencyStats {
		fmt.Printf("===> %s <===\n", action)
		fmt.Printf("avg. latency (ms): %.3f\n", float64(latencyStat.avgLatency)/1e6)
		fmt.Printf("std dev (ms): %.3f\n", float64(latencyStat.stdDeviation)/1e6)
		fmt.Printf("p50 latency (ms): %.3f\n", float64(latencyStat.p50Latency)/1e6)
		fmt.Printf("p90 latency (ms): %.3f\n", float64(latencyStat.p90Latency)/1e6)
		fmt.Printf("p99 latency (ms): %.3f\n", float64(latencyStat.p99Latency)/1e6)
		fmt.Printf("Number of requests: %d\n", latencyStat.numRequests)
		totalRequests += latencyStat.numRequests
	}

	fmt.Printf("Total requests: %d\n", totalRequests)
}

func addResultsJsonFormat(config *benchmarkConfig, results *benchmarkResults) {
	jsonResult := make(map[string]interface{})

	jsonResult["client"] = config.ClientName
	jsonResult["is_cluster"] = config.ConnectionSettings.ClusterModeEnabled
	jsonResult["num_of_tasks"] = config.NumConcurrentTasks
	jsonResult["data_size"] = config.DataSize
	jsonResult["client_count"] = config.ClientCount
	jsonResult["tps"] = results.tps

	for key, value := range results.latencyStats {
		jsonResult[key+"_p50_latency"] = float64(value.p50Latency) / 1e6
		jsonResult[key+"_p90_latency"] = float64(value.p90Latency) / 1e6
		jsonResult[key+"_p99_latency"] = float64(value.p99Latency) / 1e6
		jsonResult[key+"_average_latency"] = float64(value.avgLatency) / 1e6
		jsonResult[key+"_std_dev"] = float64(value.stdDeviation) / 1e6
	}

	jsonResults = append(jsonResults, jsonResult)
}
