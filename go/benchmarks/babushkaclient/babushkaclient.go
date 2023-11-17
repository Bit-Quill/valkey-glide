package babushkaclient

/*
#cgo LDFLAGS: -L./target/release -lbabushkaclient
#include "lib.h"

void successCallback(char *message, uintptr_t channelPtr);
void failureCallback(char *errMessage, uintptr_t channelPtr);
*/
import "C"

import (
	"fmt"
	"unsafe"

	"github.com/aws/babushka/go/benchmarks"
)

type BabushkaRedisClient struct {
	coreClient unsafe.Pointer
}

type ValueOrError struct {
	value      string
	errMessage error
}

//export successCallback
func successCallback(message *C.char, channelPtr C.uintptr_t) {
	goMessage := C.GoString(message)
	channelAddress := uintptr(channelPtr)
	channel := *(*chan ValueOrError)(unsafe.Pointer(channelAddress))
	channel <- ValueOrError{value: goMessage, errMessage: nil}
}

//export failureCallback
func failureCallback(errMessage *C.char, channelPtr C.uintptr_t) {
	goMessage := C.GoString(errMessage)
	channelAddress := uintptr(channelPtr)
	channel := *(*chan ValueOrError)(unsafe.Pointer(channelAddress))
	channel <- ValueOrError{value: "", errMessage: fmt.Errorf("error at redis operation: %s", goMessage)}
}

func (babushkaRedisClient *BabushkaRedisClient) ConnectToRedis(connectionSettings *benchmarks.ConnectionSettings) error {
	caddress := C.CString(connectionSettings.Host)
	defer C.free(unsafe.Pointer(caddress))

	babushkaRedisClient.coreClient = C.create_connection(caddress, C.uint32_t(connectionSettings.Port), C._Bool(connectionSettings.UseSsl), C._Bool(connectionSettings.ClusterModeEnabled), (C.SuccessCallback)(unsafe.Pointer(C.successCallback)), (C.FailureCallback)(unsafe.Pointer(C.failureCallback)))
	if babushkaRedisClient.coreClient == nil {
		return fmt.Errorf("error connecting to babushkaRedisClient")
	}
	return nil
}

func (babushkaRedisClient *BabushkaRedisClient) Set(key string, value interface{}) error {
	strValue := fmt.Sprintf("%v", value)
	ckey := C.CString(key)
	cval := C.CString(strValue)
	defer C.free(unsafe.Pointer(ckey))
	defer C.free(unsafe.Pointer(cval))

	result := make(chan ValueOrError)
	chAddress := uintptr(unsafe.Pointer(&result))

	C.set(babushkaRedisClient.coreClient, ckey, cval, C.uintptr_t(chAddress))

	valueOrError := <-result

	return valueOrError.errMessage
}

func (babushkaRedisClient *BabushkaRedisClient) Get(key string) (string, error) {
	ckey := C.CString(key)
	defer C.free(unsafe.Pointer(ckey))

	result := make(chan ValueOrError)
	chAddress := uintptr(unsafe.Pointer(&result))

	C.get(babushkaRedisClient.coreClient, ckey, C.uintptr_t(chAddress))
	valueOrError := <-result
	if valueOrError.errMessage != nil {
		return "", valueOrError.errMessage
	}

	return valueOrError.value, nil
}

func (babushkaRedisClient *BabushkaRedisClient) Ping() (string, error) {
	result := make(chan ValueOrError)
	chAddress := uintptr(unsafe.Pointer(&result))

	C.ping(babushkaRedisClient.coreClient, C.uintptr_t(chAddress))
	valueOrError := <-result
	if valueOrError.errMessage != nil {
		return "", valueOrError.errMessage
	}

	return valueOrError.value, nil
}

func (babushkaRedisClient *BabushkaRedisClient) Info() (string, error) {
	result := make(chan ValueOrError)
	chAddress := uintptr(unsafe.Pointer(&result))

	C.info(babushkaRedisClient.coreClient, C.uintptr_t(chAddress))
	valueOrError := <-result
	if valueOrError.errMessage != nil {
		return "", valueOrError.errMessage
	}

	return valueOrError.value, nil
}

func (babushkaRedisClient *BabushkaRedisClient) CloseConnection() error {
	C.close_connection(babushkaRedisClient.coreClient)
	return nil
}

func (babushkaRedisClient *BabushkaRedisClient) GetName() string {
	return "babushka"
}
