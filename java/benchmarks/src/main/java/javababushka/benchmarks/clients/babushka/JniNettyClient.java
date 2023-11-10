package javababushka.benchmarks.clients.babushka;

import static connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import static connection_request.ConnectionRequestOuterClass.AddressInfo;
import static connection_request.ConnectionRequestOuterClass.ReadFromReplicaStrategy;
import static connection_request.ConnectionRequestOuterClass.ConnectionRetryStrategy;
import static connection_request.ConnectionRequestOuterClass.AuthenticationInfo;
import static connection_request.ConnectionRequestOuterClass.TlsMode;
import static response.ResponseOuterClass.Response;
import static redis_request.RedisRequestOuterClass.Command.ArgsArray;
import static redis_request.RedisRequestOuterClass.Command;
import static redis_request.RedisRequestOuterClass.RequestType;
import static redis_request.RedisRequestOuterClass.RedisRequest;
import static redis_request.RedisRequestOuterClass.SimpleRoutes;
import static redis_request.RedisRequestOuterClass.Routes;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.AbstractMessageLite;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.UnixChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import javababushka.benchmarks.clients.AsyncClient;
import javababushka.benchmarks.clients.SyncClient;
import javababushka.benchmarks.utils.ConnectionSettings;
import io.netty.channel.unix.DomainSocketAddress;
import javababushka.client.RedisClient;
import lombok.SneakyThrows;

import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@VisibleForTesting
public class JniNettyClient implements SyncClient, AsyncClient<Response>, AutoCloseable {

  public static boolean ALWAYS_FLUSH_ON_WRITE = false;

  // https://netty.io/3.6/api/org/jboss/netty/handler/queue/BufferedWriteHandler.html
  // Flush every N bytes if !ALWAYS_FLUSH_ON_WRITE
  public static int AUTO_FLUSH_THRESHOLD_BYTES = 512;//1024;
  private final AtomicInteger nonFlushedBytesCounter = new AtomicInteger(0);

  // Flush every N writes if !ALWAYS_FLUSH_ON_WRITE
  public static int AUTO_FLUSH_THRESHOLD_WRITES = 10;
  private final AtomicInteger nonFlushedWritesCounter = new AtomicInteger(0);

  // If !ALWAYS_FLUSH_ON_WRITE and a command has no response in N millis, flush (probably it wasn't send)
  public static int AUTO_FLUSH_RESPONSE_TIMEOUT_MILLIS = 100;
  // If !ALWAYS_FLUSH_ON_WRITE flush on timer (like a cron)
  public static int AUTO_FLUSH_TIMER_MILLIS = 200;

  public static int PENDING_RESPONSES_ON_CLOSE_TIMEOUT_MILLIS = 1000;

  public final AtomicLong READ_TIME = new AtomicLong(0);
  public final AtomicLong READ_COUNT = new AtomicLong(0);
  public final AtomicLong WRITE_TIME = new AtomicLong(0);
  public final AtomicLong WRITE_TIME_INNER = new AtomicLong(0);
  public final AtomicLong WRITE_COUNT = new AtomicLong(0);

  public final AtomicLong FROM_SUBMIT_WRITE_TO_HANDLER_WRITE_TIME = new AtomicLong(0);
  public final AtomicLong BUFFER_ON_WRITE_TIME = new AtomicLong(0);
  public final AtomicLong BUFFER_ON_READ_TIME = new AtomicLong(0);
  public final AtomicLong PARSING_ON_READ_TIME = new AtomicLong(0);
  public final AtomicLong SERIALIZATION_ON_WRITE_TIME = new AtomicLong(0);
  public final AtomicLong SET_FUTURE_ON_READ_TIME1 = new AtomicLong(0);
  public final AtomicLong SET_FUTURE_ON_READ_TIME2 = new AtomicLong(0);
  public final AtomicLong GET_FUTURE_RESULT_AFTER_SET = new AtomicLong(0);

  public final AtomicLong BUILD_COMMAND_ON_WRITE_TIME = new AtomicLong(0);
  public final AtomicLong WRITE_ON_WRITE_TIME = new AtomicLong(0);

