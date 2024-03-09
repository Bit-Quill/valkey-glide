/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */
 package glide

/*
#cgo LDFLAGS: -L../target/release -lglide_rs
#include "../lib.h"

void successCallback(uintptr_t channelPtr, char *message);
void failureCallback(uintptr_t channelPtr, char *errMessage, RequestErrorType errType);
*/
import "C"

import (
	"fmt"
	"github.com/aws/glide-for-redis/go/glide/protobuf"
	"google.golang.org/protobuf/proto"
	"unsafe"
)

type GlideRedisClient struct {
	coreClient unsafe.Pointer
}

type payload struct {
	value      string
	errMessage error
}

type RequestType uint32

type ErrorType uint32

const (
	ClosingError = iota
	RequestError
	TimeoutError
	ExecAbortError
	ConnectionError
)

//export successCallback
func successCallback(channelPtr C.uintptr_t, message *C.char) {
	// TODO: Implement when we implement the command logic
}

//export failureCallback
func failureCallback(channelPtr C.uintptr_t, errMessage *C.char, errType C.RequestErrorType) {
	// TODO: Implement when we implement the command logic
}

func (glideRedisClient *GlideRedisClient) ConnectToRedis(request *protobuf.ConnectionRequest) error {
	marshalledRequest, err := proto.Marshal(request)
	if err != nil {
		return fmt.Errorf("Failed to encode connection request: %v", err)
	}
	byteCount := len(marshalledRequest)
	requestBytes := C.CBytes(marshalledRequest)
	response := (*C.struct_ConnectionResponse)(C.create_client_using_protobuf((*C.uchar)(requestBytes), C.uintptr_t(byteCount), (C.SuccessCallback)(unsafe.Pointer(C.successCallback)), (C.FailureCallback)(unsafe.Pointer(C.failureCallback))))
	defer C.free_connection_response(response)
	if response.error_message != nil {
		return fmt.Errorf(C.GoString(response.error_message))
	}
	glideRedisClient.coreClient = response.conn_ptr
	return nil
}

func (glideRedisClient *GlideRedisClient) CloseClient() error {
	if glideRedisClient.coreClient == nil {
		return fmt.Errorf("Cannot close glide client before it has been created.")
	}
	C.close_client(glideRedisClient.coreClient)
	return nil
}
