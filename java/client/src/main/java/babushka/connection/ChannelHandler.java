package babushka.connection;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.unix.UnixChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.NonNull;

public class ChannelHandler extends ChannelInitializer<UnixChannel> {
  @Override
  public void initChannel(@NonNull UnixChannel ch) throws Exception {
    ch.pipeline()
        // https://netty.io/4.1/api/io/netty/handler/codec/protobuf/ProtobufEncoder.html
        .addLast("protobufDecoder", new ProtobufVarint32FrameDecoder())
        .addLast("protobufEncoder", new ProtobufVarint32LengthFieldPrepender())
        .addLast(new ReadHandler())
        .addLast(new WriteHandler());
  }
}
