package glide.api;

import static glide.api.models.commands.SetOptions.CONDITIONAL_SET_ONLY_IF_DOES_NOT_EXIST;
import static glide.api.models.commands.SetOptions.CONDITIONAL_SET_ONLY_IF_EXISTS;
import static glide.api.models.commands.SetOptions.ConditionalSet.ONLY_IF_EXISTS;
import static glide.api.models.commands.SetOptions.RETURN_OLD_VALUE;
import static glide.api.models.commands.SetOptions.TIME_TO_LIVE_KEEP_EXISTING;
import static glide.api.models.commands.SetOptions.TIME_TO_LIVE_UNIX_SECONDS;
import static glide.managers.CommandManager.RequestType.CUSTOM_COMMAND;
import static glide.managers.CommandManager.RequestType.GET_STRING;
import static glide.managers.CommandManager.RequestType.INFO;
import static glide.managers.CommandManager.RequestType.PING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.SetOptions;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import redis_request.RedisRequestOuterClass;

public class RedisClientTest {

    RedisClient service;

    ConnectionManager connectionManager;

    CommandManager commandManager;

    @BeforeEach
    public void setUp() {
        connectionManager = mock(ConnectionManager.class);
        commandManager = mock(CommandManager.class);
        service = new RedisClient(connectionManager, commandManager);
    }

