package benchmarks

type latencyResults struct {
	avgLatency   float64
	p50Latency   int64
	p90Latency   int64
	p99Latency   int64
	stdDeviation float64
}

func NewLatencyResults(avgLatency float64, p50Latency int64, p90Latency int64, p99Latency int64, stdDeviation float64) *latencyResults {
	return &latencyResults{
		avgLatency:   avgLatency,
		p50Latency:   p50Latency,
		p90Latency:   p90Latency,
		p99Latency:   p99Latency,
		stdDeviation: stdDeviation}
}
