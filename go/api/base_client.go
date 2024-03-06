// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package api

// #cgo LDFLAGS: -L../target/release -lglide_rs
// #include "../lib.h"
//
// void successCallback(uintptr_t channelPtr, char *message);
// void failureCallback(uintptr_t channelPtr, struct RedisErrorFFI *cErr);
import "C"

import (
	"github.com/aws/glide-for-redis/go/glide/protobuf"
	"google.golang.org/protobuf/proto"
	"unsafe"
)

//export successCallback
func successCallback(channelPtr C.uintptr_t, message *C.char) {
	// TODO: Implement this function when command logic is added
	return
}

//export failureCallback
func failureCallback(channelPtr C.uintptr_t, cErr *C.RedisErrorFFI) {
	// TODO: Implement this function when command logic is added
	return
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
	cResponse := (*C.struct_ConnectionResponse)(C.create_client((*C.uchar)(requestBytes), C.uintptr_t(byteCount), (C.SuccessCallback)(unsafe.Pointer(C.successCallback)), (C.FailureCallback)(unsafe.Pointer(C.failureCallback))))
	defer C.free_connection_response(cResponse)

	cErr := cResponse.error
	if cErr != nil {
		defer C.free_error(cErr)
		return nil, redisErrorFromCError(cErr)
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
