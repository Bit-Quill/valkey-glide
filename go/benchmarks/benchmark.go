package benchmarks

import (
	"encoding/json"
	"fmt"
	"math"
	"math/rand"
	"os"
	"sort"
	"time"
)

type chosenActions struct {
	getExisting    string
	getNonExisting string
	set            string
}

type actionLatency struct {
	action  string
	latency int64
}

type operations func(client Client) (string, error)

var chosenActionOptions = chosenActions{
	getExisting:    "get_existing",
	getNonExisting: "get_non_existing",
	set:            "set",
}

var jsonResults = make([]map[string]interface{}, 0)

const probGet = 0.8
const probGetExistingKey = 0.8
const sizeNewKeyspace = 3750000
const sizeExistingKeyspace = 3000000

var numberOfIterations = func(numOfConcurrentTasks int) int {
	return int(math.Min(math.Max(1e5, float64(numOfConcurrentTasks*1e4)), 1e7))
}

func generateKeyExisting() string {
	localRand := rand.New(rand.NewSource(time.Now().UnixNano()))
	return fmt.Sprint(int(math.Floor(localRand.Float64()*float64(sizeExistingKeyspace))+1), "")
}

func generateKeyNew() string {
	localRand := rand.New(rand.NewSource(time.Now().UnixNano()))
	totalRange := sizeNewKeyspace - sizeExistingKeyspace
	return fmt.Sprint(int(math.Floor(localRand.Float64()*float64(totalRange)+sizeExistingKeyspace+1)), "")
}

func randomAlphanumeric(length int) string {
	localRand := rand.New(rand.NewSource(time.Now().UnixNano()))
	const charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	b := make([]byte, length)
	for i := range b {
		b[i] = charset[localRand.Intn(len(charset))]
	}
	return string(b)
}

func randomAction() string {
	localRand := rand.New(rand.NewSource(time.Now().UnixNano()))
	if localRand.Float64() > probGet {
		return chosenActionOptions.set
	}

	if localRand.Float64() > probGetExistingKey {
		return chosenActionOptions.getNonExisting
	}

	return chosenActionOptions.getExisting
}