    @Test
    public void customCommand_success() throws ExecutionException, InterruptedException {
        // setup
        String key = "testKey";
        Object value = "testValue";
        String cmd = "GETSTRING";
        String[] arguments = new String[] {cmd, key};
        CompletableFuture<Object> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(value);

        ArgumentCaptor<RedisRequestOuterClass.RedisRequest.Builder> captor =
                ArgumentCaptor.forClass(RedisRequestOuterClass.RedisRequest.Builder.class);

        // match on protobuf request
        when(commandManager.submitNewCommand(
                        eq(CUSTOM_COMMAND), eq(arguments), eq(Optional.empty()), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object> response = service.customCommand(arguments);
        String payload = (String) response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @Test
    public void customCommand_interruptedException() throws ExecutionException, InterruptedException {
        // setup
        String key = "testKey";
        Object value = "testValue";
        String cmd = "GETSTRING";
        String[] arguments = new String[] {cmd, key};
        CompletableFuture<Object> testResponse = mock(CompletableFuture.class);
        InterruptedException interruptedException = new InterruptedException();
        when(testResponse.get()).thenThrow(interruptedException);

        // match on protobuf request
        when(commandManager.submitNewCommand(
                        eq(CUSTOM_COMMAND), eq(arguments), eq(Optional.empty()), any()))
                .thenReturn(testResponse);

        // exercise
        InterruptedException exception =
                assertThrows(
                        InterruptedException.class,
                        () -> {
                            CompletableFuture<Object> response = service.customCommand(arguments);
                            response.get();
                        });

        // verify
        assertEquals(interruptedException, exception);
    }

    @Test
    public void ping_success() throws ExecutionException, InterruptedException {
        // setup
        CompletableFuture<String> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn("PONG");

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(
                        eq(PING), eq(new String[0]), eq(Optional.empty()), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.ping();
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals("PONG", payload);
    }

    @Test
    public void pingWithMessage_success() throws ExecutionException, InterruptedException {
        // setup
        String message = "RETURN OF THE PONG";
        String[] arguments = new String[] {message};
        CompletableFuture<String> testResponse = new CompletableFuture();
        testResponse.complete(message);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(
                        eq(PING), eq(arguments), eq(Optional.empty()), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.ping(message);
        String pong = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(message, pong);
    }

    @Test
    public void info_success() throws ExecutionException, InterruptedException {
        // setup
        CompletableFuture<Map> testResponse = mock(CompletableFuture.class);
        Map testPayload = new HashMap<String, String>();
        testPayload.put("key1", "value1");
        testPayload.put("key2", "value2");
        testPayload.put("key3", "value3");
        when(testResponse.get()).thenReturn(testPayload);
        when(commandManager.<Map>submitNewCommand(
                        eq(INFO), eq(new String[0]), eq(Optional.empty()), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map> response = service.info();
        Map payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(testPayload, payload);
    }

    @Test
    public void infoWithMultipleOptions_success() throws ExecutionException, InterruptedException {
        // setup
        String[] arguments =
                new String[] {InfoOptions.Section.ALL.toString(), InfoOptions.Section.DEFAULT.toString()};
        CompletableFuture<Map> testResponse = mock(CompletableFuture.class);
        Map testPayload = new HashMap<String, String>();
        testPayload.put("key1", "value1");
        testPayload.put("key2", "value2");
        testPayload.put("key3", "value3");
        when(testResponse.get()).thenReturn(testPayload);
        when(commandManager.<Map>submitNewCommand(eq(INFO), eq(arguments), eq(Optional.empty()), any()))
                .thenReturn(testResponse);

        // exercise
        InfoOptions options =
                InfoOptions.builder()
                        .section(InfoOptions.Section.ALL)
                        .section(InfoOptions.Section.DEFAULT)
                        .build();
        CompletableFuture<Map> response = service.info(options);
        Map payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(testPayload, payload);
    }

    @Test
    public void infoEmptyWithOptions_success() throws ExecutionException, InterruptedException {
        // setup
        CompletableFuture<Map> testResponse = mock(CompletableFuture.class);
        Map testPayload = new HashMap<String, String>();
        testPayload.put("key1", "value1");
        testPayload.put("key2", "value2");
        testPayload.put("key3", "value3");
        when(testResponse.get()).thenReturn(testPayload);
        when(commandManager.<Map>submitNewCommand(
                        eq(INFO), eq(new String[0]), eq(Optional.empty()), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map> response = service.info(InfoOptions.builder().build());
        Map payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(testPayload, payload);
    }

    @Test
    public void get_success() throws ExecutionException, InterruptedException {
        // setup
        // TODO: randomize keys
        String key = "testKey";
        String value = "testValue";
        CompletableFuture<String> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(value);
        when(commandManager.<String>submitNewCommand(
                        eq(GET_STRING), eq(new String[] {key}), eq(Optional.empty()), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.get(key);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @Test
    public void set_success() throws ExecutionException, InterruptedException {
        // setup
        // TODO: randomize keys
        String key = "testKey";
        String value = "testValue";
        CompletableFuture<Void> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(null);
        when(commandManager.<Void>submitNewCommand(any(), any(), any(), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Void> response = service.set(key, value);
        Object nullResponse = response.get();

        // verify
        assertEquals(testResponse, response);
        assertNull(nullResponse);
    }

    @Test
    public void set_withOptionsOnlyIfExists_success()
            throws ExecutionException, InterruptedException {
        // setup
        String key = "testKey";
        String value = "testValue";
        SetOptions setOptions =
                SetOptions.builder()
                        .conditionalSet(ONLY_IF_EXISTS)
                        .returnOldValue(false)
                        .expiry(
                                SetOptions.TimeToLive.builder()
                                        .type(SetOptions.TimeToLiveType.KEEP_EXISTING)
                                        .build())
                        .build();
        String[] arguments =
                new String[] {key, value, CONDITIONAL_SET_ONLY_IF_EXISTS, TIME_TO_LIVE_KEEP_EXISTING};

        CompletableFuture<String> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(null);
        when(commandManager.<String>submitNewCommand(any(), any(), any(), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.set(key, value, setOptions);

        // verify
        assertEquals(testResponse, response);
        assertNull(response.get());
    }

    @Test
    public void set_withOptionsOnlyIfDoesNotExist_success()
            throws ExecutionException, InterruptedException {
        // setup
        String key = "testKey";
        String value = "testValue";
        SetOptions setOptions =
                SetOptions.builder()
                        .conditionalSet(SetOptions.ConditionalSet.ONLY_IF_DOES_NOT_EXIST)
                        .returnOldValue(true)
                        .expiry(
                                SetOptions.TimeToLive.builder()
                                        .type(SetOptions.TimeToLiveType.UNIX_SECONDS)
                                        .count(60)
                                        .build())
                        .build();
        String[] arguments =
                new String[] {
                    key,
                    value,
                    CONDITIONAL_SET_ONLY_IF_DOES_NOT_EXIST,
                    RETURN_OLD_VALUE,
                    TIME_TO_LIVE_UNIX_SECONDS + " 60"
                };
        CompletableFuture<String> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(value);
        when(commandManager.<String>submitNewCommand(any(), any(), any(), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.set(key, value, setOptions);

        // verify
        assertNotNull(response);
        assertEquals(value, response.get());
    }
}
