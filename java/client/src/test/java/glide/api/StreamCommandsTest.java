/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api;

import static glide.api.models.commands.Stream.StreamAddOptions.NO_MAKE_STREAM_REDIS_API;
import static glide.api.models.commands.Stream.StreamTrimOptions.TRIM_EXACT_REDIS_API;
import static glide.api.models.commands.Stream.StreamTrimOptions.TRIM_LIMIT_REDIS_API;
import static glide.api.models.commands.Stream.StreamTrimOptions.TRIM_MAXLEN_REDIS_API;
import static glide.api.models.commands.Stream.StreamTrimOptions.TRIM_MINID_REDIS_API;
import static glide.api.models.commands.Stream.StreamTrimOptions.TRIM_NOT_EXACT_REDIS_API;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;

import glide.api.models.commands.Stream.StreamAddOptions;
import glide.api.models.commands.Stream.StreamTrimOptions;
import glide.api.models.commands.Stream.StreamTrimOptions.MaxLen;
import glide.api.models.commands.Stream.StreamTrimOptions.MinId;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.List;
import lombok.SneakyThrows;
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
                        // no TRIM option
                        "test_xadd_no_trim",
                        StreamAddOptions.builder().id("id").makeStream(Boolean.FALSE).build(),
                        new String[] {NO_MAKE_STREAM_REDIS_API, "id"},
                        Arguments.of(
                                // MAXLEN with LIMIT
                                "test_xadd_maxlen_with_limit",
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
                        Arguments.of(
                                // MAXLEN with non exact match
                                "test_xadd_maxlen_with_non_exact_match",
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
                        Arguments.of(
                                // MIN ID with LIMIT
                                "test_xadd_minid_with_limit",
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
                        Arguments.of(
                                // MIN ID with non exact match
                                "test_xadd_minid_with_non_exact_match",
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
    @ParameterizedTest(name = "{0}")
    @MethodSource("getStreamAddOptions")
    public void xadd_with_options_returns_success(
            String testName, StreamAddOptions options, String[] expectedArgs) {
        assertArrayEquals(expectedArgs, options.toArgs());
    }

    private static List<Arguments> getStreamTrimOptions() {
        return List.of(
                Arguments.of(
                        // MAXLEN just THRESHOLD
                        "test_xtrim_maxlen", new MaxLen(5L), new String[] {TRIM_MAXLEN_REDIS_API, "5"}),
                Arguments.of(
                        // MAXLEN with LIMIT
                        "test_xtrim_maxlen_with_limit",
                        new MaxLen(5L, 10L),
                        new String[] {
                            TRIM_MAXLEN_REDIS_API, TRIM_NOT_EXACT_REDIS_API, "5", TRIM_LIMIT_REDIS_API, "10"
                        }),
                Arguments.of(
                        // MAXLEN with exact
                        "test_xtrim_exact_maxlen",
                        new MaxLen(true, 10L),
                        new String[] {TRIM_MAXLEN_REDIS_API, TRIM_EXACT_REDIS_API, "10"}),
                Arguments.of(
                        // MINID just THRESHOLD
                        "test_xtrim_minid", new MinId("0-1"), new String[] {TRIM_MINID_REDIS_API, "0-1"}),
                Arguments.of(
                        // MINID with exact
                        "test_xtrim_exact_minid",
                        new MinId(true, "0-2"),
                        new String[] {TRIM_MINID_REDIS_API, TRIM_EXACT_REDIS_API, "0-2"}),
                Arguments.of(
                        // MINID with LIMIT
                        "test_xtrim_minid_with_limit",
                        new MinId("0-3", 10L),
                        new String[] {
                            TRIM_MINID_REDIS_API, TRIM_NOT_EXACT_REDIS_API, "0-3", TRIM_LIMIT_REDIS_API, "10"
                        }));
    }

    @SneakyThrows
    @ParameterizedTest(name = "{0}")
    @MethodSource("getStreamTrimOptions")
    public void xtrim_with_options_returns_success(
        String testName, StreamTrimOptions options, String[] expectedArgs) {
        assertArrayEquals(expectedArgs, options.toArgs());
    }
}
