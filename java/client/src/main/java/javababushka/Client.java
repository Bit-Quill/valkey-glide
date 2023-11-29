package javababushka;

import static connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import static redis_request.RedisRequestOuterClass.Command;
import static redis_request.RedisRequestOuterClass.Command.ArgsArray;
import static redis_request.RedisRequestOuterClass.RedisRequest;
import static redis_request.RedisRequestOuterClass.RequestType;
import static redis_request.RedisRequestOuterClass.Routes;
import static redis_request.RedisRequestOuterClass.SimpleRoutes;
import static response.ResponseOuterClass.Response;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

public class Client {
  private static final long DEFAULT_TIMEOUT_MILLISECONDS = 1000;
  private static final int REQUEST_TIMEOUT_MILLISECONDS = 250;

  private NettyWrapper wrapper;

  public Client() {
    this(NettyWrapper.getInstance());
  }

  public Client(NettyWrapper nettyWrapper) {
    wrapper = nettyWrapper;
  }

  public void set(String key, String value) {
    waitForResult(asyncSet(key, value));
    // TODO parse response and rethrow an exception if there is an error
  }

  public String get(String key) {
    return waitForResult(asyncGet(key));
    // TODO support non-strings
  }

  public void connectToRedis(String host, int port, boolean useSsl, boolean clusterMode) {
    waitForResult(asyncConnectToRedis(host, port, useSsl, clusterMode));
  }

  private synchronized Pair<Integer, CompletableFuture<Response>> getNextCallback() {
    var future = new CompletableFuture<Response>();
    int callbackId = wrapper.registerRequest(future);
    return Pair.of(callbackId, future);
  }

  public static ConnectionRequest.Builder getConnectionRequest(
      String host, int port, boolean useSsl, boolean clusterMode) {
    ConnectionRequest.Builder connectionRequest = ConnectionRequest.newBuilder();
    connectionRequest.addAddresses(
        connection_request.ConnectionRequestOuterClass.NodeAddress.newBuilder()
            .setHost(host)
            .setPort(port)
            .build());
    connectionRequest.setTlsMode(
        useSsl
            ? connection_request.ConnectionRequestOuterClass.TlsMode.SecureTls
            : connection_request.ConnectionRequestOuterClass.TlsMode.NoTls);
    connectionRequest.setClusterModeEnabled(clusterMode);
    connectionRequest
        .setReadFrom(connection_request.ConnectionRequestOuterClass.ReadFrom.Primary)
        .setConnectionRetryStrategy(
            connection_request.ConnectionRequestOuterClass.ConnectionRetryStrategy.newBuilder()
                .setNumberOfRetries(1)
                .setFactor(1)
                .setExponentBase(1)
                .build())
        .setAuthenticationInfo(
            connection_request.ConnectionRequestOuterClass.AuthenticationInfo.newBuilder()
                .setPassword("")
                .setUsername("default")
                .build())
        .setDatabaseId(0);

    return connectionRequest;
  }

  public CompletableFuture<Response> asyncConnectToRedis(
      String host, int port, boolean useSsl, boolean clusterMode) {
    var request = getConnectionRequest(host, port, useSsl, clusterMode).build();

    // connection request has hardcoded callback id = 0
    // https://github.com/aws/babushka/issues/600
    var future = new CompletableFuture<Response>();
    wrapper.registerConnection(future);
    wrapper.writeAndFlush(request);
    return future;
  }

  private CompletableFuture<Response> submitNewCommand(RequestType command, List<String> args) {
    var commandId = getNextCallback();
    // System.out.printf("== %s(%s), callback %d%n", command, String.join(", ", args), commandId);

    // TODO this explicitly uses ForkJoin thread pool. May be we should use another one.
    return CompletableFuture.supplyAsync(
            () -> {
              var commandArgs = ArgsArray.newBuilder();
              for (var arg : args) {
                commandArgs.addArgs(arg);
              }

              RedisRequest request =
                  RedisRequest.newBuilder()
                      .setCallbackIdx(commandId.getKey())
                      .setSingleCommand(
                          Command.newBuilder()
                              .setRequestType(command)
                              .setArgsArray(commandArgs.build())
                              .build())
                      .setRoute(Routes.newBuilder().setSimpleRoutes(SimpleRoutes.AllNodes).build())
                      .build();
              wrapper.writeAndFlush(request);
              return commandId.getValue();
            })
        .thenCompose(f -> f);
  }

  public Future<Response> asyncSet(String key, String value) {
    // System.out.printf("== set(%s, %s), callback %d%n", key, value, callbackId);
    return submitNewCommand(RequestType.SetString, List.of(key, value));
  }

  public Future<String> asyncGet(String key) {
    // System.out.printf("== get(%s), callback %d%n", key, callbackId);
    return submitNewCommand(RequestType.GetString, List.of(key))
        .thenApply(
            response ->
                response.getRespPointer() != 0
                    ? BabushkaCoreNativeDefinitions.valueFromPointer(response.getRespPointer())
                        .toString()
                    : null);
  }

  public <T> T waitForResult(Future<T> future) {
    return waitForResult(future, DEFAULT_TIMEOUT_MILLISECONDS);
  }

  public <T> T waitForResult(Future<T> future, long timeout) {
    try {
      return future.get(timeout, TimeUnit.MILLISECONDS);
    } catch (Exception ignored) {
      return null;
    }
  }
}
