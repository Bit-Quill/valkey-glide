package babushka.api;

import static babushka.api.commands.Command.RequestType.CUSTOM_COMMAND;
import static babushka.api.commands.Command.RequestType.GETSTRING;
import static babushka.api.commands.Command.RequestType.SETSTRING;
import static babushka.api.models.commands.SetOptions.createSetOptions;

import babushka.api.commands.BaseCommands;
import babushka.api.commands.Command;
import babushka.api.commands.StringCommands;
import babushka.api.commands.Transaction;
import babushka.api.commands.VoidCommands;
import babushka.api.models.commands.SetOptions;
import babushka.api.models.configuration.RedisClientConfiguration;
import babushka.managers.CommandManager;
import connection_request.ConnectionRequestOuterClass;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import response.ResponseOuterClass.Response;

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
   * Close the Redis client
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
  public <T> CompletableFuture exec(Command command, Function<Response, T> responseHandler) {
    return commandManager.submitNewCommand(command, responseHandler);
  }

  // TODO: fix for transaction
  public CompletableFuture<?> exec(Transaction transaction) {
    return new CompletableFuture<>();
  }

  /**
   * Executes a single custom command, without checking inputs. Every part of the command, including
   * subcommands, should be added as a separate value in args.
   *
   * @param cmd to be executed
   * @param args arguments for the command
   * @return CompletableFuture with the response
   */
  public CompletableFuture<Object> customCommand(String cmd, String[] args) {
    String[] commandArguments = (String[]) Array.newInstance(String.class, args.length + 1);
    commandArguments[0] = cmd;
    System.arraycopy(args, 0, commandArguments, 1, args.length);

    Command command =
        Command.builder().requestType(CUSTOM_COMMAND).arguments(commandArguments).build();
    return exec(command, BaseCommands::handleResponse);
  }

  /**
   * Get the value associated with the given key, or null if no such value exists. See
   * https://redis.io/commands/set/ for details.
   *
   * @param key - The key to retrieve from the database.
   * @return If `key` exists, returns the value of `key` as a string. Otherwise, return null
   */
  public CompletableFuture<String> get(String key) {
    Command command =
        Command.builder().requestType(GETSTRING).arguments(new String[] {key}).build();
    return exec(command, StringCommands::handleStringResponse);
  }

  /**
   * Set the given key with the given value. Return value is dependent on the passed options. See
   * https://redis.io/commands/set/ for details.
   *
   * @param key - The key to store.
   * @param value - The value to store with the given key.
   * @return null
   */
  public CompletableFuture<Void> set(String key, String value) {
    Command command =
        Command.builder().requestType(SETSTRING).arguments(new String[] {key, value}).build();
    return exec(command, VoidCommands::handleVoidResponse);
  }

  /**
   * Set the given key with the given value. Return value is dependent on the passed options. See
   * https://redis.io/commands/set/ for details.
   *
   * @param key - The key to store.
   * @param value - The value to store with the given key.
   * @param options - The Set options
   * @return string or null If value isn't set because of `onlyIfExists` or `onlyIfDoesNotExist`
   *     conditions, return null. If `returnOldValue` is set, return the old value as a string.
   */
  public CompletableFuture<String> set(String key, String value, SetOptions options) {
    LinkedList<String> args = new LinkedList<>();
    args.add(key);
    args.add(value);
    args.addAll(createSetOptions(options));
    Command command =
        Command.builder().requestType(SETSTRING).arguments(args.toArray(new String[0])).build();
    return exec(command, StringCommands::handleStringResponse);
  }
}
