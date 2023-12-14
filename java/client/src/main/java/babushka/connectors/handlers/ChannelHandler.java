package babushka.connectors.handlers;

import babushka.connectors.resources.Platform;
import babushka.managers.CallbackManager;
import connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.unix.DomainSocketAddress;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import redis_request.RedisRequestOuterClass.RedisRequest;
import response.ResponseOuterClass.Response;

/**
 * Class responsible for manipulations with Netty's {@link Channel}.<br>
 * Uses a {@link CallbackManager} to record callbacks of every request sent.
 */
public class ChannelHandler {
  private final Channel channel;
  private final CallbackManager callbackManager;

  /** Open a new channel for a new client. */
  public ChannelHandler(CallbackManager callbackManager, String socketPath) {
    channel =
        new Bootstrap()
            .group(Platform.createNettyThreadPool("babushka-channel", OptionalInt.empty()))
            .channel(Platform.getClientUdsNettyChannelType())
            .handler(new ProtobufSocketChannelInitializer(callbackManager))
            .connect(new DomainSocketAddress(socketPath))
            // TODO call here .sync() if needed or remove this comment
            .channel();
    this.callbackManager = callbackManager;
  }

  /** Write a protobuf message to the socket. */
  public CompletableFuture<Response> write(RedisRequest.Builder request, boolean flush) {
    var commandId = callbackManager.registerRequest();
    request.setCallbackIdx(commandId.getKey());

    if (flush) {
      channel.writeAndFlush(request.build());
    } else {
      channel.write(request.build());
    }
    return commandId.getValue();
  }

  /** Write a protobuf message to the socket. */
  public CompletableFuture<Response> connect(ConnectionRequest request) {
    channel.writeAndFlush(request);
    return callbackManager.getConnectionPromise();
  }

  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Closes the UDS connection and frees corresponding resources. */
  public void close() {
    if (closed.compareAndSet(false, true)) {
      channel.close();
      callbackManager.shutdownGracefully();
    }
  }
}
