package babushka.managers;

public class ClientState {

  private ClientState() {}

  /**
   * A read only client state. It is supposed that main Client class will have instance of the state
   * of this type and won't be able to change the state directly.
   */
  public static interface ReadOnlyClientState {
    /** Check that connection established. This doesn't validate whether it is alive. */
    boolean isConnected();
  }

  /** A client state which accepts switching to <em>Connected</em> or <em>Closed</em> states. */
  public static interface OpenableAndClosableClientState extends ClosableClientState {
    /** Report connection status. */
    void connect(boolean successful);
  }

  /** A client state which accepts only one way switching - to <em>Closed</em> state only. */
  public static interface ClosableClientState extends ReadOnlyClientState {
    /** Report disconnection. */
    void disconnect();
  }

  private enum InnerStates {
    INITIALIZING,
    CONNECTED,
    CLOSED
  }

  /**
   * Create an instance of {@link ReadOnlyClientState} which can be safely shipped to {@link
   * CommandManager} and {@link ConnectionManager}. Only those classes are responsible to switch the
   * state.
   */
  public static ReadOnlyClientState create() {
    return new OpenableAndClosableClientState() {
      private InnerStates state = InnerStates.INITIALIZING;

      @Override
      public boolean isConnected() {
        return state == InnerStates.CONNECTED;
      }

      @Override
      public void connect(boolean successful) {
        if (state != InnerStates.INITIALIZING) {
          throw new IllegalStateException();
        }
        state = successful ? InnerStates.CONNECTED : InnerStates.CLOSED;
      }

      @Override
      public void disconnect() {
        state = InnerStates.CLOSED;
      }
    };
  }
}