  public final AtomicLong WAIT_FOR_RESULT = new AtomicLong(0);
  public final List<Long> REQUEST_TIMESTAMPS = Collections.synchronizedList(new ArrayList<>());
  public final List<Long> RESPONSE_TIMESTAMPS = Collections.synchronizedList(new ArrayList<>());


  private void resetCounters() {
    READ_TIME.set(0);
    READ_COUNT.set(0);
    WRITE_TIME.set(0);
    WRITE_TIME_INNER.set(0);
    WRITE_COUNT.set(0);
    FROM_SUBMIT_WRITE_TO_HANDLER_WRITE_TIME.set(0);
    BUFFER_ON_WRITE_TIME.set(0);
    BUFFER_ON_READ_TIME.set(0);
    PARSING_ON_READ_TIME.set(0);
    SERIALIZATION_ON_WRITE_TIME.set(0);
    SET_FUTURE_ON_READ_TIME1.set(0);
    SET_FUTURE_ON_READ_TIME2.set(0);
    BUILD_COMMAND_ON_WRITE_TIME.set(0);
    WRITE_ON_WRITE_TIME.set(0);
    WAIT_FOR_RESULT.set(0);
    GET_FUTURE_RESULT_AFTER_SET.set(0);
    REQUEST_TIMESTAMPS.clear();
    RESPONSE_TIMESTAMPS.clear();
  }

  // Futures to handle responses. Index is callback id, starting from 1 (0 index is for connection request always).
  // TODO clean up completed futures
  // TODO avoid same numbers for multiple clients
  private final List<CompletableFuture<Response>> responses = Collections.synchronizedList(new ArrayList<>());

  private static final String unixSocket = getSocket();

  // TODO static or move to constructor?
  private static String getSocket() {
    try {
      return RedisClient.startSocketListenerExternal();
    } catch (Exception | UnsatisfiedLinkError e) {
      System.err.printf("Failed to get UDS from babushka and dedushka: %s%n%n", e);
      throw new RuntimeException(e);
    }
  }

  private Channel channel = null;
  private EventLoopGroup group = null;

