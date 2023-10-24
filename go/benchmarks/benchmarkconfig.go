package benchmarks

type BenchmarkConfig struct {
	ClientName  string
	TasksCount  int
	DataSize    int
	ClientCount int
	IsCluster   bool
}

func NewBenchmarkConfig(clientName string, tasksCount int, dataSize int, clientCount int, isCluster bool) *BenchmarkConfig {
	return &BenchmarkConfig{
		ClientName:  clientName,
		TasksCount:  tasksCount,
		DataSize:    dataSize,
		ClientCount: clientCount,
		IsCluster:   isCluster}
}
