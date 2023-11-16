package javababushka.benchmarks.clients.babushka;

import static connection_request.ConnectionRequestOuterClass.AddressInfo;
import static connection_request.ConnectionRequestOuterClass.AuthenticationInfo;
import static connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import static connection_request.ConnectionRequestOuterClass.ConnectionRetryStrategy;
import static connection_request.ConnectionRequestOuterClass.ReadFromReplicaStrategy;
import static connection_request.ConnectionRequestOuterClass.TlsMode;
import static redis_request.RedisRequestOuterClass.Command;
import static redis_request.RedisRequestOuterClass.Command.ArgsArray;
import static redis_request.RedisRequestOuterClass.RedisRequest;
import static redis_request.RedisRequestOuterClass.RequestType;
import static redis_request.RedisRequestOuterClass.Routes;
import static redis_request.RedisRequestOuterClass.SimpleRoutes;
import static response.ResponseOuterClass.Response;

import com.google.common.annotations.VisibleForTesting;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.UnixChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javababushka.benchmarks.clients.AsyncClient;
import javababushka.benchmarks.clients.SyncClient;
import javababushka.benchmarks.utils.ConnectionSettings;
import javababushka.client.RedisClient;
import org.apache.commons.lang3.tuple.Pair;

@VisibleForTesting
public class JniNettyClient implements SyncClient, AsyncClient<Response>, AutoCloseable {

  public static boolean ALWAYS_FLUSH_ON_WRITE = false;

  // https://netty.io/3.6/api/org/jboss/netty/handler/queue/BufferedWriteHandler.html
  // Flush every N bytes if !ALWAYS_FLUSH_ON_WRITE
  public static int AUTO_FLUSH_THRESHOLD_BYTES = 512; // 1024;
  private final AtomicInteger nonFlushedBytesCounter = new AtomicInteger(0);

  // Flush every N writes if !ALWAYS_FLUSH_ON_WRITE
  public static int AUTO_FLUSH_THRESHOLD_WRITES = 10;
  private final AtomicInteger nonFlushedWritesCounter = new AtomicInteger(0);

  // If !ALWAYS_FLUSH_ON_WRITE and a command has no response in N millis, flush (probably it wasn't
  // send)
  public static int AUTO_FLUSH_RESPONSE_TIMEOUT_MILLIS = 100;
  // If !ALWAYS_FLUSH_ON_WRITE flush on timer (like a cron)
  public static int AUTO_FLUSH_TIMER_MILLIS = 200;

  public static int PENDING_RESPONSES_ON_CLOSE_TIMEOUT_MILLIS = 1000;

  // Futures to handle responses. Index is callback id, starting from 1 (0 index is for connection
  // request always).
  // Is it not a concurrent nor sync collection, but it is synced on adding. No removes.
  // TODO clean up completed futures
  private final List<CompletableFuture<Response>> responses = new ArrayList<>();
  // Unique offset for every client to avoid having multiple commands with the same id at a time.
  // For debugging replace with: new Random().nextInt(1000) * 1000
  private final int callbackOffset = new Random().nextInt();

  private final String unixSocket = getSocket();

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

