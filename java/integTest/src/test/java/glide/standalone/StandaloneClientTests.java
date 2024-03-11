/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.standalone;

import static glide.TestUtilities.commonClientConfig;
import static glide.TestUtilities.getRandomString;
import static glide.TestUtilities.parseInfoResponseToMap;
import static glide.api.BaseClient.OK;
import static glide.api.models.commands.InfoOptions.Section.SERVER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import glide.api.RedisClient;
import glide.api.models.commands.InfoOptions;
import glide.api.models.configuration.RedisCredentials;
import glide.api.models.exceptions.ClosingException;
import glide.api.models.exceptions.RequestException;
import java.lang.module.ModuleDescriptor.Version;
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
public class StandaloneClientTests {

    @SneakyThrows
    private Boolean check_if_server_version_gte(RedisClient client, String minVersion) {
        String infoStr = client.info(InfoOptions.builder().section(SERVER).build()).get();
        String redisVersion = parseInfoResponseToMap(infoStr).get("redis_version");
        assertNotNull(redisVersion);
        return Version.parse(redisVersion).compareTo(Version.parse(minVersion)) >= 0;
    }

    @SneakyThrows
    @Test
    public void register_client_name_and_version() {
        RedisClient client = RedisClient.CreateClient(commonClientConfig().build()).get();

        String minVersion = "7.2.0";
        assumeTrue(
                check_if_server_version_gte(client, minVersion), "Redis version required >= " + minVersion);
        String info = (String) client.customCommand(new String[] {"CLIENT", "INFO"}).get();
        assertTrue(info.contains("lib-name=GlideJava"));
        assertTrue(info.contains("lib-ver=unknown"));

        client.close();
    }

    @SneakyThrows
    @Test
    public void can_connect_with_auth_require_pass() {
        RedisClient client = RedisClient.CreateClient(commonClientConfig().build()).get();

        String password = "TEST_AUTH";
        client.customCommand(new String[] {"CONFIG", "SET", "requirepass", password}).get();

        // Creation of a new client without a password should fail
        ExecutionException exception =
                assertThrows(
                        ExecutionException.class,
                        () -> RedisClient.CreateClient(commonClientConfig().build()).get());
        assertTrue(exception.getCause() instanceof ClosingException);

        // Creation of a new client with credentials
        RedisClient auth_client =
                RedisClient.CreateClient(
                                commonClientConfig()
                                        .credentials(RedisCredentials.builder().password(password).build())
                                        .build())
                        .get();

        String key = getRandomString(10);
        String value = getRandomString(10);

        assertEquals(OK, auth_client.set(key, value).get());
        assertEquals(value, auth_client.get(key).get());

        // Reset password
        client.customCommand(new String[] {"CONFIG", "SET", "requirepass", ""}).get();

        auth_client.close();
        client.close();
    }

    @SneakyThrows
    @Test
    public void can_connect_with_auth_acl() {
        RedisClient client = RedisClient.CreateClient(commonClientConfig().build()).get();

        String username = "testuser";
        String password = "TEST_AUTH";
        assertEquals(
                OK,
                client
                        .customCommand(
                                new String[] {
                                    "ACL",
                                    "SETUSER",
                                    username,
                                    "on",
                                    "allkeys",
                                    "+get",
                                    "+cluster",
                                    "+ping",
                                    "+info",
                                    "+client",
                                    ">" + password,
                                })
                        .get());

        String key = getRandomString(10);
        String value = getRandomString(10);

        assertEquals(OK, client.set(key, value).get());

        // Creation of a new client with credentials
        RedisClient testUserClient =
                RedisClient.CreateClient(
                                commonClientConfig()
                                        .credentials(
                                                RedisCredentials.builder().username(username).password(password).build())
                                        .build())
                        .get();

        assertEquals(value, testUserClient.get(key).get());
        ExecutionException executionException =
                assertThrows(ExecutionException.class, () -> testUserClient.set("foo", "bar").get());
        assertTrue(executionException.getCause() instanceof RequestException);

        client.customCommand(new String[] {"ACL", "DELUSER", username}).get();

        testUserClient.close();
        client.close();
    }

    @SneakyThrows
    @Test
    public void select_standalone_database_id() {
        RedisClient client = RedisClient.CreateClient(commonClientConfig().databaseId(4).build()).get();

        String clientInfo = (String) client.customCommand(new String[] {"CLIENT", "INFO"}).get();
        assertTrue(clientInfo.contains("db=4"));

        client.close();
    }

    @SneakyThrows
    @Test
    public void client_name() {
        RedisClient client =
                RedisClient.CreateClient(commonClientConfig().clientName("TEST_CLIENT_NAME").build()).get();

        String clientInfo = (String) client.customCommand(new String[] {"CLIENT", "INFO"}).get();
        assertTrue(clientInfo.contains("name=TEST_CLIENT_NAME"));

        client.close();
    }

    @Test
    @SneakyThrows
    public void close_client_throws_ExecutionException_with_ClosingException_cause() {
        RedisClient client = RedisClient.CreateClient(commonClientConfig().build()).get();

        client.close();
        ExecutionException executionException =
                assertThrows(ExecutionException.class, () -> client.set("key", "value").get());
        assertTrue(executionException.getCause() instanceof ClosingException);
    }
}
