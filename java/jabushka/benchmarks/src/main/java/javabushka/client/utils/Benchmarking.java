package javabushka.client.utils;

import io.lettuce.core.RedisFuture;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javabushka.client.AsyncClient;
import javabushka.client.Client;
import javabushka.client.SyncClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class Benchmarking {
  static final double PROB_GET = 0.8;
  static final double PROB_GET_EXISTING_KEY = 0.8;
  static final int SIZE_GET_KEYSPACE = 3750000;
  static final int SIZE_SET_KEYSPACE = 3000000;

  private static ChosenAction randomAction() {
    if (Math.random() > PROB_GET) {
      return ChosenAction.SET;
    }
    if (Math.random() > PROB_GET_EXISTING_KEY) {
      return ChosenAction.GET_NON_EXISTING;
    }
    return ChosenAction.GET_EXISTING;
  }

  public static String generateKeyNew() {
    int range = SIZE_GET_KEYSPACE - SIZE_SET_KEYSPACE;
    return Math.floor(Math.random() * range + SIZE_SET_KEYSPACE + 1) + "";
  }

  public static String generateKeyExisting() {
    return (Math.floor(Math.random() * SIZE_SET_KEYSPACE) + 1) + "";
  }

  public interface Operation {
    void go();
  }

  public static Map<ChosenAction, ArrayList<Long>> getLatencies(
      int iterations, Map<ChosenAction, Operation> actions) {
    Map<ChosenAction, ArrayList<Long>> latencies = new HashMap<ChosenAction, ArrayList<Long>>();
    for (ChosenAction action : actions.keySet()) {
      latencies.put(action, new ArrayList<Long>());
    }

    for (int i = 0; i < iterations; i++) {
      ChosenAction action = randomAction();
      Operation op = actions.get(action);
      ArrayList<Long> actionLatencies = latencies.get(action);
      addLatency(op, actionLatencies);
    }

    return latencies;
  }

  public static Long getLatency(Operation op) {
    long before = System.nanoTime();
    op.go();
    long after = System.nanoTime();

    return after - before;
  }

  private static void addLatency(Operation op, ArrayList<Long> latencies) {
    long before = System.nanoTime();
    op.go();
    long after = System.nanoTime();
    latencies.add(after - before);
  }

  // Assumption: latencies is sorted in ascending order
  private static Long percentile(ArrayList<Long> latencies, int percentile) {
    int N = latencies.size();
    double n = (N - 1) * percentile / 100. + 1;
    if (n == 1d) return latencies.get(0);
    else if (n == N) return latencies.get(N - 1);
    int k = (int) n;
    double d = n - k;
    return Math.round(latencies.get(k - 1) + d * (latencies.get(k) - latencies.get(k - 1)));
  }

  private static double stdDeviation(ArrayList<Long> latencies, Double avgLatency) {
    double stdDeviation =
        latencies.stream()
            .mapToDouble(Long::doubleValue)
            .reduce(0.0, (stdDev, latency) -> stdDev + Math.pow(latency - avgLatency, 2));
    return Math.sqrt(stdDeviation / latencies.size());
  }

  // This has the side-effect of sorting each latencies ArrayList
  public static Map<ChosenAction, LatencyResults> calculateResults(
      Map<ChosenAction, ArrayList<Long>> actionLatencies) {
    Map<ChosenAction, LatencyResults> results = new HashMap<ChosenAction, LatencyResults>();

    for (Map.Entry<ChosenAction, ArrayList<Long>> entry : actionLatencies.entrySet()) {
      ChosenAction action = entry.getKey();
      ArrayList<Long> latencies = entry.getValue();

      Double avgLatency =
          latencies.stream().collect(Collectors.summingLong(Long::longValue))
              / Double.valueOf(latencies.size());

      Collections.sort(latencies);
      results.put(
          action,
          new LatencyResults(
              avgLatency,
              percentile(latencies, 50),
              percentile(latencies, 90),
              percentile(latencies, 99),
              stdDeviation(latencies, avgLatency)));
    }

    return results;
  }

  public static void printResults(
      Map<ChosenAction, LatencyResults> calculatedResults, Optional<FileWriter> resultsFile)
      throws IOException {
    if (resultsFile.isPresent()) {
      printResults(calculatedResults, resultsFile.get());
    } else {
      printResults(calculatedResults);
    }
  }

  public static void printResults(
      Map<ChosenAction, LatencyResults> resultsMap, FileWriter resultsFile) throws IOException {
    for (Map.Entry<ChosenAction, LatencyResults> entry : resultsMap.entrySet()) {
      ChosenAction action = entry.getKey();
      LatencyResults results = entry.getValue();

      resultsFile.write("Avg. time in ms per " + action + ": " + results.avgLatency / 1000000.0);
      resultsFile.write(action + " p50 latency in ms: " + results.p50Latency / 1000000.0);
      resultsFile.write(action + " p90 latency in ms: " + results.p90Latency / 1000000.0);
      resultsFile.write(action + " p99 latency in ms: " + results.p99Latency / 1000000.0);
      resultsFile.write(action + " std dev in ms: " + results.stdDeviation / 1000000.0);
    }
  }

  public static void printResults(Map<ChosenAction, LatencyResults> resultsMap) {
    for (Map.Entry<ChosenAction, LatencyResults> entry : resultsMap.entrySet()) {
      ChosenAction action = entry.getKey();
      LatencyResults results = entry.getValue();

      System.out.println("Avg. time in ms per " + action + ": " + results.avgLatency / 1000000.0);
      System.out.println(action + " p50 latency in ms: " + results.p50Latency / 1000000.0);
      System.out.println(action + " p90 latency in ms: " + results.p90Latency / 1000000.0);
      System.out.println(action + " p99 latency in ms: " + results.p99Latency / 1000000.0);
      System.out.println(action + " std dev in ms: " + results.stdDeviation / 1000000.0);
    }
  }

  public static Pair<ChosenAction, Long> measureSyncPerformance(
      Client client, int setDataSize) {

    ChosenAction action = randomAction();
    switch (action) {
      case GET_EXISTING:
        return Pair.of(action,
            Benchmarking.getLatency(
                () -> ((SyncClient) client).get(Benchmarking.generateKeyExisting())
            )
        );
      case GET_NON_EXISTING:
        return Pair.of(action,
            Benchmarking.getLatency(
                () -> ((SyncClient) client).get(Benchmarking.generateKeyNew())
            )
        );
      case SET:
        return Pair.of(action,
            Benchmarking.getLatency(
                () -> ((SyncClient) client).set(Benchmarking.generateKeyExisting(),
                    RandomStringUtils.randomAlphanumeric(setDataSize))
            )
        );
      default:
        throw new RuntimeException("Unexpected operation");
    }
  }

  /**
   * Setup action/tasks to measure and track performance
   * @param client      - async client to perform action
   * @param setDataSize - request SET actions of data size
   * @param futures     - record async futures for gathering response
   * @return a pair of latency actions
   */
  public static Pair<ChosenAction, Long> measureAsyncPerformance(
      Client client,
      int setDataSize,
      int index,
      List<Pair<ChosenAction, Future<?>>> futures) {

    ChosenAction action = randomAction();
    switch (action) {
      case GET_EXISTING:
        return Pair.of(action,
            Benchmarking.getLatency(
              () -> futures.add(index, Pair.of(
                  ChosenAction.GET_EXISTING,
                  ((AsyncClient) client).asyncGet(Benchmarking.generateKeyExisting()))
              )
            )
        );
      case GET_NON_EXISTING:
        return Pair.of(action,
            Benchmarking.getLatency(
                () -> futures.add(index, Pair.of(
                    ChosenAction.GET_NON_EXISTING,
                    ((AsyncClient) client).asyncGet(Benchmarking.generateKeyNew()))
                )
            )
        );
      case SET:
        return Pair.of(action,
            Benchmarking.getLatency(
                () -> futures.add(index, Pair.of(
                    ChosenAction.SET,
                    ((AsyncClient) client).asyncSet(Benchmarking.generateKeyExisting(),
                        RandomStringUtils.randomAlphanumeric(setDataSize)))
                )
            )
        );
      default:
        throw new RuntimeException("Unexpected operation");
    }
  }

  public static Pair<ChosenAction, Long> measureAsyncFetchPerformance(
      Client client, Future<?> future) {

    Long latency = Benchmarking.getLatency(() -> ((AsyncClient) client).waitForResult(future));
    return Pair.of(ChosenAction.FETCH, latency);
  }
}
