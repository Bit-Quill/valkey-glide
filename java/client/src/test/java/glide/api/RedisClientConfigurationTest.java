package glide.api;

import static glide.api.models.configuration.NodeAddress.DEFAULT_HOST;
import static glide.api.models.configuration.NodeAddress.DEFAULT_PORT;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

import glide.api.models.configuration.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class RedisClientConfigurationTest {
  private static String HOST = "aws.com";
  private static int PORT = 9999;

  private static String USERNAME = "JohnDoe";
  private static String PASSWORD = "Password1";

  private static int NUM_OF_RETRIES = 5;
  private static int FACTOR = 10;
  private static int EXPONENT_BASE = 50;

  private static int DATABASE_ID = 1;

  private static int REQUEST_TIMEOUT = 3;

  @Test
  public void NodeAddress_DefaultConfig() {
    NodeAddress nodeAddress = NodeAddress.builder().build();

    assertEquals(DEFAULT_HOST, nodeAddress.getHost());
    assertEquals(DEFAULT_PORT, nodeAddress.getPort());
  }

  @Test
  public void NodeAddress_CustomConfig() {
    NodeAddress nodeAddress = NodeAddress.builder().host(HOST).port(PORT).build();

    assertEquals(HOST, nodeAddress.getHost());
    assertEquals(PORT, nodeAddress.getPort());
  }

  @Test
  public void BackoffStrategy_CustomConfig() {
    BackoffStrategy backoffStrategy =
        BackoffStrategy.builder()
            .numOfRetries(NUM_OF_RETRIES)
            .factor(FACTOR)
            .exponentBase(EXPONENT_BASE)
            .build();

    assertEquals(NUM_OF_RETRIES, backoffStrategy.getNumOfRetries());
    assertEquals(FACTOR, backoffStrategy.getFactor());
    assertEquals(EXPONENT_BASE, backoffStrategy.getExponentBase());
  }

  @Test
  public void RedisCredentials_CustomConfig() {
    RedisCredentials redisCredentials =
        RedisCredentials.builder().password(PASSWORD).username(USERNAME).build();

    assertEquals(PASSWORD, redisCredentials.getPassword());
    assertEquals(USERNAME, redisCredentials.getUsername());
  }

  @Test
  public void RedisClientConfiguration_DefaultConfig() {
    RedisClientConfiguration redisClientConfiguration = RedisClientConfiguration.builder().build();

    assertEquals(new ArrayList<NodeAddress>(), redisClientConfiguration.getAddresses());
    assertFalse(redisClientConfiguration.isUseTLS());
    assertEquals(ReadFrom.PRIMARY, redisClientConfiguration.getReadFrom());
    assertNull(redisClientConfiguration.getCredentials());
    assertNull(redisClientConfiguration.getRequestTimeout());
    assertNull(redisClientConfiguration.getDatabaseId());
    assertNull(redisClientConfiguration.getReconnectStrategy());
  }

  @Test
  public void RedisClientConfiguration_CustomConfig() {
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

    List<NodeAddress> expectedAddresses = new ArrayList<>();
    NodeAddress address1 = NodeAddress.builder().host(HOST).port(PORT).build();
    NodeAddress address2 = NodeAddress.builder().host(DEFAULT_HOST).port(DEFAULT_PORT).build();
    expectedAddresses.add(address1);
    expectedAddresses.add(address2);

    List<NodeAddress> actualAddresses = redisClientConfiguration.getAddresses();
    assertEquals(
        expectedAddresses.size(), actualAddresses.size(), "Lists should be of the same size");
    for (int i = 0; i < actualAddresses.size(); i++) {
      NodeAddress actualNodeAddress = actualAddresses.get(i);
      NodeAddress expectedNodeAddress = expectedAddresses.get(i);
      assertAll(
          "Object fields should match",
          () -> assertEquals(expectedNodeAddress.getHost(), actualNodeAddress.getHost()),
          () -> assertEquals(expectedNodeAddress.getPort(), actualNodeAddress.getPort()));
    }
    assertTrue(redisClientConfiguration.isUseTLS());
    assertEquals(ReadFrom.PREFER_REPLICA, redisClientConfiguration.getReadFrom());
    assertEquals(PASSWORD, redisClientConfiguration.getCredentials().getPassword());
    assertEquals(USERNAME, redisClientConfiguration.getCredentials().getUsername());
    assertEquals(REQUEST_TIMEOUT, redisClientConfiguration.getRequestTimeout());
    assertEquals(NUM_OF_RETRIES, redisClientConfiguration.getReconnectStrategy().getNumOfRetries());
    assertEquals(FACTOR, redisClientConfiguration.getReconnectStrategy().getFactor());
    assertEquals(EXPONENT_BASE, redisClientConfiguration.getReconnectStrategy().getExponentBase());
    assertEquals(DATABASE_ID, redisClientConfiguration.getDatabaseId());
  }

  @Test
  public void RedisClusterClientConfiguration_DefaultConfig() {
    RedisClusterClientConfiguration redisClusterClientConfiguration =
        RedisClusterClientConfiguration.builder().build();

    assertEquals(new ArrayList<NodeAddress>(), redisClusterClientConfiguration.getAddresses());
    assertFalse(redisClusterClientConfiguration.isUseTLS());
    assertEquals(ReadFrom.PRIMARY, redisClusterClientConfiguration.getReadFrom());
    assertNull(redisClusterClientConfiguration.getCredentials());
    assertNull(redisClusterClientConfiguration.getRequestTimeout());
  }
}
