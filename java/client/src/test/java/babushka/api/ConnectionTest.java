package babushka.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import babushka.managers.ConnectionManager;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ConnectionTest {

  Connection service;

  ConnectionManager connectionManager;

  private static String HOST = "host";
  private static int PORT = 9999;

  @BeforeEach
  public void setUp() {
    connectionManager = mock(ConnectionManager.class);
    service = new Connection(connectionManager);
  }

  @Test
  public void asyncConnectToRedis_success() {
    // setup
    boolean useSsl = false;
    boolean clusterMode = false;
    CompletableFuture<String> testResponse = mock(CompletableFuture.class);
    when(connectionManager.connectToRedis(anyString(), anyInt(), anyBoolean(), anyBoolean()))
        .thenReturn(testResponse);

    // exercise
    CompletableFuture<String> connectionResponse =
        service.connectToRedis(HOST, PORT, useSsl, clusterMode);

    // verify
    Mockito.verify(connectionManager, times(1))
        .connectToRedis(eq(HOST), eq(PORT), eq(useSsl), eq(clusterMode));
    assertEquals(testResponse, connectionResponse);

    // teardown
  }
}
