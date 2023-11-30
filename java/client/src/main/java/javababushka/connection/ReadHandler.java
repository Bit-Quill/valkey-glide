package javababushka.connection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.NonNull;
import response.ResponseOuterClass;

public class ReadHandler extends ChannelInboundHandlerAdapter {
  @Override
  public void channelRead(@NonNull ChannelHandlerContext ctx, @NonNull Object msg)
      throws Exception {
    // System.out.printf("=== channelRead %s %s %n", ctx, msg);
    var buf = (ByteBuf) msg;
    var bytes = new byte[buf.readableBytes()];
    buf.readBytes(bytes);
    // TODO surround parsing with try-catch, set error to future if
    // parsing failed.
    var response = ResponseOuterClass.Response.parseFrom(bytes);
    int callbackId = response.getCallbackIdx();
    // System.out.printf("== Received response with callback %d%n",
    if (callbackId == 0) {
      // can't distinguish connection requests since they have no
      // callback ID
      // https://github.com/aws/babushka/issues/600
      CommonResources.connectionRequests.pop().complete(response);
    } else {
      CommonResources.responses.get(callbackId).complete(response);
      CommonResources.responses.remove(callbackId);
    }
    buf.release();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    System.out.printf("=== exceptionCaught %s %s %n", ctx, cause);
    cause.printStackTrace(System.err);
    super.exceptionCaught(ctx, cause);
  }
}
