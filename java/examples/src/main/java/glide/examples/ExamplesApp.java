package glide.examples;

import glide.api.RedisClient;
import glide.examples.clients.GlideClient;
import glide.examples.clients.JedisClient;
import glide.examples.clients.LettuceAsyncClient;
import java.util.concurrent.ExecutionException;
import redis.clients.jedis.commands.JedisCommands;

public class ExamplesApp {

    // main application entrypoint
    public static void main(String[] args) {

        //    runJedisExamples();
        //    runLettuceExamples();
        runGlideExamples();
    }

    private static void runJedisExamples() {
        ConnectionSettings settings = new ConnectionSettings("localhost", 6379, false, false);

        System.out.println("Connecting to Redis using Jedis-client");
        JedisCommands jedisCommands = JedisClient.connectToRedis(settings);
        System.out.println("Jedis SET(myKey, myValue): " + jedisCommands.set("myKey", "myValue"));
        System.out.println("Jedis GET(myKey): " + jedisCommands.get("myKey"));
        System.out.println("Jedis GET(invalid): " + jedisCommands.get("invalid"));
    }

    private static void runLettuceExamples() {

        ConnectionSettings settings = new ConnectionSettings("localhost", 6379, false, false);

        System.out.println("Connecting to Redis using Lettuce-client");
        LettuceAsyncClient lettuceAsyncClient = new LettuceAsyncClient(settings);

        try {
            System.out.println(
                    "Lettuce SET(myKey, myValue): "
                            + lettuceAsyncClient.asyncCommands.set("myKey", "myValue").get());
            System.out.println(
                    "Lettuce GET(myKey): " + lettuceAsyncClient.asyncCommands.get("myKey").get());
            System.out.println(
                    "Lettuce GET(invalid): " + lettuceAsyncClient.asyncCommands.get("invalid").get());

        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Lettuce example failed with an exception: ");
            e.printStackTrace();
        }
    }

    private static void runGlideExamples() {
        ConnectionSettings settings = new ConnectionSettings("localhost", 6379, false, false);

        try {
            RedisClient client = GlideClient.connectToGlide(settings);

            System.out.println("Glide PING: " + client.ping().get());
            System.out.println("Glide PING(custom): " + client.ping("found you!").get());

            // panic
            // System.out.println("Glide INFO(): " + client.info().get());

            System.out.println("Glide SET(myKey, myValue): " + client.set("myKey", "myValue").get());
            System.out.println("Glide GET(myKey): " + client.get("myKey").get());
            System.out.println("Glide SET(myKey, yourValue): " + client.set("myKey", "yourValue").get());
            System.out.println("Glide GET(myKey): " + client.get("myKey").get());
            System.out.println("Glide GET(invalid): " + client.get("invalid").get());

            Object customGetMyKey = client.customCommand(new String[] {"get", "myKey"}).get();
            System.out.println("Glide CUSTOM_COMMAND(get, myKey): " + customGetMyKey);

            Object customGetInvalid = client.customCommand(new String[] {"get", "invalid"}).get();
            System.out.println("Glide CUSTOM_COMMAND(get, invalid): " + customGetInvalid);

        } catch (ExecutionException | InterruptedException e) {
            System.out.println("Glide example failed with an exception: ");
            e.printStackTrace();
        }
    }

    /** Redis-client settings */
    public static class ConnectionSettings {
        public final String host;
        public final int port;
        public final boolean useSsl;
        public final boolean clusterMode;

        public ConnectionSettings(String host, int port, boolean useSsl, boolean clusterMode) {
            this.host = host;
            this.port = port;
            this.useSsl = useSsl;
            this.clusterMode = clusterMode;
        }
    }
}
