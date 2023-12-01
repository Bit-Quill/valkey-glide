package babushka.benchmarks.lettuce;

import babushka.benchmarks.clients.lettuce.LettuceClient;
import babushka.benchmarks.utils.Benchmarking;
import babushka.benchmarks.utils.ChosenAction;
import babushka.benchmarks.utils.ConnectionSettings;
import java.util.HashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LettuceClientIT {

  private static LettuceClient lettuceClient;

  @BeforeAll
  static void initializeLettuceClient() {
    lettuceClient = new LettuceClient();
    lettuceClient.connectToRedis(new ConnectionSettings("localhost", 6379, false, false));
  }

  @AfterAll
  static void closeConnection() {
    lettuceClient.closeConnection();
  }

  @Test
  public void testResourceSetGet() {
    int iterations = 100000;
    String value = "my-value";

    HashMap<ChosenAction, Benchmarking.Operation> actions = new HashMap<>();
    actions.put(ChosenAction.GET_EXISTING, () -> lettuceClient.get(Benchmarking.generateKeySet()));
    actions.put(
        ChosenAction.GET_NON_EXISTING, () -> lettuceClient.get(Benchmarking.generateKeyGet()));
    actions.put(ChosenAction.SET, () -> lettuceClient.set(Benchmarking.generateKeySet(), value));
  }
}
