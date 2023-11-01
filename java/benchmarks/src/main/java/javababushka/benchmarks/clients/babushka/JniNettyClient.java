package javababushka.benchmarks.clients.babushka;

import static connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import static connection_request.ConnectionRequestOuterClass.AddressInfo;
import static connection_request.ConnectionRequestOuterClass.ReadFromReplicaStrategy;
import static connection_request.ConnectionRequestOuterClass.ConnectionRetryStrategy;
import static connection_request.ConnectionRequestOuterClass.AuthenticationInfo;
import static connection_request.ConnectionRequestOuterClass.TlsMode;
import static response.ResponseOuterClass.Response;
import static response.ResponseOuterClass.ConstantResponse;
import static redis_request.RedisRequestOuterClass.Command.ArgsArray;
import static redis_request.RedisRequestOuterClass.Command;
import static redis_request.RedisRequestOuterClass.RequestType;
import static redis_request.RedisRequestOuterClass.RedisRequest;
import static redis_request.RedisRequestOuterClass.SimpleRoutes;
import static redis_request.RedisRequestOuterClass.Routes;

import com.google.protobuf.InvalidProtocolBufferException;
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
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.unix.UnixChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import javababushka.benchmarks.clients.AsyncClient;
import javababushka.benchmarks.clients.SyncClient;
import javababushka.benchmarks.utils.ConnectionSettings;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import javababushka.client.RedisClient;
import response.ResponseOuterClass;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JniNettyClient implements SyncClient, AsyncClient<Response>, AutoCloseable {

  // Futures to handle responses. Index is callback id, starting from 1 (0 index is for connection request always).
  private final List<CompletableFuture<Response>> responses = Collections.synchronizedList(new ArrayList<>());

  private final static String unixSocket = getSocket();

  // TODO static or move to constructor?
  private static String getSocket() {
    try {
      return RedisClient.startSocketListenerExternal();
    } catch (Exception | UnsatisfiedLinkError e) {
      System.err.printf("Failed to get UDS from babushka and dedushka: %s%n%n", e);
      return null;
    }
  }

  private Channel channel = null;
  private EventLoopGroup group = null;

  private final static boolean isMacOs = isMacOs();
  private static boolean isMacOs() {
    try {
      Class.forName("io.netty.channel.kqueue.KQueue");
      return KQueue.isAvailable();
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  static {
    InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
  }

  public JniNettyClient(boolean async) {
    name += async ? " async" : " sync";
  }

  public JniNettyClient() {}

  private String name = "JNI Netty";

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void connectToRedis() {
    connectToRedis(new ConnectionSettings("localhost", 6379, false));
  }

  @Override
  public void connectToRedis(ConnectionSettings connectionSettings) {

    Response connected = null;
    try {
      connected = waitForResult(asyncConnectToRedis(connectionSettings));
      System.out.printf("Connection %s%n", connected != null ? connected.getConstantResponse() : null);
    } catch (Exception e) {
      System.err.println("Connection time out");
    }

    int a = 5;
  }

  private void createChannel() {
    // TODO maybe move to constructor or to static?
    // ======
    //EventLoopGroup group = new NioEventLoopGroup();
    try {
      channel = new Bootstrap()
          .group(group = isMacOs ? new KQueueEventLoopGroup() : new EpollEventLoopGroup())
          .channel(isMacOs ? KQueueDomainSocketChannel.class : EpollDomainSocketChannel.class)
          .handler(new ChannelInitializer<UnixChannel>() {
            @Override
            public void initChannel(UnixChannel ch) throws Exception {
              ch
                  .pipeline()
                  .addLast("logger", new LoggingHandler(LogLevel.DEBUG))
                  //https://netty.io/4.1/api/io/netty/handler/codec/protobuf/ProtobufEncoder.html
                  .addLast("protobufDecoder", new ProtobufVarint32FrameDecoder())
                  .addLast("protobufEncoder", new ProtobufVarint32LengthFieldPrepender())

                  .addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                      //System.out.printf("=== channelRead %s %s %n", ctx, msg);
                      var buf = (ByteBuf) msg;
                      var bytes = new byte[buf.readableBytes()];
                      buf.readBytes(bytes);
                      // TODO surround parsing with try-catch
                      var response = Response.parseFrom(bytes);
                      //System.out.printf("== Received response with callback %d%n", response.getCallbackIdx());
                      responses.get(response.getCallbackIdx()).complete(response);
                      super.channelRead(ctx, bytes);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                      System.out.printf("=== exceptionCaught %s %s %n", ctx, cause);
                      cause.printStackTrace();
                      super.exceptionCaught(ctx, cause);
                    }
                  })
                  .addLast(new ChannelOutboundHandlerAdapter() {
                    @Override
                    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
                      //System.out.printf("=== bind %s %s %s %n", ctx, localAddress, promise);
                      super.bind(ctx, localAddress, promise);
                    }

                    @Override
                    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
                      //System.out.printf("=== connect %s %s %s %s %n", ctx, remoteAddress, localAddress, promise);
                      super.connect(ctx, remoteAddress, localAddress, promise);
                    }

                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                      //System.out.printf("=== write %s %s %s %n", ctx, msg, promise);

                      super.write(ctx, Unpooled.copiedBuffer((byte[])msg), promise);
                    }

                    @Override
                    public void flush(ChannelHandlerContext ctx) throws Exception {
                      //System.out.printf("=== flush %s %n", ctx);
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
          })
      .connect(new DomainSocketAddress(unixSocket)).sync().channel();

    }
    catch (Exception e) {
      System.err.printf("Failed to create a channel %s: %s%n", e.getClass().getSimpleName(), e.getMessage());
      e.printStackTrace(System.err);
    }
  }

  @Override
  public void closeConnection() {
    try {
//      channel.closeFuture().sync();
//   } catch (InterruptedException ignored) {
    } finally {
      group.shutdownGracefully();
    }
  }

  @Override
  public void set(String key, String value) {
    waitForResult(asyncSet(key, value));
    // TODO parse response and rethrow an exception if there is an error
  }

  @Override
  public String get(String key) {
    return waitForResult(asyncGet(key));
    /*
    try {
      var response = responses.get(callbackId).get(DEFAULT_FUTURE_TIMEOUT_SEC, TimeUnit.SECONDS);
      return response.hasRespPointer()
          ? RedisClient.valueFromPointer(response.getRespPointer()).toString()
          : null;
    } catch (Exception e) {
      System.err.printf("Failed to process `get` response, callback = %d: %s %s%n",
          callbackId, e.getClass().getSimpleName(), e.getMessage());
      e.printStackTrace(System.err);
      return null;
    }
    */
  }

  // TODO use reentrant lock
  // https://www.geeksforgeeks.org/reentrant-lock-java/
  private synchronized int getNextCallbackId() {
    responses.add(new CompletableFuture<>());
    return responses.size() - 1;
  }

  public static void main(String[] args) {
    var client = new JniNettyClient();
    client.connectToRedis();

    var get_ne = client.get("sdf");
    var key = String.valueOf(ProcessHandle.current().pid());
    client.set(key, "asfsdf");
    var get_e = client.get(key);

    var get_nea = client.asyncGet("sdf");
    var set_a = client.asyncSet(key, "asfsdf");
    var get_ea = client.asyncGet(key);

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
    var res1 = client.waitForResult(get_nea);
    var res2 = client.waitForResult(set_a);
    var res3 = client.waitForResult(get_ea);

    long beforeSet = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
      client.set("name", "value");
    }
    long afterSet = System.nanoTime();
    System.out.printf("++++ set: %d%n", afterSet - beforeSet);

    long beforeGetNE = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
      client.get("namevalue");
    }
    long afterGetNE = System.nanoTime();
    System.out.printf("++++ get NE: %d%n", afterGetNE - beforeGetNE);

    long beforeGetE = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
      client.get(key);
    }
    long afterGetE = System.nanoTime();
    System.out.printf("++++ get E: %d%n", afterGetE - beforeGetE);

    ///////

    long beforeSetA = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
      client.asyncSet("name", "value");
    }
    long afterSetA = System.nanoTime();
    System.out.printf("++++ set: %d%n", afterSetA - beforeSetA);

    long beforeGetNEA = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
      client.asyncGet("namevalue");
    }
    long afterGetNEA = System.nanoTime();
    System.out.printf("++++ get NE: %d%n", afterGetNEA - beforeGetNEA);

    long beforeGetEA = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
      client.asyncGet(key);
    }
    long afterGetEA = System.nanoTime();
    System.out.printf("++++ get E: %d%n", afterGetEA - beforeGetEA);

    client.closeConnection();
  }

  @Override
  public void close() throws Exception {
    closeConnection();
  }

  @Override
  public Future<Response> asyncConnectToRedis(ConnectionSettings connectionSettings) {
    createChannel();

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

    var future = new CompletableFuture<Response>();
    responses.add(future);
    channel.writeAndFlush(request.toByteArray());
    return future;
  }

  @Override
  public Future<Response> asyncSet(String key, String value) {
    int callbackId = getNextCallbackId();
    //System.out.printf("== set(%s, %s), callback %d%n", key, value, callbackId);
    RedisRequest request =
        RedisRequest.newBuilder()
            .setCallbackIdx(callbackId)
            .setSingleCommand(
                Command.newBuilder()
                    .setRequestType(RequestType.SetString)
                    .setArgsArray(ArgsArray.newBuilder().addArgs(key).addArgs(value).build())
                    .build())
            .setRoute(
                Routes.newBuilder()
                    .setSimpleRoutes(SimpleRoutes.AllNodes)
                    .build())
            .build();
    channel.writeAndFlush(request.toByteArray());
    return responses.get(callbackId);
  }

  @Override
  public Future<String> asyncGet(String key) {
    int callbackId = getNextCallbackId();
    //System.out.printf("== get(%s), callback %d%n", key, callbackId);
    RedisRequest request =
        RedisRequest.newBuilder()
            .setCallbackIdx(callbackId)
            .setSingleCommand(
                Command.newBuilder()
                    .setRequestType(RequestType.GetString)
                    .setArgsArray(ArgsArray.newBuilder().addArgs(key).build())
                    .build())
            .setRoute(
                Routes.newBuilder()
                    .setSimpleRoutes(SimpleRoutes.AllNodes)
                    .build())
            .build();
    channel.writeAndFlush(request.toByteArray());
    return responses.get(callbackId)
        .thenApply(response -> response.hasRespPointer()
            ? RedisClient.valueFromPointer(response.getRespPointer()).toString()
            : null);
  }
}
