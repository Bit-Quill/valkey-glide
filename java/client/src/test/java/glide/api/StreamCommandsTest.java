/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api;

import static glide.api.models.commands.StreamOptions.StreamAddOptions.NO_MAKE_STREAM_REDIS_API;
import static glide.api.models.commands.StreamOptions.StreamTrimOptions.TRIM_EXACT_REDIS_API;
import static glide.api.models.commands.StreamOptions.StreamTrimOptions.TRIM_LIMIT_REDIS_API;
import static glide.api.models.commands.StreamOptions.StreamTrimOptions.TRIM_MAXLEN_REDIS_API;
import static glide.api.models.commands.StreamOptions.StreamTrimOptions.TRIM_MINID_REDIS_API;
import static glide.api.models.commands.StreamOptions.StreamTrimOptions.TRIM_NOT_EXACT_REDIS_API;
import static glide.utils.ArrayTransformUtils.convertMapToKeyValueStringArray;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static redis_request.RedisRequestOuterClass.RequestType.XAdd;
import static redis_request.RedisRequestOuterClass.RequestType.XTrim;

import glide.api.models.commands.StreamOptions.MaxLen;
import glide.api.models.commands.StreamOptions.MinId;
import glide.api.models.commands.StreamOptions.StreamAddOptions;
import glide.api.models.commands.StreamOptions.StreamTrimOptions;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class StreamCommandsTest {

    RedisClient service;

    ConnectionManager connectionManager;

    CommandManager commandManager;

    @BeforeEach
    public void setUp() {
        connectionManager = mock(ConnectionManager.class);
        commandManager = mock(CommandManager.class);
        service = new RedisClient(connectionManager, commandManager);
    }

    private static List<Arguments> getStreamAddOptions() {
        return List.of(
                Arguments.of(
                        Pair.of(
                                // no TRIM option
                                StreamAddOptions.builder().id("id").makeStream(Boolean.FALSE).build(),
                                new String[] {NO_MAKE_STREAM_REDIS_API, "id"}),
                        Pair.of(
                                // MAXLEN with LIMIT
                                StreamAddOptions.builder()
                                        .id("id")
                                        .makeStream(Boolean.TRUE)
                                        .trim(new MaxLen(5L, 10L))
                                        .build(),
                                new String[] {
                                    TRIM_MAXLEN_REDIS_API,
                                    TRIM_EXACT_REDIS_API,
                                    Long.toString(5L),
                                    TRIM_LIMIT_REDIS_API,
                                    Long.toString(10L),
                                    "id"
                                }),
                        Pair.of(
                                // MAXLEN with non exact match
                                StreamAddOptions.builder()
                                        .makeStream(Boolean.FALSE)
                                        .trim(new MaxLen(false, 2L))
                                        .build(),
                                new String[] {
                                    NO_MAKE_STREAM_REDIS_API,
                                    TRIM_MAXLEN_REDIS_API,
                                    TRIM_NOT_EXACT_REDIS_API,
                                    Long.toString(2L),
                                    "*"
                                }),
                        Pair.of(
                                // MIN ID with LIMIT
                                StreamAddOptions.builder()
                                        .id("id")
                                        .makeStream(Boolean.TRUE)
                                        .trim(new MinId("testKey", 10L))
                                        .build(),
                                new String[] {
                                    TRIM_MINID_REDIS_API,
                                    TRIM_EXACT_REDIS_API,
                                    Long.toString(5L),
                                    TRIM_LIMIT_REDIS_API,
                                    Long.toString(10L),
                                    "id"
                                }),
                        Pair.of(
                                // MIN ID with non exact match
                                StreamAddOptions.builder()
                                        .makeStream(Boolean.FALSE)
                                        .trim(new MinId(false, "testKey"))
                                        .build(),
                                new String[] {
                                    NO_MAKE_STREAM_REDIS_API,
                                    TRIM_MINID_REDIS_API,
                                    TRIM_NOT_EXACT_REDIS_API,
                                    Long.toString(5L),
                                    "*"
                                })));
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getStreamAddOptions")
    public void xadd_with_options_returns_success(Pair<StreamAddOptions, String[]> optionAndArgs) {
        // setup
        String key = "testKey";
        Map<String, String> fieldValues = new LinkedHashMap<>();
        fieldValues.put("testField1", "testValue1");
        fieldValues.put("testField2", "testValue2");
        String[] arguments =
                ArrayUtils.addFirst(
                        ArrayUtils.addAll(
                                optionAndArgs.getRight(), convertMapToKeyValueStringArray(fieldValues)),
                        key);

        String returnId = "testId";
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(returnId);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(XAdd), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.xadd(key, fieldValues, optionAndArgs.getLeft());
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(returnId, payload);
    }

    private static List<Arguments> getStreamTrimOptions() {
        return List.of(
                Arguments.of(
                        Pair.of(
                                // MAXLEN just THRESHOLD
                                new MaxLen(5L), new String[] {TRIM_MAXLEN_REDIS_API, "5"}),
                        Pair.of(
                                // MAXLEN with LIMIT
                                new MaxLen(5L, 10L),
                                new String[] {
                                    TRIM_MAXLEN_REDIS_API, TRIM_NOT_EXACT_REDIS_API, "5", TRIM_LIMIT_REDIS_API, "10"
                                }),
                        Pair.of(
                                // MAXLEN with exact
                                new MaxLen(true, 10L),
                                new String[] {TRIM_MAXLEN_REDIS_API, TRIM_EXACT_REDIS_API, "10"}),
                        Pair.of(
                                // MINID just THRESHOLD
                                new MinId("0-1"), new String[] {TRIM_MINID_REDIS_API, "0-1"}),
                        Pair.of(
                                // MINID with exact
                                new MinId(true, "0-2"),
                                new String[] {TRIM_MINID_REDIS_API, TRIM_EXACT_REDIS_API, "0-2"}),
                        Pair.of(
                                // MINID with LIMIT
                                new MinId("0-3", 10L),
                                new String[] {
                                    TRIM_MINID_REDIS_API, TRIM_NOT_EXACT_REDIS_API, "0-3", TRIM_LIMIT_REDIS_API, "10"
                                })));
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getStreamTrimOptions")
    public void xtrim_with_options_returns_success(Pair<StreamTrimOptions, String[]> optionAndArgs) {
        // setup
        String key = "testKey";
        String[] arguments = ArrayUtils.addFirst(optionAndArgs.getRight(), key);

        Long expectedPayload = 0L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(expectedPayload);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(XTrim), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.xtrim(key, optionAndArgs.getLeft());
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(expectedPayload, payload);
    }
}
