package babushka.api;

import static babushka.api.models.configuration.NodeAddress.DEFAULT_HOST;
import static babushka.api.models.configuration.NodeAddress.DEFAULT_PORT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import babushka.api.models.configuration.*;
import babushka.connectors.handlers.ChannelHandler;
import babushka.managers.ConnectionManager;
import connection_request.ConnectionRequestOuterClass;
import connection_request.ConnectionRequestOuterClass.AuthenticationInfo;
import connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import connection_request.ConnectionRequestOuterClass.ConnectionRetryStrategy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import response.ResponseOuterClass;
import response.ResponseOuterClass.Response;

public class ConnectionManagerTest {
  ConnectionManager connectionManager;
  AtomicBoolean connectionStatus = new AtomicBoolean();
  ;
  ChannelHandler channel;

  private static String HOST = "aws.com";
  private static int PORT = 9999;

  private static String USERNAME = "JohnDoe";
  private static String PASSWORD = "Password1";

  private static int NUM_OF_RETRIES = 5;
  private static int FACTOR = 10;
  private static int EXPONENT_BASE = 50;

  private static int DATABASE_ID = 1;

  private static int REQUEST_TIMEOUT = 3;

  @BeforeEach
  public void setUp() {
    channel = mock(ChannelHandler.class);
    connectionManager = new ConnectionManager(channel, connectionStatus);
  }

  @Test
  public void DefaultRedisClientConfiguration_True()
      throws ExecutionException, InterruptedException {
    RedisClientConfiguration redisClientConfiguration = RedisClientConfiguration.builder().build();
    ConnectionRequest expectedProtobufConnectionRequest =
        ConnectionRequest.newBuilder()
            .addAddresses(
                ConnectionRequestOuterClass.NodeAddress.newBuilder()
                    .setHost(DEFAULT_HOST)
                    .setPort(DEFAULT_PORT)
                    .build())
            .setTlsMode(ConnectionRequestOuterClass.TlsMode.NoTls)
            .setClusterModeEnabled(false)
            .setReadFrom(ConnectionRequestOuterClass.ReadFrom.Primary)
            .build();
    CompletableFuture<Response> completedFuture = new CompletableFuture<>();
    Response response =
        Response.newBuilder().setConstantResponse(ResponseOuterClass.ConstantResponse.OK).build();
    completedFuture.complete(response);
    connectionStatus.set(false);
    when(channel.connect(eq(expectedProtobufConnectionRequest))).thenReturn(completedFuture);
    CompletableFuture<Boolean> result = connectionManager.connectToRedis(redisClientConfiguration);
    assertTrue(result.get());
  }

  @Test
  public void DefaultRedisClusterClientConfiguration_True()
      throws ExecutionException, InterruptedException {
    RedisClusterClientConfiguration redisClusterClientConfiguration =
        RedisClusterClientConfiguration.builder().build();
    ConnectionRequest expectedProtobufConnectionRequest =
        ConnectionRequest.newBuilder()
            .addAddresses(
                ConnectionRequestOuterClass.NodeAddress.newBuilder()
                    .setHost(DEFAULT_HOST)
                    .setPort(DEFAULT_PORT)
                    .build())
            .setTlsMode(ConnectionRequestOuterClass.TlsMode.NoTls)
            .setClusterModeEnabled(true)
            .setReadFrom(ConnectionRequestOuterClass.ReadFrom.Primary)
            .build();
    CompletableFuture<Response> completedFuture = new CompletableFuture<>();
    Response response =
        Response.newBuilder().setConstantResponse(ResponseOuterClass.ConstantResponse.OK).build();
    completedFuture.complete(response);
    connectionStatus.set(false);
    when(channel.connect(eq(expectedProtobufConnectionRequest))).thenReturn(completedFuture);
    CompletableFuture<Boolean> result =
        connectionManager.connectToRedis(redisClusterClientConfiguration);
    assertTrue(result.get());
  }

  @Test
  public void RedisClientAllFieldsSet_True() throws ExecutionException, InterruptedException {
    RedisClientConfiguration redisClientConfiguration =
        RedisClientConfiguration.builder()
            .address(NodeAddress.builder().host(HOST).port(PORT).build())
            .address(NodeAddress.builder().host(DEFAULT_HOST).port(DEFAULT_PORT).build())
            .useTLS(true)
            .readFrom(ReadFrom.PREFER_REPLICA)
            .credentials(RedisCredentials.builder().username(USERNAME).password(PASSWORD).build())
            .requestTimeout(REQUEST_TIMEOUT)
            .reconnectStrategy(
                BackoffStrategy.builder()
                    .numOfRetries(NUM_OF_RETRIES)
                    .exponentBase(EXPONENT_BASE)
                    .factor(FACTOR)
                    .build())
            .databaseId(DATABASE_ID)
            .build();
    ConnectionRequest expectedProtobufConnectionRequest =
        ConnectionRequest.newBuilder()
            .addAddresses(
                ConnectionRequestOuterClass.NodeAddress.newBuilder()
                    .setHost(HOST)
                    .setPort(PORT)
                    .build())
            .addAddresses(
                ConnectionRequestOuterClass.NodeAddress.newBuilder()
                    .setHost(DEFAULT_HOST)
                    .setPort(DEFAULT_PORT)
                    .build())
            .setTlsMode(ConnectionRequestOuterClass.TlsMode.SecureTls)
            .setReadFrom(ConnectionRequestOuterClass.ReadFrom.PreferReplica)
            .setClusterModeEnabled(false)
            .setAuthenticationInfo(
                AuthenticationInfo.newBuilder().setUsername(USERNAME).setPassword(PASSWORD).build())
            .setRequestTimeout(REQUEST_TIMEOUT)
            .setConnectionRetryStrategy(
                ConnectionRetryStrategy.newBuilder()
                    .setNumberOfRetries(NUM_OF_RETRIES)
                    .setFactor(FACTOR)
                    .setExponentBase(EXPONENT_BASE)
                    .build())
            .setDatabaseId(DATABASE_ID)
            .build();
    CompletableFuture<Response> completedFuture = new CompletableFuture<>();
    Response response =
        Response.newBuilder().setConstantResponse(ResponseOuterClass.ConstantResponse.OK).build();
    completedFuture.complete(response);
    connectionStatus.set(false);
    when(channel.connect(eq(expectedProtobufConnectionRequest))).thenReturn(completedFuture);
    CompletableFuture<Boolean> result = connectionManager.connectToRedis(redisClientConfiguration);
    assertTrue(result.get());
  }

