package utils

type LatencyResults struct {
	AvgLatency   float64
	P50Latency   int64
	P90Latency   int64
	P99Latency   int64
	StdDeviation float64
}

func NewLatencyResults(avgLatency float64, p50Latency int64, p90Latency int64, p99Latency int64, stdDeviation float64) *LatencyResults {
	return &LatencyResults{
		AvgLatency:   avgLatency,
		P50Latency:   p50Latency,
		P90Latency:   p90Latency,
		P99Latency:   p99Latency,
		StdDeviation: stdDeviation}
}
