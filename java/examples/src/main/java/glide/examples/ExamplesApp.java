/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.examples;

import glide.api.RedisClient;
import glide.api.models.commands.stream.StreamAddOptions;
import glide.api.models.commands.stream.StreamReadOptions;
import glide.api.models.configuration.NodeAddress;
import glide.api.models.configuration.RedisClientConfiguration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class ExamplesApp {

    // main application entrypoint
    public static void main(String[] args) {
        runGlideExamples();
    }

    private static void runGlideExamples() {
        String host = "localhost";
        Integer port = 6379;
        boolean useSsl = false;

        RedisClientConfiguration config =
                RedisClientConfiguration.builder()
                        .address(NodeAddress.builder().host(host).port(port).build())
                        .useTLS(useSsl)
                        .build();

        try {
            RedisClient client = RedisClient.CreateClient(config).get();

            String key1 = "{key1}-" + UUID.randomUUID();
            String key2 = "{key2}-" + UUID.randomUUID();
            String field1 = "f1_";
            String field2 = "f2_";
            String field3 = "f3_";

            Map<String, String> timestamp_1_1_map = new LinkedHashMap<>();
            timestamp_1_1_map.put(field1, field1 + "1");
            timestamp_1_1_map.put(field3, field3 + "1");
            String timestamp_1_1 =
                    client.xadd(key1, timestamp_1_1_map, StreamAddOptions.builder().id("1-1").build()).get();

            String timestamp_2_1 =
                    client
                            .xadd(
                                    key2, Map.of(field2, field2 + "1"), StreamAddOptions.builder().id("2-1").build())
                            .get();

            String timestamp_1_2 =
                    client
                            .xadd(
                                    key1, Map.of(field1, field1 + "2"), StreamAddOptions.builder().id("1-2").build())
                            .get();

            String timestamp_2_2 =
                    client
                            .xadd(
                                    key2, Map.of(field2, field2 + "2"), StreamAddOptions.builder().id("2-2").build())
                            .get();

            // setup third entries in streams key1 and key2
            Map<String, String> timestamp_1_3_map = new LinkedHashMap<>();
            timestamp_1_3_map.put(field1, field1 + "3");
            timestamp_1_3_map.put(field3, field3 + "3");

            String timestamp_1_3 =
                    client.xadd(key1, timestamp_1_3_map, StreamAddOptions.builder().id("1-3").build()).get();

            String timestamp_2_3 =
                    client
                            .xadd(
                                    key2, Map.of(field2, field2 + "3"), StreamAddOptions.builder().id("2-3").build())
                            .get();

            StreamReadOptions blockOption = StreamReadOptions.builder().block(1L).build();

            Map<String, Map<String, Map<String, String>>> result =
                    client.xread(Map.of(key1, timestamp_1_1, key2, timestamp_2_1), blockOption).get();

            for (var keyentry : result.entrySet()) {
                for (var streamEntry : keyentry.getValue().entrySet()) {
                    for (var fieldEntry : streamEntry.getValue().entrySet()) {
                        System.out.printf(
                                "Key: %s; stream id: %s; field: %s; value: %s\n",
                                keyentry.getKey(),
                                streamEntry.getKey(),
                                fieldEntry.getKey(),
                                fieldEntry.getValue());
                    }
                }
            }

        } catch (ExecutionException | InterruptedException e) {
            System.out.println("Glide example failed with an exception: ");
            e.printStackTrace();
        }
    }
}
