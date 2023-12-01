package babushka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import babushka.api.Client;
import babushka.managers.CommandManager;
import babushka.managers.ConnectionManager;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import response.ResponseOuterClass.Response;

public class ClientTest {

  Client testClient;

  @Mock
  ConnectionManager connectionManager;

  @Mock
  CommandManager commandManager;

  private static String HOST = "host";
  private static int PORT = 9999;

  @BeforeEach
  public void setUp() {
    testClient = new Client(connectionManager, commandManager);
  }

  @Test
  public void test_asyncConnectToRedis_success() {
    // setup
    boolean useSsl = false;
    boolean clusterMode = false;
    CompletableFuture<Response> testResponse = mock(CompletableFuture.class);
    when(connectionManager.connectToRedis(any(), anyInt(), anyBoolean(), anyBoolean()))
        .thenReturn(testResponse);

    // exercise
    CompletableFuture<Response> connectionResponse =
        testClient.asyncConnectToRedis(HOST, PORT, useSsl, clusterMode);

    // verify
    Mockito.verify(connectionManager, times(1))
        .connectToRedis(eq(HOST), eq(PORT), eq(useSsl), eq(clusterMode));
    assertEquals(testResponse, connectionResponse);

    // teardown
  }

  @Test
  public void test_close_success() {
    // setup

    // exercise
    testClient.closeConnection();

    // verify
    Mockito.verify(connectionManager, times(1)).closeConnection();

    // teardown
  }


  @Test
  public void test_get_success() {
    // setup

    // exercise

    // verify

    // teardown
  }

  @Test
  public void test_set_success() {
    // setup

    // exercise

    // verify

    // teardown
  }

  @Test
  public void test_ping_success() {
    // setup

    // exercise

    // verify

    // teardown
  }

  @Test
  public void test_info_success() {
    // setup

    // exercise

    // verify

    // teardown
  }
}
