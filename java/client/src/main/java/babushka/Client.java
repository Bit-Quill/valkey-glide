package babushka;

import babushka.client.ChannelHolder;
import babushka.client.Commands;
import babushka.client.Connection;
import babushka.connection.CallbackManager;
import babushka.connection.SocketManager;
import java.nio.channels.NotYetConnectedException;
import lombok.Getter;

public class Client {

  private final ChannelHolder channelHolder;
  private final Commands commands;
  @Getter private final Connection connection;

  public Client() {
    var callBackManager = new CallbackManager();
    channelHolder =
        new ChannelHolder(
            SocketManager.getInstance().openNewChannel(callBackManager), callBackManager);
    connection = new Connection(channelHolder);
    commands = new Commands(channelHolder);
  }

  public Commands getCommands() {
    if (!connection.isConnected()) {
      throw new NotYetConnectedException();
    }
    return commands;
  }
}
