package glide.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import glide.api.commands.BaseCommands;
import glide.api.commands.Command;
import glide.api.models.exceptions.ClosingException;
import glide.api.models.exceptions.ConnectionException;
import glide.api.models.exceptions.ExecAbortException;
import glide.api.models.exceptions.RedisException;
import glide.api.models.exceptions.TimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import response.ResponseOuterClass;

public class CommandManagerTest {

  CommandManager service;

  // ignored for now
  Command command;

  @Test
  public void submitNewCommand_returnObjectResult()
      throws ExecutionException, InterruptedException {

    CompletableFuture<ResponseOuterClass.Response> channel = new CompletableFuture<>();
    CommandManager service = new CommandManager(channel);

    long pointer = -1;
    ResponseOuterClass.Response respPointerResponse =
        ResponseOuterClass.Response.newBuilder().setRespPointer(pointer).build();
    Object respObject = mock(Object.class);

    CompletableFuture result =
        service.submitNewCommand(
            command,
            new BaseCommands.BaseCommandResponseResolver(
                (ptr) -> ptr == pointer ? respObject : null));
    channel.complete(respPointerResponse);
    Object respPointer = result.get();

    assertEquals(respObject, respPointer);
  }

  @Test
  public void submitNewCommand_returnNullResult() throws ExecutionException, InterruptedException {

    CompletableFuture<ResponseOuterClass.Response> channel = new CompletableFuture<>();
    CommandManager service = new CommandManager(channel);

    ResponseOuterClass.Response respPointerResponse =
        ResponseOuterClass.Response.newBuilder().build();

    CompletableFuture result =
        service.submitNewCommand(
            command, new BaseCommands.BaseCommandResponseResolver((p) -> new RuntimeException("")));
    channel.complete(respPointerResponse);
    Object respPointer = result.get();

    assertNull(respPointer);
  }

  @Test
  public void submitNewCommand_returnStringResult()
      throws ExecutionException, InterruptedException {

    long pointer = 123;
    String testString = "TEST STRING";

    CompletableFuture<ResponseOuterClass.Response> channel = new CompletableFuture<>();
    CommandManager service = new CommandManager(channel);

    ResponseOuterClass.Response respPointerResponse =
        ResponseOuterClass.Response.newBuilder().setRespPointer(pointer).build();

    CompletableFuture result =
        service.submitNewCommand(
            command,
            new BaseCommands.BaseCommandResponseResolver((p) -> p == pointer ? testString : null));
    channel.complete(respPointerResponse);
    Object respPointer = result.get();

    assertTrue(respPointer instanceof String);
    assertEquals(testString, respPointer);
  }

  @Test
  public void submitNewCommand_throwClosingException() {

    String errorMsg = "Closing";

    ExecutionException e =
        assertThrows(
            ExecutionException.class,
            () -> {
              CompletableFuture<ResponseOuterClass.Response> channel = new CompletableFuture<>();
              CommandManager service = new CommandManager(channel);

              ResponseOuterClass.Response closingErrorResponse =
                  ResponseOuterClass.Response.newBuilder().setClosingError(errorMsg).build();

              CompletableFuture result =
                  service.submitNewCommand(
                      command, new BaseCommands.BaseCommandResponseResolver((ptr) -> new Object()));
              channel.complete(closingErrorResponse);
              result.get();
            });

    assertTrue(e.getCause() instanceof ClosingException);
    assertEquals(errorMsg, e.getCause().getMessage());
  }

  @Test
  public void submitNewCommand_throwConnectionException() {

    int disconnectedType = 3;
    String errorMsg = "Disconnected";

    ExecutionException e =
        assertThrows(
            ExecutionException.class,
            () -> {
              CompletableFuture<ResponseOuterClass.Response> channel = new CompletableFuture<>();
              CommandManager service = new CommandManager(channel);

              ResponseOuterClass.Response respPointerResponse =
                  ResponseOuterClass.Response.newBuilder()
                      .setRequestError(
                          ResponseOuterClass.RequestError.newBuilder()
                              .setTypeValue(disconnectedType)
                              .setMessage(errorMsg)
                              .build())
                      .build();

              CompletableFuture result =
                  service.submitNewCommand(
                      command, new BaseCommands.BaseCommandResponseResolver((ptr) -> new Object()));
              channel.complete(respPointerResponse);
              result.get();
            });

    assertTrue(e.getCause() instanceof ConnectionException);
    assertEquals(errorMsg, e.getCause().getMessage());
  }

  @Test
  public void submitNewCommand_throwTimeoutException() {

    int timeoutType = 2;
    String errorMsg = "Timeout";

    ExecutionException e =
        assertThrows(
            ExecutionException.class,
            () -> {
              CompletableFuture<ResponseOuterClass.Response> channel = new CompletableFuture<>();
              CommandManager service = new CommandManager(channel);

              ResponseOuterClass.Response timeoutErrorResponse =
                  ResponseOuterClass.Response.newBuilder()
                      .setRequestError(
                          ResponseOuterClass.RequestError.newBuilder()
                              .setTypeValue(timeoutType)
                              .setMessage(errorMsg)
                              .build())
                      .build();

              CompletableFuture result =
                  service.submitNewCommand(
                      command, new BaseCommands.BaseCommandResponseResolver((ptr) -> new Object()));
              channel.complete(timeoutErrorResponse);
              result.get();
            });

    assertTrue(e.getCause() instanceof TimeoutException);
    assertEquals(errorMsg, e.getCause().getMessage());
  }

  @Test
  public void submitNewCommand_throwExecAbortException() {

    int execAbortType = 1;
    String errorMsg = "ExecAbort";

    ExecutionException e =
        assertThrows(
            ExecutionException.class,
            () -> {
              CompletableFuture<ResponseOuterClass.Response> channel = new CompletableFuture<>();
              CommandManager service = new CommandManager(channel);

              ResponseOuterClass.Response execAbortErrorResponse =
                  ResponseOuterClass.Response.newBuilder()
                      .setRequestError(
                          ResponseOuterClass.RequestError.newBuilder()
                              .setTypeValue(execAbortType)
                              .setMessage(errorMsg)
                              .build())
                      .build();

              CompletableFuture result =
                  service.submitNewCommand(
                      command, new BaseCommands.BaseCommandResponseResolver((ptr) -> new Object()));
              channel.complete(execAbortErrorResponse);
              result.get();
            });

    assertTrue(e.getCause() instanceof ExecAbortException);
    assertEquals(errorMsg, e.getCause().getMessage());
  }

  @Test
  public void submitNewCommand_handleUnspecifiedError() {

    int unspecifiedType = 0;
    String errorMsg = "Unspecified";

    ExecutionException e =
        assertThrows(
            ExecutionException.class,
            () -> {
              CompletableFuture<ResponseOuterClass.Response> channel = new CompletableFuture<>();
              CommandManager service = new CommandManager(channel);

              ResponseOuterClass.Response unspecifiedErrorResponse =
                  ResponseOuterClass.Response.newBuilder()
                      .setRequestError(
                          ResponseOuterClass.RequestError.newBuilder()
                              .setTypeValue(unspecifiedType)
                              .setMessage(errorMsg)
                              .build())
                      .build();

              CompletableFuture result =
                  service.submitNewCommand(
                      command, new BaseCommands.BaseCommandResponseResolver((ptr) -> new Object()));
              channel.complete(unspecifiedErrorResponse);
              result.get();
            });

    assertTrue(e.getCause() instanceof RedisException);
    assertEquals(errorMsg, e.getCause().getMessage());
  }
}
