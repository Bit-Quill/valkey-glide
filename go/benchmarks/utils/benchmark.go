package utils

import (
	"context"
	"fmt"
	"math"
	"math/rand"
	"os"
	"sort"
	"time"
)

type ChosenAction struct {
	GetExisting    string
	GetNonExisting string
	Set            string
}

type ActionLatency struct {
	Action  string
	Latency int64
}

type Operations func() (string, error)

var chosenAction = ChosenAction{
	GetExisting:    "Get_Existing",
	GetNonExisting: "Get_Non_Existing",
	Set:            "Set",
}

const ProbGet = 0.8
const ProbGetExistingKey = 0.8
const SizeGetKeyspace = 3750000
const SizeSetKeyspace = 3000000

var packageRand = rand.New(rand.NewSource(time.Now().UnixNano()))

func generateKeyExisting() string {
	return fmt.Sprint(int(math.Floor(packageRand.Float64()*float64(SizeSetKeyspace))+1), "")
}

func generateKeyNew() string {
	totalRange := SizeGetKeyspace - SizeSetKeyspace
	return fmt.Sprint(int(math.Floor(packageRand.Float64()*float64(totalRange)+SizeSetKeyspace+1)), "")
}

func randomAlphanumeric(length int) string {
	const charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	b := make([]byte, length)
	for i := range b {
		b[i] = charset[packageRand.Intn(len(charset))]
	}
	return string(b)
}

func randomAction() string {
	if packageRand.Float64() > ProbGet {
		return chosenAction.Set
	}

	if packageRand.Float64() > ProbGetExistingKey {
		return chosenAction.GetNonExisting
	}

	return chosenAction.GetExisting
}

func getLatency(operation Operations) int64 {
	before := time.Now()
	operation()
	timeSince := time.Since(before)
	return timeSince.Microseconds()
}

func calculateAverageLatency(latencies []int64) float64 {
	var sum int64
	for _, latency := range latencies {
		sum += latency
	}
	return float64(sum) / float64(len(latencies))
}

func standardDeviation(numbers []int64) float64 {
	var sum, mean, sd float64
	lengthNumbers := len(numbers)

	for i := 0; i < lengthNumbers; i++ {
		sum += float64(numbers[i])
	}

	mean = sum / float64(lengthNumbers)

	for j := 0; j < lengthNumbers; j++ {
		sd += math.Pow(float64(numbers[j])-mean, 2)
	}

	sd = math.Sqrt(sd / float64(lengthNumbers))
	return sd
}

func percentile(latencies []int64, p float64) int64 {
	N := float64(len(latencies))
	n := (N-1)*float64(p)/100 + 1

	if n == 1.0 {
		return latencies[0]
	} else if n == N {
		return latencies[int(N)-1]
	}

	k := int(n)
	d := n - float64(k)
	interpolatedValue := float64(latencies[k-1]) + d*(float64(latencies[k])-float64(latencies[k-1]))
	return int64(math.Round(interpolatedValue))
}

func calculateResults(actionLatencies map[string][]int64) map[string]LatencyResults {
	results := make(map[string]LatencyResults)

	for action, latencies := range actionLatencies {
		sort.Slice(latencies, func(i, j int) bool {
			return latencies[i] < latencies[j]
		})

		results[action] = *NewLatencyResults(
			calculateAverageLatency(latencies),
			percentile(latencies, 50),
			percentile(latencies, 90),
			percentile(latencies, 99),
			standardDeviation(latencies))
	}

	return results
}

func getLatencies(iterations int, actions map[string]Operations) map[string][]int64 {
	latencies := map[string][]int64{
		chosenAction.GetExisting:    {},
		chosenAction.GetNonExisting: {},
		chosenAction.Set:            {},
	}

	for i := 0; i < iterations; i++ {
		action := randomAction()
		operation := actions[action]
		latency := getLatency(operation)
		latencies[action] = append(latencies[action], latency)
	}
	return latencies
}

func getLatenciesWorker(worker int, jobs <-chan int, results chan<- ActionLatency, actions map[string]Operations) {
	for _ = range jobs {
		action := randomAction()
		operation := actions[action]
		latency := getLatency(operation)
		results <- ActionLatency{Action: action, Latency: latency}
	}
}

