package babushka.api;

import static babushka.api.commands.Command.RequestType.CUSTOM_COMMAND;
import static babushka.api.commands.Command.RequestType.GETSTRING;
import static babushka.api.commands.Command.RequestType.SETSTRING;

import babushka.api.commands.BaseCommands;
import babushka.api.commands.Command;
import babushka.api.commands.StringCommands;
import babushka.api.commands.Transaction;
import babushka.api.commands.VoidCommands;
import babushka.api.models.configuration.RedisClientConfiguration;
import babushka.managers.CommandManager;
import connection_request.ConnectionRequestOuterClass;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import response.ResponseOuterClass;

/** Factory class for creating Babushka-Redis client connections */
public class RedisClient extends BaseClient
    implements BaseCommands<Object>, StringCommands, VoidCommands {

  private CommandManager commandManager;

  /**
   * Async (non-blocking) connection to Redis.
   *
   * @param config - Redis Client Configuration
   * @return a promise to connect and return a RedisClient
   */
  public static CompletableFuture<RedisClient> CreateClient(RedisClientConfiguration config) {
    // convert configuration to protobuf connection request
    ConnectionRequestOuterClass.ConnectionRequest connectionRequest =
        RequestBuilder.createConnectionRequest(
            config.getHost(), config.getPort(), config.isTls(), config.isClusterMode());

    // TODO: send request to connection manager
    // return connectionManager.connectToRedis(connectionRequest);
    return new CompletableFuture<>();
  }

  public RedisClient(CommandManager commandManager) {
    this.commandManager = commandManager;
  }

  /**
   * Close the client if it's open
   *
   * @return
   */
  @Override
  public CompletableFuture<RedisClient> close() {
    CompletableFuture<RedisClient> result;
    result = new CompletableFuture<>();
    result.complete(this);
    return result;
  }

  @Override
  public <T> CompletableFuture exec(
      Command command, Function<ResponseOuterClass.Response, T> responseHandler) {
    return commandManager.<T>submitNewCommand(command, responseHandler);
  }

  // TODO: fix for transaction
  public CompletableFuture<?> exec(Transaction transaction) {
    return new CompletableFuture<>();
  }

  public CompletableFuture<Object> customCommand(String cmd, String[] args) {
    Command command = Command.builder().requestType(CUSTOM_COMMAND).arguments(args).build();
    return exec(command, BaseCommands::handleResponse);
  }

  public CompletableFuture<?> get(String key) {
    Command command =
        Command.builder().requestType(GETSTRING).arguments(new String[] {key}).build();
    return exec(command, StringCommands::handleStringResponse);
  }

  public CompletableFuture<Void> set(String key, String value) {
    Command command =
        Command.builder().requestType(SETSTRING).arguments(new String[] {key, value}).build();
    return exec(command, VoidCommands::handleVoidResponse);
  }
}
