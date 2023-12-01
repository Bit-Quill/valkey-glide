package babushka.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import babushka.managers.CommandManager;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommandsTest {

  Commands service;

  CommandManager commandsManager;

  private static String OK_RESPONSE = response.ResponseOuterClass.ConstantResponse.OK.toString();

  @BeforeEach
  public void setUp() {
    commandsManager = mock(CommandManager.class);
    service = new Commands(commandsManager);
  }

  @Test
  public void test_asyncGet_success() throws ExecutionException, InterruptedException {
    // setup
    // TODO: randomize keys
    String key = "testKey";
    String value = "testValue";
    CompletableFuture<String> testResponse = mock(CompletableFuture.class);
    when(testResponse.get()).thenReturn(value);
    when(commandsManager.get(eq(key))).thenReturn(testResponse);

    // exercise
    Future<String> response = service.asyncGet(key);
    String payload = response.get();

    // verify
    assertEquals(testResponse, response);
    assertEquals(value, payload);

    // teardown
  }

  // TODO: test_asyncGet_InterruptedException and ExecutionException

  @Test
  public void test_get_success() throws ExecutionException, InterruptedException, TimeoutException {
    // setup
    // TODO: randomize keys
    String key = "testKey";
    String value = "testValue";
    CompletableFuture<String> testResponse = mock(CompletableFuture.class);
    when(testResponse.get(anyLong(), any())).thenReturn(value);
    when(commandsManager.get(eq(key))).thenReturn(testResponse);

    // exercise
    String payload = service.get(key);

    // verify
    assertEquals(value, payload);

    // teardown
  }

  @Test
  public void test_asyncSet_success() throws ExecutionException, InterruptedException {
    // setup
    // TODO: randomize key and value
    String key = "testKey";
    String value = "testValue";
    CompletableFuture<String> testResponse = mock(CompletableFuture.class);
    when(testResponse.get()).thenReturn(OK_RESPONSE);
    when(commandsManager.set(eq(key), eq(value))).thenReturn(testResponse);

    // exercise
    Future<String> response = service.asyncSet(key, value);
    String payload = response.get();

    // verify
    assertEquals(testResponse, response);
    assertEquals(OK_RESPONSE, payload);

    // teardown
  }

  @Test
  public void test_set_success() throws ExecutionException, InterruptedException, TimeoutException {
    // setup
    // TODO: randomize key/value
    String key = "testKey";
    String value = "testValue";
    CompletableFuture<String> testResponse = mock(CompletableFuture.class);
    when(testResponse.get(anyLong(), any())).thenReturn(value);
    when(commandsManager.set(eq(key), eq(value))).thenReturn(testResponse);

    // exercise
    service.set(key, value);

    // verify
    // nothing to do

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
