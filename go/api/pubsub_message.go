// Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0

package api

// #include "../lib.h"
import "C"

import (
	"encoding/json"
)

type PubSubMessage struct {
	Message string
	Channel string
	Pattern Result[string]
}

func NewPubSubMessage(message, channel string) *PubSubMessage {
	return &PubSubMessage{
		Message: message,
		Channel: channel,
		Pattern: CreateNilStringResult(),
	}
}

func NewPubSubMessageWithPattern(message, channel, pattern string) *PubSubMessage {
	return &PubSubMessage{
		Message: message,
		Channel: channel,
		Pattern: CreateStringResult(pattern),
	}
}

func (msg *PubSubMessage) ToString() string {
	jsonBytes, err := json.Marshal(msg)
	if err != nil {
		return ""
	}
	return string(jsonBytes)
}

// PushInfo represents a message received from the server
type PushInfo struct {
	Kind    C.PushKind
	Message *PubSubMessage
}
