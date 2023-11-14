package babushkaclient

/*
#cgo LDFLAGS: -L./target/release -lbabushkaclient
#include "lib.h"

void successCallback(char *message, uintptr_t channelPtr);
void failureCallback(uintptr_t channelPtr);
*/
import "C"

import (
	"fmt"
	"github.com/aws/babushka/go/benchmarks"
	"unsafe"
)

type BabushkaRedisClient struct {
	coreClient unsafe.Pointer
}

//export successCallback
func successCallback(message *C.char, channelPtr C.uintptr_t) {
	goMessage := C.GoString(message)
	channelAddress := uintptr(channelPtr)
	channel := *(*chan string)(unsafe.Pointer(channelAddress))
	channel <- goMessage
}

//export failureCallback
func failureCallback(channelPtr C.uintptr_t) {
	panic("Failure for get or set")
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

	result := make(chan string)
	chAddress := uintptr(unsafe.Pointer(&result))

	C.set(babushkaRedisClient.coreClient, ckey, cval, C.uintptr_t(chAddress))

	<-result

	return nil
}

func (babushkaRedisClient *BabushkaRedisClient) Get(key string) (string, error) {
	ckey := C.CString(key)
	defer C.free(unsafe.Pointer(ckey))

	result := make(chan string)
	chAddress := uintptr(unsafe.Pointer(&result))

	C.get(babushkaRedisClient.coreClient, ckey, C.uintptr_t(chAddress))
	value := <-result

	return value, nil
}

func (babushkaRedisClient *BabushkaRedisClient) CloseConnection() error {
	C.close_connection(babushkaRedisClient.coreClient)
	return nil
}

func (babushkaRedisClient *BabushkaRedisClient) GetName() string {
	return "babushka"
}
