package javababushka.connection;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class WriteHandler extends ChannelOutboundHandlerAdapter {
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    // System.out.printf("=== write %s %s %s %n", ctx, msg, promise);
    var bytes = (byte[]) msg;

    super.write(ctx, Unpooled.copiedBuffer(bytes), promise);
  }
}
