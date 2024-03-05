/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */

package api

import "C"

type RedisClient struct {
	baseClient
}

func CreateClient(converter connectionRequestConverter) (*RedisClient, error) {
	connPtr, err := createClient(converter)
	if err != nil {
		return nil, err
	}

	return &RedisClient{baseClient{connPtr}}, nil
}
