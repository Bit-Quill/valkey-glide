package glide.api;

import static glide.api.RedisClient.CreateClient;
import static glide.api.RedisClient.buildChannelHandler;
import static glide.api.RedisClient.buildCommandManager;
import static glide.api.RedisClient.buildConnectionManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import glide.api.models.configuration.RedisClientConfiguration;
import glide.api.models.exceptions.ClosingException;
import glide.connectors.handlers.ChannelHandler;
import glide.connectors.resources.ThreadPoolResource;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class RedisClientCreateTest {

  private MockedStatic<RedisClient> mockedClient;
  private ChannelHandler channelHandler;
  private ConnectionManager connectionManager;
  private CommandManager commandManager;
  private ThreadPoolResource threadPoolResource;

  @BeforeEach
  public void init() {
    mockedClient = Mockito.mockStatic(RedisClient.class);

    channelHandler = mock(ChannelHandler.class);
    commandManager = mock(CommandManager.class);
    connectionManager = mock(ConnectionManager.class);
    threadPoolResource = mock(ThreadPoolResource.class);

    mockedClient.when(() -> buildChannelHandler(any())).thenReturn(channelHandler);
    mockedClient.when(() -> buildConnectionManager(channelHandler)).thenReturn(connectionManager);
    mockedClient.when(() -> buildCommandManager(channelHandler)).thenReturn(commandManager);
  }

  @AfterEach
  public void teardown() {
    mockedClient.close();
  }

  @Test
  @SneakyThrows
  public void createClient_withConfig_successfullyReturnsRedisClient() {

    // setup
    CompletableFuture<Void> connectToRedisFuture = new CompletableFuture<>();
    connectToRedisFuture.complete(null);
    RedisClientConfiguration config =
        RedisClientConfiguration.builder().threadPoolResource(threadPoolResource).build();

    when(connectionManager.connectToRedis(eq(config))).thenReturn(connectToRedisFuture);
    mockedClient.when(() -> CreateClient(config)).thenCallRealMethod();

    CompletableFuture<Void> closeRedisFuture = new CompletableFuture<>();
    closeRedisFuture.complete(null);
    when(connectionManager.closeConnection()).thenReturn(closeRedisFuture);

    // exercise
    try (RedisClient client = CreateClient(config).get()) {
      // verify
      assertEquals(connectionManager, client.connectionManager);
      assertEquals(commandManager, client.commandManager);
    }

    verify(connectionManager, times(1)).closeConnection();
  }

  @SneakyThrows
  @Test
  public void createClient_errorOnConnectionThrowsExecutionException() {
    // setup
    CompletableFuture<Void> connectToRedisFuture = new CompletableFuture<>();
    ClosingException exception = new ClosingException("disconnected");
    connectToRedisFuture.completeExceptionally(exception);
    RedisClientConfiguration config = RedisClientConfiguration.builder().build();

    when(connectionManager.connectToRedis(any())).thenReturn(connectToRedisFuture);
    mockedClient.when(() -> CreateClient(any())).thenCallRealMethod();

    // exercise
    CompletableFuture<RedisClient> result = CreateClient(config);

    ExecutionException executionException =
        assertThrows(ExecutionException.class, () -> result.get());

    // verify
    assertEquals(exception, executionException.getCause());
  }

  @SneakyThrows
  @Test
  public void redisClientClose_throwsCancellationException() {

    // setup
    CompletableFuture<Void> connectToRedisFuture = new CompletableFuture<>();
    connectToRedisFuture.complete(null);
    RedisClientConfiguration config =
        RedisClientConfiguration.builder().threadPoolResource(threadPoolResource).build();

    when(connectionManager.connectToRedis(eq(config))).thenReturn(connectToRedisFuture);
    mockedClient.when(() -> CreateClient(config)).thenCallRealMethod();

    CompletableFuture<Void> closeRedisFuture = new CompletableFuture<>();
    InterruptedException interruptedException = new InterruptedException("Interrupted");
    closeRedisFuture.cancel(true);
    when(connectionManager.closeConnection()).thenReturn(closeRedisFuture);

    // exercise
    try (RedisClient client = CreateClient(config).get()) {
      // verify
      assertEquals(connectionManager, client.connectionManager);
      assertEquals(commandManager, client.commandManager);
    } catch (Exception cancellationException) {
      assertTrue(cancellationException instanceof CancellationException);
    }

    verify(connectionManager, times(1)).closeConnection();
  }

  @SneakyThrows
  @Test
  public void redisClientClose_throwsInterruptedException() {

    // setup
    CompletableFuture<Void> connectToRedisFuture = new CompletableFuture<>();
    connectToRedisFuture.complete(null);
    RedisClientConfiguration config =
        RedisClientConfiguration.builder().threadPoolResource(threadPoolResource).build();

    when(connectionManager.connectToRedis(eq(config))).thenReturn(connectToRedisFuture);
    mockedClient.when(() -> CreateClient(config)).thenCallRealMethod();

    CompletableFuture<Void> closeRedisFuture = mock(CompletableFuture.class);
    InterruptedException interruptedException = new InterruptedException("Interrupted");
    when(closeRedisFuture.get()).thenThrow(interruptedException);

    when(connectionManager.closeConnection()).thenReturn(closeRedisFuture);

    // exercise
    try (RedisClient client = CreateClient(config).get()) {
      // verify
      assertEquals(connectionManager, client.connectionManager);
      assertEquals(commandManager, client.commandManager);
    } catch (Exception exception) {
      assertEquals(interruptedException, exception.getCause());
    }

    verify(connectionManager, times(1)).closeConnection();
  }
}
