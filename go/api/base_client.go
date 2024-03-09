// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package api

// #cgo LDFLAGS: -L../target/release -lglide_rs
// #include "../lib.h"
//
// void successCallback(uintptr_t channelPtr, char *message);
// void failureCallback(uintptr_t channelPtr, char *errMessage, RequestErrorType errType);
import "C"

import (
	"github.com/aws/glide-for-redis/go/glide/protobuf"
	"google.golang.org/protobuf/proto"
	"unsafe"
)

//export successCallback
func successCallback(channelPtr C.uintptr_t, cResponse *C.char) {
	response := C.GoString(cResponse)
	goChannelPointer := uintptr(channelPtr)
	resultChannel := *(*chan payload)(unsafe.Pointer(goChannelPointer))
	resultChannel <- payload{value: &response, error: nil}
}

//export failureCallback
func failureCallback(channelPtr C.uintptr_t, cErrorMessage *C.char, cErrorType C.RequestErrorType) {
	goChannelPointer := uintptr(channelPtr)
	resultChannel := *(*chan payload)(unsafe.Pointer(goChannelPointer))
	resultChannel <- payload{value: nil, error: goError(cErrorType, cErrorMessage)}
}

type connectionRequestConverter interface {
	toProtobuf() *protobuf.ConnectionRequest
}

type baseClient struct {
	coreClient unsafe.Pointer
}

func createClient(converter connectionRequestConverter) (unsafe.Pointer, error) {
	request := converter.toProtobuf()
	msg, err := proto.Marshal(request)
	if err != nil {
		return nil, err
	}

	byteCount := len(msg)
	requestBytes := C.CBytes(msg)
	cResponse := (*C.struct_ConnectionResponse)(C.create_client_using_protobuf((*C.uchar)(requestBytes), C.uintptr_t(byteCount), (C.SuccessCallback)(unsafe.Pointer(C.successCallback)), (C.FailureCallback)(unsafe.Pointer(C.failureCallback))))
	defer C.free_connection_response(cResponse)

	cErr := cResponse.error_message
	if cErr != nil {
		return nil, goError(cResponse.error_type, cResponse.error_message)
	}

	return cResponse.conn_ptr, nil
}

// Close terminates the client by closing all associated resources.
func (client *baseClient) Close() error {
	if client.coreClient == nil {
		return &GlideError{"The glide client was not open. Either it was not initialized, or it was already closed."}
	}

	C.close_client(client.coreClient)
	client.coreClient = nil
	return nil
}

func (client *baseClient) CustomCommand(args []string) (interface{}, error) {
	cArgs := toCStrings(args)
	defer freeCStrings(cArgs)

	resultChannel := make(chan payload)
	resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

	requestType := C.uint32_t(customCommand)
	C.command(client.coreClient, C.uintptr_t(resultChannelPtr), requestType, C.uintptr_t(len(args)), &cArgs[0])

	payload := <-resultChannel
	if payload.error != nil {
		return nil, payload.error
	}

	if payload.value != nil {
		return *payload.value, nil
	}

	return nil, nil
}

func toCStrings(args []string) []*C.char {
	cArgs := make([]*C.char, len(args))
	for i, arg := range args {
		cString := C.CString(arg)
		cArgs[i] = cString
	}
	return cArgs
}

func freeCStrings(cArgs []*C.char) {
	for _, arg := range cArgs {
		C.free(unsafe.Pointer(arg))
	}
}
