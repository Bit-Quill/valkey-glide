/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.standalone;

import static glide.api.models.commands.InfoOptions.Section.CLUSTER;
import static glide.api.models.commands.InfoOptions.Section.CPU;
import static glide.api.models.commands.InfoOptions.Section.EVERYTHING;
import static glide.api.models.commands.InfoOptions.Section.MEMORY;
import static glide.cluster.CommandTests.DEFAULT_INFO_SECTIONS;
import static glide.cluster.CommandTests.EVERYTHING_INFO_SECTIONS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;

import glide.TestConfiguration;
import glide.api.RedisClient;
import glide.api.models.commands.InfoOptions;
import glide.api.models.configuration.NodeAddress;
import glide.api.models.configuration.RedisClientConfiguration;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CommandTests {
    private static RedisClient regularClient = null;

    @BeforeAll
    @SneakyThrows
    public static void init() {
        regularClient =
                RedisClient.CreateClient(
                                RedisClientConfiguration.builder()
                                        .address(
                                                NodeAddress.builder().port(TestConfiguration.STANDALONE_PORTS[0]).build())
                                        .build())
                        .get(10, SECONDS);
    }

    @AfterAll
    @SneakyThrows
    public static void teardown() {
        regularClient.close();
    }

    @Test
    @SneakyThrows
    public void custom_command_info() {
        var data = regularClient.customCommand("info").get(10, SECONDS);
        assertTrue(((String) data).contains("# Stats"));
    }

    @Test
    @SneakyThrows
    public void info_without_options() {
        String data = regularClient.info().get(10, SECONDS);
        for (var section : DEFAULT_INFO_SECTIONS) {
            assertTrue(data.contains("# " + section), "Section " + section + " is missing");
        }
    }

    @Test
    @SneakyThrows
    public void info_with_multiple_options() {
        var builder = InfoOptions.builder().section(CLUSTER);
        if (TestConfiguration.REDIS_VERSION.feature() >= 7) {
            builder.section(CPU).section(MEMORY);
        }
        var options = builder.build();
        String data = regularClient.info(options).get(10, SECONDS);
        for (var section : options.toArgs()) {
            assertTrue(
                    data.toLowerCase().contains("# " + section.toLowerCase()),
                    "Section " + section + " is missing");
        }
    }

    @Test
    @SneakyThrows
    public void info_with_everything_option() {
        InfoOptions options = InfoOptions.builder().section(EVERYTHING).build();
        String data = regularClient.info(options).get(10, SECONDS);
        for (var section : EVERYTHING_INFO_SECTIONS) {
            assertTrue(data.contains("# " + section), "Section " + section + " is missing");
        }
    }
}
