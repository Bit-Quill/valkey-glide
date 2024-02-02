/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.examples;

import glide.api.RedisClient;
import glide.examples.clients.GlideClient;
import java.util.concurrent.ExecutionException;

public class ExamplesApp {

    // main application entrypoint
    public static void main(String[] args) {
        runGlideExamples();
    }

    private static void runGlideExamples() {
        ConnectionSettings settings = new ConnectionSettings("localhost", 6379, false, false);

        try {
            RedisClient client = GlideClient.connectToGlide(settings);

            System.out.println("Glide PING: " + client.customCommand(new String[] {"ping"}).get());
            System.out.println(
                    "Glide PING(custom): " + client.customCommand(new String[] {"ping", "found you!"}).get());

            System.out.println("Glide INFO(): " + client.customCommand(new String[] {"info"}).get());

            System.out.println(
                    "Glide SET(myKey, myValue): "
                            + client.customCommand(new String[] {"set", "myKey", "myValue"}).get());
            System.out.println(
                    "Glide GET(myKey): " + client.customCommand(new String[] {"get", "myKey"}).get());
            System.out.println(
                    "Glide SET(myKey, yourValue): "
                            + client.customCommand(new String[] {"set", "myKey", "yourValue"}).get());
            System.out.println(
                    "Glide GET(myKey): " + client.customCommand(new String[] {"get", "myKey"}).get());
            System.out.println(
                    "Glide GET(invalid): " + client.customCommand(new String[] {"get", "invalid"}).get());

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
