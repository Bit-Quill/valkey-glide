/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */
package glide

/*
#cgo LDFLAGS: -L../target/release -lglide_rs
#include "lib.h"

void successCallback(uintptr_t channelPtr, char *message);
void failureCallback(uintptr_t channelPtr, char *errMessage);
*/
import "C"

import (
    "fmt"
    "unsafe"
    "github.com/aws/glide-for-redis/go/glide/protobuf"
    "github.com/golang/protobuf/proto"
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
    goMessage := C.GoString(message)
    goChannelPointer := uintptr(channelPtr)
    resultChannel := *(*chan payload)(unsafe.Pointer(goChannelPointer))
    resultChannel <- payload{value: goMessage, errMessage: nil}
}

//export failureCallback
func failureCallback(channelPtr C.uintptr_t, errMessage *C.char) {
    goMessage := C.GoString(errMessage)
    goChannelPointer := uintptr(channelPtr)
    resultChannel := *(*chan payload)(unsafe.Pointer(goChannelPointer))
    resultChannel <- payload{value: "", errMessage: fmt.Errorf("error at redis operation: %s", goMessage)}
}

func (glideRedisClient *GlideRedisClient) ConnectToRedis(request *protobuf.ConnectionRequest) error {
    marshalledRequest, err := proto.Marshal(request)
    if err != nil {
        return fmt.Errorf("Failed to encode connection request:", err)
    }
    byteCount := len(marshalledRequest)
    requestBytes := C.CBytes(marshalledRequest)
    response := (*C.struct_ConnectionResponse)(C.create_client((*C.uchar)(requestBytes), C.uintptr_t(byteCount), (C.SuccessCallback)(unsafe.Pointer(C.successCallback)), (C.FailureCallback)(unsafe.Pointer(C.failureCallback))))
    defer C.free(unsafe.Pointer(response))
    if response.error != nil {
        return fmt.Errorf(C.GoString(response.error.message))
    }
    glideRedisClient.coreClient = response.conn_ptr
    return nil
}

func (glideRedisClient *GlideRedisClient) CloseClient() {
    C.close_client(glideRedisClient.coreClient)
}
