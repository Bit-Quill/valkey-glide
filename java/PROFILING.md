# Install tools

You need to install `dotnet` and `ansi2txt`. The last one used to remove coloring from `babushka`'s logs.

```
sudo apt-get install -y dotnet-sdk-7.0 colorized-logs
```

# Run test

Application output should be saved into a file using a tool or manually.

```
./gradlew :benchmarks:run | ansi2txt > jni-netty.log
./gradlew :benchmarks:run | tee >(ansi2txt > log.txt)
```
The second command prints output to the file and to console both (buffered, doesn't print in real time).

# Analyze log

```
dotnet run --project netty-log-parser/netty-log-parser.csproj jni-netty.log
```

# Modify or create a test

1. Print markers before and after each test:
```
++++ START OF TEST ++++
++++ END OF TEST ++++
```
2. Call `dumpStats` to log test measurements
3. Use new client instance for every test or call `resetCounters`
