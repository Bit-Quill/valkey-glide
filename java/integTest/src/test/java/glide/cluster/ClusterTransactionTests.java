/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.cluster;

import static glide.TransactionTestUtilities.HashCommandTransactionBuilder;
import static glide.TransactionTestUtilities.ListCommandTransactionBuilder;
import static glide.TransactionTestUtilities.ServerManagementCommandTransactionBuilder;
import static glide.TransactionTestUtilities.SetCommandTransactionBuilder;
import static glide.TransactionTestUtilities.SortedSetCommandTransactionBuilder;
import static glide.TransactionTestUtilities.StringCommandTransactionBuilder;
import static glide.api.BaseClient.OK;
import static glide.api.models.configuration.RequestRoutingConfiguration.SimpleSingleNodeRoute.RANDOM;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import glide.TestConfiguration;
import glide.TransactionTestUtilities.TransactionBuilder;
import glide.api.RedisClusterClient;
import glide.api.models.ClusterTransaction;
import glide.api.models.configuration.NodeAddress;
import glide.api.models.configuration.RedisClusterClientConfiguration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Timeout(10) // seconds
public class ClusterTransactionTests {

    private static RedisClusterClient clusterClient = null;

    @BeforeAll
    @SneakyThrows
    public static void init() {
        clusterClient =
                RedisClusterClient.CreateClient(
                                RedisClusterClientConfiguration.builder()
                                        .address(NodeAddress.builder().port(TestConfiguration.CLUSTER_PORTS[0]).build())
                                        .requestTimeout(5000)
                                        .build())
                        .get(10, TimeUnit.SECONDS);
    }

    @AfterAll
    @SneakyThrows
    public static void teardown() {
        clusterClient.close();
    }

    @Test
    @SneakyThrows
    public void custom_command_info() {
        ClusterTransaction transaction = new ClusterTransaction().customCommand(new String[] {"info"});
        Object[] result = clusterClient.exec(transaction).get(10, TimeUnit.SECONDS);
        assertTrue(((String) result[0]).contains("# Stats"));
    }

    @Test
    @SneakyThrows
    public void WATCH_transaction_failure_returns_null() {
        ClusterTransaction transaction = new ClusterTransaction();
        transaction.get("key");
        assertEquals(
                OK, clusterClient.customCommand(new String[] {"WATCH", "key"}).get().getSingleValue());
        assertEquals(OK, clusterClient.set("key", "foo").get());
        assertNull(clusterClient.exec(transaction).get());
    }

    @Test
    @SneakyThrows
    public void info_simple_route_test() {
        ClusterTransaction transaction = new ClusterTransaction().info().info();
        Object[] result = clusterClient.exec(transaction, RANDOM).get();

        assertTrue(((String) result[0]).contains("# Stats"));
        assertTrue(((String) result[1]).contains("# Stats"));
    }

    public static Stream<Arguments> getTransactionBuilders() {
        return Stream.of(
                Arguments.of("String Commands", StringCommandTransactionBuilder),
                Arguments.of("Hash Commands", HashCommandTransactionBuilder),
                Arguments.of("List Commands", ListCommandTransactionBuilder),
                Arguments.of("Set Commands", SetCommandTransactionBuilder),
                Arguments.of("Sorted Set Commands", SortedSetCommandTransactionBuilder),
                Arguments.of("Server Management Commands", ServerManagementCommandTransactionBuilder));
    }

    @SneakyThrows
    @ParameterizedTest(name = "{0}")
    @MethodSource("getTransactionBuilders")
    public void transactions_with_group_of_command(String testName, TransactionBuilder builder) {
        ClusterTransaction transaction = new ClusterTransaction();
        Object[] expectedResult = builder.apply(transaction);

        Object[] results = clusterClient.exec(transaction, RANDOM).get();
        assertArrayEquals(expectedResult, results);
    }
}
