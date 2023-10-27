package asyncClientRawFFI

/*
#cgo LDFLAGS: -L./target/debug -lgorustffi
#include "lib.h"

void successCallback(uintptr_t id , char *cstr1, uintptr_t channelPtr);
void failureCallback(uintptr_t id);
*/
import "C"

import (
	"fmt"
	"github.com/aws/babushka/go/benchmarks"
	"unsafe"
)

// TODO proper error handling for all functions
type AsyncRedisClient struct {
	coreClient unsafe.Pointer
}

//export successCallback
func successCallback(connectionID C.uintptr_t, message *C.char, channelPtr C.uintptr_t) {
	goMessage := C.GoString(message)
	returnedAddress := uintptr(channelPtr)
	channel := *(*chan string)(unsafe.Pointer(returnedAddress))
	channel <- goMessage
}

//export failureCallback
func failureCallback(connectionID C.uintptr_t) {
	panic("In failure callback get or set")
}

func (asyncRedisClient *AsyncRedisClient) ConnectToRedis(connectionSettings *benchmarks.ConnectionSettings) error {
	address := fmt.Sprintf("redis://%s:%d", connectionSettings.Host, connectionSettings.Port)
	caddress := C.CString(address)
	defer C.free(unsafe.Pointer(caddress))

	asyncRedisClient.coreClient = C.create_connection(caddress, (C.success_callback)(unsafe.Pointer(C.successCallback)), (C.failure_callback)(unsafe.Pointer(C.failureCallback)))
	if asyncRedisClient.coreClient == nil {
		return fmt.Errorf("error connecting to asyncRedisClient")
	}
	return nil
}

func (asyncRedisClient *AsyncRedisClient) Set(key string, value interface{}) error {
	strValue := fmt.Sprintf("%v", value)
	ckey := C.CString(key)
	cval := C.CString(strValue)
	defer C.free(unsafe.Pointer(ckey))
	defer C.free(unsafe.Pointer(cval))

	result := make(chan string)
	chAddress := uintptr(unsafe.Pointer(&result))

	C.set(asyncRedisClient.coreClient, C.uintptr_t(1), ckey, cval, C.uintptr_t(chAddress))

	<-result

	return nil
}

func (asyncRedisClient *AsyncRedisClient) Get(key string) (string, error) {
	ckey := C.CString(key)
	defer C.free(unsafe.Pointer(ckey))

	result := make(chan string)
	chAddress := uintptr(unsafe.Pointer(&result)) //Gives you the raw memory address of the result variable as an integer.

	C.get(asyncRedisClient.coreClient, C.uintptr_t(1), ckey, C.uintptr_t(chAddress))
	value := <-result

	return value, nil
}

func (asyncRedisClient *AsyncRedisClient) CloseConnection() error {
	C.close_connection(asyncRedisClient.coreClient)
	return nil
}

func (asyncRedisClient *AsyncRedisClient) GetName() string {
	return "babushka"
}
