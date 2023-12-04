package babushka.connectors.handlers;

import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.unix.UnixChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.NonNull;

/** Builder for the channel used by {@link babushka.connectors.SocketConnection}. */
public class ChannelBuilder extends ChannelInitializer<UnixChannel> {
  @Override
  public void initChannel(@NonNull UnixChannel ch) {
    ch.pipeline()
        // https://netty.io/4.1/api/io/netty/handler/codec/protobuf/ProtobufEncoder.html
        .addLast("protobufDecoder", new ProtobufVarint32FrameDecoder())
        .addLast("protobufEncoder", new ProtobufVarint32LengthFieldPrepender())
        .addLast(new ReadHandler())
        .addLast(new WriteHandler());
  }
}
