package glide.connectors.handlers;

import connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import glide.connectors.resources.Platform;
import glide.connectors.resources.ThreadPoolAllocator;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import java.util.Optional;
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
      CallbackDispatcher callbackDispatcher, String socketPath, Integer configThreadPoolSize) {
    channel =
        new Bootstrap()
            .group(
                ThreadPoolAllocator.createOrGetNettyThreadPool(
                    THREAD_POOL_NAME, Optional.ofNullable(configThreadPoolSize)))
            .channel(Platform.getClientUdsNettyChannelType())
            .handler(new ProtobufSocketChannelInitializer(callbackDispatcher))
            .connect(new DomainSocketAddress(socketPath))
            // TODO call here .sync() if needed or remove this comment
            .channel();
    this.callbackDispatcher = callbackDispatcher;
  }

  public ChannelHandler(
      CallbackDispatcher callbackDispatcher, String socketPath, EventLoopGroup eventLoopGroup) {
    Class<? extends DomainSocketChannel> channelClass;
    if (eventLoopGroup instanceof KQueueEventLoopGroup) {
      channelClass = KQueueDomainSocketChannel.class;
    } else if (eventLoopGroup instanceof EpollEventLoopGroup) {
      channelClass = EpollDomainSocketChannel.class;
    } else {
      throw new RuntimeException(
          "Current platform supports no known socket types for the event loop group");
    }

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
