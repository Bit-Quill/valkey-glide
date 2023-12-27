package glide.api.commands;

import lombok.Builder;
import lombok.EqualsAndHashCode;

@Builder
@EqualsAndHashCode
public class Command {

  final RequestType requestType;
  final String[] arguments;

  public enum RequestType {
    CUSTOM_COMMAND,
    GETSTRING,
    SETSTRING,
  }
}
