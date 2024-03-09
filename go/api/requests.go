// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

package api

// #cgo LDFLAGS: -L../target/release -lglide_rs
// #include "../lib.h"
import "C"

type payload struct {
	value *string
	error error
}

type RequestType uint32

const (
	_                         = iota
	customCommand RequestType = iota
	getString
	setString
	ping
	info
	del
	selectDB
	configGet
	configSet
	configResetStat
	configRewrite
	clientGetName
	clientGetRedir
	clientId
	clientInfo
	clientKill
	clientList
	clientNoEvict
	clientNoTouch
	clientPause
	clientReply
	clientSetInfo
	clientSetName
	clientUnblock
	clientUnpause
	expire
	hashSet
	hashGet
	hashDel
	hashExists
	mGet
	mSet
	incr
	incrBy
	decr
	incrByFloat
	decrBy
	hashGetAll
	hashMSet
	hashMGet
	hashIncrBy
	hashIncrByFloat
	lPush
	lPop
	rPush
	rPop
	lLen
	lRem
	lRange
	lTrim
	sAdd
	sRem
	sMembers
	sCard
	pExpireAt
	pExpire
	expireAt
	exists
	unlink
	ttl
	zAdd
	zRem
	zRange
	zCard
	zCount
	zIncrBy
	zScore
	keyType
	hLen
	echo
	zPopMin
	strlen
	lIndex
	zPopMax
	xRead
	xAdd
	xReadGroup
	xAck
	xTrim
	xGroupCreate
	xGroupDestroy
)
