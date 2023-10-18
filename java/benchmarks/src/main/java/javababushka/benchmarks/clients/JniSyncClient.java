package javababushka.benchmarks.clients;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import javababushka.benchmarks.utils.ConnectionSettings;
import javababushka.client.RedisClient;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import redis_request.RedisRequestOuterClass;
import response.ResponseOuterClass;

/** A JNI-built client using Unix Domain Sockets with async capabilities */
public class JniSyncClient implements SyncClient {

  private static int MAX_TIMEOUT = 1000;

  private RedisClient client;

  private SocketChannel channel;

  private boolean isChannelWriting = false;

  @Override
  public void connectToRedis() {
    connectToRedis(new ConnectionSettings("localhost", 6379, false));
  }

  @Override
  public void connectToRedis(ConnectionSettings connectionSettings) {

    // Create redis client
    client = new RedisClient();

    // Get socket listener address/path
    RedisClient.startSocketListenerExternal(client);

    int timeout = 0;
    int maxTimeout = 1000;
    while (client.socketPath == null && timeout < maxTimeout) {
      timeout++;
      try {
        Thread.sleep(250);
      } catch (InterruptedException exception) {
        // ignored
      }
    }

    System.out.println("Socket Path: " + client.socketPath);
    UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(client.socketPath);

    // Start the socket listener
    try {
      channel = SocketChannel.open(StandardProtocolFamily.UNIX);
      channel.connect(socketAddress);
    } catch (IOException ioException) {
      ioException.printStackTrace();
      return;
    }

    String host = connectionSettings.host;
    int port = connectionSettings.port;
    connection_request.ConnectionRequestOuterClass.TlsMode tls =
        connectionSettings.useSsl
            ?
            // TODO: secure or insecure TLS?
            connection_request.ConnectionRequestOuterClass.TlsMode.SecureTls
            : connection_request.ConnectionRequestOuterClass.TlsMode.NoTls;

    connection_request.ConnectionRequestOuterClass.ConnectionRequest request =
        connection_request.ConnectionRequestOuterClass.ConnectionRequest.newBuilder()
            .addAddresses(
                connection_request.ConnectionRequestOuterClass.AddressInfo.newBuilder()
                    .setHost(host)
                    .setPort(port))
            .setTlsMode(tls)
            .setClusterModeEnabled(false)
            // In millis
            .setResponseTimeout(250)
            // In millis
            .setClientCreationTimeout(2500)
            .setReadFromReplicaStrategy(
                connection_request.ConnectionRequestOuterClass.ReadFromReplicaStrategy
                    .AlwaysFromPrimary)
            .setConnectionRetryStrategy(
                connection_request.ConnectionRequestOuterClass.ConnectionRetryStrategy.newBuilder()
                    .setNumberOfRetries(1)
                    .setFactor(1)
                    .setExponentBase(1))
            .setAuthenticationInfo(
                connection_request.ConnectionRequestOuterClass.AuthenticationInfo.newBuilder()
                    .setPassword("")
                    .setUsername("default"))
            .setDatabaseId(0)
            .build();

    makeConnection(request);
  }

  @Override
  public void set(String key, String value) {

    int futureIdx = 1;
    RedisRequestOuterClass.Command.ArgsArray args =
        RedisRequestOuterClass.Command.ArgsArray.newBuilder().addArgs(key).addArgs(value).build();
    RedisRequestOuterClass.RedisRequest request =
        RedisRequestOuterClass.RedisRequest.newBuilder()
            .setCallbackIdx(futureIdx)
            .setSingleCommand(
                RedisRequestOuterClass.Command.newBuilder()
                    .setRequestType(RedisRequestOuterClass.RequestType.SetString)
                    .setArgsArray(args))
            .setRoute(
                RedisRequestOuterClass.Routes.newBuilder()
                    .setSimpleRoutes(RedisRequestOuterClass.SimpleRoutes.AllNodes))
            .build();

    ResponseOuterClass.Response response = makeRedisRequest(request);
    // nothing to do with the response
  }

  @Override
  public String get(String key) {
    int futureIdx = 1;
    RedisRequestOuterClass.RedisRequest getStringRequest =
        RedisRequestOuterClass.RedisRequest.newBuilder()
            .setCallbackIdx(futureIdx)
            .setSingleCommand(
                RedisRequestOuterClass.Command.newBuilder()
                    .setRequestType(RedisRequestOuterClass.RequestType.GetString)
                    .setArgsArray(
                        RedisRequestOuterClass.Command.ArgsArray.newBuilder().addArgs(key)))
            .setRoute(
                RedisRequestOuterClass.Routes.newBuilder()
                    .setSimpleRoutes(RedisRequestOuterClass.SimpleRoutes.AllNodes))
            .build();

    ResponseOuterClass.Response response = makeRedisRequest(getStringRequest);
    return response.toString();
  }

