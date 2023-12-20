package babushka.managers;

import babushka.connectors.ClientState;

public class ClientStateImpl {
  private enum InnerStates {
    INITIALIZING,
    CONNECTED,
    CLOSED
  }

  /**
   * Create an instance of {@link ClientState.ReadOnlyClientState} which can be safely shipped to
   * {@link CommandManager} and {@link ConnectionManager}. Only those classes are responsible to
   * switch the state.
   */
  public static ClientState.ReadOnlyClientState create() {
    return new ClientState.OpenableAndClosableClientState() {
      private ClientStateImpl.InnerStates state = ClientStateImpl.InnerStates.INITIALIZING;

      @Override
      public boolean isConnected() {
        return state == ClientStateImpl.InnerStates.CONNECTED;
      }

      @Override
      public boolean isInitializing() {
        return state == ClientStateImpl.InnerStates.INITIALIZING;
      }

      @Override
      public void connect(boolean successful) {
        if (state != ClientStateImpl.InnerStates.INITIALIZING) {
          throw new IllegalStateException();
        }
        state =
            successful ? ClientStateImpl.InnerStates.CONNECTED : ClientStateImpl.InnerStates.CLOSED;
      }

      @Override
      public void disconnect() {
        state = ClientStateImpl.InnerStates.CLOSED;
      }
    };
  }
}
