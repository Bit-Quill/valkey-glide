package benchmarks

type ConnectionSettings struct {
	Host               string
	Port               int
	UseSsl             bool
	ClusterModeEnabled bool
}

func NewConnectionSettings(host string, port int, useSsl bool, clusterModeEnabled bool) *ConnectionSettings {
	return &ConnectionSettings{
		Host:               host,
		Port:               port,
		UseSsl:             useSsl,
		ClusterModeEnabled: clusterModeEnabled,
	}
}
