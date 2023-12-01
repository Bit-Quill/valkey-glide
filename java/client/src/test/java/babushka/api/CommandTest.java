package babushka.api;

import static org.mockito.Mockito.mock;

import babushka.managers.CommandManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommandTest {

  Commands service;

  CommandManager commandsManager;

  @BeforeEach
  public void setUp() {
    commandsManager = mock(CommandManager.class);
    service = new Commands(commandsManager);
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
