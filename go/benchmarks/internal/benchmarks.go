// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package internal

type ConnectionSettings struct {
	Host               string
	Port               int
	UseTLS             bool
	ClusterModeEnabled bool
}
