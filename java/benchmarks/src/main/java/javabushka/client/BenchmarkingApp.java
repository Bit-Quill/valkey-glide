package javabushka.client;

import javabushka.client.jedis.JedisClient;
import javabushka.client.lettuce.LettuceAsyncClient;
import javabushka.client.utils.Benchmarking;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Benchmarking app for reporting performance of various redis-rs Java-clients
 */
public class BenchmarkingApp {

  // main application entrypoint
  public static void main(String[] args) {

    // create the parser
    CommandLineParser parser = new DefaultParser();
    Options options = getOptions();
    try {
      // parse the command line arguments
      CommandLine line = parser.parse(options, args);
      RunConfiguration runConfiguration = verifyOptions(line);

      if (runConfiguration.clients.equalsIgnoreCase("jedis") || runConfiguration.clients.equalsIgnoreCase("all")) {
        // run jedis test
        testJedisClientResourceSetGet();
      } else if (runConfiguration.clients.equalsIgnoreCase("lettuce") || runConfiguration.clients.equalsIgnoreCase("all")) {
        // run lettuce async test
        testLettuceClientResourceSetGet();
      }
    }
    catch (ParseException exp) {
      // oops, something went wrong
      System.err.println("Parsing failed.  Reason: " + exp.getMessage());
    }
  }

  private static Options getOptions() {
    // create the Options
    Options options = new Options();

    options.addOption("c", "configuration", true, "Configuration flag [Release]");
    options.addOption("f", "resultsFile", true, "Result filepath []");
    options.addOption("C", "concurrentTasks", true, "Number of concurrent tasks [1 10 100]");
    options.addOption("l", "clients", true, "one of: jedis|jedis_async|lettuce [jedis]");
    options.addOption("h", "host", true, "host url [localhost]");
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
      runConfiguration.resultsFile = line.getOptionValue("resultsFile");
    }

    if (line.hasOption("concurrentTasks")) {
      // TODO validate format
      runConfiguration.concurrentTasks = line.getOptionValue("concurrentTasks");
    }

    if (line.hasOption("clients")) {
      String clients = line.getOptionValue("clients");
      if (clients.equalsIgnoreCase("all")
          || clients.equalsIgnoreCase("jedis")
          || clients.equalsIgnoreCase("jedis_async")
          || clients.equalsIgnoreCase("lettuce")) {
        runConfiguration.clients = clients;
      } else {
        throw new ParseException("Invalid clients option: all|jedis|jedis_async|lettuce");
      }
    }

    if (line.hasOption("host")) {
      runConfiguration.host = line.getOptionValue("host");
    }

    if (line.hasOption("clientCount")) {
      runConfiguration.clientCount = Integer.parseInt(line.getOptionValue("clientCount"));
    }

    if (line.hasOption("tls")) {
      runConfiguration.tls = Boolean.parseBoolean(line.getOptionValue("tls"));
    }

    return runConfiguration;
  }

  private static JedisClient initializeJedisClient() {
    JedisClient jedisClient = new JedisClient();
    jedisClient.connectToRedis();
    return jedisClient;
  }

  private static void testJedisClientResourceSetGet() {
    JedisClient jedisClient = initializeJedisClient();

    int iterations = 100000;
    String value = "my-value";

    Benchmarking.printResults(
        "SET",
        Benchmarking.calculateResults(
            Benchmarking.getLatencies(
                iterations,
                () -> jedisClient.set(Benchmarking.generateKeySet(), value)
            )
        )
    );
    Benchmarking.printResults(
        "GET",
        Benchmarking.calculateResults(
            Benchmarking.getLatencies(
                iterations,
                () -> jedisClient.get(Benchmarking.generateKeySet())
            )
        )
    );
    Benchmarking.printResults(
        "GET non-existing",
        Benchmarking.calculateResults(
            Benchmarking.getLatencies(
                iterations,
                () -> jedisClient.get(Benchmarking.generateKeyGet())
            )
        )
    );
  }

  private static LettuceAsyncClient initializeLettuceClient() {
    LettuceAsyncClient lettuceClient = new LettuceAsyncClient();
    lettuceClient.connectToRedis();
    return lettuceClient;
  }

  private static void testLettuceClientResourceSetGet() {
    LettuceAsyncClient lettuceClient = initializeLettuceClient();

    int iterations = 100000;
    String value = "my-value";

    Benchmarking.printResults(
        "SET",
        Benchmarking.calculateResults(
            Benchmarking.getLatencies(
                iterations,
                () -> lettuceClient.set(Benchmarking.generateKeySet(), value)
            )
        )
    );
    Benchmarking.printResults(
        "GET",
        Benchmarking.calculateResults(
            Benchmarking.getLatencies(
                iterations,
                () -> lettuceClient.get(Benchmarking.generateKeySet())
            )
        )
    );
    Benchmarking.printResults(
        "GET non-existing",
        Benchmarking.calculateResults(
            Benchmarking.getLatencies(
                iterations,
                () -> lettuceClient.get(Benchmarking.generateKeyGet())
            )
        )
    );
  }

  public static class RunConfiguration {
    public String configuration;
    public String resultsFile;
    public String concurrentTasks;
    public String clients;
    public String host;
    public int clientCount;
    public boolean tls;

    public RunConfiguration() {
      configuration = "Release";
      resultsFile = "";
      concurrentTasks = "1 10 100";
      clients = "all";
      host = "localhost";
      clientCount = 1;
      tls = true;
    }
  }
}
