/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import static glide.TestConfiguration.CLUSTER_PORTS;
import static glide.TestConfiguration.STANDALONE_PORTS;
import static glide.api.BaseClient.OK;
import static glide.api.models.commands.SetOptions.ConditionalSet.ONLY_IF_DOES_NOT_EXIST;
import static glide.api.models.commands.SetOptions.ConditionalSet.ONLY_IF_EXISTS;
import static glide.api.models.commands.SetOptions.Expiry.Milliseconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import glide.api.BaseClient;
import glide.api.RedisClient;
import glide.api.RedisClusterClient;
import glide.api.models.commands.SetOptions;
import glide.api.models.configuration.NodeAddress;
import glide.api.models.configuration.RedisClientConfiguration;
import glide.api.models.configuration.RedisClusterClientConfiguration;
import glide.api.models.exceptions.RequestException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Timeout(10)
public class SharedCommandTests {

    private static RedisClient standaloneClient = null;
    private static RedisClusterClient clusterClient = null;

    @Getter private static List<Arguments> clients;

    private static final String KEY_NAME = "key";
    private static final String INITIAL_VALUE = "VALUE";
    private static final String ANOTHER_VALUE = "VALUE2";

    @BeforeAll
    @SneakyThrows
    public static void init() {
        standaloneClient =
                RedisClient.CreateClient(
                                RedisClientConfiguration.builder()
                                        .address(NodeAddress.builder().port(STANDALONE_PORTS[0]).build())
                                        .build())
                        .get();

        clusterClient =
                RedisClusterClient.CreateClient(
                                RedisClusterClientConfiguration.builder()
                                        .address(NodeAddress.builder().port(CLUSTER_PORTS[0]).build())
                                        .requestTimeout(5000)
                                        .build())
                        .get();

        clients = List.of(Arguments.of(standaloneClient), Arguments.of(clusterClient));
    }

