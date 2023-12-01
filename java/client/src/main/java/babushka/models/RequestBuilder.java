package babushka.models;

import babushka.ffi.resolvers.BabushkaCoreNativeDefinitions;
import connection_request.ConnectionRequestOuterClass;
import java.util.List;
import redis_request.RedisRequestOuterClass;
import response.ResponseOuterClass;

public class RequestBuilder {

  /** Build a protobuf connection request object */
  // TODO support more parameters and/or configuration object
  public static ConnectionRequestOuterClass.ConnectionRequest getConnectionRequest(
      String host, int port, boolean useSsl, boolean clusterMode) {
    return ConnectionRequestOuterClass.ConnectionRequest.newBuilder()
        .addAddresses(
            ConnectionRequestOuterClass.NodeAddress.newBuilder()
                .setHost(host)
                .setPort(port)
                .build())
        .setTlsMode(
            useSsl
                ? ConnectionRequestOuterClass.TlsMode.SecureTls
                : ConnectionRequestOuterClass.TlsMode.NoTls)
        .setClusterModeEnabled(clusterMode)
        .setReadFrom(ConnectionRequestOuterClass.ReadFrom.Primary)
        .setDatabaseId(0)
        .build();
  }

  public static RedisRequestOuterClass.RedisRequest redisSingleCommand(
      RedisRequestOuterClass.RequestType command, List<String> args) {
    var commandArgs = RedisRequestOuterClass.Command.ArgsArray.newBuilder();
    for (var arg : args) {
      commandArgs.addArgs(arg);
    }

    RedisRequestOuterClass.RedisRequest.Builder builder =
        RedisRequestOuterClass.RedisRequest.newBuilder()
            .setSingleCommand(
                RedisRequestOuterClass.Command.newBuilder()
                    .setRequestType(command)
                    .setArgsArray(commandArgs.build())
                    .build())
            .setRoute(
                RedisRequestOuterClass.Routes.newBuilder()
                    .setSimpleRoutes(RedisRequestOuterClass.SimpleRoutes.AllNodes)
                    .build());
    // TODO: set callback index?

    return builder.build();
  }

  /**
   * Returns a String from the redis response if a resp2 response exists, or Ok. Otherwise, returns
   * null
   *
   * @param response Redis Response
   * @return String or null
   */
  public static String resolveRedisResponseToString(ResponseOuterClass.Response response) {
    if (response.hasConstantResponse()) {
      return BabushkaCoreNativeDefinitions.valueFromPointer(response.getRespPointer()).toString();
    }
    if (response.hasRespPointer()) {
      return response.getConstantResponse().toString();
    }
    return null;
  }
}
