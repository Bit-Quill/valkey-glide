package glide.api;

import static glide.ffi.resolvers.SocketListenerResolver.getSocket;

import glide.api.commands.BaseCommands;
import glide.api.models.configuration.RedisClientConfiguration;
import glide.connectors.handlers.CallbackDispatcher;
import glide.connectors.handlers.ChannelHandler;
import glide.connectors.resources.Platform;
import glide.connectors.resources.ThreadPoolResource;
import glide.connectors.resources.ThreadPoolResourceAllocator;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import glide.managers.models.Command;
import java.util.concurrent.CompletableFuture;

/**
 * Async (non-blocking) client for Redis in Standalone mode. Use {@link
 * #CreateClient(RedisClientConfiguration)} to request a client to Redis.
 */
public class RedisClient extends BaseClient implements BaseCommands {

  /**
   * Request an async (non-blocking) Redis client in Standalone mode.
   *
   * @param config - Redis Client Configuration
   * @return a future to connect and return a RedisClient
   */
  public static CompletableFuture<RedisClient> CreateClient(RedisClientConfiguration config) {
    ThreadPoolResource threadPoolResource = config.getThreadPoolResource();
    if (threadPoolResource == null) {
      threadPoolResource = ThreadPoolResourceAllocator.getOrCreate(Platform.getThreadPoolResourceSupplier());
    }
    ChannelHandler channelHandler = buildChannelHandler(threadPoolResource);
    var connectionManager = buildConnectionManager(channelHandler);
    var commandManager = buildCommandManager(channelHandler);
    // TODO: Support exception throwing, including interrupted exceptions
    return connectionManager
        .connectToRedis(config)
        .thenApply(ignore -> new RedisClient(connectionManager, commandManager));
  }

  protected static ChannelHandler buildChannelHandler(ThreadPoolResource threadPoolResource) {
    CallbackDispatcher callbackDispatcher = new CallbackDispatcher();
    return new ChannelHandler(callbackDispatcher, getSocket(), threadPoolResource);
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
     * Executes a single command, without checking inputs. Every part of the command, including
     * subcommands, should be added as a separate value in args.
     *
     * @remarks This function should only be used for single-response commands. Commands that don't
     *     return response (such as SUBSCRIBE), or that return potentially more than a single response
     *     (such as XREAD), or that change the client's behavior (such as entering pub/sub mode on
     *     RESP2 connections) shouldn't be called using this function.
     * @example Returns a list of all pub/sub clients:
     *     <pre>
     * Object result = client.customCommand(new String[]{"CLIENT","LIST","TYPE", "PUBSUB"}).get();
     * </pre>
     *
     * @param args arguments for the custom command
     * @return a CompletableFuture with response result from Redis
     */
    public CompletableFuture<Object> customCommand(String[] args) {
        Command command =
                Command.builder().requestType(Command.RequestType.CUSTOM_COMMAND).arguments(args).build();
        return commandManager.submitNewCommand(command, BaseClient::handleObjectResponse);
    }
}
