# Rust core

## Prerequisites

Please consider installing the following packages:

- GCC
- pkg-config
- openssl
- openssl-dev
- rust
- protoc (protobuf compiler)

**Protoc installation**

Download a binary matching your system from the [official release page](https://github.com/protocolbuffers/protobuf/releases/tag/v25.1) and make it accessible in your $PATH by moving it or creating a symlink.
For example, on Linux you can copy it to `/usr/bin`:

```bash
sudo cp protoc /usr/bin/
```

Check that the protobuf compiler is installed:
```bash
protoc --version
```

**Dependencies installation for Ubuntu**

```bash
sudo apt update -y
sudo apt install -y libssl-dev openssl gcc curl pkg-config
```

**Dependencies installation for MacOS**

```bash
brew update
brew install gcc pkgconfig openssl curl
```

**Dependencies installation for CentOS**

```bash
sudo yum update -y
sudo yum install -y gcc pkgconfig openssl openssl-devel curl
```

**Dependencies installation for Amazon Linux**

```bash
sudo yum update -y
sudo yum install -y gcc pkgconfig openssl openssl-devel which curl redis6 gettext --allowerasing
```

**Rust toolchain installation**

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
source "$HOME/.cargo/env"
```

Check that the Rust compiler is installed:
```bash
rustc --version
```

## Recommended VSCode extensions

[rust-analyzer](https://marketplace.visualstudio.com/items?itemName=rust-lang.rust-analyzer) - Rust language server.
[CodeLLDB](https://marketplace.visualstudio.com/items?itemName=vadimcn.vscode-lldb) - Debugger.
[Even Better TOML](https://marketplace.visualstudio.com/items?itemName=tamasfe.even-better-toml) - TOML language support.
