package babushka.api.commands;

import lombok.Builder;

@Builder
public class Command {

  final RequestType requestType;
  final String[] arguments;

  public enum RequestType {
    CUSTOM_COMMAND,
    GETSTRING,
    SETSTRING
  }
}