func getLatency(operation operations, client Client) int64 {
	before := time.Now()
	operation(client)
	timeSince := time.Since(before)
	return timeSince.Nanoseconds()
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

func calculateTPS(tasks int, nanoseconds int64) float64 {
	seconds := float64(nanoseconds) / 1e9
	return float64(tasks) / seconds
}

func calculateResults(actionLatencies map[string][]int64) map[string]latencyResults {
	results := make(map[string]latencyResults)

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

func getActions(dataSize int) map[string]operations {
	actions := map[string]operations{
		chosenActionOptions.getExisting: func(client Client) (string, error) {
			return client.Get(generateKeyExisting())
		},
		chosenActionOptions.getNonExisting: func(client Client) (string, error) {
			return client.Get(generateKeyNew())
		},
		chosenActionOptions.set: func(client Client) (string, error) {
			return "", client.Set(generateKeyExisting(), randomAlphanumeric(dataSize))
		},
	}

	return actions
}

func getLatencies(iterations int, concurrentTasks int, actions map[string]operations, clients []Client) (int64, map[string][]int64) {
	latencies := map[string][]int64{
		chosenActionOptions.getExisting:    {},
		chosenActionOptions.getNonExisting: {},
		chosenActionOptions.set:            {},
	}

	jobs := make(chan int, iterations)
	results := make(chan actionLatency, iterations)

	for tasks := 0; tasks < concurrentTasks; tasks++ {
		go runTasks(tasks, jobs, results, actions, clients)
	}

	before := time.Now()
	for job := 0; job < iterations; job++ {
		jobs <- job
	}

	close(jobs)

	for i := 0; i < iterations; i++ {
		result := <-results
		latencies[result.action] = append(latencies[result.action], result.latency)
	}
	totalTaskTime := time.Since(before)
	return totalTaskTime.Nanoseconds(), latencies
}

func runTasks(taskId int, jobs <-chan int, results chan<- actionLatency, actions map[string]operations, clients []Client) {
	for job := range jobs {
		clientIndex := job % len(clients)
		action := randomAction()
		operation := actions[action]
		latency := getLatency(operation, clients[clientIndex])
		results <- actionLatency{action: action, latency: latency}
	}
}

func writeFileOrPanic(file *os.File, toWrite string) {
	_, err := fmt.Fprintf(file, toWrite)
	if err != nil {
		panic(err)
	}
}

func getKeysInSortedOrderForPrint(m map[string]latencyResults) []string {
	keys := make([]string, 0, len(m))
	for key := range m {
		keys = append(keys, key)
	}
	sort.Strings(keys)
	return keys
}

func PrintResultsStdOut(benchmarkConfig *BenchmarkConfig, resultMap map[string]latencyResults, tps float64, resultsFile *os.File) {
	writeFileOrPanic(resultsFile, fmt.Sprintf("Client Name: %s, Tasks Count: %d, Data Size: %d, Client Count: %d, TPS: %f\n",
		benchmarkConfig.ClientName, benchmarkConfig.TasksCount, benchmarkConfig.DataSize, benchmarkConfig.ClientCount, tps))
	keys := getKeysInSortedOrderForPrint(resultMap)
	for _, key := range keys {
		action := key
		latencyResult := resultMap[action]
		writeFileOrPanic(resultsFile, fmt.Sprintf("Avg. time in ms per %s: %f\n", action, latencyResult.avgLatency/1e6))
		writeFileOrPanic(resultsFile, fmt.Sprintf("%s p50 latency in ms: %f\n", action, float64(latencyResult.p50Latency)/1e6))
		writeFileOrPanic(resultsFile, fmt.Sprintf("%s p90 latency in ms: %f\n", action, float64(latencyResult.p90Latency)/1e6))
		writeFileOrPanic(resultsFile, fmt.Sprintf("%s p99 latency in ms: %f\n", action, float64(latencyResult.p99Latency)/1e6))
		writeFileOrPanic(resultsFile, fmt.Sprintf("%s std dev in ms: %f\n", action, latencyResult.stdDeviation/1e6))
	}
}

func AddResultsJsonFormat(benchmarkConfig *BenchmarkConfig, results map[string]latencyResults, tps float64) {
	jsonResult := make(map[string]interface{})

	jsonResult["client"] = benchmarkConfig.ClientName
	jsonResult["is_cluster"] = benchmarkConfig.IsCluster
	jsonResult["num_of_tasks"] = benchmarkConfig.TasksCount
	jsonResult["data_size"] = benchmarkConfig.DataSize
	jsonResult["client_count"] = benchmarkConfig.ClientCount
	jsonResult["tps"] = tps

	for key, value := range results {
		jsonResult[key+"_p50_latency"] = float64(value.p50Latency) / 1e6
		jsonResult[key+"_p90_latency"] = float64(value.p90Latency) / 1e6
		jsonResult[key+"_p99_latency"] = float64(value.p99Latency) / 1e6
		jsonResult[key+"_average_latency"] = float64(value.avgLatency) / 1e6
		jsonResult[key+"_std_dev"] = float64(value.stdDeviation) / 1e6
	}

	jsonResults = append(jsonResults, jsonResult)
}

func ProcessResults(file *os.File) error {
	encoder := json.NewEncoder(file)
	err := encoder.Encode(jsonResults)
	if err != nil {
		return fmt.Errorf("error encoding JSON: %v", err)
	}
	return nil
}

func MeasurePerformance(clients []Client, concurrentTasks int, dataSize int) (float64, map[string]latencyResults) {
	iterations := numberOfIterations(concurrentTasks)
	actions := getActions(dataSize)
	totalTaskTime, latencies := getLatencies(iterations, concurrentTasks, actions, clients)
	results := calculateResults(latencies)
	tps := calculateTPS(iterations, totalTaskTime)
	return tps, results
}
