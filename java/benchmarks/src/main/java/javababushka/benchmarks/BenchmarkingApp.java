package javababushka.benchmarks;

import static javababushka.benchmarks.utils.Benchmarking.testClientSetGet;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import javababushka.benchmarks.clients.babushka.JniNettyClient;
import javababushka.benchmarks.clients.babushka.JniSyncClient;
import javababushka.benchmarks.clients.jedis.JedisClient;
import javababushka.benchmarks.clients.jedis.JedisPseudoAsyncClient;
import javababushka.benchmarks.clients.lettuce.LettuceAsyncClient;
import javababushka.benchmarks.clients.lettuce.LettuceClient;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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

      // generate the help statement
      if (line.hasOption("help")) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("javababushka", options);
        return;
      }

      runConfiguration = verifyOptions(line);
    } catch (ParseException exp) {
      // oops, something went wrong
      System.err.println("Parsing failed.  Reason: " + exp.getMessage());
    }

    for (ClientName client : runConfiguration.clients) {
      switch (client) {
        case JEDIS:
          // run testClientSetGet on JEDIS sync client
          testClientSetGet(JedisClient::new, runConfiguration, false);
          break;
        case JEDIS_ASYNC:
          // run testClientSetGet on JEDIS pseudo-async client
          testClientSetGet(JedisPseudoAsyncClient::new, runConfiguration, true);
          break;
        case LETTUCE:
          testClientSetGet(LettuceClient::new, runConfiguration, false);
          break;
        case LETTUCE_ASYNC:
          testClientSetGet(LettuceAsyncClient::new, runConfiguration, true);
          break;
        case BABUSHKA_JNI:
          testClientSetGet(JniSyncClient::new, runConfiguration, false);
          break;
        case JNI_NETTY:
          testClientSetGet(() -> new JniNettyClient(false), runConfiguration, false);
          testClientSetGet(() -> new JniNettyClient(true), runConfiguration, true);
          break;
        case BABUSHKA_ASYNC:
          System.out.println("Babushka async not yet configured");
          break;
      }
    }
  }

  private static Options getOptions() {
    // create the Options
    Options options = new Options();

    options.addOption(Option.builder("help").desc("print this message").build());
    options.addOption(
        Option.builder("configuration").hasArg(true).desc("Configuration flag [Release]").build());
    options.addOption(
        Option.builder("resultsFile")
            .hasArg(true)
            .desc("Result filepath (stdout if empty) []")
            .build());
    options.addOption(
        Option.builder("dataSize").hasArg(true).desc("Data block size [100 4000]").build());
    options.addOption(
        Option.builder("concurrentTasks")
            .hasArg(true)
            .desc("Number of concurrent tasks [100, 1000]")
            .build());
    options.addOption(
        Option.builder("clients")
            .hasArg(true)
            .desc(
                "one of: all|jedis|jedis_async|lettuce|lettuce_async|"
                    + "babushka|babushka_async|babushka_jni|all_async|all_sync")
            .build());
    options.addOption(Option.builder("host").hasArg(true).desc("Hostname [localhost]").build());
    options.addOption(Option.builder("port").hasArg(true).desc("Port number [6379]").build());
    options.addOption(
        Option.builder("clientCount").hasArg(true).desc("Number of clients to run [1]").build());
    options.addOption(Option.builder("tls").hasArg(false).desc("TLS [false]").build());

    return options;
  }

  private static RunConfiguration verifyOptions(CommandLine line) throws ParseException {
    RunConfiguration runConfiguration = new RunConfiguration();

    if (line.hasOption("configuration")) {
      String configuration = line.getOptionValue("configuration");
      if (configuration.equalsIgnoreCase("Release") || configuration.equalsIgnoreCase("Debug")) {
        runConfiguration.configuration = configuration;
      } else {
        throw new ParseException(
            "Invalid run configuration (" + configuration + "), must be (Release|Debug)");
      }
    }

    if (line.hasOption("resultsFile")) {
      runConfiguration.resultsFile = Optional.ofNullable(line.getOptionValue("resultsFile"));
    }

    if (line.hasOption("concurrentTasks")) {
      runConfiguration.concurrentTasks = parseIntListOption(line.getOptionValue("concurrentTasks"));
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
                            // ClientName.BABUSHKA_ASYNC,
                            ClientName.BABUSHKA,
                            ClientName.BABUSHKA_JNI,
                            ClientName.LETTUCE,
                            ClientName.LETTUCE_ASYNC);
                      case ALL_ASYNC:
                        return Stream.of(
                            ClientName.JEDIS_ASYNC,
                            // ClientName.BABUSHKA_ASYNC,
                            ClientName.LETTUCE_ASYNC);
                      case ALL_SYNC:
                        return Stream.of(
                            ClientName.JEDIS,
                            // ClientName.BABUSHKA,
                            ClientName.BABUSHKA_JNI,
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
      runConfiguration.clientCount = parseIntListOption(line.getOptionValue("clientCount"));
    }

    if (line.hasOption("dataSize")) {
      runConfiguration.dataSize = parseIntListOption(line.getOptionValue("dataSize"));
    }

    runConfiguration.tls = line.hasOption("tls");

    return runConfiguration;
  }

  private static int[] parseIntListOption(String line) throws ParseException {
    String lineValue = line;

    // remove optional square brackets
    if (lineValue.startsWith("[") && lineValue.endsWith("]")) {
      lineValue = lineValue.substring(1, lineValue.length() - 1);
    }
    // check if it's the correct format
    if (!lineValue.matches("\\d+(\\s+\\d+)?")) {
      throw new ParseException("Invalid option: " + line);
    }
    // split the string into a list of integers
    return Arrays.stream(lineValue.split("\\s+")).mapToInt(Integer::parseInt).toArray();
  }

  public enum ClientName {
    JNI_NETTY("JNI netty"),
    JEDIS("Jedis"),
    JEDIS_ASYNC("Jedis async"),
    LETTUCE("Lettuce"),
    LETTUCE_ASYNC("Lettuce async"),
    BABUSHKA_JNI("JNI sync"),
    BABUSHKA_ASYNC("Babushka async"),
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
    public int[] dataSize;
    public int[] concurrentTasks;
    public Optional<String> resultsFile;
    public ClientName[] clients;
    public String host;
    public int port;
    public int[] clientCount;
    public boolean tls;
    public boolean debugLogging = false;

    public RunConfiguration() {
      configuration = "Release";
      resultsFile = Optional.of("res_java.json");//Optional.empty();
      dataSize = new int[] {100, 4000};
      concurrentTasks = new int[] {100};
      clients =
          new ClientName[] {
            // ClientName.BABUSHKA_ASYNC,
            //ClientName.JEDIS, ClientName.JEDIS_ASYNC, ClientName.LETTUCE, ClientName.LETTUCE_ASYNC
              ClientName.JNI_NETTY, ClientName.LETTUCE, ClientName.LETTUCE_ASYNC
          };
      host = "localhost";
      port = 6379;
      clientCount = new int[] {1, 2};
      tls = false;
    }
  }
}
