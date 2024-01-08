package glide.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import glide.connectors.handlers.CallbackDispatcher;
import glide.connectors.handlers.ReadHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import response.ResponseOuterClass;

public class ReadHandlerTest {

  EmbeddedChannel embeddedChannel;
  ReadHandler readHandler;
  CallbackDispatcher dispatcher;

  @BeforeEach
  public void init() {
    dispatcher = mock(CallbackDispatcher.class);
    readHandler = new ReadHandler(dispatcher);
    embeddedChannel = new EmbeddedChannel(readHandler);
  }

  @AfterEach
  public void teardown() {
    embeddedChannel.finishAndReleaseAll();
  }

  @Test
  public void readHandlerRead_testInboundProtobufMessages() {
    ResponseOuterClass.Response msg = ResponseOuterClass.Response.newBuilder()
        .setConstantResponse(ResponseOuterClass.ConstantResponse.OK)
        .build();

    assertTrue(embeddedChannel.writeInbound(msg, msg, msg));
    assertTrue(embeddedChannel.finish());

    verify(dispatcher, times(3)).completeRequest(msg);
  }

  @Test
  public void readHandlerRead_testInboundProtobufMessages_invalidMessage() {

    String invalidMsg = "Invalid";

    Exception e = assertThrows(Exception.class, () -> embeddedChannel.writeInbound(invalidMsg, invalidMsg, invalidMsg));
    assertEquals("Unexpected message in socket", e.getMessage());

    verify(dispatcher, times(0)).completeRequest(any());

    ResponseOuterClass.Response msg = ResponseOuterClass.Response.newBuilder()
        .setConstantResponse(ResponseOuterClass.ConstantResponse.OK)
        .build();
    assertTrue(embeddedChannel.writeInbound(msg));
    assertTrue(embeddedChannel.finish());

    verify(dispatcher, times(1)).completeRequest(msg);
  }


}
