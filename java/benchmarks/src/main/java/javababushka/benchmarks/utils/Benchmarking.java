package javababushka.benchmarks.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javababushka.benchmarks.BenchmarkingApp;
import javababushka.benchmarks.clients.AsyncClient;
import javababushka.benchmarks.clients.Client;
import javababushka.benchmarks.clients.SyncClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

/** Class to calculate latency on client-actions */
public class Benchmarking {
  static final double PROB_GET = 0.8;
  static final double PROB_GET_EXISTING_KEY = 0.8;
  static final int SIZE_GET_KEYSPACE = 3750000;
  static final int SIZE_SET_KEYSPACE = 3000000;
  static final int ASYNC_OPERATION_TIMEOUT_SEC = 1;
  static final double LATENCY_NORMALIZATION = 1000000.0;
  static final int LATENCY_MIN = 100000;
  static final int LATENCY_MAX = 10000000;
  static final int LATENCY_MULTIPLIER = 10000;
  static final double TPS_NORMALIZATION = 1000000000.0; // nano to seconds

  private static ChosenAction randomAction() {
    if (Math.random() > PROB_GET) {
      return ChosenAction.SET;
    }
    if (Math.random() > PROB_GET_EXISTING_KEY) {
      return ChosenAction.GET_NON_EXISTING;
    }
    return ChosenAction.GET_EXISTING;
  }

  public static String generateKeyGet() {
    int range = SIZE_GET_KEYSPACE - SIZE_SET_KEYSPACE;
    return Integer.toString((int) Math.floor(Math.random() * range + SIZE_SET_KEYSPACE + 1));
  }

  public static String generateKeySet() {
    return Integer.toString((int) (Math.floor(Math.random() * SIZE_SET_KEYSPACE) + 1));
  }

  public interface Operation {
    void go() throws InterruptedException, ExecutionException, TimeoutException;
  }

  private static Pair<ChosenAction, Long> getLatency(Map<ChosenAction, Operation> actions) {
    var action = randomAction();
    long before = System.nanoTime();
    try {
      actions.get(action).go();
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      // timed out - exception from Future::get
      return null;
    }
    long after = System.nanoTime();
    return Pair.of(action, after - before);
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

  // This has the side effect of sorting each latency ArrayList
  public static Map<ChosenAction, LatencyResults> calculateResults(
      Map<ChosenAction, ArrayList<Long>> actionLatencies) {
    Map<ChosenAction, LatencyResults> results = new HashMap<ChosenAction, LatencyResults>();

    for (Map.Entry<ChosenAction, ArrayList<Long>> entry : actionLatencies.entrySet()) {
      ChosenAction action = entry.getKey();
      ArrayList<Long> latencies = entry.getValue();

      if (latencies.size() == 0) {
        results.put(
            action,
            new LatencyResults(
                0,
                0,
                0,
                0,
                0,
                0
            ));
      } else {
        Double avgLatency = latencies.size() <= 0 ? 0 :
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
                stdDeviation(latencies, avgLatency),
                latencies.size()
            ));
      }
    }

