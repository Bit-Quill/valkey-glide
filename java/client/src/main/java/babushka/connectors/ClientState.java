package babushka.connectors;

public class ClientState {

  private ClientState() {}

  /**
   * A read only client state. It is supposed that main Client class will have instance of the state
   * of this type and won't be able to change the state directly.
   */
  public static interface ReadOnlyClientState {
    /** Check that connection established. This doesn't validate whether it is alive. */
    boolean isConnected();

    /** Check that connection is not yet established. */
    boolean isInitializing();
  }

  /** A client state which accepts switching to <em>Connected</em> state. */
  public static interface OpenableClientState extends ReadOnlyClientState {
    /** Report connection status. */
    void connect(boolean successful);
  }
}
