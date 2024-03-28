// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package main

import (
	"encoding/json"
	"fmt"
	"log"
	"math"
	"math/rand"
	"os"
	"sort"
	"strings"
	"time"
)

type connectionSettings struct {
	Host               string
	Port               int
	UseTLS             bool
	ClusterModeEnabled bool
}

func runBenchmarks(runConfig *runConfiguration) error {
	connSettings := &connectionSettings{
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
	ConnectionSettings *connectionSettings
	ResultsFile        *os.File
}

func executeBenchmarks(runConfig *runConfiguration, connectionSettings *connectionSettings) error {
	for _, clientName := range runConfig.clientNames {
		for _, numConcurrentTasks := range runConfig.concurrentTasks {
			for _, clientCount := range runConfig.clientCount {
				for _, dataSize := range runConfig.dataSize {
					benchmarkConfig := &benchmarkConfig{
						ClientName:         clientName,
						NumConcurrentTasks: numConcurrentTasks,
						ClientCount:        clientCount,
						DataSize:           dataSize,
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
	fmt.Printf("\n =====> %s <===== clientCount: %d, concurrentTasks: %d, dataSize: %d \n\n", config.ClientName, config.ClientCount, config.NumConcurrentTasks, config.DataSize)

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
			client = &goRedisBenchmarkClient{}
		case glide:
			client = &glideBenchmarkClient{}
		}

		err := client.connect(config.ConnectionSettings)
		if err != nil {
			return nil, err
		}

		clients = append(clients, client)
	}

	return clients, nil
}

func closeClients(clients []benchmarkClient) error {
	for _, client := range clients {
		err := client.close()
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
	connect(connectionSettings *connectionSettings) error
	set(key string, value string) (string, error)
	get(key string) (string, error)
	close() error
	getName() string
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
	tps := calculateTPS(latencies, duration)
	stats := getLatencyStats(latencies)
	return &benchmarkResults{
		iterationsPerTask: iterationsPerTask,
		durationNano:      duration,
		tps:               tps,
		latencyStats:      stats,
	}
}

func calculateTPS(latencies map[string][]time.Duration, totalDuration time.Duration) float64 {
	numRequests := 0
	for _, durations := range latencies {
		numRequests += len(durations)
	}

	return float64(numRequests) / totalDuration.Seconds()
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
			return client.get(keyFromExistingKeyspace())
		},
		getNonExisting: func(client benchmarkClient) (string, error) {
			return client.get(keyFromNewKeyspace())
		},
		set: func(client benchmarkClient) (string, error) {
			return client.set(keyFromExistingKeyspace(), strings.Repeat("0", dataSize))
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
	results := make(chan *actionLatency, numResults)
	for i := 0; i < concurrentTasks; i++ {
		go runTask(results, iterationsPerTask, actions, clients)
	}

	for i := 0; i < numResults; i++ {
		result := <-results
		latencies[result.action] = append(latencies[result.action], result.latency)
	}

	return time.Since(start), latencies
}

func runTask(results chan<- *actionLatency, iterations int, actions map[string]operations, clients []benchmarkClient) {
	for i := 0; i < iterations; i++ {
		clientIndex := i % len(clients)
		action := randomAction()
		operation := actions[action]
		latency := measureOperation(operation, clients[clientIndex])
		results <- &actionLatency{action: action, latency: latency}
	}
}

func measureOperation(operation operations, client benchmarkClient) time.Duration {
	start := time.Now()
	_, err := operation(client)
	if err != nil {
		log.Print("Error while executing operation: ", err)
	}

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