  // We support MacOS and Linux only, because Babushka does not support Windows, because tokio does
  // not support it.
  // Probably we should use NIO (NioEventLoopGroup) for Windows.
  private static final boolean isMacOs = isMacOs();

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
    waitForResult(asyncConnectToRedis(connectionSettings));
  }

  private void createChannel() {
    // TODO maybe move to constructor or to static?
    try {
      channel =
          new Bootstrap()
              .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024, 4096))
              .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
              .group(group = isMacOs ? new KQueueEventLoopGroup() : new EpollEventLoopGroup())
              .channel(isMacOs ? KQueueDomainSocketChannel.class : EpollDomainSocketChannel.class)
              .handler(
                  new ChannelInitializer<UnixChannel>() {
                    @Override
                    public void initChannel(UnixChannel ch) throws Exception {
                      ch.pipeline()
                          .addLast("logger", new LoggingHandler(LogLevel.DEBUG))
                          // https://netty.io/4.1/api/io/netty/handler/codec/protobuf/ProtobufEncoder.html
                          .addLast("protobufDecoder", new ProtobufVarint32FrameDecoder())
                          .addLast("protobufEncoder", new ProtobufVarint32LengthFieldPrepender())
                          .addLast(
                              new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg)
                                    throws Exception {
                                  // System.out.printf("=== channelRead %s %s %n", ctx, msg);
                                  var buf = (ByteBuf) msg;
                                  var bytes = new byte[buf.readableBytes()];
                                  buf.readBytes(bytes);
                                  // TODO surround parsing with try-catch, set error to future if
                                  // parsing failed.
                                  var response = Response.parseFrom(bytes);
                                  int callbackId = response.getCallbackIdx();
                                  if (callbackId != 0) {
                                    // connection request has hardcoded callback id = 0
                                    // https://github.com/aws/babushka/issues/600
                                    callbackId -= callbackOffset;
                                  }
                                  // System.out.printf("== Received response with callback %d%n",
                                  // response.getCallbackIdx());
                                  responses.get(callbackId).complete(response);
                                  responses.set(callbackId, null);
                                  super.channelRead(ctx, bytes);
                                }

                                @Override
                                public void exceptionCaught(
                                    ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                  System.out.printf("=== exceptionCaught %s %s %n", ctx, cause);
                                  cause.printStackTrace();
                                  super.exceptionCaught(ctx, cause);
                                }
                              })
                          .addLast(
                              new ChannelOutboundHandlerAdapter() {
                                @Override
                                public void write(
                                    ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
                                    throws Exception {
                                  // System.out.printf("=== write %s %s %s %n", ctx, msg, promise);
                                  var bytes = (byte[]) msg;

                                  boolean needFlush = false;
                                  if (!ALWAYS_FLUSH_ON_WRITE) {
                                    synchronized (nonFlushedBytesCounter) {
                                      if (nonFlushedBytesCounter.addAndGet(bytes.length)
                                              >= AUTO_FLUSH_THRESHOLD_BYTES
                                          || nonFlushedWritesCounter.incrementAndGet()
                                              >= AUTO_FLUSH_THRESHOLD_WRITES) {
                                        nonFlushedBytesCounter.set(0);
                                        nonFlushedWritesCounter.set(0);
                                        needFlush = true;
                                      }
                                    }
                                  }
                                  super.write(ctx, Unpooled.copiedBuffer(bytes), promise);
                                  if (needFlush) {
                                    // flush outside the sync block
                                    flush(ctx);
                                    // System.out.println("-- auto flush - buffer");
                                  }
                                }
                              });
                    }
                  })
              .connect(new DomainSocketAddress(unixSocket))
              .sync()
              .channel();

    } catch (Exception e) {
      System.err.printf(
          "Failed to create a channel %s: %s%n", e.getClass().getSimpleName(), e.getMessage());
      e.printStackTrace(System.err);
    }

    if (!ALWAYS_FLUSH_ON_WRITE) {
      new Timer(true)
          .scheduleAtFixedRate(
              new TimerTask() {
                @Override
                public void run() {
                  channel.flush();
                  nonFlushedBytesCounter.set(0);
                  nonFlushedWritesCounter.set(0);
                }
              },
              0,
              AUTO_FLUSH_TIMER_MILLIS);
    }
  }

  @Override
  public void closeConnection() {
    try {
      channel.flush();

      long waitStarted = System.nanoTime();
      long waitUntil =
          waitStarted + PENDING_RESPONSES_ON_CLOSE_TIMEOUT_MILLIS * 100_000; // in nanos
      for (var future : responses) {
        if (future == null || future.isDone()) {
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
    } finally {
      var shuttingDown = group.shutdownGracefully();
      try {
        shuttingDown.get();
      } catch (InterruptedException | ExecutionException exception) {
        exception.printStackTrace();
      }
      assert group.isShutdown(): "Redis connection failed to shutdown gracefully";
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
    // TODO support non-strings
  }

  private synchronized Pair<Integer, CompletableFuture<Response>> getNextCallback() {
    var future = new CompletableFuture<Response>();
    responses.add(future);
    return Pair.of(responses.size() - 1, future);
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

    var request =
        ConnectionRequest.newBuilder()
            .addAddresses(
                AddressInfo.newBuilder()
                    .setHost(connectionSettings.host)
                    .setPort(connectionSettings.port)
                    .build())
            .setTlsMode(
                connectionSettings.useSsl // TODO: secure or insecure TLS?
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
                AuthenticationInfo.newBuilder().setPassword("").setUsername("default").build())
            .setDatabaseId(0)
            .build();

    var future = new CompletableFuture<Response>();
    responses.add(future);
    channel.writeAndFlush(request.toByteArray());
    return future;
  }

  private CompletableFuture<Response> submitNewCommand(RequestType command, List<String> args) {
    var commandId = getNextCallback();
    // System.out.printf("== %s(%s), callback %d%n", command, String.join(", ", args), commandId);

    return CompletableFuture.supplyAsync(
            () -> {
              var commandArgs = ArgsArray.newBuilder();
              for (var arg : args) {
                commandArgs.addArgs(arg);
              }

              RedisRequest request =
                  RedisRequest.newBuilder()
                      .setCallbackIdx(commandId.getKey() + callbackOffset)
                      .setSingleCommand(
                          Command.newBuilder()
                              .setRequestType(command)
                              .setArgsArray(commandArgs.build())
                              .build())
                      .setRoute(Routes.newBuilder().setSimpleRoutes(SimpleRoutes.AllNodes).build())
                      .build();
              if (ALWAYS_FLUSH_ON_WRITE) {
                channel.writeAndFlush(request.toByteArray());
                return commandId.getRight();
              }
              channel.write(request.toByteArray());
              return autoFlushFutureWrapper(commandId.getRight());
            })
        .thenCompose(f -> f);
  }

  private <T> CompletableFuture<T> autoFlushFutureWrapper(Future<T> future) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return future.get(AUTO_FLUSH_RESPONSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          } catch (TimeoutException e) {
            // System.out.println("-- auto flush - timeout");
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
    // System.out.printf("== set(%s, %s), callback %d%n", key, value, callbackId);
    return submitNewCommand(RequestType.SetString, List.of(key, value));
  }

  @Override
  public Future<String> asyncGet(String key) {
    // System.out.printf("== get(%s), callback %d%n", key, callbackId);
    return submitNewCommand(RequestType.GetString, List.of(key))
        .thenApply(
            response ->
                response.hasRespPointer()
                    ? RedisClient.valueFromPointer(response.getRespPointer()).toString()
                    : null);
  }
}
