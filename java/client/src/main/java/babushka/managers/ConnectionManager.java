package babushka.managers;

import static babushka.api.models.configuration.NodeAddress.DEFAULT_HOST;
import static babushka.api.models.configuration.NodeAddress.DEFAULT_PORT;

import babushka.api.models.configuration.BaseClientConfiguration;
import babushka.api.models.configuration.ReadFrom;
import babushka.api.models.configuration.RedisClientConfiguration;
import babushka.api.models.configuration.RedisClusterClientConfiguration;
import babushka.connectors.handlers.ChannelHandler;
import babushka.ffi.resolvers.RedisValueResolver;
import connection_request.ConnectionRequestOuterClass;
import connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import response.ResponseOuterClass.ConstantResponse;
import response.ResponseOuterClass.Response;

@RequiredArgsConstructor
public class ConnectionManager {

  /** UDS connection representation. */
  private final ChannelHandler channel;

  private final AtomicBoolean connectionStatus;

  /**
   * Connect to Redis using a ProtoBuf connection request.
   *
   * @param configuration Connection Request Configuration
   */
  public CompletableFuture<Boolean> connectToRedis(BaseClientConfiguration configuration) {
    ConnectionRequest request = createConnectionRequest(configuration);
    return channel.connect(request).thenApplyAsync(this::checkBabushkaResponse);
  }

  /**
   * Creates a ConnectionRequest protobuf message based on the type of client Standalone/Cluster.
   *
   * @param configuration Connection Request Configuration
   * @return ConnectionRequest protobuf message
   */
  private ConnectionRequest createConnectionRequest(BaseClientConfiguration configuration) {
    if (configuration instanceof RedisClusterClientConfiguration) {
      return setupConnectionRequestBuilderRedisClusterClient(
              (RedisClusterClientConfiguration) configuration)
          .build();
    }

    return setupConnectionRequestBuilderRedisClient((RedisClientConfiguration) configuration)
        .build();
  }

  /**
   * Modifies ConnectionRequestBuilder, so it has appropriate fields for the BaseClientConfiguration
   * where the Standalone/Cluster inherit from.
   *
   * @param configuration
   */
  private ConnectionRequest.Builder setupConnectionRequestBuilderBaseConfiguration(
      BaseClientConfiguration configuration) {
    ConnectionRequest.Builder connectionRequestBuilder = ConnectionRequest.newBuilder();
    if (!configuration.getAddresses().isEmpty()) {
      for (babushka.api.models.configuration.NodeAddress nodeAddress :
          configuration.getAddresses()) {
        connectionRequestBuilder.addAddresses(
            ConnectionRequestOuterClass.NodeAddress.newBuilder()
                .setHost(nodeAddress.getHost())
                .setPort(nodeAddress.getPort())
                .build());
      }
    } else {
      connectionRequestBuilder.addAddresses(
          ConnectionRequestOuterClass.NodeAddress.newBuilder()
              .setHost(DEFAULT_HOST)
              .setPort(DEFAULT_PORT)
              .build());
    }

    connectionRequestBuilder
        .setTlsMode(
            configuration.isUseTLS()
                ? ConnectionRequestOuterClass.TlsMode.SecureTls
                : ConnectionRequestOuterClass.TlsMode.NoTls)
        .setReadFrom(mapReadFromEnum(configuration.getReadFrom()));

    if (configuration.getCredentials() != null) {
      ConnectionRequestOuterClass.AuthenticationInfo.Builder authenticationInfoBuilder =
          ConnectionRequestOuterClass.AuthenticationInfo.newBuilder();
      if (configuration.getCredentials().getUsername() != null) {
        authenticationInfoBuilder.setUsername(configuration.getCredentials().getUsername());
      }
      authenticationInfoBuilder.setPassword(configuration.getCredentials().getPassword());

      connectionRequestBuilder.setAuthenticationInfo(authenticationInfoBuilder.build());
    }

    if (configuration.getRequestTimeout() != null) {
      connectionRequestBuilder.setRequestTimeout(configuration.getRequestTimeout());
    }

    return connectionRequestBuilder;
  }

  /**
   * Modifies ConnectionRequestBuilder, so it has appropriate fields for the Redis Standalone
   * Client.
   *
   * @param configuration Connection Request Configuration
   */
  private ConnectionRequest.Builder setupConnectionRequestBuilderRedisClient(
      RedisClientConfiguration configuration) {
    ConnectionRequest.Builder connectionRequestBuilder =
        setupConnectionRequestBuilderBaseConfiguration(configuration);
    connectionRequestBuilder.setClusterModeEnabled(false);
    if (configuration.getReconnectStrategy() != null) {
      connectionRequestBuilder.setConnectionRetryStrategy(
          ConnectionRequestOuterClass.ConnectionRetryStrategy.newBuilder()
              .setNumberOfRetries(configuration.getReconnectStrategy().getNumOfRetries())
              .setFactor(configuration.getReconnectStrategy().getFactor())
              .setExponentBase(configuration.getReconnectStrategy().getExponentBase())
              .build());
    }

    if (configuration.getDatabaseId() != null) {
      connectionRequestBuilder.setDatabaseId(configuration.getDatabaseId());
    }

    return connectionRequestBuilder;
  }

  /**
   * Modifies ConnectionRequestBuilder, so it has appropriate fields for the Redis Cluster Client.
   *
   * @param configuration
   */
  private ConnectionRequest.Builder setupConnectionRequestBuilderRedisClusterClient(
      RedisClusterClientConfiguration configuration) {
    ConnectionRequest.Builder connectionRequestBuilder =
        setupConnectionRequestBuilderBaseConfiguration(configuration);
    connectionRequestBuilder.setClusterModeEnabled(true);

    return connectionRequestBuilder;
  }

  /**
   * Look up for java ReadFrom enum to protobuf defined ReadFrom enum.
   *
   * @param readFrom
   * @return Protobuf defined ReadFrom enum
   */
  private ConnectionRequestOuterClass.ReadFrom mapReadFromEnum(
      babushka.api.models.configuration.ReadFrom readFrom) {
    if (readFrom == ReadFrom.PREFER_REPLICA) {
      return ConnectionRequestOuterClass.ReadFrom.PreferReplica;
    }

    return ConnectionRequestOuterClass.ReadFrom.Primary;
  }

  /** Check a response received from Babushka. */
  private boolean checkBabushkaResponse(Response response) {
    // TODO do we need to check callback value? It could be -1 or 0
    if (response.hasRequestError()) {
      // TODO do we need to support different types of exceptions and distinguish them by type?
      throw new RuntimeException(
          String.format(
              "%s: %s",
              response.getRequestError().getType(), response.getRequestError().getMessage()));
    } else if (response.hasClosingError()) {
      throw new RuntimeException("Connection closed: " + response.getClosingError());
    } else if (response.hasConstantResponse()) {
      return connectionStatus.compareAndSet(
          false, response.getConstantResponse() == ConstantResponse.OK);
    } else if (response.hasRespPointer()) {
      throw new RuntimeException(
          "Unexpected response data: "
              + RedisValueResolver.valueFromPointer(response.getRespPointer()));
    }
    // TODO commented out due to #710 https://github.com/aws/babushka/issues/710
    //      empty response means a successful connection
    // throw new IllegalStateException("A malformed response received: " + response.toString());
    return connectionStatus.compareAndSet(false, true);
  }

  /** Close the connection and the corresponding channel. */
  public CompletableFuture<Void> closeConnection() {
    return CompletableFuture.runAsync(channel::close);
  }
}
