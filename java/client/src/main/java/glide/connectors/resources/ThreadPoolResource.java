package glide.connectors.resources;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.unix.DomainSocketChannel;
import lombok.Getter;

@Getter
public abstract class ThreadPoolResource {
  protected EventLoopGroup eventLoopGroup;
  protected Class<? extends DomainSocketChannel> domainSocketChannelClass;

  public ThreadPoolResource(
      EventLoopGroup eventLoopGroup,
      Class<? extends DomainSocketChannel> domainSocketChannelClass) {
    this.eventLoopGroup = eventLoopGroup;
    this.domainSocketChannelClass = domainSocketChannelClass;
  }
}
