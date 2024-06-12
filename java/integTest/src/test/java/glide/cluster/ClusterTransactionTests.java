/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.cluster;

import static glide.TestConfiguration.REDIS_VERSION;
import static glide.TestUtilities.assertDeepEquals;
import static glide.TestUtilities.commonClusterClientConfig;
import static glide.TestUtilities.createLuaLibWithLongRunningFunction;
import static glide.api.BaseClient.OK;
import static glide.api.models.configuration.RequestRoutingConfiguration.SimpleSingleNodeRoute.RANDOM;
import static glide.api.models.configuration.RequestRoutingConfiguration.SlotType.PRIMARY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import glide.TestConfiguration;
import glide.TransactionTestUtilities.TransactionBuilder;
import glide.api.RedisClusterClient;
import glide.api.models.ClusterTransaction;
import glide.api.models.configuration.NodeAddress;
import glide.api.models.configuration.RedisClusterClientConfiguration;
import glide.api.models.configuration.RequestRoutingConfiguration.SingleNodeRoute;
import glide.api.models.configuration.RequestRoutingConfiguration.SlotIdRoute;
import glide.api.models.configuration.RequestRoutingConfiguration.SlotKeyRoute;
import glide.api.models.configuration.RequestRoutingConfiguration.SlotType;
import glide.api.models.exceptions.RequestException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
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
                        .get();
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
        Object[] result = clusterClient.exec(transaction).get();
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

    @SneakyThrows
    @ParameterizedTest(name = "{0}")
    @MethodSource("glide.TransactionTestUtilities#getCommonTransactionBuilders")
    public void transactions_with_group_of_commands(String testName, TransactionBuilder builder) {
        ClusterTransaction transaction = new ClusterTransaction();
        Object[] expectedResult = builder.apply(transaction);

        Object[] results = clusterClient.exec(transaction).get();
        assertDeepEquals(expectedResult, results);
    }

    @SneakyThrows
    @ParameterizedTest(name = "{0}")
    @MethodSource("glide.TransactionTestUtilities#getPrimaryNodeTransactionBuilders")
    public void keyless_transactions_with_group_of_commands(
            String testName, TransactionBuilder builder) {
        ClusterTransaction transaction = new ClusterTransaction();
        Object[] expectedResult = builder.apply(transaction);

        SingleNodeRoute route = new SlotIdRoute(1, SlotType.PRIMARY);
        Object[] results = clusterClient.exec(transaction, route).get();
        assertDeepEquals(expectedResult, results);
    }

    @Test
    @SneakyThrows
    public void lastsave() {
        var yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        var response = clusterClient.exec(new ClusterTransaction().lastsave()).get();
        assertTrue(Instant.ofEpochSecond((long) response[0]).isAfter(yesterday));
    }

    @Test
    @SneakyThrows
    public void objectFreq() {
        String objectFreqKey = "key";
        String maxmemoryPolicy = "maxmemory-policy";
        String oldPolicy =
                clusterClient.configGet(new String[] {maxmemoryPolicy}).get().get(maxmemoryPolicy);
        try {
            ClusterTransaction transaction = new ClusterTransaction();
            transaction.configSet(Map.of(maxmemoryPolicy, "allkeys-lfu"));
            transaction.set(objectFreqKey, "");
            transaction.objectFreq(objectFreqKey);
            var response = clusterClient.exec(transaction).get();
            assertEquals(OK, response[0]);
            assertEquals(OK, response[1]);
            assertTrue((long) response[2] >= 0L);
        } finally {
            clusterClient.configSet(Map.of(maxmemoryPolicy, oldPolicy));
        }
    }

    @Test
    @SneakyThrows
    public void objectIdletime() {
        String objectIdletimeKey = "key";
        ClusterTransaction transaction = new ClusterTransaction();
        transaction.set(objectIdletimeKey, "");
        transaction.objectIdletime(objectIdletimeKey);
        var response = clusterClient.exec(transaction).get();
        assertEquals(OK, response[0]);
        assertTrue((long) response[1] >= 0L);
    }

    @Test
    @SneakyThrows
    public void objectRefcount() {
        String objectRefcountKey = "key";
        ClusterTransaction transaction = new ClusterTransaction();
        transaction.set(objectRefcountKey, "");
        transaction.objectRefcount(objectRefcountKey);
        var response = clusterClient.exec(transaction).get();
        assertEquals(OK, response[0]);
        assertTrue((long) response[1] >= 0L);
    }

    @Test
    @SneakyThrows
    public void zrank_zrevrank_withscores() {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.2.0"));
        String zSetKey1 = "{key}:zsetKey1-" + UUID.randomUUID();
        ClusterTransaction transaction = new ClusterTransaction();
        transaction.zadd(zSetKey1, Map.of("one", 1.0, "two", 2.0, "three", 3.0));
        transaction.zrankWithScore(zSetKey1, "one");
        transaction.zrevrankWithScore(zSetKey1, "one");

        Object[] result = clusterClient.exec(transaction).get();
        assertEquals(3L, result[0]);
        assertArrayEquals(new Object[] {0L, 1.0}, (Object[]) result[1]);
        assertArrayEquals(new Object[] {2L, 1.0}, (Object[]) result[2]);
    }

    @Test
    @SneakyThrows
    public void functionStats_and_functionKill_with_key_based_route() {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0"), "This feature added in redis 7");

        String libName = "functionStats_and_functionKill_with_key_based_route_cluster_transaction";
        String funcName = "deadlock_with_key_based_route_cluster_transaction";
        String key = libName;
        String code = createLuaLibWithLongRunningFunction(libName, funcName, 15, true);
        SingleNodeRoute route = new SlotKeyRoute(key, PRIMARY);
        String error = "";
        ClusterTransaction transaction = new ClusterTransaction().functionKill();

        try {
            // nothing to kill
            var exception =
                    assertThrows(
                            ExecutionException.class, () -> clusterClient.exec(transaction, route).get());
            assertInstanceOf(RequestException.class, exception.getCause());
            assertTrue(exception.getMessage().toLowerCase().contains("notbusy"));

            // load the lib
            assertEquals(libName, clusterClient.functionLoadReplace(code, route).get());

            try (var testClient =
                    RedisClusterClient.CreateClient(commonClusterClientConfig().requestTimeout(7000).build())
                            .get()) {
                // call the function without await
                // TODO use FCALL
                var promise = testClient.customCommand(new String[] {"FCALL", funcName, "1", key});

                int timeout = 5200; // ms
                while (timeout > 0) {
                    var stats = clusterClient.customCommand(new String[] {"FUNCTION", "STATS"}, route).get();
                    var response = stats.getSingleValue();
                    if (((Map<String, Object>) response).get("running_script") != null) {
                        break;
                    }
                    Thread.sleep(100);
                    timeout -= 100;
                }
                if (timeout == 0) {
                    error += "Can't find a running function.";
                }

                // redis kills a function with 5 sec delay
                assertArrayEquals(new Object[] {OK}, clusterClient.exec(transaction, route).get());

                exception =
                        assertThrows(
                                ExecutionException.class, () -> clusterClient.exec(transaction, route).get());
                assertInstanceOf(RequestException.class, exception.getCause());
                assertTrue(exception.getMessage().toLowerCase().contains("notbusy"));

                exception = assertThrows(ExecutionException.class, promise::get);
                assertInstanceOf(RequestException.class, exception.getCause());
                assertTrue(exception.getMessage().contains("Script killed by user"));
            }
        } finally {
            // If function wasn't killed, and it didn't time out - it blocks the server and cause rest
            // test to fail.
            try {
                clusterClient.functionKill(route).get();
                // should throw `notbusy` error, because the function should be killed before
                error += "Function should be killed before.";
            } catch (Exception ignored) {
            }
        }

        // TODO replace with FUNCTION DELETE
        assertEquals(
                OK,
                clusterClient
                        .customCommand(new String[] {"FUNCTION", "DELETE", libName}, route)
                        .get()
                        .getSingleValue());

        assertTrue(error.isEmpty(), "Something went wrong during the test");
    }
}
