package javababushka.benchmarks;

import com.google.common.io.Files;
import javababushka.benchmarks.clients.babushka.JniNettyClient;
import javababushka.benchmarks.utils.ChosenAction;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class JniNettyTests {

  public static Stream<Arguments> generateTestDataWithFlushOnWrite() {
    var dataSizes = List.of(20, 100, 400);
    var clients = List.of(2, 4);
    var threads = List.of(20, 100);
    return dataSizes.stream().flatMap(f ->
        clients.stream().flatMap(g ->
            threads.stream().map(h ->
                Arguments.of(true, 0, 0, 0, 0, f, g, h))));
  }

  public static Stream<Arguments> generateTestDataWithoutFlushOnWrite() {
    var bytesThresholds = List.of(200, 400, 800, 1600);
    var writesThresholds = List.of(5, 10, 20, 50);
    var responseTimeouts = List.of(50, 100, 200);
    var flushTimers = List.of(100, 200, 500);
    var dataSizes = List.of(20, 100, 400);
    var clients = List.of(2, 4);
    var threads = List.of(20, 100);
    return bytesThresholds.stream().flatMap(b ->
        writesThresholds.stream().flatMap(c ->
            responseTimeouts.stream().flatMap(d ->
                flushTimers.stream().flatMap(e ->
                    dataSizes.stream().flatMap(f ->
                        clients.stream().flatMap(g ->
                            threads.stream().map(h ->
                                Arguments.of(false, b, c, d, e, f, g, h))))))));
  }

  private static FileWriter log;
  private static final String key = String.valueOf(ProcessHandle.current().pid());
  private static final int iterations = 1000000;

  @BeforeAll
  @SneakyThrows
  public static void openLog() {
    log = new FileWriter(Paths.get(System.getProperty("user.dir"),
            "JniNettyClient-test-report.txt").toFile(), true);
    log.append(String.format("\n\n=========================\niterations = %d, key = %s\n", iterations, key));
  }

  @AfterAll
  @SneakyThrows
  public static void closeLog() {
    log.append("\n\n\n============== RECORDS ==============\n");
    for (var record : records.entrySet()) {
      log.append(String.format("%20s\t%20d\t%s\n",
          record.getKey(), record.getValue().getKey(), record.getValue().getValue()));
    }
    log.append("\n\n\n");
    log.close();
  }

  private static final Map<ChosenAction, Pair<Long, String>> records = new HashMap<>(Map.of(
      ChosenAction.SET, Pair.of(Long.MAX_VALUE, null),
      ChosenAction.GET_EXISTING, Pair.of(Long.MAX_VALUE, null),
      ChosenAction.GET_NON_EXISTING, Pair.of(Long.MAX_VALUE, null)
  ));

  @ParameterizedTest(name = "flushOnWrite = {0}, bytesThreshold = {1}, writesThreshold = {2},"
      + " responseTimeout = {3}, flushTimer = {4},"
      + " dataSize = {5}, clients = {6}, threads = {7}")
  @MethodSource({ "generateTestDataWithFlushOnWrite", "generateTestDataWithoutFlushOnWrite" })
  @SneakyThrows
  public void experiment(boolean flushOnWrite,
                         int bytesThreshold,
                         int writesThreshold,
                         int responseTimeout,
                         int flushTimer,
                         int dataSize,
                         int clients,
                         int threads) {
    var line = String.format("flushOnWrite = %s, bytesThreshold = %d, writesThreshold = %d,"
      + " responseTimeout = %d, flushTimer = %d,"
      + " dataSize = %d, clients = %d, threads = %d\n", flushOnWrite, bytesThreshold, writesThreshold,
            responseTimeout, flushTimer, dataSize, clients, threads);
    log.append(line);

    JniNettyClient.ALWAYS_FLUSH_ON_WRITE = flushOnWrite;
    JniNettyClient.AUTO_FLUSH_THRESHOLD_BYTES = bytesThreshold;
    JniNettyClient.AUTO_FLUSH_THRESHOLD_WRITES = writesThreshold;
    JniNettyClient.AUTO_FLUSH_RESPONSE_TIMEOUT_MILLIS = responseTimeout;
    JniNettyClient.AUTO_FLUSH_TIMER_MILLIS = flushTimer;

    var clientsArr = new JniNettyClient[clients];
    String value = RandomStringUtils.randomAlphanumeric(dataSize);

    for (int i = 1; i < clients; i++) {
      clientsArr[i - 1] = new JniNettyClient();
      clientsArr[i - 1].connectToRedis();
    }

    List<Runnable> tasks = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      tasks.add(() -> {
        int clientIndex = threads % clients;
        for (int j = 0; j < iterations; j++) {
          clientsArr[clientIndex].get(key);
        }
      });
    }
    long before = System.nanoTime();
    tasks.forEach(Runnable::run);
    long after = System.nanoTime();
    long elapsed = after - before;
    log.append(String.format("   GET NE %20d\n", elapsed));
    if (elapsed < records.get(ChosenAction.GET_NON_EXISTING).getKey()) {
      records.put(ChosenAction.GET_NON_EXISTING, Pair.of(elapsed, line));
    }
    for (int i = 1; i < clients; i++) {
      clientsArr[i - 1].closeConnection();
      clientsArr[i - 1] = new JniNettyClient();
      clientsArr[i - 1].connectToRedis();
    }
    tasks.clear();
    for (int i = 0; i < threads; i++) {
      tasks.add(() -> {
        int clientIndex = threads % clients;
        for (int j = 0; j < iterations; j++) {
          clientsArr[clientIndex].set(key, value);
        }
      });
    }
    before = System.nanoTime();
    tasks.forEach(Runnable::run);
    after = System.nanoTime();
    elapsed = after - before;
    log.append(String.format("   SET    %20d\n", elapsed));
    if (elapsed < records.get(ChosenAction.SET).getKey()) {
      records.put(ChosenAction.SET, Pair.of(elapsed, line));
    }
    for (int i = 1; i < clients; i++) {
      clientsArr[i - 1].closeConnection();
      clientsArr[i - 1] = new JniNettyClient();
      clientsArr[i - 1].connectToRedis();
    }
    tasks.clear();
    for (int i = 0; i < threads; i++) {
      tasks.add(() -> {
        int clientIndex = threads % clients;
        for (int j = 0; j < iterations; j++) {
          clientsArr[clientIndex].get(key);
        }
      });
    }
    before = System.nanoTime();
    tasks.forEach(Runnable::run);
    after = System.nanoTime();
    elapsed = after - before;
    log.append(String.format("   GET E  %20d\n", elapsed));
    if (elapsed < records.get(ChosenAction.GET_EXISTING).getKey()) {
      records.put(ChosenAction.GET_EXISTING, Pair.of(elapsed, line));
    }
  }
}
