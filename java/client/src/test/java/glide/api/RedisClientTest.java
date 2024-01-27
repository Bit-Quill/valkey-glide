package glide.api;

import static glide.api.models.commands.SetOptions.CONDITIONAL_SET_ONLY_IF_DOES_NOT_EXIST;
import static glide.api.models.commands.SetOptions.CONDITIONAL_SET_ONLY_IF_EXISTS;
import static glide.api.models.commands.SetOptions.RETURN_OLD_VALUE;
import static glide.api.models.commands.SetOptions.TIME_TO_LIVE_KEEP_EXISTING;
import static glide.api.models.commands.SetOptions.TIME_TO_LIVE_UNIX_SECONDS;
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
import glide.managers.models.Command;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @SneakyThrows
    @Test
    public void customCommand_success() {
        // setup
        String key = "testKey";
        Object value = "testValue";
        String cmd = "GETSTRING";
        CompletableFuture<Object> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(value);
        when(commandManager.submitNewCommand(any(), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Object> response = service.customCommand(new String[] {cmd, key});
        String payload = (String) response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void customCommand_interruptedException() {
        // setup
        String key = "testKey";
        String cmd = "GETSTRING";
        CompletableFuture<Object> testResponse = mock(CompletableFuture.class);
        InterruptedException interruptedException = new InterruptedException();
        when(testResponse.get()).thenThrow(interruptedException);
        when(commandManager.submitNewCommand(any(), any())).thenReturn(testResponse);

        // exercise
        InterruptedException exception =
                assertThrows(
                        InterruptedException.class,
                        () -> {
                            CompletableFuture<Object> response = service.customCommand(new String[] {cmd, key});
                            response.get();
                        });

        // verify
        assertEquals(interruptedException, exception);
    }

    @SneakyThrows
    @Test
    public void ping_success() {
        // setup
        Command cmd = Command.builder().requestType(Command.RequestType.PING).build();
        CompletableFuture<String> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn("PONG");
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.ping();
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals("PONG", payload);
    }

    @SneakyThrows
    @Test
    public void pingWithMessage_success() {
        // setup
        String message = "RETURN OF THE PONG";
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.PING)
                        .arguments(new String[] {message})
                        .build();
        CompletableFuture<String> testResponse = new CompletableFuture();
        testResponse.complete(message);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.ping(message);
        String pong = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(message, pong);
    }

    @SneakyThrows
    @Test
    public void info_success() {
        // setup
        Command cmd = Command.builder().requestType(Command.RequestType.INFO).build();
        CompletableFuture<Map> testResponse = mock(CompletableFuture.class);
        Map testPayload = new HashMap<String, String>();
        testPayload.put("key1", "value1");
        testPayload.put("key2", "value2");
        testPayload.put("key3", "value3");
        when(testResponse.get()).thenReturn(testPayload);
        when(commandManager.<Map>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Map> response = service.info();
        Map payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(testPayload, payload);
    }

    @SneakyThrows
    @Test
    public void infoWithMultipleOptions_success() {
        // setup
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.INFO)
                        .arguments(new String[] {"ALL", "DEFAULT"})
                        .build();
        CompletableFuture<Map> testResponse = mock(CompletableFuture.class);
        Map testPayload = new HashMap<String, String>();
        testPayload.put("key1", "value1");
        testPayload.put("key2", "value2");
        testPayload.put("key3", "value3");
        when(testResponse.get()).thenReturn(testPayload);
        when(commandManager.<Map>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

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

    @SneakyThrows
    @Test
    public void infoEmptyWithOptions_success() {
        // setup
        Command cmd =
                Command.builder().requestType(Command.RequestType.INFO).arguments(new String[] {}).build();
        CompletableFuture<Map> testResponse = mock(CompletableFuture.class);
        Map testPayload = new HashMap<String, String>();
        testPayload.put("key1", "value1");
        testPayload.put("key2", "value2");
        testPayload.put("key3", "value3");
        when(testResponse.get()).thenReturn(testPayload);
        when(commandManager.<Map>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Map> response = service.info(InfoOptions.builder().build());
        Map payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(testPayload, payload);
    }

    @SneakyThrows
    @Test
    public void get_success() {
        // setup
        // TODO: randomize keys
        String key = "testKey";
        String value = "testValue";
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.GET_STRING)
                        .arguments(new String[] {key})
                        .build();
        CompletableFuture<String> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(value);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.get(key);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void set_success() {
        // setup
        // TODO: randomize keys
        String key = "testKey";
        String value = "testValue";
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.SET_STRING)
                        .arguments(new String[] {key, value})
                        .build();
        CompletableFuture<Void> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(null);
        when(commandManager.<Void>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Void> response = service.set(key, value);
        Object nullResponse = response.get();

        // verify
        assertEquals(testResponse, response);
        assertNull(nullResponse);
    }

    @SneakyThrows
    @Test
    public void set_withOptionsOnlyIfExists_success() {
        // setup
        String key = "testKey";
        String value = "testValue";
        SetOptions setOptions =
                SetOptions.builder()
                        .conditionalSet(SetOptions.ConditionalSet.ONLY_IF_EXISTS)
                        .returnOldValue(false)
                        .expiry(
                                SetOptions.TimeToLive.builder()
                                        .type(SetOptions.TimeToLiveType.KEEP_EXISTING)
                                        .build())
                        .build();
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.SET_STRING)
                        .arguments(
                                new String[] {
                                    key, value, CONDITIONAL_SET_ONLY_IF_EXISTS, TIME_TO_LIVE_KEEP_EXISTING
                                })
                        .build();
        CompletableFuture<String> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(null);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.set(key, value, setOptions);

        // verify
        assertNotNull(response);
        assertNull(response.get());
    }

    @SneakyThrows
    @Test
    public void set_withOptionsOnlyIfDoesNotExist_success() {
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
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.SET_STRING)
                        .arguments(
                                new String[] {
                                    key,
                                    value,
                                    CONDITIONAL_SET_ONLY_IF_DOES_NOT_EXIST,
                                    RETURN_OLD_VALUE,
                                    TIME_TO_LIVE_UNIX_SECONDS + " 60"
                                })
                        .build();
        CompletableFuture<String> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(value);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.set(key, value, setOptions);

        // verify
        assertNotNull(response);
        assertEquals(value, response.get());
    }

    @SneakyThrows
    @Test
    public void decr_success() {
        // setup
        String key = "testKey";
        Long value = 10L;
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.DECR)
                        .arguments(new String[] {key})
                        .build();
        CompletableFuture testResponse = mock(CompletableFuture.class);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);
        when(testResponse.get()).thenReturn(value);

        // exercise
        CompletableFuture<Long> response = service.decr(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void decrBy_success() {
        // setup
        String key = "testKey";
        long amount = 1L;
        Long value = 10L;
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.DECR_BY)
                        .arguments(new String[] {key, Long.toString(amount)})
                        .build();
        CompletableFuture testResponse = mock(CompletableFuture.class);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);
        when(testResponse.get()).thenReturn(value);

        // exercise
        CompletableFuture<Long> response = service.decrBy(key, amount);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void incr_success() {
        // setup
        String key = "testKey";
        Long value = 10L;
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.INCR)
                        .arguments(new String[] {key})
                        .build();
        CompletableFuture testResponse = mock(CompletableFuture.class);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);
        when(testResponse.get()).thenReturn(value);

        // exercise
        CompletableFuture<Long> response = service.incr(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void incrBy_success() {
        // setup
        String key = "testKey";
        long amount = 1L;
        Long value = 10L;
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.INCR_BY)
                        .arguments(new String[] {key, Long.toString(amount)})
                        .build();
        CompletableFuture testResponse = mock(CompletableFuture.class);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);
        when(testResponse.get()).thenReturn(value);

        // exercise
        CompletableFuture<Long> response = service.incrBy(key, amount);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void incrByFloat_success() {
        // setup
        String key = "testKey";
        double amount = 1.1;
        Double value = 10.1;
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.INCR_BY_FLOAT)
                        .arguments(new String[] {key, Double.toString(amount)})
                        .build();
        CompletableFuture testResponse = mock(CompletableFuture.class);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);
        when(testResponse.get()).thenReturn(value);

        // exercise
        CompletableFuture<Double> response = service.incrByFloat(key, amount);
        Double payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void mget_success() {
        // setup
        String[] keys = {"Key1", "Key2"};
        String[] values = {"Value1", "Value2"};
        Command cmd = Command.builder().requestType(Command.RequestType.MGET).arguments(keys).build();
        CompletableFuture testResponse = mock(CompletableFuture.class);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);
        when(testResponse.thenApply(any())).thenReturn(testResponse);
        when(testResponse.get()).thenReturn(values);

        // exercise
        CompletableFuture<String[]> response = service.mget(keys);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(values, payload);
    }

    @SneakyThrows
    @Test
    public void mset_success() {
        // setup
        HashMap<String, String> keyValueMap =
                new HashMap<String, String>() {
                    {
                        put("Key1", "Value1");
                    }
                };
        String[] flattenedKeyValueMap = {"Key1", "Value1"};
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.MSET)
                        .arguments(flattenedKeyValueMap)
                        .build();
        CompletableFuture<Void> testResponse = mock(CompletableFuture.class);
        when(commandManager.<Void>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);
        when(testResponse.get()).thenReturn(null);

        // exercise
        CompletableFuture<Void> response = service.mset(keyValueMap);
        Object nullResponse = response.get();

        // verify
        assertEquals(testResponse, response);
        assertNull(nullResponse);
    }
}
