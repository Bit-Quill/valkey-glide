package configproto

import "github.com/aws/babushka/go/babushkaclient-proto/protobuf"

type AddressInfo struct {
	Host string
	Port uint32
}

type BackoffStrategy struct {
	NumOfRetries int
	Factor       int
	ExponentBase int
}

func NewBackoffStrategy(numOfRetries int, factor int, exponentBase int) *BackoffStrategy {
	return &BackoffStrategy{NumOfRetries: numOfRetries, Factor: factor, ExponentBase: exponentBase}
}

// TODO: Username is optional param
type AuthenticationOptions struct {
	Password string
	Username string
}

// TODO
func NewAuthenticationOptions(password string, username string) *AuthenticationOptions {
	return &AuthenticationOptions{Password: password, Username: username}
}

// TODO: Optional Params
type ClientConfiguration struct {
	Addresses             []AddressInfo
	UseTLS                bool
	Credentials           AuthenticationOptions
	ReadFromReplica       protobuf.ReadFromReplicaStrategy
	ClientCreationTimeOut uint32
	ResponseTimout        uint32
}

func NewClientConfiguration(addresses []AddressInfo, useTLS bool, credentials AuthenticationOptions, readFromReplica protobuf.ReadFromReplicaStrategy, clientCreationTimeOut uint32, responseTimout uint32) *ClientConfiguration {
	return &ClientConfiguration{Addresses: addresses, UseTLS: useTLS, Credentials: credentials, ReadFromReplica: readFromReplica, ClientCreationTimeOut: clientCreationTimeOut, ResponseTimout: responseTimout}
}

func (clientConfiguration *ClientConfiguration) CreateAProtobufConnRequest(clusterMode bool) *protobuf.ConnectionRequest {
	request := protobuf.ConnectionRequest{}
	for _, address := range clientConfiguration.Addresses {
		addressInfo := &protobuf.AddressInfo{
			Host: address.Host,
			Port: address.Port,
		}
		request.Addresses = append(request.Addresses, addressInfo)
	}
	if clientConfiguration.UseTLS {
		request.TlsMode = protobuf.TlsMode_SecureTls
	} else {
		request.TlsMode = protobuf.TlsMode_NoTls
	}

	//TODO will need to be changed as enum
	request.ReadFromReplicaStrategy = protobuf.ReadFromReplicaStrategy_AlwaysFromPrimary

	//TODO will need to be changed as optional values
	if clientConfiguration.ResponseTimout != 0 {
		request.ResponseTimeout = clientConfiguration.ResponseTimout
	}

	//TODO will need to be changed as optional values
	if clientConfiguration.ClientCreationTimeOut != 0 {
		request.ClientCreationTimeout = clientConfiguration.ClientCreationTimeOut
	}

	if clusterMode {
		request.ClusterModeEnabled = true
	} else {
		request.ClusterModeEnabled = false
	}

	//TODO will need to be changed as optional
	if clientConfiguration.Credentials.Username != "" {
		request.ClusterModeEnabled = true
	} else {
		request.ClusterModeEnabled = false
	}

	if clientConfiguration.Credentials.Username != "" {
		request.AuthenticationInfo.Username = clientConfiguration.Credentials.Username
	}

	if clientConfiguration.Credentials.Password != "" {
		request.AuthenticationInfo.Password = clientConfiguration.Credentials.Password
	}

	return &request
}
