package glide.connectors.resources;

import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Implementation of ThreadPoolResource for Epoll-based systems. Enabling custom/default
 * configurations.
 */
public class EpollResource extends ThreadPoolResource {
  private static final String EPOLL_EVENT_LOOP_IDENTIFIER = "glide-channel-epoll-elg";

  public EpollResource() {
    super(
        new EpollEventLoopGroup(
            Runtime.getRuntime().availableProcessors(),
            new DefaultThreadFactory(EPOLL_EVENT_LOOP_IDENTIFIER, true)),
        EpollDomainSocketChannel.class);
  }

  public EpollResource(EpollEventLoopGroup epollEventLoopGroup) {
    super(epollEventLoopGroup, EpollDomainSocketChannel.class);
  }
}