  // We support MacOS and Linux only, because Babushka does not support Windows, because tokio does not support it.
  // Probably we should use NIO (NioEventLoopGroup) for Windows.
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
    // TODO fix: netty still doesn't use slf4j nor log4j
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
      //System.out.printf("Connection %s%n", connected != null ? connected.getConstantResponse() : null);
    } catch (Exception e) {
      System.err.println("Connection time out");
    }

    resetCounters();
  }

  private void createChannel() {
    // TODO maybe move to constructor or to static?
    // ======
    try {
      channel = new Bootstrap()
//          .option(ChannelOption.WRITE_BUFFER_WATER_MARK,
//              new WriteBufferWaterMark(1024, 4096))
//          .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
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
            long readBefore = System.nanoTime();

                      var buf = (ByteBuf) msg;
                      var bytes = new byte[buf.readableBytes()];
            long bufferBefore = System.nanoTime();
                      buf.readBytes(bytes);
            BUFFER_ON_READ_TIME.addAndGet(System.nanoTime() - bufferBefore);
                      // TODO surround parsing with try-catch
            long parseBefore = System.nanoTime();
                      var response = Response.parseFrom(bytes);
            PARSING_ON_READ_TIME.addAndGet(System.nanoTime() - parseBefore);
            if (response.getCallbackIdx() > 0) {
              System.out.printf("%s     received callback id %d%n",
                  LocalDateTime.now().minusNanos(System.nanoTime() - readBefore), response.getCallbackIdx());
            }
                      //System.out.printf("== Received response with callback %d%n", response.getCallbackIdx());
            long futureBefore1 = System.nanoTime();
                      var future = responses.get(response.getCallbackIdx());
            SET_FUTURE_ON_READ_TIME1.addAndGet(System.nanoTime() - futureBefore1);
            long futureBefore2 = System.nanoTime();
                      future.complete(response);
            long futureAfter2 = System.nanoTime();
            SET_FUTURE_ON_READ_TIME2.addAndGet(futureAfter2 - futureBefore2);
                      buf.release();
            READ_TIME.addAndGet(System.nanoTime() - readBefore);
            READ_COUNT.incrementAndGet();
            RESPONSE_TIMESTAMPS.add(readBefore);
            GET_FUTURE_RESULT_AFTER_SET.addAndGet(-futureAfter2);
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
            long writeBefore = System.nanoTime();
                      byte[] bytes = ((AbstractMessageLite)msg).toByteArray();
            SERIALIZATION_ON_WRITE_TIME.addAndGet(System.nanoTime() - writeBefore);

                      boolean needFlush = false;
                      if (!ALWAYS_FLUSH_ON_WRITE) {
                        synchronized (nonFlushedBytesCounter) {
                          if (nonFlushedBytesCounter.addAndGet(bytes.length) >= AUTO_FLUSH_THRESHOLD_BYTES
                            || nonFlushedWritesCounter.incrementAndGet() >= AUTO_FLUSH_THRESHOLD_WRITES) {
                            nonFlushedBytesCounter.set(0);
                            nonFlushedWritesCounter.set(0);
                            needFlush = true;
                          }
                        }
                      }
            long bufferBefore = System.nanoTime();
                      var buffer = Unpooled.copiedBuffer(bytes);
            BUFFER_ON_WRITE_TIME.addAndGet(System.nanoTime() - bufferBefore);
            if (msg instanceof RedisRequest) {
              System.out.printf("%s      sending callback id %d%n", LocalDateTime.now(), ((RedisRequest)msg).getCallbackIdx());
            }
            long innerWriteBefore = System.nanoTime();
                      super.write(ctx, buffer, promise);
            long innerWriteAfter = System.nanoTime();
            WRITE_TIME_INNER.addAndGet(innerWriteAfter - innerWriteBefore);
                      if (needFlush) {
                        // flush outside the sync block
                        flush(ctx);
                        //System.out.println("-- auto flush - buffer");
                      }
                      //buffer.release();
            WRITE_TIME.addAndGet(System.nanoTime() - writeBefore);
            WRITE_COUNT.incrementAndGet();
            REQUEST_TIMESTAMPS.add(innerWriteAfter);
            FROM_SUBMIT_WRITE_TO_HANDLER_WRITE_TIME.addAndGet(writeBefore);
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

    if (!ALWAYS_FLUSH_ON_WRITE) {
      new Timer(true).scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
          channel.flush();
          nonFlushedBytesCounter.set(0);
          nonFlushedWritesCounter.set(0);
        }
      }, 0, AUTO_FLUSH_TIMER_MILLIS);
    }
  }

  @Override
  public void closeConnection() {
    try {
      channel.flush();

      long waitStarted = System.nanoTime();
      long waitUntil = waitStarted + PENDING_RESPONSES_ON_CLOSE_TIMEOUT_MILLIS * 100_000; // in nanos
      for (var future : responses) {
        if (future.isDone()) {
          continue;
        }
        try {
          future.get(waitUntil - System.nanoTime(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException | ExecutionException ignored) {
        } catch (TimeoutException e) {
          future.cancel(true);
          // TODO cancel the rest
          break;
        }
      }

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

  @Override
  public <T> T waitForResult(Future<T> future) {
long before = System.nanoTime();
    var res = AsyncClient.super.waitForResult(future);
long after = System.nanoTime();
WAIT_FOR_RESULT.addAndGet(after - before);
GET_FUTURE_RESULT_AFTER_SET.addAndGet(after);
    return res;
  }

  // TODO use reentrant lock
  // https://www.geeksforgeeks.org/reentrant-lock-java/
  private synchronized int getNextCallbackId() {
    responses.add(new CompletableFuture<>());
    return responses.size() - 1;
  }

  @SneakyThrows
  public static void dumpStats(JniNettyClient client, String operation, long total) {
    System.out.printf("%n==== %s ====%n", operation);
    System.out.printf("-> total:                 %10d%n", total);
    System.out.printf("-> waiting:               %10d%n", client.WAIT_FOR_RESULT.get());
    System.out.printf("-> write: count writes    %10d%n", client.WRITE_COUNT.get());
    System.out.printf("-> write: build command   %10d%n", client.BUILD_COMMAND_ON_WRITE_TIME.get());
    System.out.printf("-> write: submit command  %10d%n", client.WRITE_ON_WRITE_TIME.get());
    System.out.printf("-> write: handling        %10d%n", client.FROM_SUBMIT_WRITE_TO_HANDLER_WRITE_TIME.get());
    System.out.printf("-> write: buffer          %10d%n", client.BUFFER_ON_WRITE_TIME.get());
    System.out.printf("-> write: serialize       %10d%n", client.SERIALIZATION_ON_WRITE_TIME.get());
    System.out.printf("-> write: submit          %10d%n", client.WRITE_TIME_INNER.get());
    System.out.printf("-> write: total netty     %10d%n", client.WRITE_TIME.get());
    System.out.printf("-> read: count reads      %10d%n", client.READ_COUNT.get());
    System.out.printf("-> read: buffer           %10d%n", client.BUFFER_ON_READ_TIME.get());
    System.out.printf("-> read: parse            %10d%n", client.PARSING_ON_READ_TIME.get());
    System.out.printf("-> read: set future res   %10d%n", client.SET_FUTURE_ON_READ_TIME1.get() + client.SET_FUTURE_ON_READ_TIME2.get());
    System.out.printf("-> read: total netty      %10d%n", client.READ_TIME.get());
    System.out.printf("-> read: get future res   %10d%n", client.GET_FUTURE_RESULT_AFTER_SET.get());

    long prev = 0;
    for (int i = 0; i < client.REQUEST_TIMESTAMPS.size(); i++) {
      if (prev > client.REQUEST_TIMESTAMPS.get(i)) {
        System.err.printf("Request timestamps %d <-> %d are reordered%n", i, i - 1);
      }
      prev = client.REQUEST_TIMESTAMPS.get(i);
    }
    prev = 0;
    for (int i = 0; i < client.RESPONSE_TIMESTAMPS.size(); i++) {
      if (prev > client.RESPONSE_TIMESTAMPS.get(i)) {
        System.err.printf("Response timestamps %d <-> %d are reordered%n", i, i - 1);
      }
      prev = client.RESPONSE_TIMESTAMPS.get(i);
    }
    if (client.REQUEST_TIMESTAMPS.size() != client.RESPONSE_TIMESTAMPS.size()) {
      System.err.printf("Req and res data size mismatch: requests %d responses %d%n", client.REQUEST_TIMESTAMPS.size(), client.RESPONSE_TIMESTAMPS.size());
    } else {
      long avg = 0;
      for (int i = 0; i < client.REQUEST_TIMESTAMPS.size(); i++) {
        avg += client.RESPONSE_TIMESTAMPS.get(i) - client.REQUEST_TIMESTAMPS.get(i);
      }
      System.out.printf("-> avg response time      %10d%n", avg / client.REQUEST_TIMESTAMPS.size());
    }
    System.out.printf("========%n");
  }

  @SneakyThrows
  public static void main(String[] args) {
    JniNettyClient.ALWAYS_FLUSH_ON_WRITE = true;
    var key = String.valueOf(ProcessHandle.current().pid());

    var clientSet = new JniNettyClient();
    clientSet.connectToRedis();

    /*
    var get_ne = client.get("sdf");
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
    */
    System.out.printf("%n++++ START OF TEST ++++%n");
    long beforeSet = System.nanoTime();
    for (int i = 0; i < 100; i++) {
      clientSet.set("name", "value");
    }
    long afterSet = System.nanoTime();
    clientSet.closeConnection();

    dumpStats(clientSet, "SET", afterSet - beforeSet);
    System.out.printf("%n++++ END OF TEST ++++%n");

    var clientGetNE = new JniNettyClient();
    clientGetNE.connectToRedis();
    System.out.printf("%n++++ START OF TEST ++++%n");
    long beforeGetNE = System.nanoTime();
    for (int i = 0; i < 100; i++) {
      clientGetNE.get("namevalue");
    }
    long afterGetNE = System.nanoTime();
    clientGetNE.closeConnection();

    dumpStats(clientGetNE, "GET NE", afterGetNE - beforeGetNE);
    System.out.printf("%n++++ END OF TEST ++++%n");

    var clientGetE = new JniNettyClient();
    clientGetE.connectToRedis();
    System.out.printf("%n++++ START OF TEST ++++%n");
    long beforeGetE = System.nanoTime();
    for (int i = 0; i < 100; i++) {
      clientGetE.get(key);
    }
    long afterGetE = System.nanoTime();
    clientGetE.closeConnection();

    dumpStats(clientGetE, "GET E", afterGetE - beforeGetE);
    System.out.printf("%n++++ END OF TEST ++++%n");
    System.out.printf("%n%n%n%n%n");

    dumpStats(clientSet, "SET", afterSet - beforeSet);
    dumpStats(clientGetE, "GET E", afterGetE - beforeGetE);
    dumpStats(clientGetNE, "GET NE", afterGetNE - beforeGetNE);

    if (true)
      return;
    ///////

    long beforeSetA = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
      clientGetE.asyncSet("name", "value");
    }
    long afterSetA = System.nanoTime();
    System.out.printf("++++ set: %d%n", afterSetA - beforeSetA);

    long beforeGetNEA = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
      clientGetE.asyncGet("namevalue");
    }
    long afterGetNEA = System.nanoTime();
    System.out.printf("++++ get NE: %d%n", afterGetNEA - beforeGetNEA);

    long beforeGetEA = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
      clientGetE.asyncGet(key);
    }
    long afterGetEA = System.nanoTime();
    System.out.printf("++++ get E: %d%n", afterGetEA - beforeGetEA);

    clientGetE.closeConnection();
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
    channel.writeAndFlush(request);
    return future;
  }

  private CompletableFuture<Response> submitNewCommand(RequestType command, List<String> args) {
long beforeBuild = System.nanoTime();
    int callbackId = getNextCallbackId();
    //System.out.printf("== %s(%s), callback %d%n", command, String.join(", ", args), callbackId);
    RedisRequest request =
        RedisRequest.newBuilder()
            .setCallbackIdx(callbackId)
            .setSingleCommand(
                Command.newBuilder()
                    .setRequestType(command)
                    .setArgsArray(ArgsArray.newBuilder().addAllArgs(args).build())
                    .build())
            .setRoute(
                Routes.newBuilder()
                    .setSimpleRoutes(SimpleRoutes.AllNodes)
                    .build())
            .build();
BUILD_COMMAND_ON_WRITE_TIME.addAndGet(System.nanoTime() - beforeBuild);
long beforeWrite = System.nanoTime();
    if (ALWAYS_FLUSH_ON_WRITE) {
      channel.writeAndFlush(request);
long afterWrite = System.nanoTime();
WRITE_ON_WRITE_TIME.addAndGet(afterWrite - beforeWrite);
FROM_SUBMIT_WRITE_TO_HANDLER_WRITE_TIME.addAndGet(-afterWrite);
      return responses.get(callbackId);
    }
    channel.write(request);
long afterWrite = System.nanoTime();
WRITE_ON_WRITE_TIME.addAndGet(afterWrite - beforeWrite);
FROM_SUBMIT_WRITE_TO_HANDLER_WRITE_TIME.addAndGet(-afterWrite);
    return autoFlushFutureWrapper(responses.get(callbackId));
  }

  private <T> CompletableFuture<T> autoFlushFutureWrapper(Future<T> future) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return future.get(AUTO_FLUSH_RESPONSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      } catch (TimeoutException e) {
        //System.out.println("-- auto flush - timeout");
        channel.flush();
        nonFlushedBytesCounter.set(0);
        nonFlushedWritesCounter.set(0);
      }
      try {
        return future.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public Future<Response> asyncSet(String key, String value) {
    //System.out.printf("== set(%s, %s), callback %d%n", key, value, callbackId);
    return submitNewCommand(RequestType.SetString, List.of(key, value));
  }

  @Override
  public Future<String> asyncGet(String key) {
    //System.out.printf("== get(%s), callback %d%n", key, callbackId);
    return submitNewCommand(RequestType.GetString, List.of(key))
        .thenApply(response -> response.hasRespPointer()
            ? RedisClient.valueFromPointer(response.getRespPointer()).toString()
            : null);
  }
}
