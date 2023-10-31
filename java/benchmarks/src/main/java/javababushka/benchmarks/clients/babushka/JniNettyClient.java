package javababushka.benchmarks.clients.babushka;

import static connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import static connection_request.ConnectionRequestOuterClass.AddressInfo;
import static connection_request.ConnectionRequestOuterClass.ReadFromReplicaStrategy;
import static connection_request.ConnectionRequestOuterClass.ConnectionRetryStrategy;
import static connection_request.ConnectionRequestOuterClass.AuthenticationInfo;
import static connection_request.ConnectionRequestOuterClass.TlsMode;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.SimpleUserEventChannelHandler;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.UnixChannel;
import javababushka.benchmarks.clients.SyncClient;
import javababushka.benchmarks.utils.ConnectionSettings;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import javababushka.client.RedisClient;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class JniNettyClient implements SyncClient {

  private final static String unixSocket = getSocket();

  private Channel channel = null;

  // TODO static or move to constructor?
  private static String getSocket() {
    try {
      return RedisClient.startSocketListenerExternal();
    } catch (Exception | UnsatisfiedLinkError e) {
      System.err.printf("Failed to get UDS from babushka and dedushka: %s%n%n", e);
      return null;
    }
  }

  @Override
  public void connectToRedis() {
    connectToRedis(new ConnectionSettings("localhost", 6379, false));
  }

  @Override
  public void connectToRedis(ConnectionSettings connectionSettings) {

    // TODO maybe move to constructor or to static?
    // ======
    Bootstrap bootstrap = new Bootstrap();
    EventLoopGroup group = new EpollEventLoopGroup();
    //EventLoopGroup group = new NioEventLoopGroup();
    try {
      bootstrap
          .group(group)
          .channel(EpollDomainSocketChannel.class)
          .handler(new ChannelInitializer<UnixChannel>() {
            @Override
            public void initChannel(UnixChannel ch) throws Exception {
              ch
                  .pipeline()
                      // TODO encoder/decoder
                  .addLast(new ChannelInboundHandlerAdapter())
                  .addLast(new ChannelOutboundHandlerAdapter() {
                    @Override
                    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
                      System.out.printf("=== bind %s %s %s %n", ctx, localAddress, promise);
                      super.bind(ctx, localAddress, promise);
                    }

                    @Override
                    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
                      System.out.printf("=== connect %s %s %s %s %n", ctx, remoteAddress, localAddress, promise);
                      super.connect(ctx, remoteAddress, localAddress, promise);
                    }

                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                      System.out.printf("=== write %s %s %s %n", ctx, msg, promise);
                      //var arr = (byte[])msg;
                      //new ByteBuf().writeBytes(arr, 0, arr.length);

                      super.write(ctx, Unpooled.copiedBuffer((byte[])msg), promise);
                    }

                    @Override
                    public void flush(ChannelHandlerContext ctx) throws Exception {
                      System.out.printf("=== flush %s %n", ctx);
                      super.flush(ctx);
                    }
                  });
                    /*
                  .addLast(new SimpleUserEventChannelHandler<String>() {
                    @Override
                    protected void eventReceived(ChannelHandlerContext ctx, String evt) throws Exception {

                    }
                  });
                  */
                      //.addLast(new CombinedChannelDuplexHandler(new ChannelInboundHandler(), new ChannelOutboundHandler()));
            }
          });
      channel = bootstrap.connect(new DomainSocketAddress(unixSocket)).sync().channel();


      //channel.writeAndFlush(request);

      //channel.closeFuture().sync();
    }
    catch (Exception e) {
      int a = 5;
    } finally {
        //epollEventLoopGroup.shutdownGracefully();
    }
    // ======

    var request = ConnectionRequest.newBuilder()
        .addAddresses(
            AddressInfo.newBuilder()
                .setHost(connectionSettings.host)
                .setPort(connectionSettings.port)
                .build())
        .setTlsMode(connectionSettings.useSsl // TODO: secure or insecure TLS?
            ? TlsMode.SecureTls
            : TlsMode.NoTls)
        .setClusterModeEnabled(false)
        // In millis
        .setResponseTimeout(250)
        // In millis
        .setClientCreationTimeout(2500)
        .setReadFromReplicaStrategy(ReadFromReplicaStrategy.AlwaysFromPrimary)
        .setConnectionRetryStrategy(
            ConnectionRetryStrategy.newBuilder()
                .setNumberOfRetries(1)
                .setFactor(1)
                .setExponentBase(1)
                .build())
        .setAuthenticationInfo(
            AuthenticationInfo.newBuilder()
                .setPassword("")
                .setUsername("default")
                .build())
        .setDatabaseId(0)
        .build();

    var bytes = request.toByteArray();
    var varint = getVarInt(bytes.length);

    ByteBuffer buffer = ByteBuffer.allocate(bytes.length + varint.length);
    buffer.clear();
    for (Byte b : varint) {
      buffer.put(b);
    }
    buffer.put(bytes);
    buffer.flip();

    channel.writeAndFlush(buffer.array());
    //channel.read();
  }

  private static Byte[] getVarInt(int value) {
    List<Byte> output = new ArrayList<>();
    int bits = value & 0x7F;
    value >>= 7;
    while (value > 0) {
      output.add((byte) (0x80 | bits));
      bits = value & 0x7F;
      value >>= 7;
    }
    output.add((byte) bits);
    Byte[] arr = new Byte[] {};
    return output.toArray(arr);
  }

  @Override
  public String getName() {
    return "JNI Netty";
  }

  @Override
  public void set(String key, String value) {

  }

  @Override
  public String get(String key) {
    return null;
  }

  public static void main(String[] args) {
    var client = new JniNettyClient();
    client.connectToRedis();
  }
}
