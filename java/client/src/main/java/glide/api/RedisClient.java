package glide.api;

import static glide.ffi.resolvers.SocketListenerResolver.getSocket;

import glide.api.commands.BaseCommands;
import glide.api.commands.Command;
import glide.api.commands.RedisExceptionCheckedFunction;
import glide.api.commands.StringCommands;
import glide.api.commands.Transaction;
import glide.api.commands.VoidCommands;
import glide.api.models.commands.SetOptions;
import glide.api.models.configuration.RedisClientConfiguration;
import glide.connectors.handlers.CallbackDispatcher;
import glide.connectors.handlers.ChannelHandler;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import response.ResponseOuterClass;

/**
 * Async (non-blocking) client for Redis in Standalone mode. Use {@link
 * #CreateClient(RedisClientConfiguration)} to request a client to Redis.
 */
public class RedisClient extends BaseClient implements BaseCommands, VoidCommands, StringCommands {

  /**
   * Request an async (non-blocking) Redis client in Standalone mode.
   *
   * @param config - Redis Client Configuration
   * @return a Future to connect and return a RedisClient
   */
  public static CompletableFuture<RedisClient> CreateClient(RedisClientConfiguration config) {
    ChannelHandler channelHandler = buildChannelHandler();
    ConnectionManager connectionManager = buildConnectionManager(channelHandler);
    CommandManager commandManager = buildCommandManager(channelHandler);
    // TODO: Support exception throwing, including interrupted exceptions
    return connectionManager
        .connectToRedis(config)
        .thenApply(ignore -> new RedisClient(connectionManager, commandManager));
  }

  protected static ChannelHandler buildChannelHandler() {
    CallbackDispatcher callbackDispatcher = new CallbackDispatcher();
    return new ChannelHandler(callbackDispatcher, getSocket());
  }

  protected static ConnectionManager buildConnectionManager(ChannelHandler channelHandler) {
    return new ConnectionManager(channelHandler);
  }

  protected static CommandManager buildCommandManager(ChannelHandler channelHandler) {
    return new CommandManager(channelHandler);
  }

  protected RedisClient(ConnectionManager connectionManager, CommandManager commandManager) {
    super(connectionManager, commandManager);
  }

  /**
   * Execute a single command against Redis. <br>
   *
   * @param command to be executed
   * @param responseHandler handler responsible for assigning type to the response
   * @return A CompletableFuture completed with the result from Redis
   * @param <T> Response value type
   */
  protected <T> CompletableFuture exec(
      Command command,
      RedisExceptionCheckedFunction<ResponseOuterClass.Response, T> responseHandler) {
    return commandManager.submitNewCommand(command, responseHandler);
  }

  /**
   * Execute a transaction by processing the queued commands. <br>
   * See https://redis.io/topics/Transactions/ for details on Redis Transactions. <br>
   *
   * @param transaction with commands to be executed
   * @return A CompletableFuture completed with the results from Redis
   */
  public CompletableFuture<List<Object>> exec(Transaction transaction) {
    // TODO: call commandManager.submitNewTransaction()
    return exec(transaction, BaseCommands::handleTransactionResponse);
  }

  /**
   * Execute a transaction by processing the queued commands. <br>
   * See https://redis.io/topics/Transactions/ for details on Redis Transactions. <br>
   *
   * @param transaction with commands to be executed
   * @param responseHandler handler responsible for assigning type to the list of response objects
   * @return A CompletableFuture completed with the results from Redis
   */
  protected CompletableFuture<List<Object>> exec(
      Transaction transaction,
      Function<ResponseOuterClass.Response, List<Object>> responseHandler) {
    // TODO: call commandManager.submitNewTransaction()
    return new CompletableFuture<>();
  }

  /**
   * Executes a single custom command, without checking inputs. Every part of the command, including
   * subcommands, should be added as a separate value in args.
   *
   * @param args command and arguments for the custom command call
   * @return CompletableFuture with the response
   */
  public CompletableFuture<Object> customCommand(String[] args) {
    Command command =
        Command.builder().requestType(Command.RequestType.CUSTOM_COMMAND).arguments(args).build();
    return exec(command, BaseCommands::handleObjectResponse);
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
        Command.builder()
            .requestType(Command.RequestType.GET_STRING)
            .arguments(new String[] {key})
            .build();
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
        Command.builder()
            .requestType(Command.RequestType.SET_STRING)
            .arguments(new String[] {key, value})
            .build();
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
    args.addAll(SetOptions.createSetOptions(options));
    Command command =
        Command.builder()
            .requestType(Command.RequestType.SET_STRING)
            .arguments(args.toArray(new String[0]))
            .build();
    return exec(command, StringCommands::handleStringResponse);
  }
}
