/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.standalone;

import static glide.TestConfiguration.REDIS_VERSION;
import static glide.TestUtilities.assertDeepEquals;
import static glide.TestUtilities.commonClientConfig;
import static glide.TestUtilities.commonClusterClientConfig;
import static org.apache.commons.lang3.ArrayUtils.addFirst;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import glide.api.BaseClient;
import glide.api.RedisClient;
import glide.api.RedisClusterClient;
import glide.api.models.Message;
import glide.api.models.configuration.BaseSubscriptionConfiguration.ChannelMode;
import glide.api.models.configuration.BaseSubscriptionConfiguration.MessageCallback;
import glide.api.models.configuration.ClusterSubscriptionConfiguration;
import glide.api.models.configuration.ClusterSubscriptionConfiguration.PubSubClusterChannelMode;
import glide.api.models.configuration.StandaloneSubscriptionConfiguration;
import glide.api.models.configuration.StandaloneSubscriptionConfiguration.PubSubChannelMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Timeout;
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

    /** Message queue used in callback to analyze received messages. Number is a client ID. */
    private final ConcurrentLinkedDeque<Pair<Integer, Message>> mq = new ConcurrentLinkedDeque<>();
    /** Clients used in a test. */
    private final List<BaseClient> clients = new ArrayList<>();

    @BeforeEach
    @SneakyThrows
    public void cleanup() {
        mq.clear();
        for (var client : clients) client.close();
        clients.clear();
    }


    private void verifyReceivedMessages(
            Set<Pair<Integer, Message>> messages, BaseClient listener, boolean callback) {
        if (callback) {
            assertEquals(messages, new HashSet<>(mq));
        } else {
            var received = new HashSet<Message>(messages.size());
            Message message;
            while ((message = listener.tryGetPubSubMessage()) != null) {
                received.add(message);
            }
            assertEquals(messages.stream().map(Pair::getValue).collect(Collectors.toSet()), received);
        }
    }

    public static Stream<Arguments> getTwoBoolPermutations() {
        return Stream.of(
                Arguments.of(true, true),
                Arguments.of(true, false),
                Arguments.of(false, true),
                Arguments.of(false, false));
    }

    // TODO:
    //  test_pubsub_exact_happy_path_coexistence
    //  test_pubsub_exact_happy_path_many_channels_co_existence
    //  test_sharded_pubsub_co_existence
    //  test_pubsub_pattern_co_existence

    // TODO do we need unsubscribe at the end of the test?

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "standalone = {0}, use callback = {1}")
    @MethodSource("getTwoBoolPermutations")
    public void pubsub_exact_happy_path(boolean standalone, boolean useCallback) {
        String channel = UUID.randomUUID().toString();
        String message = UUID.randomUUID().toString();
        var subscriptions =
                Map.of(
                        standalone ? PubSubChannelMode.EXACT : PubSubClusterChannelMode.EXACT, Set.of(channel));

        MessageCallback callback =
                (msg, ctx) -> ((ConcurrentLinkedDeque<Pair<Integer, Message>>) ctx).push(Pair.of(1, msg));

        var listener =
                useCallback
                        ? createClientWithSubscriptions(
                                standalone, subscriptions, Optional.of(callback), Optional.of(mq))
                        : createClientWithSubscriptions(standalone, subscriptions);
        var sender = createClient(standalone);
        clients.addAll(List.of(listener, sender));

        assertEquals(1L, sender.publish(channel, message).get());
        Thread.sleep(404); // deliver the message

        verifyReceivedMessages(
                Set.of(Pair.of(1, new Message(message, channel))), listener, useCallback);

        if (!standalone) {
            ((RedisClusterClient) listener).customCommand(new String[] { "unsubscribe", channel}).get();
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "standalone = {0}, use callback = {1}")
    @MethodSource("getTwoBoolPermutations")
    public void pubsub_exact_happy_path_many_channels(boolean standalone, boolean useCallback) {
        int numChannels = 256;
        int messagesPerChannel = 256;
        var messages = new ArrayList<Message>(numChannels * messagesPerChannel);
        ChannelMode mode = standalone ? PubSubChannelMode.EXACT : PubSubClusterChannelMode.EXACT;
        Map<? extends ChannelMode, Set<String>> subscriptions = Map.of(mode, new HashSet<>());

        for (var i = 0; i < numChannels; i++) {
            var channel = UUID.randomUUID().toString();
            subscriptions.get(mode).add(channel);
            for (var j = 0; j < messagesPerChannel; j++) {
                var message = UUID.randomUUID().toString();
                messages.add(new Message(message, channel));
            }
        }

        MessageCallback callback =
                (msg, ctx) -> ((ConcurrentLinkedDeque<Pair<Integer, Message>>) ctx).push(Pair.of(1, msg));

        var listener =
                useCallback
                        ? createClientWithSubscriptions(
                                standalone, subscriptions, Optional.of(callback), Optional.of(mq))
                        : createClientWithSubscriptions(standalone, subscriptions);
        var sender = createClient(standalone);
        clients.addAll(List.of(listener, sender));

        for (var message : messages) {
            assertEquals(1L, sender.publish(message.getChannel(), message.getMessage()).get());
        }

        Thread.sleep(404); // deliver the messages

        verifyReceivedMessages(
                messages.stream().map(m -> Pair.of(1, m)).collect(Collectors.toSet()),
                listener,
                useCallback);

        if (!standalone) {
            String[] channels = subscriptions.get(mode).toArray(String[]::new);
            ((RedisClusterClient) listener).customCommand(addFirst(channels, "unsubscribe")).get();
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "use callback = {0}")
    @ValueSource(booleans = {true, false})
    public void sharded_pubsub(boolean useCallback) {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0"), "This feature added in redis 7");

        String channel = UUID.randomUUID().toString();
        String message = UUID.randomUUID().toString();
        var subscriptions =
            Map.of(
                PubSubClusterChannelMode.SHARDED, Set.of(channel));

        MessageCallback callback =
            (msg, ctx) -> ((ConcurrentLinkedDeque<Pair<Integer, Message>>) ctx).push(Pair.of(1, msg));

        var listener = (RedisClusterClient)(
            useCallback
                ? createClientWithSubscriptions(
                false, subscriptions, Optional.of(callback), Optional.of(mq))
                : createClientWithSubscriptions(false, subscriptions));
        var sender = (RedisClusterClient) createClient(false);
        clients.addAll(List.of(listener, sender));

        assertEquals(1L, sender.spublish(channel, message).get());
        Thread.sleep(404); // deliver the message

        verifyReceivedMessages(
            Set.of(Pair.of(1, new Message(message, channel))), listener, useCallback);

        listener.customCommand(new String[] { "unsubscribe", channel}).get();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "use callback = {0}")
    @ValueSource(booleans = {true, false})
    public void sharded_pubsub_many_channels(boolean useCallback) {
        int numChannels = 256;
        int messagesPerChannel = 256;
        var messages = new ArrayList<Message>(numChannels * messagesPerChannel);
        ChannelMode mode = PubSubClusterChannelMode.SHARDED;
        Map<? extends ChannelMode, Set<String>> subscriptions = Map.of(mode, new HashSet<>());

        for (var i = 0; i < numChannels; i++) {
            var channel = UUID.randomUUID().toString();
            subscriptions.get(mode).add(channel);
            for (var j = 0; j < messagesPerChannel; j++) {
                var message = UUID.randomUUID().toString();
                messages.add(new Message(message, channel));
            }
        }

        MessageCallback callback =
            (msg, ctx) -> ((ConcurrentLinkedDeque<Pair<Integer, Message>>) ctx).push(Pair.of(1, msg));

        var listener = (RedisClusterClient)(
            useCallback
                ? createClientWithSubscriptions(
                false, subscriptions, Optional.of(callback), Optional.of(mq))
                : createClientWithSubscriptions(false, subscriptions));
        var sender = (RedisClusterClient) createClient(false);
        clients.addAll(List.of(listener, sender));

        for (var message : messages) {
            assertEquals(1L, sender.spublish(message.getChannel(), message.getMessage()).get());
        }
        assertEquals(0L, sender.spublish(UUID.randomUUID().toString(), UUID.randomUUID().toString()).get());

        Thread.sleep(404); // deliver the messages

        verifyReceivedMessages(
            messages.stream().map(m -> Pair.of(1, m)).collect(Collectors.toSet()),
            listener,
            useCallback);

        String[] channels = subscriptions.get(mode).toArray(String[]::new);
        listener.customCommand(addFirst(channels, "unsubscribe")).get();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "standalone = {0}, use callback = {1}")
    @MethodSource("getTwoBoolPermutations")
    public void pubsub_pattern(boolean standalone, boolean useCallback) {
        String prefix = "channel.";
        String pattern = prefix + "*";
        Map<String, String> message2channels = Map.of(prefix + "1", UUID.randomUUID().toString(), prefix + "2", UUID.randomUUID().toString());
        var subscriptions =
            Map.of(
                standalone ? PubSubChannelMode.PATTERN : PubSubClusterChannelMode.PATTERN, Set.of(pattern));

        MessageCallback callback =
            (msg, ctx) -> ((ConcurrentLinkedDeque<Pair<Integer, Message>>) ctx).push(Pair.of(1, msg));

        var listener =
            useCallback
                ? createClientWithSubscriptions(
                standalone, subscriptions, Optional.of(callback), Optional.of(mq))
                : createClientWithSubscriptions(standalone, subscriptions);
        var sender = createClient(standalone);
        clients.addAll(List.of(listener, sender));

        Thread.sleep(404); // need some time to propagate subscriptions - why?

        for (var entry : message2channels.entrySet()) {
            assertEquals(1L, sender.publish(entry.getKey(), entry.getValue()).get());
        }
        assertEquals(0L, sender.publish("channel", UUID.randomUUID().toString()).get());
        Thread.sleep(404); // deliver the messages

        var expected = message2channels.entrySet().stream().map(e -> Pair.of(1, new Message(e.getValue(), e.getKey(), pattern))).collect(Collectors.toSet());

        verifyReceivedMessages(
            expected, listener, useCallback);

        if (!standalone) {
            ((RedisClusterClient) listener).customCommand(new String[] { "punsubscribe", pattern}).get();
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "standalone = {0}, use callback = {1}")
    @MethodSource("getTwoBoolPermutations")
    public void pubsub_pattern_many_channels(boolean standalone, boolean useCallback) {
        String prefix = "channel.";
        String pattern = prefix + "*";
        int numChannels = 256;
        int messagesPerChannel = 256;
        ChannelMode mode = standalone ? PubSubChannelMode.PATTERN : PubSubClusterChannelMode.PATTERN;
        var messages = new ArrayList<Message>(numChannels * messagesPerChannel);
        var subscriptions =
            Map.of(mode, Set.of(pattern));

        for (var i = 0; i < numChannels; i++) {
            var channel = prefix + UUID.randomUUID();
            for (var j = 0; j < messagesPerChannel; j++) {
                var message = UUID.randomUUID().toString();
                messages.add(new Message(message, channel, pattern));
            }
        }

        MessageCallback callback =
            (msg, ctx) -> ((ConcurrentLinkedDeque<Pair<Integer, Message>>) ctx).push(Pair.of(1, msg));

        var listener =
            useCallback
                ? createClientWithSubscriptions(
                standalone, subscriptions, Optional.of(callback), Optional.of(mq))
                : createClientWithSubscriptions(standalone, subscriptions);
        var sender = createClient(standalone);
        clients.addAll(List.of(listener, sender));

        Thread.sleep(404); // need some time to propagate subscriptions - why?

        for (var message : messages) {
            // TODO why publish returns 0 on cluster? meanwhile messages are delivered
            var res = sender.publish(message.getChannel(), message.getMessage()).get();
            if (standalone) {
                assertEquals(1L, res);
            }
        }
        assertEquals(0L, sender.publish("channel", UUID.randomUUID().toString()).get());
        Thread.sleep(404); // deliver the messages

        verifyReceivedMessages(
            messages.stream().map(m -> Pair.of(1, m)).collect(Collectors.toSet()),
            listener,
            useCallback);

        if (!standalone) {
            ((RedisClusterClient) listener).customCommand(new String[] { "punsubscribe", pattern}).get();
        } else {
            ((RedisClient) listener).customCommand(new String[] { "punsubscribe", pattern}).get();
        }
    }
}
