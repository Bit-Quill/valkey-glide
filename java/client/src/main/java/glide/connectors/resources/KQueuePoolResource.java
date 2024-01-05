package glide.connectors.resources;

import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

public class KQueuePoolResource extends ThreadPoolResource {
  private static final String KQUEUE_EVENT_LOOP_IDENTIFIER = "glide-channel" + "-kqueue-elg";

  public KQueuePoolResource() {
    super(
        new KQueueEventLoopGroup(
            Runtime.getRuntime().availableProcessors(),
            new DefaultThreadFactory(KQUEUE_EVENT_LOOP_IDENTIFIER, true)),
        KQueueDomainSocketChannel.class);
  }

  public KQueuePoolResource(KQueueEventLoopGroup eventLoopGroup) {
    super(eventLoopGroup, KQueueDomainSocketChannel.class);
  }
}
