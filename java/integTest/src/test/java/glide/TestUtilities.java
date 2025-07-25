/** Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import static glide.TestConfiguration.AZ_CLUSTER_HOSTS;
import static glide.TestConfiguration.CLUSTER_HOSTS;
import static glide.TestConfiguration.STANDALONE_HOSTS;
import static glide.TestConfiguration.TLS;
import static glide.api.BaseClient.OK;
import static glide.api.models.GlideString.gs;
import static glide.api.models.configuration.RequestRoutingConfiguration.SimpleSingleNodeRoute.RANDOM;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import glide.api.BaseClient;
import glide.api.GlideClient;
import glide.api.GlideClusterClient;
import glide.api.models.ClusterValue;
import glide.api.models.GlideString;
import glide.api.models.commands.InfoOptions.Section;
import glide.api.models.configuration.GlideClientConfiguration;
import glide.api.models.configuration.GlideClusterClientConfiguration;
import glide.api.models.configuration.NodeAddress;
import glide.cluster.ValkeyCluster;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtilities {
    /** Key names for versions returned in info command. */
    private static final String VALKEY_VERSION_KEY = "valkey_version";

    private static final String REDIS_VERSION_KEY = "redis_version";

    /**
     * Creates a Glide client for testing purposes
     *
     * @param addresses Optional list of node addresses
     * @param valkeyCluster Optional ValkeyCluster instance
     * @param lazyConnect Whether to connect lazily
     * @return A BaseClient that resolves to either a GlideClient or GlideClusterClient
     */
    @SneakyThrows
    public static BaseClient createDedicatedClient(
            boolean clusterMode,
            List<NodeAddress> addresses,
            ValkeyCluster valkeyCluster,
            Boolean lazyConnect) {

        if (valkeyCluster == null) {
            throw new IllegalArgumentException(
                    "ValkeyCluster instance is required for create dedicated client");
        }

        // For cluster mode, select k random seed nodes (k = min(3, total nodes))
        if (clusterMode) {
            List<NodeAddress> seedNodes = addresses;
            if (seedNodes == null) {
                List<NodeAddress> allNodes = valkeyCluster.getNodesAddr();
                int k = Math.min(3, allNodes.size());
                seedNodes =
                        new Random()
                                .ints(0, allNodes.size())
                                .distinct()
                                .limit(k)
                                .mapToObj(allNodes::get)
                                .collect(Collectors.toList());
            }

            return GlideClusterClient.createClient(
                            GlideClusterClientConfiguration.builder()
                                    .addresses(seedNodes)
                                    .requestTimeout(2000)
                                    .lazyConnect(lazyConnect)
                                    .build())
                    .get();
        } else {
            List<NodeAddress> nodeAddresses =
                    addresses != null ? addresses : valkeyCluster.getNodesAddr();

            return GlideClient.createClient(
                            GlideClientConfiguration.builder()
                                    .addresses(nodeAddresses)
                                    .requestTimeout(2000)
                                    .lazyConnect(lazyConnect)
                                    .build())
                    .get();
        }
    }

    /** Extract integer parameter value from INFO command output */
    public static long getValueFromInfo(String data, String value) {
        for (var line : data.split("\r\n")) {
            if (line.contains(value)) {
                return Long.parseLong(line.split(":")[1]);
            }
        }
        fail();
        return 0;
    }

    /** Extract first value from {@link ClusterValue} assuming it contains a multi-value. */
    public static <T> T getFirstEntryFromMultiValue(ClusterValue<T> data) {
        return data.getMultiValue().get(data.getMultiValue().keySet().toArray(String[]::new)[0]);
    }

    /** Generates a random string of a specified length using ASCII letters. */
    public static String getRandomString(int length) {
        String asciiLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(asciiLetters.length());
            char randomChar = asciiLetters.charAt(index);
            sb.append(randomChar);
        }

        return sb.toString();
    }

    /**
     * Transforms server info string into a Map, using lines with ":" to create key-value pairs,
     * replacing duplicates with the last encountered value.
     */
    public static Map<String, String> parseInfoResponseToMap(String serverInfo) {
        return serverInfo
                .lines()
                .filter(line -> line.contains(":"))
                .map(line -> line.split(":", 2))
                .collect(
                        Collectors.toMap(
                                parts -> parts[0],
                                parts -> parts[1],
                                (existingValue, newValue) -> newValue,
                                LinkedHashMap::new));
    }

    // copied from glide.utils.ArrayTransformUtils.concatenateArrays, because it is not exported
    /**
     * Concatenates multiple arrays of type T and returns a single concatenated array.
     *
     * @param arrays Varargs parameter for arrays to be concatenated.
     * @param <T> The type of the elements in the arrays.
     * @return A concatenated array of type T.
     */
    @SafeVarargs
    public static <T> T[] concatenateArrays(T[]... arrays) {
        return Stream.of(arrays).flatMap(Stream::of).toArray(size -> Arrays.copyOf(arrays[0], size));
    }

    public static GlideClientConfiguration.GlideClientConfigurationBuilder<?, ?>
            commonClientConfig() {
        var builder = GlideClientConfiguration.builder();
        for (var host : STANDALONE_HOSTS) {
            var parts = host.split(":");
            builder.address(
                    NodeAddress.builder().host(parts[0]).port(Integer.parseInt(parts[1])).build());
        }
        return builder.useTLS(TLS);
    }

    public static GlideClusterClientConfiguration.GlideClusterClientConfigurationBuilder<?, ?>
            commonClusterClientConfig() {
        var builder = GlideClusterClientConfiguration.builder();
        for (var host : CLUSTER_HOSTS) {
            var parts = host.split(":");
            builder.address(
                    NodeAddress.builder().host(parts[0]).port(Integer.parseInt(parts[1])).build());
        }
        return builder.useTLS(TLS);
    }

    public static GlideClusterClientConfiguration.GlideClusterClientConfigurationBuilder<?, ?>
            azClusterClientConfig() {
        var builder = GlideClusterClientConfiguration.builder();
        for (var host : AZ_CLUSTER_HOSTS) {
            var parts = host.split(":");
            builder.address(
                    NodeAddress.builder().host(parts[0]).port(Integer.parseInt(parts[1])).build());
        }
        return builder.useTLS(TLS);
    }

    /**
     * Deep traverse and compare two objects, including comparing content of all nested collections
     * recursively. Floating point numbers comparison performed with <code>1e-6</code> delta.
     *
     * @apiNote <code>Map</code> and <code>Set</code> comparison ignores element order.<br>
     *     <code>List</code> and <code>Array</code> comparison is order-sensitive.
     */
    public static void assertDeepEquals(Object expected, Object actual) {
        if (expected == null || actual == null) {
            assertEquals(expected, actual);
        } else if (expected.getClass().isArray()) {
            var expectedArray = (Object[]) expected;
            var actualArray = (Object[]) actual;
            assertEquals(expectedArray.length, actualArray.length);
            for (int i = 0; i < expectedArray.length; i++) {
                assertDeepEquals(expectedArray[i], actualArray[i]);
            }
        } else if (expected instanceof List) {
            var expectedList = (List<?>) expected;
            var actualList = (List<?>) actual;
            assertEquals(expectedList.size(), actualList.size());
            for (int i = 0; i < expectedList.size(); i++) {
                assertDeepEquals(expectedList.get(i), actualList.get(i));
            }
        } else if (expected instanceof Set) {
            var expectedSet = (Set<?>) expected;
            var actualSet = (Set<?>) actual;
            assertEquals(expectedSet.size(), actualSet.size());
            assertTrue(expectedSet.containsAll(actualSet) && actualSet.containsAll(expectedSet));
        } else if (expected instanceof Map) {
            var expectedMap = (Map<?, ?>) expected;
            var actualMap = (Map<?, ?>) actual;
            assertEquals(expectedMap.size(), actualMap.size());
            for (var key : expectedMap.keySet()) {
                assertDeepEquals(expectedMap.get(key), actualMap.get(key));
            }
        } else if (expected instanceof Double || actual instanceof Double) {
            assertEquals((Double) expected, (Double) actual, 1e-6);
        } else {
            assertEquals(expected, actual);
        }
    }

    /**
     * Validate whether `FUNCTION LIST` response contains required info.
     *
     * @param response The response from valkey.
     * @param libName Expected library name.
     * @param functionDescriptions Expected function descriptions. Key - function name, value -
     *     description.
     * @param functionFlags Expected function flags. Key - function name, value - flags set.
     * @param libCode Expected library to check if given.
     */
    @SuppressWarnings("unchecked")
    public static void checkFunctionListResponse(
            Map<String, Object>[] response,
            String libName,
            Map<String, String> functionDescriptions,
            Map<String, Set<String>> functionFlags,
            Optional<String> libCode) {
        assertTrue(response.length > 0);
        boolean hasLib = false;
        for (var lib : response) {
            hasLib = lib.containsValue(libName);
            if (hasLib) {
                var functions = (Object[]) lib.get("functions");
                assertEquals(functionDescriptions.size(), functions.length);
                for (var functionInfo : functions) {
                    var function = (Map<String, Object>) functionInfo;
                    var functionName = (String) function.get("name");
                    assertEquals(functionDescriptions.get(functionName), function.get("description"));
                    assertEquals(functionFlags.get(functionName), function.get("flags"));
                }
                if (libCode.isPresent()) {
                    assertEquals(libCode.get(), lib.get("library_code"));
                }
                break;
            }
        }
        assertTrue(hasLib);
    }

    private <T> void assertSetsEqual(Set<T> expected, Set<T> actual) {
        // Convert both sets to lists. It is needed due to issue that rust return the flags as string
        List<GlideString> expectedList =
                expected.stream().sorted().map(GlideString::of).collect(Collectors.toList());
        List<GlideString> actualList =
                actual.stream().sorted().map(GlideString::of).collect(Collectors.toList());

        assertEquals(expectedList, actualList);
    }

    /**
     * Validate whether `FUNCTION LIST` response contains required info.
     *
     * @param response The response from valkey.
     * @param libName Expected library name.
     * @param functionDescriptions Expected function descriptions. Key - function name, value -
     *     description.
     * @param functionFlags Expected function flags. Key - function name, value - flags set.
     * @param libCode Expected library to check if given.
     */
    @SuppressWarnings("unchecked")
    public static void checkFunctionListResponseBinary(
            Map<GlideString, Object>[] response,
            GlideString libName,
            Map<GlideString, GlideString> functionDescriptions,
            Map<GlideString, Set<GlideString>> functionFlags,
            Optional<GlideString> libCode) {
        assertTrue(response.length > 0);
        boolean hasLib = false;
        for (var lib : response) {
            hasLib = lib.containsValue(libName);
            if (hasLib) {
                var functions = (Object[]) lib.get(gs("functions"));
                assertEquals(functionDescriptions.size(), functions.length);
                for (var functionInfo : functions) {
                    var function = (Map<GlideString, Object>) functionInfo;
                    var functionName = (GlideString) function.get(gs("name"));
                    assertEquals(functionDescriptions.get(functionName), function.get(gs("description")));
                    assertSetsEqual(
                            functionFlags.get(functionName), (Set<GlideString>) function.get(gs("flags")));
                }
                if (libCode.isPresent()) {
                    assertEquals(libCode.get(), lib.get(gs("library_code")));
                }
                break;
            }
        }
        assertTrue(hasLib);
    }

    /**
     * Validate whether `FUNCTION STATS` response contains required info.
     *
     * @param response The response from server.
     * @param runningFunction Command line of running function expected. Empty, if nothing expected.
     * @param libCount Expected libraries count.
     * @param functionCount Expected functions count.
     */
    public static void checkFunctionStatsResponse(
            Map<String, Map<String, Object>> response,
            String[] runningFunction,
            long libCount,
            long functionCount) {
        Map<String, Object> runningScriptInfo = response.get("running_script");
        if (runningScriptInfo == null && runningFunction.length != 0) {
            fail("No running function info");
        }
        if (runningScriptInfo != null && runningFunction.length == 0) {
            String[] command = (String[]) runningScriptInfo.get("command");
            fail("Unexpected running function info: " + String.join(" ", command));
        }

        if (runningScriptInfo != null) {
            String[] command = (String[]) runningScriptInfo.get("command");
            assertArrayEquals(runningFunction, command);
            // command line format is:
            // fcall|fcall_ro <function name> <num keys> <key>* <arg>*
            assertEquals(runningFunction[1], runningScriptInfo.get("name"));
        }
        var expected =
                Map.of("LUA", Map.of("libraries_count", libCount, "functions_count", functionCount));
        assertEquals(expected, response.get("engines"));
    }

    /**
     * Validate whether `FUNCTION STATS` response contains required info.
     *
     * @param response The response from server.
     * @param runningFunction Command line of running function expected. Empty, if nothing expected.
     * @param libCount Expected libraries count.
     * @param functionCount Expected functions count.
     */
    public static void checkFunctionStatsBinaryResponse(
            Map<GlideString, Map<GlideString, Object>> response,
            GlideString[] runningFunction,
            long libCount,
            long functionCount) {
        Map<GlideString, Object> runningScriptInfo = response.get(gs("running_script"));
        if (runningScriptInfo == null && runningFunction.length != 0) {
            fail("No running function info");
        }
        if (runningScriptInfo != null && runningFunction.length == 0) {
            GlideString[] command = (GlideString[]) runningScriptInfo.get(gs("command"));
            fail("Unexpected running function info: " + String.join(" ", Arrays.toString(command)));
        }

        if (runningScriptInfo != null) {
            GlideString[] command = (GlideString[]) runningScriptInfo.get(gs("command"));
            assertArrayEquals(runningFunction, command);
            // command line format is:
            // fcall|fcall_ro <function name> <num keys> <key>* <arg>*
            assertEquals(runningFunction[1], runningScriptInfo.get(gs("name")));
        }
        var expected =
                Map.of(
                        gs("LUA"),
                        Map.of(gs("libraries_count"), libCount, gs("functions_count"), functionCount));
        assertEquals(expected, response.get(gs("engines")));
    }

    /** Generate a String of LUA library code. */
    public static String generateLuaLibCode(
            String libName, Map<String, String> functions, boolean readonly) {
        StringBuilder code = new StringBuilder("#!lua name=" + libName + "\n");
        for (var function : functions.entrySet()) {
            code.append("redis.register_function{ function_name = '")
                    .append(function.getKey())
                    .append("', callback = function(keys, args) ")
                    .append(function.getValue())
                    .append(" end");
            if (readonly) {
                code.append(", flags = { 'no-writes' }");
            }
            code.append(" }\n");
        }
        return code.toString();
    }

    /** Generate a Glidestring of LUA library code. */
    public static GlideString generateLuaLibCodeBinary(
            GlideString libName, Map<GlideString, GlideString> functions, boolean readonly) {

        Map<String, String> transformedMap =
                functions.entrySet().stream()
                        .collect(
                                Collectors.toMap(
                                        entry -> entry.getKey().toString(), entry -> entry.getValue().toString()));

        return gs(generateLuaLibCode(libName.toString(), transformedMap, readonly));
    }

    /**
     * Create a lua lib with a function which runs an endless loop up to timeout sec.<br>
     * Execution takes at least 5 sec regardless of the timeout configured.
     */
    public static String createLuaLibWithLongRunningFunction(
            String libName, String funcName, int timeout, boolean readOnly) {
        String code =
                "#!lua name=$libName\n"
                        + "local function $libName_$funcName(keys, args)\n"
                        + "  local started = tonumber(redis.pcall('time')[1])\n"
                        // fun fact - redis does no writes if 'no-writes' flag is set
                        + "  redis.pcall('set', keys[1], 42)\n"
                        + "  while (true) do\n"
                        + "    local now = tonumber(redis.pcall('time')[1])\n"
                        + "    if now > started + $timeout then\n"
                        + "      return 'Timed out $timeout sec'\n"
                        + "    end\n"
                        + "  end\n"
                        + "  return 'OK'\n"
                        + "end\n"
                        + "redis.register_function{\n"
                        + "function_name='$funcName',\n"
                        + "callback=$libName_$funcName,\n"
                        + (readOnly ? "flags={ 'no-writes' }\n" : "")
                        + "}";
        return code.replace("$timeout", Integer.toString(timeout))
                .replace("$funcName", funcName)
                .replace("$libName", libName);
    }

    /**
     * Create a lua script which runs an endless loop up to timeout sec.<br>
     * Execution takes at least 5 sec regardless of the timeout configured.
     */
    public static String createLongRunningLuaScript(int timeout, boolean readOnly) {
        String script =
                readOnly
                        ? "  local started = tonumber(redis.pcall('time')[1])\n"
                                + "  while (true) do\n"
                                + "    local now = tonumber(redis.pcall('time')[1])\n"
                                + "    if now > started + $timeout then\n"
                                + "      return 'Timed out $timeout sec'\n"
                                + "    end\n"
                                + "  end\n"
                        : "redis.call('SET', KEYS[1], 'value')\n"
                                + "  local start = redis.call('time')[1]\n"
                                + "  while redis.call('time')[1] - start < $timeout do\n"
                                + "      redis.call('SET', KEYS[1], 'value')\n"
                                + "   end\n";
        return script.replace("$timeout", Integer.toString(timeout));
    }

    /**
     * Lock test until server completes a script/function execution.
     *
     * @param lambda Client api reference to use for checking the server.
     */
    public static void waitForNotBusy(Supplier<CompletableFuture<?>> lambda) {
        // If function wasn't killed, and it didn't time out - it blocks the server and cause rest
        // test to fail.
        boolean isBusy = true;
        do {
            try {
                lambda.get().get();
            } catch (Exception busy) {
                // should throw `notbusy` error, because the function should be killed before
                if (busy.getMessage().toLowerCase().contains("notbusy")) {
                    isBusy = false;
                }
            }
        } while (isBusy);
    }

    /**
     * This method returns the server version using a glide client.
     *
     * @param client Glide client to be used for running the info command.
     * @return String The server version number.
     */
    @SneakyThrows
    public static String getServerVersion(@NonNull final BaseClient client) {
        String infoResponse =
                client instanceof GlideClient
                        ? ((GlideClient) client).info(new Section[] {Section.SERVER}).get()
                        : ((GlideClusterClient) client)
                                .info(new Section[] {Section.SERVER}, RANDOM)
                                .get()
                                .getSingleValue();
        Map<String, String> infoResponseMap = parseInfoResponseToMap(infoResponse);
        if (infoResponseMap.containsKey(VALKEY_VERSION_KEY)) {
            return infoResponseMap.get(VALKEY_VERSION_KEY);
        } else if (infoResponseMap.containsKey(REDIS_VERSION_KEY)) {
            return infoResponseMap.get(REDIS_VERSION_KEY);
        }
        return null;
    }

    /**
     * Delete an ACL user and assert it was deleted.
     *
     * @param client Glide client to be used for running the ACL DELUSER command.
     * @param username The username of the ACL user to be deleted.
     */
    @SneakyThrows
    public static void deleteAclUser(GlideClient client, String username) {
        try {
            assertEquals(1L, client.customCommand(new String[] {"ACL", "DELUSER", username}).get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Set an ACL user and a password for it.
     *
     * @param client Glide client to be used for running the ACL SETUSER command.
     * @param username The username of the ACL user to be registered.
     * @param password The password of the ACL user to be registered.
     */
    @SneakyThrows
    public static void setNewAclUserPassword(GlideClient client, String username, String password) {
        try {
            assertEquals(
                    OK,
                    client
                            .customCommand(
                                    new String[] {
                                        "ACL", "SETUSER", username, "on", ">" + password, "~*", "&*", "+@all",
                                    })
                            .get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Delete an ACL user and assert it was deleted.
     *
     * @param client Glide client to be used for running the ACL DELUSER command.
     * @param username The username of the ACL user to be deleted.
     */
    @SneakyThrows
    public static void deleteAclUser(GlideClusterClient client, String username) {
        try {
            assertEquals(
                    1L,
                    client.customCommand(new String[] {"ACL", "DELUSER", username}).get().getSingleValue());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Set an ACL user and a password for it.
     *
     * @param client Glide client to be used for running the ACL SETUSER command.
     * @param username The username of the ACL user to be registered.
     * @param password The password of the ACL user to be registered.
     */
    @SneakyThrows
    public static void setNewAclUserPassword(
            GlideClusterClient client, String username, String password) {
        try {
            assertEquals(
                    OK,
                    client
                            .customCommand(
                                    new String[] {
                                        "ACL", "SETUSER", username, "on", ">" + password, "~*", "&*", "+@all",
                                    })
                            .get()
                            .getSingleValue());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
