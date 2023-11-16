package javababushka;

import static connection_request.ConnectionRequestOuterClass.AddressInfo;
import static connection_request.ConnectionRequestOuterClass.AuthenticationInfo;
import static connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import static connection_request.ConnectionRequestOuterClass.ConnectionRetryStrategy;
import static connection_request.ConnectionRequestOuterClass.ReadFromReplicaStrategy;
import static connection_request.ConnectionRequestOuterClass.TlsMode;
import static redis_request.RedisRequestOuterClass.Command;
import static redis_request.RedisRequestOuterClass.Command.ArgsArray;
import static redis_request.RedisRequestOuterClass.RedisRequest;
import static redis_request.RedisRequestOuterClass.RequestType;
import static redis_request.RedisRequestOuterClass.Routes;
import static redis_request.RedisRequestOuterClass.SimpleRoutes;
import static response.ResponseOuterClass.Response;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

public class Client {
  private static final long DEFAULT_TIMEOUT_MILLISECONDS = 1000;

  private final NettyWrapper innerClient = NettyWrapper.INSTANCE;

  public void set(String key, String value) {
    waitForResult(asyncSet(key, value));
    // TODO parse response and rethrow an exception if there is an error
  }

  public String get(String key) {
    return waitForResult(asyncGet(key));
    // TODO support non-strings
  }

  private synchronized Pair<Integer, CompletableFuture<Response>> getNextCallback() {
    var future = new CompletableFuture<Response>();
    var callbackId = new Random().nextInt();
    innerClient.registerRequest(callbackId, future);
    return Pair.of(callbackId, future);
  }

  public Future<Response> asyncConnectToRedis(
      String host, int port, boolean useSsl, boolean clusterMode) {
    var request =
        ConnectionRequest.newBuilder()
            .addAddresses(AddressInfo.newBuilder().setHost(host).setPort(port).build())
            .setTlsMode(
                useSsl // TODO: secure or insecure TLS?
                    ? TlsMode.SecureTls
                    : TlsMode.NoTls)
            .setClusterModeEnabled(clusterMode)
            // In millis
            .setResponseTimeout(250)
            // In millis
            .setClientCreationTimeout(2500)
            .setReadFromReplicaStrategy(ReadFromReplicaStrategy.AlwaysFromPrimary)
            .setConnectionRetryStrategy(
                ConnectionRetryStrategy.newBuilder()
                    .setNumberOfRetries(1)
                    .setFactor(1)
                    .setExponentBase(1)
                    .build())
            .setAuthenticationInfo(
                AuthenticationInfo.newBuilder().setPassword("").setUsername("default").build())
            .setDatabaseId(0)
            .build();

    var future = new CompletableFuture<Response>();
    // connection request has hardcoded callback id = 0
    // https://github.com/aws/babushka/issues/600
    innerClient.registerRequest(0, future);
    innerClient.getChannel().writeAndFlush(request.toByteArray());
    return future;
  }

  private CompletableFuture<Response> submitNewCommand(RequestType command, List<String> args) {
    var commandId = getNextCallback();
    // System.out.printf("== %s(%s), callback %d%n", command, String.join(", ", args), commandId);

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
              innerClient.getChannel().writeAndFlush(request.toByteArray());
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
                    ? RustWrapper.valueFromPointer(response.getRespPointer()).toString()
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
