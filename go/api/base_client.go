// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package api

// #cgo LDFLAGS: -L./target/release -lglide-rs
// #include "lib.h"
import "C"
import (
	"github.com/aws/glide-for-redis/go/glide/protobuf"
	"google.golang.org/protobuf/proto"
	"unsafe"
)

//export successCallback
func successCallback() {
	return
}

//export failureCallback
func failureCallback() {
	return
}

type connectionRequestConverter interface {
	toProtobuf() *protobuf.ConnectionRequest
}

type baseClient struct {
	connectionPtr unsafe.Pointer
}

func createClient(converter connectionRequestConverter) (unsafe.Pointer, error) {
	request := converter.toProtobuf()
	msg, err := proto.Marshal(request)
	if err != nil {
		return nil, err
	}

	CResponse := C.create_connection(
		(*C.uint8_t)(&msg[0]),
		C.uint(len(msg)),
		(C.SuccessCallback)(unsafe.Pointer(C.successCallback)),
		(C.FailureCallback)(unsafe.Pointer(C.failureCallback)))
	defer C.free(CResponse)

	CErr := CResponse._err
	if CErr != nil {
		msg := C.GoString((*CErr)._msg)
		C.free((*CErr)._msg)
		C.free((*CErr)._errorType)
		C.free(CErr)
		return nil, &RedisError{msg}
	}

	return CResponse.connPtr, nil
}