    @AfterAll
    @SneakyThrows
    public static void teardown() {
        standaloneClient.close();
        clusterClient.close();
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void ping(BaseClient client) {
        String data = client.ping().get();
        assertEquals("PONG", data);
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void ping_with_message(BaseClient client) {
        String data = client.ping("H3LL0").get();
        assertEquals("H3LL0", data);
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void set_and_get_without_options(BaseClient client) {
        String ok = client.set(KEY_NAME, INITIAL_VALUE).get();
        assertEquals(OK, ok);

        String data = client.get(KEY_NAME).get();
        assertEquals(INITIAL_VALUE, data);
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void get_missing_value(BaseClient client) {
        String data = client.get("invalid").get();
        assertNull(data);
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void set_overwrite_value_and_returnOldValue_returns_string(BaseClient client) {
        String ok = client.set(KEY_NAME, INITIAL_VALUE).get();
        assertEquals(OK, ok);

        SetOptions options = SetOptions.builder().returnOldValue(true).build();
        String data = client.set(KEY_NAME, ANOTHER_VALUE, options).get();
        assertEquals(INITIAL_VALUE, data);
    }

    @ParameterizedTest
    @MethodSource("getClients")
    public void set_requires_a_value(BaseClient client) {
        assertThrows(NullPointerException.class, () -> client.set("SET", null));
    }

    @ParameterizedTest
    @MethodSource("getClients")
    public void set_requires_a_key(BaseClient client) {
        assertThrows(NullPointerException.class, () -> client.set(null, INITIAL_VALUE));
    }

    @ParameterizedTest
    @MethodSource("getClients")
    public void get_requires_a_key(BaseClient client) {
        assertThrows(NullPointerException.class, () -> client.get(null));
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void set_only_if_exists_overwrite(BaseClient client) {
        String key = "set_only_if_exists_overwrite";
        SetOptions options = SetOptions.builder().conditionalSet(ONLY_IF_EXISTS).build();
        client.set(key, INITIAL_VALUE).get();
        client.set(key, ANOTHER_VALUE, options).get();
        String data = client.get(key).get();
        assertEquals(ANOTHER_VALUE, data);
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void set_only_if_exists_missing_key(BaseClient client) {
        String key = "set_only_if_exists_missing_key";
        SetOptions options = SetOptions.builder().conditionalSet(ONLY_IF_EXISTS).build();
        client.set(key, ANOTHER_VALUE, options).get();
        String data = client.get(key).get();
        assertNull(data);
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void set_only_if_does_not_exists_missing_key(BaseClient client) {
        String key = "set_only_if_does_not_exists_missing_key";
        SetOptions options = SetOptions.builder().conditionalSet(ONLY_IF_DOES_NOT_EXIST).build();
        client.set(key, ANOTHER_VALUE, options).get();
        String data = client.get(key).get();
        assertEquals(ANOTHER_VALUE, data);
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void set_only_if_does_not_exists_existing_key(BaseClient client) {
        String key = "set_only_if_does_not_exists_existing_key";
        SetOptions options = SetOptions.builder().conditionalSet(ONLY_IF_DOES_NOT_EXIST).build();
        client.set(key, INITIAL_VALUE).get();
        client.set(key, ANOTHER_VALUE, options).get();
        String data = client.get(key).get();
        assertEquals(INITIAL_VALUE, data);
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void set_value_with_ttl_and_update_value_with_keeping_ttl(BaseClient client) {
        String key = "set_value_with_ttl_and_update_value_with_keeping_ttl";
        SetOptions options = SetOptions.builder().expiry(Milliseconds(2000L)).build();
        client.set(key, INITIAL_VALUE, options).get();
        String data = client.get(key).get();
        assertEquals(INITIAL_VALUE, data);

        options = SetOptions.builder().expiry(SetOptions.Expiry.KeepExisting()).build();
        client.set(key, ANOTHER_VALUE, options).get();
        data = client.get(key).get();
        assertEquals(ANOTHER_VALUE, data);

        Thread.sleep(2222); // sleep a bit more than TTL

        data = client.get(key).get();
        assertNull(data);
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void set_value_with_ttl_and_update_value_with_new_ttl(BaseClient client) {
        String key = "set_value_with_ttl_and_update_value_with_new_ttl";
        SetOptions options = SetOptions.builder().expiry(Milliseconds(100500L)).build();
        client.set(key, INITIAL_VALUE, options).get();
        String data = client.get(key).get();
        assertEquals(INITIAL_VALUE, data);

        options = SetOptions.builder().expiry(Milliseconds(2000L)).build();
        client.set(key, ANOTHER_VALUE, options).get();
        data = client.get(key).get();
        assertEquals(ANOTHER_VALUE, data);

        Thread.sleep(2222); // sleep a bit more than new TTL

        data = client.get(key).get();
        assertNull(data);
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void set_expired_value(BaseClient client) {
        String key = "set_expired_value";
        SetOptions options =
                SetOptions.builder()
                        // expiration is in the past
                        .expiry(SetOptions.Expiry.UnixSeconds(100500L))
                        .build();
        client.set(key, INITIAL_VALUE, options).get();
        String data = client.get(key).get();
        assertNull(data);
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void set_missing_value_and_returnOldValue_is_null(BaseClient client) {
        String ok = client.set(KEY_NAME, INITIAL_VALUE).get();
        assertEquals(OK, ok);

        SetOptions options = SetOptions.builder().returnOldValue(true).build();
        String data = client.set("another", ANOTHER_VALUE, options).get();
        assertNull(data);
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void incr_commands_existing_key(BaseClient client) {
        String key = RandomStringUtils.randomAlphabetic(10);

        assertEquals(OK, client.set(key, "10").get(10, SECONDS));

        assertEquals(11, client.incr(key).get(10, SECONDS));
        assertEquals("11", client.get(key).get(10, SECONDS));

        assertEquals(15, client.incrBy(key, 4).get(10, SECONDS));
        assertEquals("15", client.get(key).get(10, SECONDS));

        assertEquals(20.5, client.incrByFloat(key, 5.5).get(10, SECONDS));
        assertEquals("20.5", client.get(key).get(10, SECONDS));
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void incr_commands_non_existing_key(BaseClient client) {
        String key1 = RandomStringUtils.randomAlphabetic(10);
        String key2 = RandomStringUtils.randomAlphabetic(10);
        String key3 = RandomStringUtils.randomAlphabetic(10);

        assertNull(client.get(key1).get(10, SECONDS));
        assertEquals(1, client.incr(key1).get(10, SECONDS));
        assertEquals("1", client.get(key1).get(10, SECONDS));

        assertNull(client.get(key2).get(10, SECONDS));
        assertEquals(3, client.incrBy(key2, 3).get(10, SECONDS));
        assertEquals("3", client.get(key2).get(10, SECONDS));

        assertNull(client.get(key3).get(10, SECONDS));
        assertEquals(0.5, client.incrByFloat(key3, 0.5).get(10, SECONDS));
        assertEquals("0.5", client.get(key3).get(10, SECONDS));
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getClients")
    public void test_incr_commands_type_error(BaseClient client) {
        String key1 = RandomStringUtils.randomAlphabetic(10);

        assertEquals(OK, client.set(key1, "foo").get(10, SECONDS));

        Exception incrException =
                assertThrows(ExecutionException.class, () -> client.incr(key1).get(10, SECONDS));
        assertTrue(incrException.getCause() instanceof RequestException);
        assertTrue(incrException.getCause().getMessage().contains("value is not an integer"));

        Exception incrByException =
                assertThrows(ExecutionException.class, () -> client.incrBy(key1, 3).get(10, SECONDS));
        assertTrue(incrByException.getCause() instanceof RequestException);
        assertTrue(incrByException.getCause().getMessage().contains("value is not an integer"));

        Exception incrByFloatException =
                assertThrows(
                        ExecutionException.class, () -> client.incrByFloat(key1, 3.5).get(10, SECONDS));
        assertTrue(incrByFloatException.getCause() instanceof RequestException);
        assertTrue(incrByFloatException.getCause().getMessage().contains("value is not a valid float"));
    }
}
