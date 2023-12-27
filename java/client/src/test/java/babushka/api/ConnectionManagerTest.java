package babushka.api;

import static babushka.api.models.configuration.NodeAddress.DEFAULT_HOST;
import static babushka.api.models.configuration.NodeAddress.DEFAULT_PORT;
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
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConnectionManagerTest {
  ConnectionManager connectionManager;
  AtomicBoolean connectionStatus;
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
  public void DefaultRedisClientConfiguration() {
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

    CompletableFuture mockFuture = mock(CompletableFuture.class);
    when(mockFuture.thenApplyAsync(any())).thenReturn(mockFuture);
    when(channel.connect(eq(expectedProtobufConnectionRequest))).thenReturn(mockFuture);
    connectionManager.connectToRedis(redisClientConfiguration);
  }

  @Test
  public void DefaultRedisClusterClientConfiguration() {
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

    CompletableFuture mockFuture = mock(CompletableFuture.class);
    when(mockFuture.thenApplyAsync(any())).thenReturn(mockFuture);
    when(channel.connect(eq(expectedProtobufConnectionRequest))).thenReturn(mockFuture);
    connectionManager.connectToRedis(redisClusterClientConfiguration);
  }

  @Test
  public void RedisClientAllFieldsSet() {
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

    CompletableFuture mockFuture = mock(CompletableFuture.class);
    when(mockFuture.thenApplyAsync(any())).thenReturn(mockFuture);
    when(channel.connect(eq(expectedProtobufConnectionRequest))).thenReturn(mockFuture);
    connectionManager.connectToRedis(redisClientConfiguration);
  }
}
