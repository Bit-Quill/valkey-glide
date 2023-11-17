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

type payload struct {
	value      string
	errMessage error
}

//export successCallback
func successCallback(message *C.char, channelPtr C.uintptr_t) {
	goMessage := C.GoString(message)
	goChannelPointer := uintptr(channelPtr)
	resultChannel := *(*chan payload)(unsafe.Pointer(goChannelPointer))
	resultChannel <- payload{value: goMessage, errMessage: nil}
}

//export failureCallback
func failureCallback(errMessage *C.char, channelPtr C.uintptr_t) {
	goMessage := C.GoString(errMessage)
	goChannelPointer := uintptr(channelPtr)
	resultChannel := *(*chan payload)(unsafe.Pointer(goChannelPointer))
	resultChannel <- payload{value: "", errMessage: fmt.Errorf("error at redis operation: %s", goMessage)}
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

	resultChannel := make(chan payload)
	resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

	C.set(babushkaRedisClient.coreClient, ckey, cval, C.uintptr_t(resultChannelPtr))

	resultPayload := <-resultChannel

	return resultPayload.errMessage
}

func (babushkaRedisClient *BabushkaRedisClient) Get(key string) (string, error) {
	ckey := C.CString(key)
	defer C.free(unsafe.Pointer(ckey))

	resultChannel := make(chan payload)
	resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

	C.get(babushkaRedisClient.coreClient, ckey, C.uintptr_t(resultChannelPtr))
	resultPayload := <-resultChannel

	return resultPayload.value, nil
}

func (babushkaRedisClient *BabushkaRedisClient) Ping() (string, error) {
	resultChannel := make(chan payload)
	resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

	C.ping(babushkaRedisClient.coreClient, C.uintptr_t(resultChannelPtr))
	resultPayload := <-resultChannel

	return resultPayload.value, resultPayload.errMessage
}

func (babushkaRedisClient *BabushkaRedisClient) Info() (string, error) {
	resultChannel := make(chan payload)
	resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

	C.info(babushkaRedisClient.coreClient, C.uintptr_t(resultChannelPtr))
	resultPayload := <-resultChannel

	return resultPayload.value, resultPayload.errMessage
}

func (babushkaRedisClient *BabushkaRedisClient) CloseConnection() error {
	C.close_connection(babushkaRedisClient.coreClient)
	return nil
}

func (babushkaRedisClient *BabushkaRedisClient) GetName() string {
	return "babushka"
}
