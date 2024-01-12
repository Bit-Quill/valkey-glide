package glide.connectors.resources;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.unix.DomainSocketChannel;
import lombok.Getter;
import lombok.NonNull;

/**
 * Abstract base class that sets up the EventLoopGroup and channel configuration for Netty
 * applications.
 */
@Getter
public abstract class ThreadPoolResource {
  private EventLoopGroup eventLoopGroup;
  private Class<? extends DomainSocketChannel> domainSocketChannelClass;

  public ThreadPoolResource(
      @NonNull EventLoopGroup eventLoopGroup,
      @NonNull Class<? extends DomainSocketChannel> domainSocketChannelClass) {
    this.eventLoopGroup = eventLoopGroup;
    this.domainSocketChannelClass = domainSocketChannelClass;
  }
}