  @Override
  public void closeConnection() {}

  @Override
  public String getName() {
    return "JNI (with UDS) Sync";
  }

  // Left is length of message, right is position
  private static Pair<Long, Integer> decodeVarint(byte[] buffer, int pos) throws Exception {
    long mask = ((long) 1 << 32) - 1;
    int shift = 0;
    long result = 0;
    while (true) {
      byte b = buffer[pos];
      result |= (b & 0x7F) << shift;
      pos += 1;
      if ((b & 0x80) == 0) {
        result &= mask;
        // result = (int) result;
        return new MutablePair<>(result, pos);
      }
      shift += 7;
      if (shift >= 64) {
        throw new Exception("Too many bytes when decoding varint.");
      }
    }
  }

  private static ResponseOuterClass.Response decodeMessage(byte[] buffer) throws Exception {
    Pair<Long, Integer> pair = decodeVarint(buffer, 0);
    int startIdx = (int) pair.getRight();
    byte[] responseBytes =
        Arrays.copyOfRange(buffer, startIdx, startIdx + (int) (long) pair.getLeft());
    ResponseOuterClass.Response response = ResponseOuterClass.Response.parseFrom(responseBytes);
    return response;
  }

  private static Byte[] varintBytes(int value) {
    ArrayList<Byte> output = new ArrayList();
    int bits = value & 0x7F;
    value >>= 7;
    while (value > 0) {
      output.add(new Byte((byte) (0x80 | bits)));
      bits = value & 0x7F;
      value >>= 7;
    }
    output.add(new Byte((byte) bits));
    Byte[] arr = new Byte[] {};
    return output.toArray(arr);
  }

  private static byte[] readSocketMessage(SocketChannel channel) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    int bytesRead = channel.read(buffer);
    if (bytesRead <= 0) {
      return null;
    }

    byte[] bytes = new byte[bytesRead];
    buffer.flip();
    buffer.get(bytes);
    return bytes;
  }

  private ResponseOuterClass.Response makeConnection(
      connection_request.ConnectionRequestOuterClass.ConnectionRequest request) {
    Byte[] varint = varintBytes(request.toByteArray().length);

    //    System.out.println("Request: \n" + request.toString());
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    buffer.clear();
    for (Byte b : varint) {
      buffer.put(b);
    }
    buffer.put(request.toByteArray());
    buffer.flip();
    while (isChannelWriting) {
      try {
        Thread.sleep(250);
      } catch (InterruptedException interruptedException) {
        // ignore...
      }
    }
    isChannelWriting = true;
    while (buffer.hasRemaining()) {
      try {
        channel.write(buffer);
      } catch (IOException ioException) {
        // ignore...
      }
    }
    isChannelWriting = false;

    ResponseOuterClass.Response response = null;
    int timeout = 0;
    try {
      byte[] responseBuffer = readSocketMessage(channel);
      while (responseBuffer == null && timeout < MAX_TIMEOUT) {
        Thread.sleep(250);
        timeout++;
        responseBuffer = readSocketMessage(channel);
      }

      response = decodeMessage(responseBuffer);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return response;
  }

  private ResponseOuterClass.Response makeRedisRequest(
      RedisRequestOuterClass.RedisRequest request) {
    Byte[] varint = varintBytes(request.toByteArray().length);

    //    System.out.println("Request: \n" + request.toString());
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    buffer.clear();
    for (Byte b : varint) {
      buffer.put(b);
    }
    buffer.put(request.toByteArray());
    buffer.flip();
    while (isChannelWriting) {
      try {
        Thread.sleep(250);
      } catch (InterruptedException interruptedException) {
        // ignore...
      }
    }
    isChannelWriting = true;
    while (buffer.hasRemaining()) {
      try {
        channel.write(buffer);
      } catch (IOException ioException) {
        // ignore...
      }
    }
    isChannelWriting = false;

    int timeout = 0;
    byte[] responseBuffer = null;
    while (responseBuffer == null && timeout < MAX_TIMEOUT) {
      timeout++;
      try {
        responseBuffer = readSocketMessage(channel);
        Thread.sleep(250);
      } catch (IOException | InterruptedException exception) {
        // ignore...
      }
    }

    // nothing to do with the responseBuffer message
    ResponseOuterClass.Response response = null;
    try {
      response = decodeMessage(responseBuffer);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return response;
  }
}
