package babushka.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import babushka.managers.CommandManager;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
  public void get_success() throws ExecutionException, InterruptedException {
    // setup
    // TODO: randomize keys
    String key = "testKey";
    String value = "testValue";
    CompletableFuture<String> testResponse = mock(CompletableFuture.class);
    when(testResponse.get()).thenReturn(value);
    when(commandsManager.get(eq(key))).thenReturn(testResponse);

    // exercise
    Future<String> response = service.get(key);
    String payload = response.get();

    // verify
    assertEquals(testResponse, response);
    assertEquals(value, payload);

    // teardown
  }

  // TODO: test_get_InterruptedException and ExecutionException

  @Test
  public void set_success() throws ExecutionException, InterruptedException {
    // setup
    // TODO: randomize key and value
    String key = "testKey";
    String value = "testValue";
    CompletableFuture<String> testResponse = mock(CompletableFuture.class);
    when(testResponse.get()).thenReturn(OK_RESPONSE);
    when(commandsManager.set(eq(key), eq(value))).thenReturn(testResponse);

    // exercise
    Future<String> response = service.set(key, value);
    String payload = response.get();

    // verify
    assertEquals(testResponse, response);
    assertEquals(OK_RESPONSE, payload);

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
