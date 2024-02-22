install-tools-go1.18:
	go install github.com/vakenbolt/go-test-report@v0.9.3
	go install google.golang.org/protobuf/cmd/protoc-gen-go@v1.32.0
	go install mvdan.cc/gofumpt@v0.4.0
	go install golang.org/x/tools/cmd/goimports@v0.18.0
	go install honnef.co/go/tools/cmd/staticcheck@v0.4.6

install-tools:
	go install github.com/vakenbolt/go-test-report@v0.9.3
	go install google.golang.org/protobuf/cmd/protoc-gen-go@v1.32.0
	go install mvdan.cc/gofumpt@v0.6.0
	go install golang.org/x/tools/cmd/goimports@v0.18.0
	go install honnef.co/go/tools/cmd/staticcheck@v0.3.3

build: build-glide-core build-glide-client generate-protobuf
	go build ./...

build-glide-core:
	cd ../glide-core; cargo build --release

build-glide-client:
	cargo build --release

generate-protobuf:
	mkdir -p protobuf
	protoc --proto_path=../glide-core/src/protobuf \
		--go_opt=Mconnection_request.proto=github.com/aws/glide-for-redis/go/protobuf \
		--go_opt=Mredis_request.proto=github.com/aws/glide-for-redis/go/protobuf \
		--go_opt=Mresponse.proto=github.com/aws/glide-for-redis/go/protobuf \
		--go_out=./protobuf \
		--go_opt=paths=source_relative \
		../glide-core/src/protobuf/*.proto

lint:
	go vet ./...
	staticcheck ./...
	if [ "$$(gofumpt -l . | wc -l)" -gt 0 ]; then exit 1; fi
	if [ "$$(golines -l --shorten-comments -m 127 . | wc -l)" -gt 0 ]; then exit 1; fi
	if [ "$$(goimports -l . | wc -l)" -gt 0 ]; then exit 1; fi

format:
	gofumpt -w .
	golines -w --shorten-comments -m 127 .
	goimports -w .

unit-test-report:
	mkdir -p reports
	go test -race ./... -json | go-test-report -o reports/unit-test-report.html
