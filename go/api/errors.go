// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package api

const (
	closingError = iota
	timeoutError
	execAbortError
	connectionError
	unspecified
)

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
