package javabushka.client;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javabushka.client.jedis.JedisClient;
import javabushka.client.jedis.JedisPseudoAsyncClient;
import javabushka.client.lettuce.LettuceAsyncClient;
import javabushka.client.lettuce.LettuceClient;
import javabushka.client.utils.Benchmarking;
import javabushka.client.utils.ChosenAction;
import javabushka.client.utils.ConnectionSettings;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;

/** Benchmarking app for reporting performance of various redis-rs Java-clients */
public class BenchmarkingApp {

  // main application entrypoint
  public static void main(String[] args) {

    // create the parser
    CommandLineParser parser = new DefaultParser();
    Options options = getOptions();
    RunConfiguration runConfiguration = new RunConfiguration();
    try {
      // parse the command line arguments
      CommandLine line = parser.parse(options, args);
      runConfiguration = verifyOptions(line);
    } catch (ParseException exp) {
      // oops, something went wrong
      System.err.println("Parsing failed.  Reason: " + exp.getMessage());
    }

    try {
      for (ClientName client : runConfiguration.clients) {
        switch (client) {
          case JEDIS:
            testIterateTasksAndClientSetGet(JedisClient::new, runConfiguration, false);
            break;
          case JEDIS_ASYNC:
            testIterateTasksAndClientSetGet(JedisPseudoAsyncClient::new, runConfiguration, true);
            break;
          case LETTUCE:
            testIterateTasksAndClientSetGet(LettuceClient::new, runConfiguration, false);
            break;
          case LETTUCE_ASYNC:
            testIterateTasksAndClientSetGet(LettuceAsyncClient::new, runConfiguration, true);
            break;
          case BABUSHKA:
            System.out.println("Babushka not yet configured");
            break;
        }
      }
    } catch (IOException ioException) {
      System.out.println("Error writing results to file");
    }

    if (runConfiguration.resultsFile.isPresent()) {
      try {
        runConfiguration.resultsFile.get().close();
      } catch (IOException ioException) {
        System.out.println("Error closing results file");
      }
    }
  }

  private static Options getOptions() {
    // create the Options
    Options options = new Options();

    options.addOption("c", "configuration", true, "Configuration flag [Release]");
    options.addOption("f", "resultsFile", true, "Result filepath []");
    options.addOption("d", "dataSize", true, "Data block size [20]");
    options.addOption("C", "concurrentTasks", true, "Number of concurrent tasks [1 10 100]");
    options.addOption(
        "l", "clients", true, "one of: all|jedis|jedis_async|lettuce|lettuce_async|babushka [all]");
    options.addOption("h", "host", true, "host url [localhost]");
    options.addOption("p", "port", true, "port number [6379]");
    options.addOption("n", "clientCount", true, "Client count [1]");
    options.addOption("t", "tls", false, "TLS [true]");

    return options;
  }

  private static RunConfiguration verifyOptions(CommandLine line) throws ParseException {
    RunConfiguration runConfiguration = new RunConfiguration();
    if (line.hasOption("configuration")) {
      String configuration = line.getOptionValue("configuration");
      if (configuration.equalsIgnoreCase("Release") || configuration.equalsIgnoreCase("Debug")) {
        runConfiguration.configuration = configuration;
      } else {
        throw new ParseException("Invalid run configuration (Release|Debug)");
      }
    }

    if (line.hasOption("resultsFile")) {
      try {
        runConfiguration.resultsFile =
            Optional.of(new FileWriter(line.getOptionValue("resultsFile")));
      } catch (IOException e) {
        throw new ParseException("Unable to write to resultsFile.");
      }
    }

    if (line.hasOption("dataSize")) {
      runConfiguration.dataSize = Integer.parseInt(line.getOptionValue("dataSize"));
    }

    if (line.hasOption("concurrentTasks")) {
      String concurrentTasks = line.getOptionValue("concurrentTasks");

      // remove optional square brackets
      if (concurrentTasks.startsWith("[") && concurrentTasks.endsWith("]")) {
        concurrentTasks = concurrentTasks.substring(1, concurrentTasks.length() - 1);
      }
      // check if it's the correct format
      if (!concurrentTasks.matches("\\d+(\\s+\\d+)?")) {
        throw new ParseException("Invalid concurrentTasks");
      }
      // split the string into a list of integers
      runConfiguration.concurrentTasks =
          Arrays.stream(concurrentTasks.split("\\s+"))
              .map(Integer::parseInt)
              .collect(Collectors.toList());
    }

    if (line.hasOption("clients")) {
      String[] clients = line.getOptionValue("clients").split(",");
      runConfiguration.clients =
          Arrays.stream(clients)
              .map(c -> Enum.valueOf(ClientName.class, c.toUpperCase()))
              .flatMap(
                  e -> {
                    switch (e) {
                      case ALL:
                        return Stream.of(
                            ClientName.JEDIS,
                            ClientName.JEDIS_ASYNC,
                            ClientName.BABUSHKA,
                            ClientName.LETTUCE,
                            ClientName.LETTUCE_ASYNC);
                      case ALL_ASYNC:
                        return Stream.of(
                            ClientName.JEDIS_ASYNC,
                            // ClientName.BABUSHKA,
                            ClientName.LETTUCE_ASYNC);
                      case ALL_SYNC:
                        return Stream.of(
                            ClientName.JEDIS,
                            // ClientName.BABUSHKA,
                            ClientName.LETTUCE);
                      default:
                        return Stream.of(e);
                    }
                  })
              .toArray(ClientName[]::new);
    }

    if (line.hasOption("host")) {
      runConfiguration.host = line.getOptionValue("host");
    }

    if (line.hasOption("clientCount")) {
      String clientCount = line.getOptionValue("clientCount");

      // check if it's the correct format
      if (!clientCount.matches("\\d+(\\s+\\d+)?")) {
        throw new ParseException("Invalid clientCount");
      }
      // split the string into a list of integers
      runConfiguration.clientCount =
          Arrays.stream(clientCount.split("\\s+")).mapToInt(Integer::parseInt).toArray();
    }

    if (line.hasOption("tls")) {
      runConfiguration.tls = Boolean.parseBoolean(line.getOptionValue("tls"));
    }

    return runConfiguration;
  }

