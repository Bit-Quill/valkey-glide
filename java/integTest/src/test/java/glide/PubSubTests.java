/** Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import static glide.TestConfiguration.REDIS_VERSION;
import static glide.TestUtilities.commonClientConfig;
import static glide.TestUtilities.commonClusterClientConfig;
import static glide.api.models.configuration.RequestRoutingConfiguration.SimpleMultiNodeRoute.ALL_NODES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import glide.api.BaseClient;
import glide.api.RedisClient;
import glide.api.RedisClusterClient;
import glide.api.models.ClusterTransaction;
import glide.api.models.PubSubMessage;
import glide.api.models.Transaction;
import glide.api.models.configuration.BaseSubscriptionConfiguration.ChannelMode;
import glide.api.models.configuration.BaseSubscriptionConfiguration.MessageCallback;
import glide.api.models.configuration.ClusterSubscriptionConfiguration;
import glide.api.models.configuration.ClusterSubscriptionConfiguration.PubSubClusterChannelMode;
import glide.api.models.configuration.StandaloneSubscriptionConfiguration;
import glide.api.models.configuration.StandaloneSubscriptionConfiguration.PubSubChannelMode;
import glide.api.models.exceptions.ConfigurationError;
import glide.api.models.exceptions.RequestException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

// @Timeout(30) // sec
public class PubSubTests {

    // TODO protocol version
    @SneakyThrows
    @SuppressWarnings("unchecked")
    private <M extends ChannelMode> BaseClient createClientWithSubscriptions(
            boolean standalone,
            Map<M, Set<String>> subscriptions,
            Optional<MessageCallback> callback,
            Optional<Object> context) {
        if (standalone) {
            var subConfigBuilder =
                    StandaloneSubscriptionConfiguration.builder()
                            .subscriptions((Map<PubSubChannelMode, Set<String>>) subscriptions);

            if (callback.isPresent()) {
                subConfigBuilder.callback(callback.get(), context.get());
            }
            return RedisClient.CreateClient(
                            commonClientConfig().subscriptionConfiguration(subConfigBuilder.build()).build())
                    .get();
        } else {
            var subConfigBuilder =
                    ClusterSubscriptionConfiguration.builder()
                            .subscriptions((Map<PubSubClusterChannelMode, Set<String>>) subscriptions);

            if (callback.isPresent()) {
                subConfigBuilder.callback(callback.get(), context.get());
            }

            return RedisClusterClient.CreateClient(
                            commonClusterClientConfig()
                                    .subscriptionConfiguration(subConfigBuilder.build())
                                    .build())
                    .get();
        }
    }

    private <M extends ChannelMode> BaseClient createClientWithSubscriptions(
            boolean standalone, Map<M, Set<String>> subscriptions) {
        return createClientWithSubscriptions(
                standalone, subscriptions, Optional.empty(), Optional.empty());
    }

    @SneakyThrows
    private BaseClient createClient(boolean standalone) {
        if (standalone) {
            return RedisClient.CreateClient(commonClientConfig().build()).get();
        }
        return RedisClusterClient.CreateClient(commonClusterClientConfig().build()).get();
    }

    /**
     * PubsubMessage queue used in callback to analyze received PubsubMessages. Number is a client ID.
     */
    private final ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>> PubsubMessageQueue =
            new ConcurrentLinkedDeque<>();

    /** Clients used in a test. */
    private final List<BaseClient> clients = new ArrayList<>();

    @BeforeEach
    @SneakyThrows
    public void cleanup() {
        for (var client : clients) {
            if (client instanceof RedisClusterClient) {
                ((RedisClusterClient) client).customCommand(new String[] {"unsubscribe"}, ALL_NODES).get();
                ((RedisClusterClient) client).customCommand(new String[] {"punsubscribe"}, ALL_NODES).get();
                ((RedisClusterClient) client).customCommand(new String[] {"sunsubscribe"}, ALL_NODES).get();
            } else {
                ((RedisClient) client).customCommand(new String[] {"unsubscribe"}).get();
                ((RedisClient) client).customCommand(new String[] {"punsubscribe"}).get();
            }
            client.close();
        }
        clients.clear();
        PubsubMessageQueue.clear();
    }

    private void verifyReceivedPubsubMessages(
        Set<Pair<Integer, PubSubMessage>> PubsubMessages, BaseClient listener, boolean callback) {
        if (callback) {
            assertEquals(PubsubMessages, new HashSet<>(PubsubMessageQueue));
        } else {
            var received = new HashSet<PubSubMessage>(PubsubMessages.size());
            PubSubMessage PubsubMessage;
            while ((PubsubMessage = listener.trygetMessage()) != null) {
                received.add(PubsubMessage);
            }
            assertEquals(
                    PubsubMessages.stream().map(Pair::getValue).collect(Collectors.toSet()), received);
        }
    }

    public static Stream<Arguments> getTwoBoolPermutations() {
        return Stream.of(
                Arguments.of(true, true),
                Arguments.of(true, false),
                Arguments.of(false, true),
                Arguments.of(false, false));
    }

    // TODO add following tests from https://github.com/aws/glide-for-redis/pull/1643
    //  test_pubsub_exact_happy_path_coexistence
    //  test_pubsub_exact_happy_path_many_channels_co_existence
    //  test_sharded_pubsub_co_existence
    //  test_pubsub_pattern_co_existence
    // TODO tests below blocked by https://github.com/aws/glide-for-redis/issues/1649
    //  test_pubsub_exact_max_size_PubsubMessage
    //  test_pubsub_sharded_max_size_PubsubMessage
    //  test_pubsub_exact_max_size_PubsubMessage_callback
    //  test_pubsub_sharded_max_size_PubsubMessage_callback

    // TODO why `publish` returns 0 on cluster or > 1 on standalone when there is only 1 receiver???
    //  meanwhile, all PubsubMessages are delivered.
    //  debug this and add checks for `publish` return value

    // TODO: remove once fixed
    private void skipTestsOnMac() {
        assumeFalse(
                System.getProperty("os.name").toLowerCase().contains("mac"),
                "PubSub doesn't work on mac OS");
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "standalone = {0}, use callback = {1}")
    @MethodSource("getTwoBoolPermutations")
    public void exact_happy_path(boolean standalone, boolean useCallback) {
        skipTestsOnMac();
        String channel = UUID.randomUUID().toString();
        String PubsubMessage = UUID.randomUUID().toString();
        var subscriptions =
                Map.of(
                        standalone ? PubSubChannelMode.EXACT : PubSubClusterChannelMode.EXACT, Set.of(channel));

        MessageCallback callback =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx).push(Pair.of(1, msg));

        var listener =
                useCallback
                        ? createClientWithSubscriptions(
                                standalone, subscriptions, Optional.of(callback), Optional.of(PubsubMessageQueue))
                        : createClientWithSubscriptions(standalone, subscriptions);
        var sender = createClient(standalone);
        clients.addAll(List.of(listener, sender));

        assertEquals(1L, sender.publish(channel, PubsubMessage).get());
        Thread.sleep(404); // deliver the PubsubMessage

        verifyReceivedPubsubMessages(
                Set.of(Pair.of(1, new PubSubMessage(PubsubMessage, channel))), listener, useCallback);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "standalone = {0}, use callback = {1}")
    @MethodSource("getTwoBoolPermutations")
    public void exact_happy_path_many_channels(boolean standalone, boolean useCallback) {
        skipTestsOnMac();
        int numChannels = 256;
        int PubsubMessagesPerChannel = 256;
        var PubsubMessages = new ArrayList<PubSubMessage>(numChannels * PubsubMessagesPerChannel);
        ChannelMode mode = standalone ? PubSubChannelMode.EXACT : PubSubClusterChannelMode.EXACT;
        Map<? extends ChannelMode, Set<String>> subscriptions = Map.of(mode, new HashSet<>());

        for (var i = 0; i < numChannels; i++) {
            var channel = UUID.randomUUID().toString();
            subscriptions.get(mode).add(channel);
            for (var j = 0; j < PubsubMessagesPerChannel; j++) {
                var PubsubMessage = UUID.randomUUID().toString();
                PubsubMessages.add(new PubSubMessage(PubsubMessage, channel));
            }
        }

        MessageCallback callback =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx).push(Pair.of(1, msg));

        var listener =
                useCallback
                        ? createClientWithSubscriptions(
                                standalone, subscriptions, Optional.of(callback), Optional.of(PubsubMessageQueue))
                        : createClientWithSubscriptions(standalone, subscriptions);
        var sender = createClient(standalone);
        clients.addAll(List.of(listener, sender));

        for (var PubsubMessage : PubsubMessages) {
            assertEquals(
                    1L, sender.publish(PubsubMessage.getChannel(), PubsubMessage.getMessage()).get());
        }

        Thread.sleep(404); // deliver the PubsubMessages

        verifyReceivedPubsubMessages(
                PubsubMessages.stream().map(m -> Pair.of(1, m)).collect(Collectors.toSet()),
                listener,
                useCallback);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "use callback = {0}")
    @ValueSource(booleans = {true, false})
    public void sharded_pubsub(boolean useCallback) {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0"), "This feature added in redis 7");
        skipTestsOnMac();

        String channel = UUID.randomUUID().toString();
        String PubsubMessage = UUID.randomUUID().toString();
        var subscriptions = Map.of(PubSubClusterChannelMode.SHARDED, Set.of(channel));

        MessageCallback callback =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx).push(Pair.of(1, msg));

        var listener =
                (RedisClusterClient)
                        (useCallback
                                ? createClientWithSubscriptions(
                                        false, subscriptions, Optional.of(callback), Optional.of(PubsubMessageQueue))
                                : createClientWithSubscriptions(false, subscriptions));
        var sender = (RedisClusterClient) createClient(false);
        clients.addAll(List.of(listener, sender));

        assertEquals(1L, sender.spublish(channel, PubsubMessage).get());
        Thread.sleep(404); // deliver the PubsubMessage

        verifyReceivedPubsubMessages(
                Set.of(Pair.of(1, new PubSubMessage(PubsubMessage, channel))), listener, useCallback);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "use callback = {0}")
    @ValueSource(booleans = {true, false})
    public void sharded_pubsub_many_channels(boolean useCallback) {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0"), "This feature added in redis 7");
        skipTestsOnMac();

        int numChannels = 256;
        int PubsubMessagesPerChannel = 256;
        var PubsubMessages = new ArrayList<PubSubMessage>(numChannels * PubsubMessagesPerChannel);
        PubSubClusterChannelMode mode = PubSubClusterChannelMode.SHARDED;
        Map<PubSubClusterChannelMode, Set<String>> subscriptions = Map.of(mode, new HashSet<>());

        for (var i = 0; i < numChannels; i++) {
            var channel = UUID.randomUUID().toString();
            subscriptions.get(mode).add(channel);
            for (var j = 0; j < PubsubMessagesPerChannel; j++) {
                var PubsubMessage = UUID.randomUUID().toString();
                PubsubMessages.add(new PubSubMessage(PubsubMessage, channel));
            }
        }

        MessageCallback callback =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx).push(Pair.of(1, msg));

        var listener =
                (RedisClusterClient)
                        (useCallback
                                ? createClientWithSubscriptions(
                                        false, subscriptions, Optional.of(callback), Optional.of(PubsubMessageQueue))
                                : createClientWithSubscriptions(false, subscriptions));
        var sender = (RedisClusterClient) createClient(false);
        clients.addAll(List.of(listener, sender));

        for (var PubsubMessage : PubsubMessages) {
            assertEquals(
                    1L, sender.spublish(PubsubMessage.getChannel(), PubsubMessage.getMessage()).get());
        }
        assertEquals(
                0L, sender.spublish(UUID.randomUUID().toString(), UUID.randomUUID().toString()).get());

        Thread.sleep(404); // deliver the PubsubMessages

        verifyReceivedPubsubMessages(
                PubsubMessages.stream().map(m -> Pair.of(1, m)).collect(Collectors.toSet()),
                listener,
                useCallback);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "standalone = {0}, use callback = {1}")
    @MethodSource("getTwoBoolPermutations")
    public void pattern(boolean standalone, boolean useCallback) {
        skipTestsOnMac();
        String prefix = "channel.";
        String pattern = prefix + "*";
        Map<String, String> PubsubMessage2channels =
                Map.of(
                        prefix + "1", UUID.randomUUID().toString(), prefix + "2", UUID.randomUUID().toString());
        var subscriptions =
                Map.of(
                        standalone ? PubSubChannelMode.PATTERN : PubSubClusterChannelMode.PATTERN,
                        Set.of(pattern));

        MessageCallback callback =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx).push(Pair.of(1, msg));

        var listener =
                useCallback
                        ? createClientWithSubscriptions(
                                standalone, subscriptions, Optional.of(callback), Optional.of(PubsubMessageQueue))
                        : createClientWithSubscriptions(standalone, subscriptions);
        var sender = createClient(standalone);
        clients.addAll(List.of(listener, sender));

        Thread.sleep(404); // need some time to propagate subscriptions - why?

        for (var entry : PubsubMessage2channels.entrySet()) {
            sender.publish(entry.getKey(), entry.getValue()).get();
        }
        assertEquals(0L, sender.publish("channel", UUID.randomUUID().toString()).get());
        Thread.sleep(404); // deliver the PubsubMessages

        var expected =
                PubsubMessage2channels.entrySet().stream()
                        .map(e -> Pair.of(1, new PubSubMessage(e.getValue(), e.getKey(), pattern)))
                        .collect(Collectors.toSet());

        verifyReceivedPubsubMessages(expected, listener, useCallback);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "standalone = {0}, use callback = {1}")
    @MethodSource("getTwoBoolPermutations")
    public void pattern_many_channels(boolean standalone, boolean useCallback) {
        skipTestsOnMac();
        String prefix = "channel.";
        String pattern = prefix + "*";
        int numChannels = 256;
        int PubsubMessagesPerChannel = 256;
        ChannelMode mode = standalone ? PubSubChannelMode.PATTERN : PubSubClusterChannelMode.PATTERN;
        var PubsubMessages = new ArrayList<PubSubMessage>(numChannels * PubsubMessagesPerChannel);
        var subscriptions = Map.of(mode, Set.of(pattern));

        for (var i = 0; i < numChannels; i++) {
            var channel = prefix + UUID.randomUUID();
            for (var j = 0; j < PubsubMessagesPerChannel; j++) {
                var PubsubMessage = UUID.randomUUID().toString();
                PubsubMessages.add(new PubSubMessage(PubsubMessage, channel, pattern));
            }
        }

        MessageCallback callback =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx).push(Pair.of(1, msg));

        var listener =
                useCallback
                        ? createClientWithSubscriptions(
                                standalone, subscriptions, Optional.of(callback), Optional.of(PubsubMessageQueue))
                        : createClientWithSubscriptions(standalone, subscriptions);
        var sender = createClient(standalone);
        clients.addAll(List.of(listener, sender));

        Thread.sleep(404); // need some time to propagate subscriptions - why?

        for (var PubsubMessage : PubsubMessages) {
            sender.publish(PubsubMessage.getChannel(), PubsubMessage.getMessage()).get();
        }
        assertEquals(0L, sender.publish("channel", UUID.randomUUID().toString()).get());
        Thread.sleep(404); // deliver the PubsubMessages

        verifyReceivedPubsubMessages(
                PubsubMessages.stream().map(m -> Pair.of(1, m)).collect(Collectors.toSet()),
                listener,
                useCallback);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "standalone = {0}, use callback = {1}")
    @MethodSource("getTwoBoolPermutations")
    public void combined_exact_and_pattern_one_client(boolean standalone, boolean useCallback) {
        skipTestsOnMac();
        String prefix = "channel.";
        String pattern = prefix + "*";
        int numChannels = 256;
        int PubsubMessagesPerChannel = 256;
        var PubsubMessages = new ArrayList<PubSubMessage>(numChannels * PubsubMessagesPerChannel);
        ChannelMode mode = standalone ? PubSubChannelMode.EXACT : PubSubClusterChannelMode.EXACT;
        Map<? extends ChannelMode, Set<String>> subscriptions =
                Map.of(
                        mode,
                        new HashSet<>(),
                        standalone ? PubSubChannelMode.PATTERN : PubSubClusterChannelMode.PATTERN,
                        Set.of(pattern));

        for (var i = 0; i < numChannels; i++) {
            var channel = UUID.randomUUID().toString();
            subscriptions.get(mode).add(channel);
            for (var j = 0; j < PubsubMessagesPerChannel; j++) {
                var PubsubMessage = UUID.randomUUID().toString();
                PubsubMessages.add(new PubSubMessage(PubsubMessage, channel));
            }
        }

        for (var j = 0; j < PubsubMessagesPerChannel; j++) {
            var PubsubMessage = UUID.randomUUID().toString();
            var channel = prefix + UUID.randomUUID();
            PubsubMessages.add(new PubSubMessage(PubsubMessage, channel, pattern));
        }

        MessageCallback callback =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx).push(Pair.of(1, msg));

        var listener =
                useCallback
                        ? createClientWithSubscriptions(
                                standalone, subscriptions, Optional.of(callback), Optional.of(PubsubMessageQueue))
                        : createClientWithSubscriptions(standalone, subscriptions);
        var sender = createClient(standalone);
        clients.addAll(List.of(listener, sender));

        for (var PubsubMessage : PubsubMessages) {
            sender.publish(PubsubMessage.getChannel(), PubsubMessage.getMessage()).get();
        }

        Thread.sleep(404); // deliver the PubsubMessages

        verifyReceivedPubsubMessages(
                PubsubMessages.stream().map(m -> Pair.of(1, m)).collect(Collectors.toSet()),
                listener,
                useCallback);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "standalone = {0}, use callback = {1}")
    @MethodSource("getTwoBoolPermutations")
    public void combined_exact_and_pattern_multiple_clients(boolean standalone, boolean useCallback) {
        skipTestsOnMac();
        String prefix = "channel.";
        String pattern = prefix + "*";
        int numChannels = 256;
        var PubsubMessages = new ArrayList<PubSubMessage>(numChannels * 2);
        ChannelMode mode = standalone ? PubSubChannelMode.EXACT : PubSubClusterChannelMode.EXACT;
        Map<? extends ChannelMode, Set<String>> subscriptions = Map.of(mode, new HashSet<>());

        for (var i = 0; i < numChannels; i++) {
            var channel = UUID.randomUUID().toString();
            subscriptions.get(mode).add(channel);
            var PubsubMessage = UUID.randomUUID().toString();
            PubsubMessages.add(new PubSubMessage(PubsubMessage, channel));
        }

        for (var j = 0; j < numChannels; j++) {
            var PubsubMessage = UUID.randomUUID().toString();
            var channel = prefix + UUID.randomUUID();
            PubsubMessages.add(new PubSubMessage(PubsubMessage, channel, pattern));
        }

        MessageCallback callbackExactSub =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx).push(Pair.of(1, msg));

        MessageCallback callbackPatternSub =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx).push(Pair.of(2, msg));

        var listenerExactSub =
                useCallback
                        ? createClientWithSubscriptions(
                                standalone,
                                subscriptions,
                                Optional.of(callbackExactSub),
                                Optional.of(PubsubMessageQueue))
                        : createClientWithSubscriptions(standalone, subscriptions);
        subscriptions =
                Map.of(
                        standalone ? PubSubChannelMode.PATTERN : PubSubClusterChannelMode.PATTERN,
                        Set.of(pattern));
        var listenerPatternSub =
                useCallback
                        ? createClientWithSubscriptions(
                                standalone,
                                subscriptions,
                                Optional.of(callbackPatternSub),
                                Optional.of(PubsubMessageQueue))
                        : createClientWithSubscriptions(standalone, subscriptions);
        var sender = createClient(standalone);
        clients.addAll(List.of(listenerExactSub, listenerPatternSub, sender));

        for (var PubsubMessage : PubsubMessages) {
            sender.publish(PubsubMessage.getChannel(), PubsubMessage.getMessage()).get();
        }

        Thread.sleep(404); // deliver the PubsubMessages

        if (useCallback) {
            verifyReceivedPubsubMessages(
                    PubsubMessages.stream()
                            .map(m -> Pair.of(m.getPattern().isEmpty() ? 1 : 2, m))
                            .collect(Collectors.toSet()),
                    listenerExactSub,
                    useCallback);
        } else {
            verifyReceivedPubsubMessages(
                    PubsubMessages.stream()
                            .filter(m -> m.getPattern().isEmpty())
                            .map(m -> Pair.of(1, m))
                            .collect(Collectors.toSet()),
                    listenerExactSub,
                    useCallback);
            verifyReceivedPubsubMessages(
                    PubsubMessages.stream()
                            .filter(m -> m.getPattern().isPresent())
                            .map(m -> Pair.of(2, m))
                            .collect(Collectors.toSet()),
                    listenerPatternSub,
                    useCallback);
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "use callback = {0}")
    @ValueSource(booleans = {true, false})
    public void combined_exact_pattern_and_sharded_one_client(boolean useCallback) {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0"), "This feature added in redis 7");
        skipTestsOnMac();

        String prefix = "channel.";
        String pattern = prefix + "*";
        String shardPrefix = "{shard}";
        int numChannels = 256;
        var PubsubMessages = new ArrayList<PubSubMessage>(numChannels * 2);
        var shardedPubsubMessages = new ArrayList<PubSubMessage>(numChannels);
        Map<PubSubClusterChannelMode, Set<String>> subscriptions =
                Map.of(
                        PubSubClusterChannelMode.EXACT, new HashSet<>(),
                        PubSubClusterChannelMode.PATTERN, Set.of(pattern),
                        PubSubClusterChannelMode.SHARDED, new HashSet<>());

        for (var i = 0; i < numChannels; i++) {
            var channel = UUID.randomUUID().toString();
            subscriptions.get(PubSubClusterChannelMode.EXACT).add(channel);
            var PubsubMessage = UUID.randomUUID().toString();
            PubsubMessages.add(new PubSubMessage(PubsubMessage, channel));
        }

        for (var i = 0; i < numChannels; i++) {
            var channel = shardPrefix + UUID.randomUUID();
            subscriptions.get(PubSubClusterChannelMode.SHARDED).add(channel);
            var PubsubMessage = UUID.randomUUID().toString();
            shardedPubsubMessages.add(new PubSubMessage(PubsubMessage, channel));
        }

        for (var j = 0; j < numChannels; j++) {
            var PubsubMessage = UUID.randomUUID().toString();
            var channel = prefix + UUID.randomUUID();
            PubsubMessages.add(new PubSubMessage(PubsubMessage, channel, pattern));
        }

        MessageCallback callback =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx).push(Pair.of(1, msg));

        var listener =
                useCallback
                        ? createClientWithSubscriptions(
                                false, subscriptions, Optional.of(callback), Optional.of(PubsubMessageQueue))
                        : createClientWithSubscriptions(false, subscriptions);
        var sender = (RedisClusterClient) createClient(false);
        clients.addAll(List.of(listener, sender));

        for (var PubsubMessage : PubsubMessages) {
            sender.publish(PubsubMessage.getChannel(), PubsubMessage.getMessage()).get();
        }
        for (var PubsubMessage : shardedPubsubMessages) {
            assertEquals(
                    1L, sender.spublish(PubsubMessage.getChannel(), PubsubMessage.getMessage()).get());
        }

        Thread.sleep(404); // deliver the PubsubMessages

        PubsubMessages.addAll(shardedPubsubMessages);
        verifyReceivedPubsubMessages(
                PubsubMessages.stream().map(m -> Pair.of(1, m)).collect(Collectors.toSet()),
                listener,
                useCallback);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "use callback = {0}")
    @ValueSource(booleans = {true, false})
    public void combined_exact_pattern_and_sharded_multi_client(boolean useCallback) {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0"), "This feature added in redis 7");
        skipTestsOnMac();

        String prefix = "channel.";
        String pattern = prefix + "*";
        String shardPrefix = "{shard}";
        int numChannels = 256;
        var exactPubsubMessages = new ArrayList<PubSubMessage>(numChannels);
        var patternPubsubMessages = new ArrayList<PubSubMessage>(numChannels);
        var shardedPubsubMessages = new ArrayList<PubSubMessage>(numChannels);
        Map<PubSubClusterChannelMode, Set<String>> subscriptionsExact =
                Map.of(PubSubClusterChannelMode.EXACT, new HashSet<>());
        Map<PubSubClusterChannelMode, Set<String>> subscriptionsPattern =
                Map.of(PubSubClusterChannelMode.PATTERN, Set.of(pattern));
        Map<PubSubClusterChannelMode, Set<String>> subscriptionsSharded =
                Map.of(PubSubClusterChannelMode.SHARDED, new HashSet<>());

        for (var i = 0; i < numChannels; i++) {
            var channel = UUID.randomUUID().toString();
            subscriptionsExact.get(PubSubClusterChannelMode.EXACT).add(channel);
            var PubsubMessage = UUID.randomUUID().toString();
            exactPubsubMessages.add(new PubSubMessage(PubsubMessage, channel));
        }

        for (var i = 0; i < numChannels; i++) {
            var channel = shardPrefix + UUID.randomUUID();
            subscriptionsSharded.get(PubSubClusterChannelMode.SHARDED).add(channel);
            var PubsubMessage = UUID.randomUUID().toString();
            shardedPubsubMessages.add(new PubSubMessage(PubsubMessage, channel));
        }

        for (var j = 0; j < numChannels; j++) {
            var PubsubMessage = UUID.randomUUID().toString();
            var channel = prefix + UUID.randomUUID();
            patternPubsubMessages.add(new PubSubMessage(PubsubMessage, channel, pattern));
        }

        MessageCallback callbackExact =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx)
                                .push(Pair.of(PubSubClusterChannelMode.EXACT.ordinal(), msg));

        MessageCallback callbackPattern =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx)
                                .push(Pair.of(PubSubClusterChannelMode.PATTERN.ordinal(), msg));

        MessageCallback callbackSharded =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx)
                                .push(Pair.of(PubSubClusterChannelMode.SHARDED.ordinal(), msg));

        var listenerExact =
                useCallback
                        ? createClientWithSubscriptions(
                                false,
                                subscriptionsExact,
                                Optional.of(callbackExact),
                                Optional.of(PubsubMessageQueue))
                        : createClientWithSubscriptions(false, subscriptionsExact);
        var listenerPattern =
                useCallback
                        ? createClientWithSubscriptions(
                                false,
                                subscriptionsPattern,
                                Optional.of(callbackPattern),
                                Optional.of(PubsubMessageQueue))
                        : createClientWithSubscriptions(false, subscriptionsPattern);
        var listenerSharded =
                useCallback
                        ? createClientWithSubscriptions(
                                false,
                                subscriptionsSharded,
                                Optional.of(callbackSharded),
                                Optional.of(PubsubMessageQueue))
                        : createClientWithSubscriptions(false, subscriptionsSharded);
        var sender = (RedisClusterClient) createClient(false);
        clients.addAll(List.of(listenerExact, listenerPattern, listenerSharded, sender));

        for (var PubsubMessage : exactPubsubMessages) {
            sender.publish(PubsubMessage.getChannel(), PubsubMessage.getMessage()).get();
        }
        for (var PubsubMessage : patternPubsubMessages) {
            sender.publish(PubsubMessage.getChannel(), PubsubMessage.getMessage()).get();
        }
        for (var PubsubMessage : shardedPubsubMessages) {
            assertEquals(
                    1L, sender.spublish(PubsubMessage.getChannel(), PubsubMessage.getMessage()).get());
        }

        Thread.sleep(404); // deliver the PubsubMessages

        if (useCallback) {
            var expected = new HashSet<Pair<Integer, PubSubMessage>>();
            expected.addAll(
                    exactPubsubMessages.stream()
                            .map(m -> Pair.of(PubSubClusterChannelMode.EXACT.ordinal(), m))
                            .collect(Collectors.toSet()));
            expected.addAll(
                    patternPubsubMessages.stream()
                            .map(m -> Pair.of(PubSubClusterChannelMode.PATTERN.ordinal(), m))
                            .collect(Collectors.toSet()));
            expected.addAll(
                    shardedPubsubMessages.stream()
                            .map(m -> Pair.of(PubSubClusterChannelMode.SHARDED.ordinal(), m))
                            .collect(Collectors.toSet()));

            verifyReceivedPubsubMessages(expected, listenerExact, useCallback);
        } else {
            verifyReceivedPubsubMessages(
                    exactPubsubMessages.stream()
                            .map(m -> Pair.of(PubSubClusterChannelMode.EXACT.ordinal(), m))
                            .collect(Collectors.toSet()),
                    listenerExact,
                    useCallback);
            verifyReceivedPubsubMessages(
                    patternPubsubMessages.stream()
                            .map(m -> Pair.of(PubSubClusterChannelMode.PATTERN.ordinal(), m))
                            .collect(Collectors.toSet()),
                    listenerPattern,
                    useCallback);
            verifyReceivedPubsubMessages(
                    shardedPubsubMessages.stream()
                            .map(m -> Pair.of(PubSubClusterChannelMode.SHARDED.ordinal(), m))
                            .collect(Collectors.toSet()),
                    listenerSharded,
                    useCallback);
        }
    }

    @SneakyThrows
    @Test
    public void three_publishing_clients_same_name_with_sharded_no_callback() {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0"), "This feature added in redis 7");
        skipTestsOnMac();

        String channel = UUID.randomUUID().toString();
        var exactPubsubMessage = new PubSubMessage(UUID.randomUUID().toString(), channel);
        var patternPubsubMessage = new PubSubMessage(UUID.randomUUID().toString(), channel, channel);
        var shardedPubsubMessage = new PubSubMessage(UUID.randomUUID().toString(), channel);
        Map<PubSubClusterChannelMode, Set<String>> subscriptionsExact =
                Map.of(PubSubClusterChannelMode.EXACT, Set.of(channel));
        Map<PubSubClusterChannelMode, Set<String>> subscriptionsPattern =
                Map.of(PubSubClusterChannelMode.PATTERN, Set.of(channel));
        Map<PubSubClusterChannelMode, Set<String>> subscriptionsSharded =
                Map.of(PubSubClusterChannelMode.SHARDED, Set.of(channel));

        var listenerExact =
                (RedisClusterClient) createClientWithSubscriptions(false, subscriptionsExact);
        var listenerPattern =
                (RedisClusterClient) createClientWithSubscriptions(false, subscriptionsPattern);
        var listenerSharded =
                (RedisClusterClient) createClientWithSubscriptions(false, subscriptionsSharded);
        clients.addAll(List.of(listenerExact, listenerPattern, listenerSharded));

        assertEquals(2L, listenerPattern.publish(channel, exactPubsubMessage.getMessage()).get());
        assertEquals(2L, listenerSharded.publish(channel, patternPubsubMessage.getMessage()).get());
        assertEquals(1L, listenerExact.spublish(channel, shardedPubsubMessage.getMessage()).get());

        Thread.sleep(404); // deliver the PubsubMessages

        verifyReceivedPubsubMessages(
                Set.of(
                        Pair.of(PubSubClusterChannelMode.EXACT.ordinal(), exactPubsubMessage),
                        Pair.of(
                                PubSubClusterChannelMode.EXACT.ordinal(),
                                new PubSubMessage(patternPubsubMessage.getMessage(), channel))),
                listenerExact,
                false);
        verifyReceivedPubsubMessages(
                Set.of(
                        Pair.of(PubSubClusterChannelMode.PATTERN.ordinal(), patternPubsubMessage),
                        Pair.of(
                                PubSubClusterChannelMode.PATTERN.ordinal(),
                                new PubSubMessage(exactPubsubMessage.getMessage(), channel, channel))),
                listenerPattern,
                false);
        verifyReceivedPubsubMessages(
                Set.of(Pair.of(PubSubClusterChannelMode.SHARDED.ordinal(), shardedPubsubMessage)),
                listenerSharded,
                false);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @Test
    public void three_publishing_clients_same_name_with_sharded_with_callback() {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0"), "This feature added in redis 7");
        skipTestsOnMac();

        String channel = UUID.randomUUID().toString();
        var exactPubsubMessage = new PubSubMessage(UUID.randomUUID().toString(), channel);
        var patternPubsubMessage = new PubSubMessage(UUID.randomUUID().toString(), channel, channel);
        var shardedPubsubMessage = new PubSubMessage(UUID.randomUUID().toString(), channel);
        Map<PubSubClusterChannelMode, Set<String>> subscriptionsExact =
                Map.of(PubSubClusterChannelMode.EXACT, Set.of(channel));
        Map<PubSubClusterChannelMode, Set<String>> subscriptionsPattern =
                Map.of(PubSubClusterChannelMode.PATTERN, Set.of(channel));
        Map<PubSubClusterChannelMode, Set<String>> subscriptionsSharded =
                Map.of(PubSubClusterChannelMode.SHARDED, Set.of(channel));

        MessageCallback callbackExact =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx)
                                .push(Pair.of(PubSubClusterChannelMode.EXACT.ordinal(), msg));

        MessageCallback callbackPattern =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx)
                                .push(Pair.of(PubSubClusterChannelMode.PATTERN.ordinal(), msg));

        MessageCallback callbackSharded =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx)
                                .push(Pair.of(PubSubClusterChannelMode.SHARDED.ordinal(), msg));

        var listenerExact =
                (RedisClusterClient)
                        createClientWithSubscriptions(
                                false,
                                subscriptionsExact,
                                Optional.of(callbackExact),
                                Optional.of(PubsubMessageQueue));
        var listenerPattern =
                (RedisClusterClient)
                        createClientWithSubscriptions(
                                false,
                                subscriptionsPattern,
                                Optional.of(callbackPattern),
                                Optional.of(PubsubMessageQueue));
        var listenerSharded =
                (RedisClusterClient)
                        createClientWithSubscriptions(
                                false,
                                subscriptionsSharded,
                                Optional.of(callbackSharded),
                                Optional.of(PubsubMessageQueue));
        clients.addAll(List.of(listenerExact, listenerPattern, listenerSharded));

        assertEquals(2L, listenerPattern.publish(channel, exactPubsubMessage.getMessage()).get());
        assertEquals(2L, listenerSharded.publish(channel, patternPubsubMessage.getMessage()).get());
        assertEquals(1L, listenerExact.spublish(channel, shardedPubsubMessage.getMessage()).get());

        Thread.sleep(404); // deliver the PubsubMessages

        var expected =
                Set.of(
                        Pair.of(PubSubClusterChannelMode.EXACT.ordinal(), exactPubsubMessage),
                        Pair.of(
                                PubSubClusterChannelMode.EXACT.ordinal(),
                                new PubSubMessage(patternPubsubMessage.getMessage(), channel)),
                        Pair.of(PubSubClusterChannelMode.PATTERN.ordinal(), patternPubsubMessage),
                        Pair.of(
                                PubSubClusterChannelMode.PATTERN.ordinal(),
                                new PubSubMessage(exactPubsubMessage.getMessage(), channel, channel)),
                        Pair.of(PubSubClusterChannelMode.SHARDED.ordinal(), shardedPubsubMessage));

        verifyReceivedPubsubMessages(expected, listenerExact, true);
    }

    @SneakyThrows
    @Test
    public void error_cases() {
        skipTestsOnMac();
        // client isn't configured with subscriptions
        var client = createClient(true);
        assertThrows(ConfigurationError.class, client::tryGetPubSubMessage);
        client.close();

        // client configured with callback and doesn't return PubsubMessages via API
        MessageCallback callback = (msg, ctx) -> {};
        client =
                createClientWithSubscriptions(
                        true, Map.of(), Optional.of(callback), Optional.of(PubsubMessageQueue));
        assertThrows(ConfigurationError.class, client::tryGetPubSubMessage);
        client.close();

        // using sharded channels from different slots in a transaction causes a cross slot error
        var clusterClient = (RedisClusterClient) createClient(false);
        var transaction =
                new ClusterTransaction()
                        .spublish("abc", "one")
                        .spublish("mnk", "two")
                        .spublish("xyz", "three");
        var exception =
                assertThrows(ExecutionException.class, () -> clusterClient.exec(transaction).get());
        assertInstanceOf(RequestException.class, exception.getCause());
        assertTrue(exception.getMessage().toLowerCase().contains("crossslot"));

        // TODO test when callback throws an exception - currently nothing happens now
        //  it should terminate the client
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "standalone = {0}, use callback = {1}")
    @MethodSource("getTwoBoolPermutations")
    public void transaction_with_all_types_of_PubsubMessages(
            boolean standalone, boolean useCallback) {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0"), "This feature added in redis 7");
        skipTestsOnMac();
        assumeTrue(
                standalone, // TODO activate tests after fix
                "Test doesn't work on cluster due to Cross Slot error, probably a but in `redis-rs`");

        String prefix = "channel";
        String pattern = prefix + "*";
        String shardPrefix = "{shard}";
        String channel = UUID.randomUUID().toString();
        var exactPubsubMessage = new PubSubMessage(UUID.randomUUID().toString(), channel);
        var patternPubsubMessage = new PubSubMessage(UUID.randomUUID().toString(), prefix, pattern);
        var shardedPubsubMessage = new PubSubMessage(UUID.randomUUID().toString(), shardPrefix);

        Map<? extends ChannelMode, Set<String>> subscriptions =
                standalone
                        ? Map.of(
                                PubSubChannelMode.EXACT,
                                Set.of(channel),
                                PubSubChannelMode.PATTERN,
                                Set.of(pattern))
                        : Map.of(
                                PubSubClusterChannelMode.EXACT,
                                Set.of(channel),
                                PubSubClusterChannelMode.PATTERN,
                                Set.of(pattern),
                                PubSubClusterChannelMode.SHARDED,
                                Set.of(shardPrefix));

        MessageCallback callback =
                (msg, ctx) ->
                        ((ConcurrentLinkedDeque<Pair<Integer, PubSubMessage>>) ctx).push(Pair.of(1, msg));

        var listener =
                useCallback
                        ? createClientWithSubscriptions(
                                standalone, subscriptions, Optional.of(callback), Optional.of(PubsubMessageQueue))
                        : createClientWithSubscriptions(standalone, subscriptions);
        var sender = createClient(standalone);
        clients.addAll(List.of(listener, sender));

        if (standalone) {
            var transaction =
                    new Transaction()
                            .publish(exactPubsubMessage.getChannel(), exactPubsubMessage.getMessage())
                            .publish(patternPubsubMessage.getChannel(), patternPubsubMessage.getMessage());
            ((RedisClient) sender).exec(transaction).get();
        } else {
            var transaction =
                    new ClusterTransaction()
                            .spublish(shardedPubsubMessage.getChannel(), shardedPubsubMessage.getMessage())
                            .publish(exactPubsubMessage.getChannel(), exactPubsubMessage.getMessage())
                            .publish(patternPubsubMessage.getChannel(), patternPubsubMessage.getMessage());
            ((RedisClusterClient) sender).exec(transaction).get();
        }

        Thread.sleep(404); // deliver the PubsubMessages

        var expected =
                standalone
                        ? Set.of(Pair.of(1, exactPubsubMessage), Pair.of(1, patternPubsubMessage))
                        : Set.of(
                                Pair.of(1, exactPubsubMessage),
                                Pair.of(1, patternPubsubMessage),
                                Pair.of(1, shardedPubsubMessage));
        verifyReceivedPubsubMessages(expected, listener, useCallback);
    }
}