  @Test
  public void DoubleConnection_False() throws ExecutionException, InterruptedException {
    RedisClientConfiguration redisClientConfiguration = RedisClientConfiguration.builder().build();
    CompletableFuture<Response> completedFuture = new CompletableFuture<>();
    Response response = Response.newBuilder().build();
    completedFuture.complete(response);
    connectionStatus.set(false);
    when(channel.connect(any())).thenReturn(completedFuture);
    CompletableFuture<Boolean> resultNotConnected =
        connectionManager.connectToRedis(redisClientConfiguration);
    assertTrue(resultNotConnected.get());

    CompletableFuture<Boolean> resultConnected =
        connectionManager.connectToRedis(redisClientConfiguration);
    assertFalse(resultConnected.get());
  }

  @Test
  public void ResponseConstantResponseNotConnected_True()
      throws ExecutionException, InterruptedException {
    RedisClientConfiguration redisClientConfiguration = RedisClientConfiguration.builder().build();
    CompletableFuture<Response> completedFuture = new CompletableFuture<>();
    Response response =
        Response.newBuilder().setConstantResponse(ResponseOuterClass.ConstantResponse.OK).build();
    completedFuture.complete(response);
    connectionStatus.set(false);
    when(channel.connect(any())).thenReturn(completedFuture);
    CompletableFuture<Boolean> result = connectionManager.connectToRedis(redisClientConfiguration);
    assertTrue(result.get());
  }

  @Test
  public void ResponseConstantResponseConnected_False()
      throws ExecutionException, InterruptedException {
    RedisClientConfiguration redisClientConfiguration = RedisClientConfiguration.builder().build();
    CompletableFuture<Response> completedFuture = new CompletableFuture<>();
    Response response =
        Response.newBuilder().setConstantResponse(ResponseOuterClass.ConstantResponse.OK).build();
    completedFuture.complete(response);
    connectionStatus.set(true);
    when(channel.connect(any())).thenReturn(completedFuture);
    CompletableFuture<Boolean> result = connectionManager.connectToRedis(redisClientConfiguration);
    assertFalse(result.get());
  }

  @Test
  public void ResponseRequestError_RuntimeException()
      throws ExecutionException, InterruptedException {
    RedisClientConfiguration redisClientConfiguration = RedisClientConfiguration.builder().build();
    CompletableFuture<Response> completedFuture = new CompletableFuture<>();
    Response response =
        Response.newBuilder()
            .setRequestError(
                ResponseOuterClass.RequestError.newBuilder()
                    .setType(ResponseOuterClass.RequestErrorType.Timeout)
                    .setMessage("Timeout Occurred")
                    .build())
            .build();
    completedFuture.complete(response);
    connectionStatus.set(false);
    when(channel.connect(any())).thenReturn(completedFuture);
    CompletableFuture<Boolean> result = connectionManager.connectToRedis(redisClientConfiguration);
    ExecutionException exception =
        assertThrows(
            ExecutionException.class,
            () -> {
              result.get();
            });
    assertTrue(exception.getCause() instanceof RuntimeException);
  }

  // TODO currently getting linker error
  @Test
  public void ResponseRespPointer_RuntimeException()
      throws ExecutionException, InterruptedException {
    RedisClientConfiguration redisClientConfiguration = RedisClientConfiguration.builder().build();
    CompletableFuture<Response> completedFuture = new CompletableFuture<>();
    Response response = Response.newBuilder().setRespPointer(1).build();
    completedFuture.complete(response);
    connectionStatus.set(false);
    when(channel.connect(any())).thenReturn(completedFuture);
    CompletableFuture<Boolean> result = connectionManager.connectToRedis(redisClientConfiguration);
    ExecutionException exception =
        assertThrows(
            ExecutionException.class,
            () -> {
              result.get();
            });
    System.out.println(exception.getCause());
    assertTrue(exception.getCause() instanceof RuntimeException);
  }

  @Test
  public void ResponseClosingError_RuntimeException()
      throws ExecutionException, InterruptedException {
    RedisClientConfiguration redisClientConfiguration = RedisClientConfiguration.builder().build();
    CompletableFuture<Response> completedFuture = new CompletableFuture<>();
    Response response = Response.newBuilder().setClosingError("Closing Error Occurred").build();
    completedFuture.complete(response);
    connectionStatus.set(false);
    when(channel.connect(any())).thenReturn(completedFuture);
    CompletableFuture<Boolean> result = connectionManager.connectToRedis(redisClientConfiguration);
    ExecutionException exception =
        assertThrows(
            ExecutionException.class,
            () -> {
              result.get();
            });
    assertTrue(exception.getCause() instanceof RuntimeException);
  }
}