    return results;
  }

  public static void printResults(
      Map<ChosenAction, LatencyResults> calculatedResults, Optional<FileWriter> resultsFile) {
    if (resultsFile.isPresent()) {
      printResults(calculatedResults, resultsFile.get());
    } else {
      printResults(calculatedResults);
    }
  }

  public static void printResults(
      Map<ChosenAction, LatencyResults> resultsMap, FileWriter resultsFile) {
    for (Map.Entry<ChosenAction, LatencyResults> entry : resultsMap.entrySet()) {
      ChosenAction action = entry.getKey();
      LatencyResults results = entry.getValue();

      try {
        resultsFile.append(
            "Avg. time in ms per " + action + ": " + results.avgLatency / LATENCY_NORMALIZATION);
        resultsFile.append(
            action + " p50 latency in ms: " + results.p50Latency / LATENCY_NORMALIZATION);
        resultsFile.append(
            action + " p90 latency in ms: " + results.p90Latency / LATENCY_NORMALIZATION);
        resultsFile.append(
            action + " p99 latency in ms: " + results.p99Latency / LATENCY_NORMALIZATION);
        resultsFile.append(
            action + " std dev in ms: " + results.stdDeviation / LATENCY_NORMALIZATION);
      } catch (Exception ignored) {
      }
    }
  }

  public static void printResults(Map<ChosenAction, LatencyResults> resultsMap) {
    for (Map.Entry<ChosenAction, LatencyResults> entry : resultsMap.entrySet()) {
      ChosenAction action = entry.getKey();
      LatencyResults results = entry.getValue();

      System.out.println(
          "Avg. time in ms per " + action + ": " + results.avgLatency / LATENCY_NORMALIZATION);
      System.out.println(
          action + " p50 latency in ms: " + results.p50Latency / LATENCY_NORMALIZATION);
      System.out.println(
          action + " p90 latency in ms: " + results.p90Latency / LATENCY_NORMALIZATION);
      System.out.println(
          action + " p99 latency in ms: " + results.p99Latency / LATENCY_NORMALIZATION);
      System.out.println(
          action + " std dev in ms: " + results.stdDeviation / LATENCY_NORMALIZATION);
      System.out.println(
          action + " total hits: " + results.totalHits);
    }
  }

  public static void testClientSetGet(
      Supplier<Client> clientCreator, BenchmarkingApp.RunConfiguration config, boolean async) {
    for (int concurrentNum : config.concurrentTasks) {
      int iterations =
          Math.min(Math.max(LATENCY_MIN, concurrentNum * LATENCY_MULTIPLIER), LATENCY_MAX);
      for (int clientCount : config.clientCount) {
        for (int dataSize : config.dataSize) {
          System.out.printf(
              "%n =====> %s <===== %d clients %d concurrent %d data %n%n",
              clientCreator.get().getName(), clientCount, concurrentNum, dataSize);
          AtomicInteger iterationCounter = new AtomicInteger(0);
          Map<ChosenAction, ArrayList<Long>> actionResults =
              Map.of(
                  ChosenAction.GET_EXISTING, new ArrayList<>(),
                  ChosenAction.GET_NON_EXISTING, new ArrayList<>(),
                  ChosenAction.SET, new ArrayList<>());
          List<Runnable> tasks = new ArrayList<>();

          // create clients
          List<Client> clients = new LinkedList<>();
          for (int cc = 0; cc < clientCount; cc++) {
            Client newClient = clientCreator.get();
            newClient.connectToRedis(new ConnectionSettings(config.host, config.port, config.tls));
            clients.add(newClient);
          }

          for (int taskNum = 0; taskNum < concurrentNum; taskNum++) {
            final int taskNumDebugging = taskNum;
            tasks.add(
                () -> {
                  int iterationIncrement = iterationCounter.getAndIncrement();
                  int clientIndex = iterationIncrement % clients.size();

                  if (config.debugLogging) {
                    System.out.printf(
                        "%n concurrent = %d/%d, client# = %d/%d%n",
                        taskNumDebugging, concurrentNum, clientIndex + 1, clientCount);
                  }
                  while (iterationIncrement < iterations) {
                    if (config.debugLogging) {
                      System.out.printf(
                          "> iteration = %d/%d, client# = %d/%d%n",
                          iterationIncrement + 1, iterations, clientIndex + 1, clientCount);
                    }
                    // operate and calculate tik-tok
                    Pair<ChosenAction, Long> result =
                        measurePerformance(clients.get(clientIndex), dataSize, async);
                    if (result != null) {
                      actionResults.get(result.getLeft()).add(result.getRight());
                    }

                    iterationIncrement = iterationCounter.getAndIncrement();
                  }
                });
          }
          if (config.debugLogging) {
            System.out.printf("%s client Benchmarking: %n", clientCreator.get().getName());
            System.out.printf(
                "===> concurrentNum = %d, clientNum = %d, tasks = %d%n",
                concurrentNum, clientCount, tasks.size());
          }
          long before = System.nanoTime();

          // create threads and add them to the asyncpool.
          // This will start execution of all the concurrent tasks.
          List<CompletableFuture> asyncTasks =
              tasks.stream().map(CompletableFuture::runAsync).collect(Collectors.toList());
          try {
            // wait 1 second before waiting for threads to complete
            Thread.sleep(1000);
          } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
          }
          // wait for all futures to complete
          asyncTasks.forEach(
              future -> {
                try {
                  future.get();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              });
          long after = System.nanoTime();

          // print results per action
          printResults(calculateResults(actionResults), config.resultsFile);

          // print TPS
          if (config.resultsFile.isPresent()) {
            try {
              config
                  .resultsFile
                  .get()
                  .append("TPS: %s%n" + (iterations / ((after - before) / TPS_NORMALIZATION)));
            } catch (IOException ignored) {
            }
          } else {
            System.out.println("Runtime: " + ((after - before) / TPS_NORMALIZATION));
            System.out.println("Iterations: " + iterations);
            System.out.printf("TPS: %s%n", (iterations / ((after - before) / TPS_NORMALIZATION)));
          }
        }
      }
    }

    System.out.println();
  }

  public static Pair<ChosenAction, Long> measurePerformance(
      Client client, int dataSize, boolean async) {

    String value = RandomStringUtils.randomAlphanumeric(dataSize);
    Map<ChosenAction, Operation> actions = new HashMap<>();
    actions.put(
        ChosenAction.GET_EXISTING,
        async
            ? () ->
                ((AsyncClient) client)
                    .asyncGet(generateKeySet())
                    .get(ASYNC_OPERATION_TIMEOUT_SEC, TimeUnit.SECONDS)
            : () -> ((SyncClient) client).get(generateKeySet()));
    actions.put(
        ChosenAction.GET_NON_EXISTING,
        async
            ? () ->
                ((AsyncClient) client)
                    .asyncGet(generateKeyGet())
                    .get(ASYNC_OPERATION_TIMEOUT_SEC, TimeUnit.SECONDS)
            : () -> ((SyncClient) client).get(generateKeyGet()));
    actions.put(
        ChosenAction.SET,
        async
            ? () ->
                ((AsyncClient) client)
                    .asyncSet(generateKeySet(), value)
                    .get(ASYNC_OPERATION_TIMEOUT_SEC, TimeUnit.SECONDS)
            : () -> ((SyncClient) client).set(generateKeySet(), value));

    return getLatency(actions);
  }
}
