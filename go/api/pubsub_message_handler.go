// Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0

package api

import (
	"errors"
	"fmt"
	"log"
	"sync"
)

var (
	ErrPubSubPushInvalid       = errors.New("received invalid push: empty or in incorrect format")
	ErrPubSubPushMissingKind   = errors.New("received invalid push: missing kind field")
	ErrPubSubPushMissingValues = errors.New("received invalid push: missing values field")
)

type PushKind string

const (
	Disconnection PushKind = "Disconnection"
	Other         PushKind = "Other"
	Invalidate    PushKind = "Invalidate"
	Message       PushKind = "Message"
	PMessage      PushKind = "PMessage"
	SMessage      PushKind = "SMessage"
	Unsubscribe   PushKind = "Unsubscribe"
	PUnsubscribe  PushKind = "PUnsubscribe"
	SUnsubscribe  PushKind = "SUnsubscribe"
	Subscribe     PushKind = "Subscribe"
	PSubscribe    PushKind = "PSubscribe"
	SSubscribe    PushKind = "SSubscribe"
)

func (kind PushKind) String() string {
	return string(kind)
}

type MessageCallbackError struct {
	cause error
}

func (e *MessageCallbackError) Error() string {
	return fmt.Sprintf("error in message callback: %v", e.cause)
}

func (e *MessageCallbackError) Cause() error {
	return e.cause
}

// *** Message Handler ***

type ResponseResolver func(response any) (any, error)

type MessageHandler struct {
	callback MessageCallback
	context  any
	resolver ResponseResolver
	queue    *PubSubMessageQueue
}

func NewMessageHandler(callback MessageCallback, context any, resolver ResponseResolver) *MessageHandler {
	return &MessageHandler{
		callback: callback,
		context:  context,
		resolver: resolver,
		queue:    NewPubSubMessageQueue(),
	}
}

// Handle processes the incoming response and invokes the callback if available
func (handler *MessageHandler) Handle(response any) error {
	data, err := handler.resolver(response)
	if err != nil {
		return err
	}

	push, ok := data.(map[string]any)
	if !ok {
		log.Println("invalid push", ErrPubSubPushInvalid.Error())
		return ErrPubSubPushInvalid
	}

	kindStr, ok := push["kind"].(string)
	if !ok {
		log.Println("invalid push", ErrPubSubPushMissingKind.Error())
		return ErrPubSubPushMissingKind
	}

	kind := PushKind(kindStr)
	values, ok := push["values"].([]any)
	if !ok {
		log.Println("invalid push", ErrPubSubPushMissingValues.Error())
		return ErrPubSubPushMissingValues
	}

	switch kind {
	case Disconnection:
		log.Println("disconnect notification", "Transport disconnected, messages might be lost")
	case PMessage:
		// Pattern message: values are [pattern, channel, message]
		if len(values) < 3 {
			return fmt.Errorf("invalid PMessage: expected 3 values, got %d", len(values))
		}
		pattern := CreateStringResult(string(values[0].([]byte))) // Result[string]
		channel := string(values[1].([]byte))                     // string
		message := string(values[2].([]byte))                     // string
		return handler.handleMessage(NewPubSubMessageWithPattern(message, channel, pattern.Value()))

	case Message, SMessage:
		if len(values) < 2 {
			return fmt.Errorf("invalid Message/SMessage: expected 2 values, got %d", len(values))
		}
		channel := string(values[0].([]byte)) // string
		message := string(values[1].([]byte)) // string
		return handler.handleMessage(NewPubSubMessage(message, channel))

	case Subscribe, PSubscribe, SSubscribe, Unsubscribe, PUnsubscribe, SUnsubscribe:
		valuesStr := make([]string, len(values))
		for i, v := range values {
			valuesStr[i] = string(v.([]byte))
		}
		log.Println("subscribe/unsubscribe notification",
			fmt.Sprintf("Received push notification of type '%s': %v", kind, valuesStr))

	default:
		log.Println("unknown notification",
			fmt.Sprintf("Unknown notification message: '%s'", kind))
	}

	return nil
}

func (handler *MessageHandler) handleMessage(message *PubSubMessage) error {
	if handler.callback != nil {
		defer func() {
			if r := recover(); r != nil {
				err, ok := r.(error)
				if !ok {
					err = fmt.Errorf("%v", r)
				}
				log.Println("panic in message callback", err.Error())
			}
		}()

		handler.callback(message, handler.context)
		return nil
	}

	handler.queue.Push(message)
	return nil
}

func (handler *MessageHandler) GetQueue() *PubSubMessageQueue {
	return handler.queue
}

// *** Message Queue ***

type PubSubMessageQueue struct {
	mu                      sync.Mutex
	messages                []*PubSubMessage
	waiters                 []chan *PubSubMessage
	nextMessageReadyCh      chan struct{}
	nextMessageReadySignals []chan struct{}
}

func NewPubSubMessageQueue() *PubSubMessageQueue {
	return &PubSubMessageQueue{
		messages:           make([]*PubSubMessage, 0),
		waiters:            make([]chan *PubSubMessage, 0),
		nextMessageReadyCh: make(chan struct{}, 1),
	}
}

func (queue *PubSubMessageQueue) Push(message *PubSubMessage) {
	queue.mu.Lock()
	defer queue.mu.Unlock()

	// If there's a waiter, deliver the message directly
	if len(queue.waiters) > 0 {
		waiterCh := queue.waiters[0]
		queue.waiters = queue.waiters[1:]
		waiterCh <- message
		return
	}

	// Otherwise, add to the queue
	queue.messages = append(queue.messages, message)

	// Signal that a new message is ready
	select {
	case queue.nextMessageReadyCh <- struct{}{}:
	default:
		// Channel already has a signal
	}

	// Signal any waiters
	for _, ch := range queue.nextMessageReadySignals {
		select {
		case ch <- struct{}{}:
		default:
			// Channel is full, receiver might not be listening
		}
	}
}

func (queue *PubSubMessageQueue) Pop() *PubSubMessage {
	queue.mu.Lock()
	defer queue.mu.Unlock()

	if len(queue.messages) == 0 {
		return nil
	}

	message := queue.messages[0]
	queue.messages = queue.messages[1:]
	return message
}

func (queue *PubSubMessageQueue) WaitForMessage() <-chan *PubSubMessage {
	queue.mu.Lock()
	defer queue.mu.Unlock()

	// If a message is already queued, return it immediately
	if len(queue.messages) > 0 {
		messageCh := make(chan *PubSubMessage, 1)
		message := queue.messages[0]
		queue.messages = queue.messages[1:]
		messageCh <- message
		return messageCh
	}

	// Otherwise register a waiter
	messageCh := make(chan *PubSubMessage, 1)
	queue.waiters = append(queue.waiters, messageCh)
	return messageCh
}

func (queue *PubSubMessageQueue) RegisterSignalChannel(ch chan struct{}) {
	queue.mu.Lock()
	defer queue.mu.Unlock()
	queue.nextMessageReadySignals = append(queue.nextMessageReadySignals, ch)
}

func (queue *PubSubMessageQueue) UnregisterSignalChannel(ch chan struct{}) {
	queue.mu.Lock()
	defer queue.mu.Unlock()

	for idx, channel := range queue.nextMessageReadySignals {
		if channel == ch {
			queue.nextMessageReadySignals = append(queue.nextMessageReadySignals[:idx], queue.nextMessageReadySignals[idx+1:]...)
			break
		}
	}
}
