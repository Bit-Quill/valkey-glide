package babushka.connection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.NonNull;
import response.ResponseOuterClass.Response;

/** Handler for inbound traffic though UDS. Used by Netty. */
public class ReadHandler extends ChannelInboundHandlerAdapter {
  /**
   * Handles responses from babushka core:
   *
   * <ol>
   *   <li>Copy to a buffer;
   *   <li>Parse protobuf packet;
   *   <li>Find and resolve a corresponding future;
   * </ol>
   */
  @Override
  public void channelRead(@NonNull ChannelHandlerContext ctx, @NonNull Object msg)
      throws Exception {
    var buf = (ByteBuf) msg;
    var bytes = new byte[buf.readableBytes()];
    buf.readBytes(bytes);
    // TODO surround parsing with try-catch, set error to future if parsing failed.
    var response = Response.parseFrom(bytes);
    int callbackId = response.getCallbackIdx();
    if (callbackId == 0) {
      // can't distinguish connection requests since they have no
      // callback ID
      // https://github.com/aws/babushka/issues/600
      SocketManagerResources.connectionRequests.pop().complete(response);
    } else {
      SocketManagerResources.responses.get(callbackId).complete(response);
      SocketManagerResources.responses.remove(callbackId);
    }
    buf.release();
  }

  /** Handles uncaught exceptions from {@link #channelRead(ChannelHandlerContext, Object)}. */
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    System.out.printf("=== exceptionCaught %s %s %n", ctx, cause);
    cause.printStackTrace(System.err);
    super.exceptionCaught(ctx, cause);
  }
}
