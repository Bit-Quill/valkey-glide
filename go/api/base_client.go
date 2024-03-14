// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package api

// #cgo LDFLAGS: -L../target/release -lglide_rs
// #include "../lib.h"
//
// void successCallback(void *channelPtr, char *message);
// void failureCallback(void *channelPtr, char *errMessage, RequestErrorType errType);
import "C"

import (
	"unsafe"

	"github.com/aws/glide-for-redis/go/glide/protobuf"
	"google.golang.org/protobuf/proto"
)

type payload struct {
	value *string
	error error
}

//export successCallback
func successCallback(channelPtr unsafe.Pointer, cResponse *C.char) {
	// TODO: free response
	response := C.GoString(cResponse)
	resultChannel := *(*chan payload)(channelPtr)
	resultChannel <- payload{value: &response, error: nil}
}

//export failureCallback
func failureCallback(channelPtr unsafe.Pointer, cErrorMessage *C.char, cErrorType C.RequestErrorType) {
	// TODO: free response
	resultChannel := *(*chan payload)(channelPtr)
	resultChannel <- payload{value: nil, error: goError(cErrorType, cErrorMessage)}
}

type connectionRequestConverter interface {
	toProtobuf() *protobuf.ConnectionRequest
}

type baseClient struct {
	coreClient unsafe.Pointer
}

func createClient(converter connectionRequestConverter) (*baseClient, error) {
	request := converter.toProtobuf()
	msg, err := proto.Marshal(request)
	if err != nil {
		return nil, err
	}

	byteCount := len(msg)
	requestBytes := C.CBytes(msg)
	cResponse := (*C.struct_ConnectionResponse)(
		C.create_client(
			(*C.uchar)(requestBytes),
			C.uintptr_t(byteCount),
			(C.SuccessCallback)(unsafe.Pointer(C.successCallback)),
			(C.FailureCallback)(unsafe.Pointer(C.failureCallback)),
		),
	)
	defer C.free_connection_response(cResponse)

	cErr := cResponse.error_message
	if cErr != nil {
		return nil, goError(cResponse.error_type, cResponse.error_message)
	}

	return &baseClient{cResponse.conn_ptr}, nil
}

// Close terminates the client by closing all associated resources.
func (client *baseClient) Close() {
	if client.coreClient == nil {
		return
	}

	C.close_client(client.coreClient)
	client.coreClient = nil
}

// CustomCommand executes a single command, specified by args, without checking inputs. Every part of the command, including
// the command name and subcommands, should be added as a separate value in args. The returning value depends on the executed
// command.
//
// This function should only be used for single-response commands. Commands that don't return response (such as SUBSCRIBE), or
// that return potentially more than a single response (such as XREAD), or that change the client's behavior (such as entering
// pub/sub mode on RESP2 connections) shouldn't be called using this function.
//
// For example, to return a list of all pub/sub clients:
//
//	client.CustomCommand([]string{"CLIENT", "LIST","TYPE", "PUBSUB"})
func (client *baseClient) CustomCommand(args []string) (interface{}, error) {
	cArgs := toCStrings(args)
	defer freeCStrings(cArgs)

	resultChannel := make(chan payload)
	resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

	C.command(client.coreClient, C.uintptr_t(resultChannelPtr), C.CustomCommand, C.uintptr_t(len(args)), &cArgs[0])

	payload := <-resultChannel
	if payload.error != nil {
		return nil, payload.error
	}

	if payload.value != nil {
		return *payload.value, nil
	}

	return nil, nil
}

func toCStrings(args []string) []*C.char {
	cArgs := make([]*C.char, len(args))
	for i, arg := range args {
		cString := C.CString(arg)
		cArgs[i] = cString
	}
	return cArgs
}

func freeCStrings(cArgs []*C.char) {
	for _, arg := range cArgs {
		C.free(unsafe.Pointer(arg))
	}
}