  // call testConcurrentClientSetGet for each concurrentTask/clientCount pairing
  private static void testIterateTasksAndClientSetGet(
      Supplier<Client> clientSupplier, RunConfiguration runConfiguration, boolean async)
      throws IOException {
    System.out.printf("%n =====> %s <===== %n%n", clientSupplier.get().getName());
    for (int clientCount : runConfiguration.clientCount) {
      for (int concurrentTasks : runConfiguration.concurrentTasks) {
        Client client = clientSupplier.get();
        testConcurrentClientSetGet(
            clientSupplier, runConfiguration, concurrentTasks, clientCount, async);
      }
    }
    System.out.println();
  }

  // call one test scenario: with a number of concurrent threads against clientCount number
  // of clients
  private static void testConcurrentClientSetGet(
      Supplier<Client> clientSupplier,
      RunConfiguration runConfiguration,
      int concurrentTasks,
      int clientCount,
      boolean async)
      throws IOException {
    // fetch a reasonable number of iterations based on the number of concurrent tasks
    int iterations = Math.min(Math.max(100000, concurrentTasks * 10000), 10000000);
    AtomicInteger iterationCounter = new AtomicInteger(0);

    // create clients
    List<Client> clients = new LinkedList<>();
    for (int i = 0; i < clientCount; i++) {
      Client newClient = clientSupplier.get();
      newClient.connectToRedis(
          new ConnectionSettings(
              runConfiguration.host, runConfiguration.port, runConfiguration.tls));
      clients.add(newClient);
    }

    // create runnable tasks
    List<Runnable> tasks = new ArrayList<>();
    Map<ChosenAction, ArrayList<Long>> actionResults = new HashMap<>();
    actionResults.put(ChosenAction.GET_EXISTING, new ArrayList<>());
    actionResults.put(ChosenAction.GET_NON_EXISTING, new ArrayList<>());
    actionResults.put(ChosenAction.SET, new ArrayList<>());

    // add one runnable task for each concurrentTask
    // task will run a random action against a client, uniformly distributed amongst all clients
    for (int concurrentTaskIndex = 0;
        concurrentTaskIndex < concurrentTasks;
        concurrentTaskIndex++) {
      System.out.printf("concurrent task: %d/%d%n", concurrentTaskIndex + 1, concurrentTasks);
      tasks.add(
          () -> {
            int iterationIncrement = iterationCounter.incrementAndGet();
            while (iterationIncrement < iterations) {
              int clientIndex = iterationIncrement % clients.size();
              System.out.printf(
                  "> iteration = %d/%d, client# = %d/%d%n",
                  iterationIncrement + 1, iterations, clientIndex + 1, clientCount);

              // operate and calculate tik-tok
              Pair<ChosenAction, Long> result =
                  Benchmarking.measurePerformance(
                      clients.get(clientIndex), runConfiguration.dataSize, async);

              // save tik-tok to actionResults
              actionResults.get(result.getLeft()).add(result.getRight());
              iterationIncrement = iterationCounter.incrementAndGet();
            }
          });
    }

    // run all tasks asynchronously
    tasks.stream()
        .map(CompletableFuture::runAsync)
        .forEach(
            f -> {
              try {
                f.get();
              } catch (Exception e) {
                e.printStackTrace();
              }
            });

    // use results file to stdout/print
    Benchmarking.printResults(
        Benchmarking.calculateResults(actionResults), runConfiguration.resultsFile);

    // close connections
    clients.forEach(c -> c.closeConnection());
  }

  public enum ClientName {
    JEDIS("Jedis"),
    JEDIS_ASYNC("Jedis async"),
    LETTUCE("Lettuce"),
    LETTUCE_ASYNC("Lettuce async"),
    BABUSHKA("Babushka"),
    ALL("All"),
    ALL_SYNC("All sync"),
    ALL_ASYNC("All async");

    private String name;

    private ClientName(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }

    public boolean isEqual(String other) {
      return this.toString().equalsIgnoreCase(other);
    }
  }

  public static class RunConfiguration {
    public String configuration;
    public Optional<FileWriter> resultsFile;
    public int dataSize;
    public List<Integer> concurrentTasks;
    public ClientName[] clients;
    public String host;
    public int port;
    public int[] clientCount;
    public boolean tls;

    public RunConfiguration() {
      configuration = "Release";
      resultsFile = Optional.empty();
      dataSize = 20;
      concurrentTasks = List.of(1, 10, 100);
      clients =
          new ClientName[] {
            // ClientName.BABUSHKA,
            ClientName.JEDIS, ClientName.JEDIS_ASYNC, ClientName.LETTUCE, ClientName.LETTUCE_ASYNC
          };
      host = "localhost";
      port = 6379;
      clientCount = new int[] {1, 2};
      tls = false;
    }
  }
}
