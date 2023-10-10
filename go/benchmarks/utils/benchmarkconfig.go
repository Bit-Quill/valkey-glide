package utils

type BenchmarkConfig struct {
	ClientName  string
	TasksCount  int
	DataSize    int
	ClientCount int
}

func NewBenchmarkConfig(clientName string, tasksCount int, dataSize int, clientCount int) *BenchmarkConfig {
	return &BenchmarkConfig{ClientName: clientName, TasksCount: tasksCount, DataSize: dataSize, ClientCount: clientCount}
}
