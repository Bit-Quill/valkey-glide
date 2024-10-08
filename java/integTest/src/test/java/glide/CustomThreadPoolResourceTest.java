/** Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import static glide.TestUtilities.commonClientConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

import glide.api.GlideClient;
import glide.connectors.resources.EpollResource;
import glide.connectors.resources.KQueuePoolResource;
import glide.connectors.resources.Platform;
import glide.connectors.resources.ThreadPoolResource;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class CustomThreadPoolResourceTest {
    @Test
    @SneakyThrows
    public void standalone_client_with_custom_threadPoolResource() {
        ThreadPoolResource customThreadPoolResource;
        int numOfThreads = 8;

        if (Platform.getCapabilities().isKQueueAvailable()) {
            customThreadPoolResource = new KQueuePoolResource(new KQueueEventLoopGroup(numOfThreads));
        } else if (Platform.getCapabilities().isEPollAvailable()) {
            customThreadPoolResource = new EpollResource(new EpollEventLoopGroup(numOfThreads));
        } else {
            throw new RuntimeException("Current platform supports no known thread pool resources");
        }

        var regularClient =
                GlideClient.createClient(
                                commonClientConfig().threadPoolResource(customThreadPoolResource).build())
                        .get(10, TimeUnit.SECONDS);

        String payload = (String) regularClient.customCommand(new String[] {"PING"}).get();
        assertEquals("PONG", payload);

        regularClient.close();
        customThreadPoolResource.getEventLoopGroup().shutdownGracefully();
    }
}
