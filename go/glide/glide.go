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
)

type GlideRedisClient struct {
	coreClient unsafe.Pointer
}

type payload struct {
	value      string
	errMessage error
}

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

func (glideRedisClient *GlideRedisClient) ConnectToRedis(host string, port int, useSSL bool, clusterModeEnabled bool) error {
	caddress := C.CString(host)
	defer C.free(unsafe.Pointer(caddress))

	response := (C.struct_ConnectionResponse)(C.create_client(caddress, C.uint32_t(port), C._Bool(useSSL), (C.SuccessCallback)(unsafe.Pointer(C.successCallback)), (C.FailureCallback)(unsafe.Pointer(C.failureCallback))))
	glideRedisClient.coreClient = response.conn_ptr
	if glideRedisClient.coreClient == nil {
		return fmt.Errorf("error connecting to glideRedisClient")
	}
	return nil
}

func (glideRedisClient *GlideRedisClient) Set(key string, value interface{}) error {
	strValue := fmt.Sprintf("%v", value)
	ckey := C.CString(key)
	cval := C.CString(strValue)
	defer C.free(unsafe.Pointer(ckey))
	defer C.free(unsafe.Pointer(cval))

	resultChannel := make(chan payload)
	resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

	args := []string{key, strValue}
	argv := make([]*C.char, len(args))
	for index, string := range args {
	    cString := C.CString(string)
	    defer C.free(unsafe.Pointer(cString))
	    argv[index] = cString
	}
	requestType := C.uint32_t(3)
	C.command(glideRedisClient.coreClient, C.uintptr_t(resultChannelPtr), requestType, C.uintptr_t(2), &argv[0])

	resultPayload := <-resultChannel

	return resultPayload.errMessage
}

func (glideRedisClient *GlideRedisClient) Get(key string) (string, error) {
	ckey := C.CString(key)
	defer C.free(unsafe.Pointer(ckey))

	resultChannel := make(chan payload)
	resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

	args := []string{key}
	argv := make([]*C.char, len(args))
	for index, string := range args {
	    cString := C.CString(string)
	    defer C.free(unsafe.Pointer(cString))
	    argv[index] = cString
	}
	requestType := C.uint32_t(2)
	C.command(glideRedisClient.coreClient, C.uintptr_t(resultChannelPtr), requestType, C.uintptr_t(1), &argv[0])
	resultPayload := <-resultChannel

	return resultPayload.value, nil
}

func (glideRedisClient *GlideRedisClient) Ping() (string, error) {
	resultChannel := make(chan payload)
	resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

	args := []string{}
	argv := make([]*C.char, len(args))
	for index, string := range args {
	    cString := C.CString(string)
	    defer C.free(unsafe.Pointer(cString))
	    argv[index] = cString
	}
	requestType := C.uint32_t(4)
	C.command(glideRedisClient.coreClient, C.uintptr_t(resultChannelPtr), requestType, C.uintptr_t(0), &argv[0])
	resultPayload := <-resultChannel

	return resultPayload.value, resultPayload.errMessage
}

func (glideRedisClient *GlideRedisClient) Info() (string, error) {
	resultChannel := make(chan payload)
	resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

	args := []string{}
	argv := make([]*C.char, len(args))
	for index, string := range args {
	    cString := C.CString(string)
	    defer C.free(unsafe.Pointer(cString))
	    argv[index] = cString
	}
	requestType := C.uint32_t(5)
	C.command(glideRedisClient.coreClient, C.uintptr_t(resultChannelPtr), requestType, C.uintptr_t(0), &argv[0])
	resultPayload := <-resultChannel

	return resultPayload.value, resultPayload.errMessage
}

func (glideRedisClient *GlideRedisClient) CloseClient() {
	C.close_client(glideRedisClient.coreClient)
}
