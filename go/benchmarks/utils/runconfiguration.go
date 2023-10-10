package utils

import "os"

/*
RunConfiguration takes the parsed Options and matches them,
ensuring their validity and converting them to the appropriate types
for the program's execution requirements.
*/
type RunConfiguration struct {
	TLS             bool
	Host            string
	Port            int
	ResultsFile     *os.File
	ClientCount     int
	Clients         string
	Configuration   string
	ConcurrentTasks []int
	DataSize        int
}
