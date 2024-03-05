// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package api

// #cgo LDFLAGS: -L../target/release -lglide_rs
// #include "../lib.h"
//
// void successCallback(uintptr_t channelPtr, char *message);
// void failureCallback(uintptr_t channelPtr, char *errMessage);
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
func failureCallback(channelPtr C.uintptr_t, errMessage *C.char) {
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
	CResponse := (*C.struct_ConnectionResponse)(C.create_client((*C.uchar)(requestBytes), C.uintptr_t(byteCount), (C.SuccessCallback)(unsafe.Pointer(C.successCallback)), (C.FailureCallback)(unsafe.Pointer(C.failureCallback))))
	defer C.free(unsafe.Pointer(CResponse))

	CErr := CResponse.error
	if CErr != nil {
		CMsg := (*CErr).message
		msg := C.GoString(CMsg)
		C.free(unsafe.Pointer(CMsg))
		C.free(unsafe.Pointer(CErr))
		return nil, &ClosingError{msg}
	}

	return CResponse.conn_ptr, nil
}

func (client *baseClient) Close() {
	C.close_client(client.coreClient)
}
