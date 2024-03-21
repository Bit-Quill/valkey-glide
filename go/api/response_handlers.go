// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package api

import (
	"fmt"
	"reflect"
)

func handleStringResponse(response interface{}, err error) (string, error) {
	if err != nil {
		return "", err
	}

	str, isString := response.(string)
	if !isString {
		return "", &RedisError{fmt.Sprintf("expected string result but got %s", reflect.TypeOf(response))}
	}

	return str, nil
}
