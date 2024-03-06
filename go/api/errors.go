// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package api

// #cgo LDFLAGS: -L../target/release -lglide_rs
// #include "../lib.h"
import "C"
import "unsafe"

type ClosingError struct {
	msg string
}

func (e ClosingError) Error() string { return e.msg }

type TimeoutError struct {
	msg string
}

func (e *TimeoutError) Error() string { return e.msg }

type ExecAbortError struct {
	msg string
}

func (e *ExecAbortError) Error() string { return e.msg }

type ConnectionError struct {
	msg string
}

func (e *ConnectionError) Error() string { return e.msg }

type RequestError struct {
	msg string
}

func (e *RequestError) Error() string { return e.msg }

func errorFromType(errorType C.enum_ErrorType, msg string) error {
	switch errorType {
	case C.ClosingError:
		return &ClosingError{msg}
	case C.TimeoutError:
		return &TimeoutError{msg}
	case C.ExecAbortError:
		return &ExecAbortError{msg}
	case C.ConnectionError:
		return &ConnectionError{msg}
	default:
		return &RequestError{msg}
	}
}

func redisErrorFromCError(CErr *C.struct_RedisErrorFFI) error {
	CMsg := (*CErr).message
	defer C.free(unsafe.Pointer(CErr))
	defer C.free(unsafe.Pointer(CMsg))

	errorType := (*CErr).error_type
	msg := C.GoString(CMsg)

	return errorFromType(errorType, msg)
}
