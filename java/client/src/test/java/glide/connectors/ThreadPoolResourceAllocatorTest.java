package glide.connectors;

import static org.junit.jupiter.api.Assertions.*;

import glide.connectors.resources.*;
import org.junit.jupiter.api.Test;

public class ThreadPoolResourceAllocatorTest {

  @Test
  public void CreateThreadPoolResource() {
    ThreadPoolResource threadPoolResource =
        ThreadPoolResourceAllocator.createOrGetThreadPoolResource();
    assertTrue(
        threadPoolResource instanceof KQueuePoolResource
            || threadPoolResource instanceof EpollResource);
  }

  @Test
  public void GetThreadPoolResource() {
    ThreadPoolResource threadPoolResource1 =
        ThreadPoolResourceAllocator.createOrGetThreadPoolResource();
    assertTrue(
        threadPoolResource1 instanceof KQueuePoolResource
            || threadPoolResource1 instanceof EpollResource);
    ThreadPoolResource threadPoolResource2 =
        ThreadPoolResourceAllocator.createOrGetThreadPoolResource();
    assertEquals(threadPoolResource1, threadPoolResource2);
  }
}
