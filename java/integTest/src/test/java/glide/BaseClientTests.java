/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import static glide.api.BaseClient.OK;
import static glide.api.models.commands.SetOptions.ConditionalSet.ONLY_IF_DOES_NOT_EXIST;
import static glide.api.models.commands.SetOptions.ConditionalSet.ONLY_IF_EXISTS;
import static glide.api.models.commands.SetOptions.TimeToLiveType.KEEP_EXISTING;
import static glide.api.models.commands.SetOptions.TimeToLiveType.MILLISECONDS;
import static glide.api.models.commands.SetOptions.TimeToLiveType.UNIX_SECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BaseClientTests {

    private static final String KEY_NAME = "key";
    private static final String INITIAL_VALUE = "VALUE";
    private static final String ANOTHER_VALUE = "VALUE2";

    private static RedisClient standaloneClient = null;
    private static RedisClusterClient clusterClient = null;

    private static List<BaseClient> clients = new ArrayList<>();

    @BeforeAll
    @SneakyThrows
    public static void init() {
        standaloneClient =
                RedisClient.CreateClient(
                                RedisClientConfiguration.builder()
                                        .address(
                                                NodeAddress.builder().port(TestConfiguration.STANDALONE_PORTS[0]).build())
                                        .build())
                        .get(10, SECONDS);

        clusterClient =
                RedisClusterClient.CreateClient(
                                RedisClusterClientConfiguration.builder()
                                        .address(NodeAddress.builder().port(TestConfiguration.CLUSTER_PORTS[0]).build())
                                        .requestTimeout(5000)
                                        .build())
                        .get(10, SECONDS);

        clients.add(standaloneClient);
        clients.add(clusterClient);
    }

    @AfterAll
    @SneakyThrows
    public static void teardown() {
        standaloneClient.close();
        clusterClient.close();
    }

    @Test
    @SneakyThrows
    public void ping() {
        for (BaseClient client : clients) {
            var data = client.ping().get(10, SECONDS);
            assertEquals("PONG", data);
        }
    }

    @Test
    @SneakyThrows
    public void ping_with_message() {
        for (BaseClient client : clients) {
            var data = client.ping("H3LL0").get(10, SECONDS);
            assertEquals("H3LL0", data);
        }
    }

    @Test
    @SneakyThrows
    public void set_and_get_without_options() {
        for (BaseClient client : clients) {
            String ok = client.set(KEY_NAME, INITIAL_VALUE).get(10, SECONDS);
            assertEquals(OK, ok);

            String data = client.get(KEY_NAME).get(10, SECONDS);
            assertEquals(INITIAL_VALUE, data);
        }
    }

    @Test
    @SneakyThrows
    public void get_missing_value() {
        for (BaseClient client : clients) {
            var data = client.get("invalid").get(10, SECONDS);
            assertNull(data);
        }
    }

    @Test
    @SneakyThrows
    public void set_overwrite_value_and_returnOldValue_returns_string() {
        for (BaseClient client : clients) {
            String ok = client.set(KEY_NAME, INITIAL_VALUE).get(10, SECONDS);
            assertEquals(OK, ok);

            var options = SetOptions.builder().returnOldValue(true).build();
            var data = client.set(KEY_NAME, ANOTHER_VALUE, options).get(10, SECONDS);
            assertEquals(INITIAL_VALUE, data);
        }
    }

    @Test
    public void set_requires_a_value() {
        for (BaseClient client : clients) {
            assertThrows(NullPointerException.class, () -> client.set("SET", null));
        }
    }

    @Test
    public void set_requires_a_key() {
        for (BaseClient client : clients) {
            assertThrows(NullPointerException.class, () -> client.set(null, INITIAL_VALUE));
        }
    }

    @Test
    public void get_requires_a_key() {
        for (BaseClient client : clients) {
            assertThrows(NullPointerException.class, () -> client.get(null));
        }
    }

    @Test
    @SneakyThrows
    public void set_only_if_exists_overwrite() {
        for (BaseClient client : clients) {
            var options = SetOptions.builder().conditionalSet(ONLY_IF_EXISTS).build();
            client.set("set_only_if_exists_overwrite", INITIAL_VALUE).get(10, SECONDS);
            client.set("set_only_if_exists_overwrite", ANOTHER_VALUE, options).get(10, SECONDS);
            var data = client.get("set_only_if_exists_overwrite").get(10, SECONDS);
            assertEquals(ANOTHER_VALUE, data);
        }
    }

    @Test
    @SneakyThrows
    public void set_only_if_exists_missing_key() {
        for (BaseClient client : clients) {
            var options = SetOptions.builder().conditionalSet(ONLY_IF_EXISTS).build();
            client.set("set_only_if_exists_missing_key", ANOTHER_VALUE, options).get(10, SECONDS);
            var data = client.get("set_only_if_exists_missing_key").get(10, SECONDS);
            assertNull(data);
        }
    }

    @Test
    @SneakyThrows
    public void set_only_if_does_not_exists_missing_key() {
        for (BaseClient client : clients) {
            var options = SetOptions.builder().conditionalSet(ONLY_IF_DOES_NOT_EXIST).build();
            client
                    .set("set_only_if_does_not_exists_missing_key", ANOTHER_VALUE, options)
                    .get(10, SECONDS);
            var data = client.get("set_only_if_does_not_exists_missing_key").get(10, SECONDS);
            assertEquals(ANOTHER_VALUE, data);
        }
    }

    @Test
    @SneakyThrows
    public void set_only_if_does_not_exists_existing_key() {
        for (BaseClient client : clients) {
            var options = SetOptions.builder().conditionalSet(ONLY_IF_DOES_NOT_EXIST).build();
            client.set("set_only_if_does_not_exists_existing_key", INITIAL_VALUE).get(10, SECONDS);
            client
                    .set("set_only_if_does_not_exists_existing_key", ANOTHER_VALUE, options)
                    .get(10, SECONDS);
            var data = client.get("set_only_if_does_not_exists_existing_key").get(10, SECONDS);
            assertEquals(INITIAL_VALUE, data);
        }
    }

    @Test
    @SneakyThrows
    public void set_value_with_ttl_and_update_value_with_keeping_ttl() {
        for (BaseClient client : clients) {
            var options =
                    SetOptions.builder()
                            .expiry(SetOptions.TimeToLive.builder().count(2000).type(MILLISECONDS).build())
                            .build();
            client
                    .set("set_value_with_ttl_and_update_value_with_keeping_ttl", INITIAL_VALUE, options)
                    .get(10, SECONDS);
            var data =
                    client.get("set_value_with_ttl_and_update_value_with_keeping_ttl").get(10, SECONDS);
            assertEquals(INITIAL_VALUE, data);

            options =
                    SetOptions.builder()
                            .expiry(SetOptions.TimeToLive.builder().type(KEEP_EXISTING).build())
                            .build();
            client
                    .set("set_value_with_ttl_and_update_value_with_keeping_ttl", ANOTHER_VALUE, options)
                    .get(10, SECONDS);
            data = client.get("set_value_with_ttl_and_update_value_with_keeping_ttl").get(10, SECONDS);
            assertEquals(ANOTHER_VALUE, data);

            Thread.sleep(2222); // sleep a bit more than TTL

            data = client.get("set_value_with_ttl_and_update_value_with_keeping_ttl").get(10, SECONDS);
            assertNull(data);
        }
    }

    @Test
    @SneakyThrows
    public void set_value_with_ttl_and_update_value_with_new_ttl() {
        for (BaseClient client : clients) {
            var options =
                    SetOptions.builder()
                            .expiry(SetOptions.TimeToLive.builder().count(100500).type(MILLISECONDS).build())
                            .build();
            client
                    .set("set_value_with_ttl_and_update_value_with_new_ttl", INITIAL_VALUE, options)
                    .get(10, SECONDS);
            var data = client.get("set_value_with_ttl_and_update_value_with_new_ttl").get(10, SECONDS);
            assertEquals(INITIAL_VALUE, data);

            options =
                    SetOptions.builder()
                            .expiry(SetOptions.TimeToLive.builder().count(2000).type(MILLISECONDS).build())
                            .build();
            client
                    .set("set_value_with_ttl_and_update_value_with_new_ttl", ANOTHER_VALUE, options)
                    .get(10, SECONDS);
            data = client.get("set_value_with_ttl_and_update_value_with_new_ttl").get(10, SECONDS);
            assertEquals(ANOTHER_VALUE, data);

            Thread.sleep(2222); // sleep a bit more than new TTL

            data = client.get("set_value_with_ttl_and_update_value_with_new_ttl").get(10, SECONDS);
            assertNull(data);
        }
    }

    @Test
    @SneakyThrows
    public void set_expired_value() { // expiration is in the past
        for (BaseClient client : clients) {
            var options =
                    SetOptions.builder()
                            .expiry(SetOptions.TimeToLive.builder().count(100500).type(UNIX_SECONDS).build())
                            .build();
            client.set("set_expired_value", INITIAL_VALUE, options).get(10, SECONDS);
            var data = client.get("set_expired_value").get(10, SECONDS);
            assertNull(data);
        }
    }

    @Test
    @SneakyThrows
    public void set_missing_value_and_returnOldValue_is_null() {
        for (BaseClient client : clients) {
            String ok = client.set(KEY_NAME, INITIAL_VALUE).get(10, SECONDS);
            assertEquals(OK, ok);

            var options = SetOptions.builder().returnOldValue(true).build();
            var data = client.set("another", ANOTHER_VALUE, options).get(10, SECONDS);
            assertNull(data);
        }
    }

    @Test
    @SneakyThrows
    public void decr_and_decrBy_existing_key() {
        String key = RandomStringUtils.randomAlphabetic(10);

        for (BaseClient client : clients) {
            assertEquals(OK, client.set(key, "10").get(10, SECONDS));

            assertEquals(client.decr(key).get(10, SECONDS), 9);
            assertEquals(client.get(key).get(10, SECONDS), "9");

            assertEquals(client.decrBy(key, 4).get(10, SECONDS), 5);
            assertEquals(client.get(key).get(10, SECONDS), "5");
        }
    }

    @Test
    @SneakyThrows
    public void decr_and_decrBy_non_existing_key() {
        String key1 = RandomStringUtils.randomAlphabetic(10);
        String key2 = RandomStringUtils.randomAlphabetic(10);

        for (BaseClient client : clients) {
            assertNull(client.get(key1).get(10, SECONDS));
            assertEquals(client.decr(key1).get(10, SECONDS), -1);
            assertEquals(client.get(key1).get(10, SECONDS), "-1");

            assertNull(client.get(key2).get(10, SECONDS));
            assertEquals(client.decrBy(key2, 3).get(10, SECONDS), -3);
            assertEquals(client.get(key2).get(10, SECONDS), "-3");
        }
    }

    @Test
    @SneakyThrows
    public void incr_commands_existing_key() {
        String key = RandomStringUtils.randomAlphabetic(10);

        for (BaseClient client : clients) {
            assertEquals(OK, client.set(key, "10").get(10, SECONDS));

            assertEquals(client.incr(key).get(10, SECONDS), 11);
            assertEquals(client.get(key).get(10, SECONDS), "11");

            assertEquals(client.incrBy(key, 4).get(10, SECONDS), 15);
            assertEquals(client.get(key).get(10, SECONDS), "15");

            assertEquals(client.incrByFloat(key, 5.5).get(10, SECONDS), 20.5);
            assertEquals(client.get(key).get(10, SECONDS), "20.5");
        }
    }

    @Test
    @SneakyThrows
    public void incr_commands_non_existing_key() {
        String key1 = RandomStringUtils.randomAlphabetic(10);
        String key2 = RandomStringUtils.randomAlphabetic(10);
        String key3 = RandomStringUtils.randomAlphabetic(10);

        for (BaseClient client : clients) {
            assertNull(client.get(key1).get(10, SECONDS));
            assertEquals(client.incr(key1).get(10, SECONDS), 1);
            assertEquals(client.get(key1).get(10, SECONDS), "1");

            assertNull(client.get(key2).get(10, SECONDS));
            assertEquals(client.incrBy(key2, 3).get(10, SECONDS), 3);
            assertEquals(client.get(key2).get(10, SECONDS), "3");

            assertNull(client.get(key3).get(10, SECONDS));
            assertEquals(client.incrByFloat(key3, 0.5).get(10, SECONDS), 0.5);
            assertEquals(client.get(key3).get(10, SECONDS), "0.5");
        }
    }

    @Test
    @SneakyThrows
    public void test_incr_commands_with_str_value() {
        String key1 = RandomStringUtils.randomAlphabetic(10);

        for (BaseClient client : clients) {
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
            assertTrue(
                    incrByFloatException.getCause().getMessage().contains("value is not a valid float"));
        }
    }

    @Test
    @SneakyThrows
    public void mset_mget() {
        String key1 = RandomStringUtils.randomAlphabetic(10);
        String key2 = RandomStringUtils.randomAlphabetic(10);
        String key3 = RandomStringUtils.randomAlphabetic(10);
        String nonExisting = RandomStringUtils.randomAlphabetic(10);
        String value = RandomStringUtils.randomAlphabetic(10);
        HashMap<String, String> keyValueMap =
                new HashMap<>() {
                    {
                        put(key1, value);
                        put(key2, value);
                        put(key3, value);
                    }
                };

        for (BaseClient client : clients) {
            assertEquals(OK, client.mset(keyValueMap).get(10, SECONDS));
            assertArrayEquals(
                    new String[] {value, value, null, value},
                    client.mget(new String[] {key1, key2, nonExisting, key3}).get(10, SECONDS));
        }
    }
}
