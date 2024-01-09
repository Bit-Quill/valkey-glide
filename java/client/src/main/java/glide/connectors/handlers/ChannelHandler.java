package glide.connectors.handlers;

import connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import glide.connectors.resources.ThreadPoolResource;
import glide.connectors.resources.ThreadPoolResourceAllocator;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import java.util.concurrent.CompletableFuture;
import redis_request.RedisRequestOuterClass.RedisRequest;
import response.ResponseOuterClass.Response;

/**
 * Class responsible for handling calls to/from a netty.io {@link Channel}. Uses a {@link
 * CallbackDispatcher} to record callbacks of every request sent.
 */
public class ChannelHandler {

  private static final String THREAD_POOL_NAME = "glide-channel";

  private final Channel channel;
  private final CallbackDispatcher callbackDispatcher;

  /** Open a new channel for a new client. */
  public ChannelHandler(
      CallbackDispatcher callbackDispatcher,
      String socketPath,
      ThreadPoolResource customThreadPoolResource) {
    EventLoopGroup eventLoopGroup = customThreadPoolResource.getEventLoopGroup();
    Class<? extends DomainSocketChannel> channelClass =
        customThreadPoolResource.getDomainSocketChannelClass();

    channel =
        new Bootstrap()
            .group(eventLoopGroup)
            .channel(channelClass)
            .handler(new ProtobufSocketChannelInitializer(callbackDispatcher))
            .connect(new DomainSocketAddress(socketPath))
            // TODO call here .sync() if needed or remove this comment
            .channel();
    this.callbackDispatcher = callbackDispatcher;
  }

  public ChannelHandler(CallbackDispatcher callbackDispatcher, String socketPath) {
    this(
        callbackDispatcher,
        socketPath,
        ThreadPoolResourceAllocator.createOrGetThreadPoolResource());
  }

  /**
   * Complete a protobuf message and write it to the channel (to UDS).
   *
   * @param request Incomplete request, function completes it by setting callback ID
   * @param flush True to flush immediately
   * @return A response promise
   */
  public CompletableFuture<Response> write(RedisRequest.Builder request, boolean flush) {
    var commandId = callbackDispatcher.registerRequest();
    request.setCallbackIdx(commandId.getKey());

    if (flush) {
      channel.writeAndFlush(request.build());
    } else {
      channel.write(request.build());
    }
    return commandId.getValue();
  }

  /**
   * Write a protobuf message to the channel (to UDS).
   *
   * @param request A connection request
   * @return A connection promise
   */
  public CompletableFuture<Response> connect(ConnectionRequest request) {
    channel.writeAndFlush(request);
    return callbackDispatcher.registerConnection();
  }

  /** Closes the UDS connection and frees corresponding resources. */
  public void close() {
    channel.close();
    callbackDispatcher.shutdownGracefully();
  }
}
