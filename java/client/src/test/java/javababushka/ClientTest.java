package javababushka;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import response.ResponseOuterClass;

public class ClientTest {

  Client testClient;

  NettyWrapper nettyWrapper;

  private static String HOST = "host";
  private static int PORT = 9999;

  @BeforeEach
  public void setUp() {
    nettyWrapper = mock(NettyWrapper.class);
    testClient = new Client(nettyWrapper);
  }

  @Test
  public void test_asyncConnectToRedis_success() {
    // setup
    boolean useSsl = false;
    boolean clusterMode = false;
    ConnectionRequest connectionRequest =
        Client.getConnectionRequest(HOST, PORT, useSsl, clusterMode).build();

    // exercise
    CompletableFuture<ResponseOuterClass.Response> connectionResponse =
        testClient.asyncConnectToRedis(HOST, PORT, useSsl, clusterMode);

    // verify
//    assertTrue(connectionResponse instanceof CompletableFuture);
    Mockito.verify(nettyWrapper, times(1))
        .registerConnection(eq(connectionResponse));
    Mockito.verify(nettyWrapper, times(1)).writeAndFlush(eq(connectionRequest));

    // teardown
  }
}