func getLatenciesAsync(iterations int, actions map[string]Operations, workers int) map[string][]int64 {
	latencies := map[string][]int64{
		chosenAction.GetExisting:    {},
		chosenAction.GetNonExisting: {},
		chosenAction.Set:            {},
	}

	//TODO do we want to limit the buffer size as it will take up a chunk in memory
	jobs := make(chan int, iterations)
	results := make(chan ActionLatency, iterations)

	for worker := 0; worker < workers; worker++ {
		go getLatenciesWorker(worker, jobs, results, actions)
	}

	for job := 0; job < iterations; job++ {
		jobs <- job
	}

	close(jobs)

	for i := 0; i < iterations; i++ {
		result := <-results
		latencies[result.Action] = append(latencies[result.Action], result.Latency)
	}

	return latencies
}

func printResults(resultMap map[string]LatencyResults, benchmarkConfig *BenchmarkConfig, resultsFile *os.File) {
	writeFileOrPanic(resultsFile, fmt.Sprintf("Client Name: %s, Tasks Count: %d, Data Size: %d, Client Count: %d\n",
		benchmarkConfig.ClientName, benchmarkConfig.TasksCount, benchmarkConfig.DataSize, benchmarkConfig.ClientCount))
	for action, latencyResult := range resultMap {
		writeFileOrPanic(resultsFile, fmt.Sprintf("Avg. time in ms per %s: %f\n", action, latencyResult.AvgLatency/1000000.0))
		writeFileOrPanic(resultsFile, fmt.Sprintf("%s p50 latency in ms: %f\n", action, float64(latencyResult.P50Latency)/1000000.0))
		writeFileOrPanic(resultsFile, fmt.Sprintf("%s p90 latency in ms: %f\n", action, float64(latencyResult.P90Latency)/1000000.0))
		writeFileOrPanic(resultsFile, fmt.Sprintf("%s p99 latency in ms: %f\n", action, float64(latencyResult.P99Latency)/1000000.0))
		writeFileOrPanic(resultsFile, fmt.Sprintf("%s std dev in ms: %f\n", action, latencyResult.StdDeviation/1000000.0))
	}
}

func writeFileOrPanic(file *os.File, toWrite string) {
	_, err := fmt.Fprintf(file, toWrite)
	if err != nil {
		panic(err)
	}
}

func MeasurePerformance(client Client, runConfiguration *RunConfiguration, async bool) error {
	ctx := context.Background()
	connectionSettings := NewConnectionSettings(
		runConfiguration.Host,
		runConfiguration.Port,
		runConfiguration.TLS)

	err := client.ConnectToRedis(ctx, connectionSettings)
	if err != nil {
		return err
	}

	iterations := 100

	actions := map[string]Operations{
		chosenAction.GetExisting: func() (string, error) {
			return client.Get(ctx, generateKeyExisting())
		},
		chosenAction.GetNonExisting: func() (string, error) {
			return client.Get(ctx, generateKeyNew())
		},
		chosenAction.Set: func() (string, error) {
			dataSize := runConfiguration.DataSize
			return "", client.Set(ctx, generateKeyExisting(), randomAlphanumeric(dataSize))
		},
	}

	if async {
		for _, workers := range runConfiguration.ConcurrentTasks {
			latencyResult := calculateResults(getLatenciesAsync(iterations, actions, workers))
			benchmarkConfig := NewBenchmarkConfig(
				runConfiguration.Clients,
				workers,
				runConfiguration.DataSize,
				runConfiguration.ClientCount)
			printResults(latencyResult, benchmarkConfig, runConfiguration.ResultsFile)
		}
	} else {
		benchmarkConfig := NewBenchmarkConfig(
			runConfiguration.Clients,
			1,
			runConfiguration.DataSize,
			runConfiguration.ClientCount)
		latencyResult := calculateResults(getLatencies(iterations, actions))
		printResults(latencyResult, benchmarkConfig, runConfiguration.ResultsFile)
	}

	err = client.CloseConnection()
	if err != nil {
		return err
	}

	return nil
}
