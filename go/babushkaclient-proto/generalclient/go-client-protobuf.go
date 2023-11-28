package generalclient

/*
#cgo LDFLAGS: -L./target/release -lclientProto
#include "lib.h"

void successCallbackProto(char *message, uintptr_t channelPtr);
void failureCallbackProto(char *errMessage, uintptr_t channelPtr);
*/
import "C"

import (
	"fmt"
	"github.com/aws/babushka/go/babushkaclient-proto"
	"github.com/aws/babushka/go/babushkaclient-proto/protobuf"
	"google.golang.org/protobuf/proto"
	"log"
	"unsafe"
)

type BaseClient struct {
	Config            configproto.ClientConfiguration
	connectionPointer unsafe.Pointer
}

type payload struct {
	value      string
	errMessage error
}

//export successCallbackProto
func successCallbackProto(message *C.char, channelPtr C.uintptr_t) {
	goMessage := C.GoString(message)
	goChannelPointer := uintptr(channelPtr)
	resultChannel := *(*chan payload)(unsafe.Pointer(goChannelPointer))
	resultChannel <- payload{value: goMessage, errMessage: nil}
}

//export failureCallbackProto
func failureCallbackProto(errMessage *C.char, channelPtr C.uintptr_t) {
	goMessage := C.GoString(errMessage)
	goChannelPointer := uintptr(channelPtr)
	resultChannel := *(*chan payload)(unsafe.Pointer(goChannelPointer))
	resultChannel <- payload{value: "", errMessage: fmt.Errorf("error at redis operation: %s", goMessage)}
}

func (baseClient *BaseClient) getProtobufConnRequest(clusterMode bool) *protobuf.ConnectionRequest {
	return baseClient.Config.CreateAProtobufConnRequest(clusterMode)
}

func (baseClient *BaseClient) getProtobufRedisRequest(requestType protobuf.RequestType, args []string) *protobuf.RedisRequest {
	command := &protobuf.Command{
		RequestType: requestType, // Set the appropriate RequestType
		Args:        &protobuf.Command_ArgsArray_{ArgsArray: &protobuf.Command_ArgsArray{Args: args}},
	}
	request := &protobuf.RedisRequest{}
	request.Command = &protobuf.RedisRequest_SingleCommand{
		SingleCommand: command,
	}

	return request
}

func (baseClient *BaseClient) ConnectToRedis(clusterMode bool) error {
	data, err := proto.Marshal(baseClient.getProtobufConnRequest(clusterMode))
	if err != nil {
		log.Fatal("Marshaling error: ", err)
	}

	baseClient.connectionPointer = C.create_connection_proto((*C.uint8_t)(&data[0]), C.uintptr_t(len(data)), (C.SuccessCallback)(unsafe.Pointer(C.successCallbackProto)), (C.FailureCallback)(unsafe.Pointer(C.failureCallbackProto)))
	if baseClient.connectionPointer == nil {
		return fmt.Errorf("error connecting to babushkaRedisClient")
	}
	return nil
}

func (baseClient *BaseClient) CloseConnection() {
	C.close_connection_proto(baseClient.connectionPointer)
}

func (baseClient *BaseClient) Ping(message ...string) (string, error) {
	var args []string
	if len(message) == 1 {
		args = append(args, message[0])
	}
	return baseClient.ExecuteCommand(baseClient.getProtobufRedisRequest(protobuf.RequestType_Ping, args))
}

func (baseClient *BaseClient) Info() (string, error) {
	return baseClient.ExecuteCommand(baseClient.getProtobufRedisRequest(protobuf.RequestType_Info, nil))
}

func (baseClient *BaseClient) Set(key string, value string) error {
	var args []string
	args = append(args, key, value)

	_, err := baseClient.ExecuteCommand(baseClient.getProtobufRedisRequest(protobuf.RequestType_SetString, args))
	return err
}
func (baseClient *BaseClient) Get(key string) (string, error) {
	var args []string
	args = append(args, key)
	return baseClient.ExecuteCommand(baseClient.getProtobufRedisRequest(protobuf.RequestType_GetString, args))
}

func (baseClient *BaseClient) ExecuteCommand(request *protobuf.RedisRequest) (string, error) {
	data, err := proto.Marshal(request)
	if err != nil {
		log.Fatal("Marshaling error: ", err)
	}

	resultChannel := make(chan payload)
	resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))
	C.execute_command_proto(baseClient.connectionPointer, (*C.uint8_t)(&data[0]), C.uintptr_t(len(data)), C.uintptr_t(resultChannelPtr))

	resultPayload := <-resultChannel
	return resultPayload.value, resultPayload.errMessage
}
