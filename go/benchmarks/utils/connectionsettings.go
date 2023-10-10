package utils

type ConnectionSettings struct {
	Host   string
	Port   int
	UseSsl bool
}

func NewConnectionSettings(host string, port int, useSsl bool) *ConnectionSettings {
	return &ConnectionSettings{
		Host:   host,
		Port:   port,
		UseSsl: useSsl,
	}
}
