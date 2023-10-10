package javababushka.benchmarks.utils;

/** Redis-client settings */
public class ConnectionSettings {
  public String host;
  public int port;
  public boolean useSsl;

  public ConnectionSettings(String host, int port, boolean useSsl) {
    this.host = host;
    this.port = port;
    this.useSsl = useSsl;
  }
}
