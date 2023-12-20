package babushka.api.commands;

import java.util.Arrays;
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

  @Override
  public boolean equals(Object o) {
    if (o instanceof Command) {
      Command otherCommand = (Command) o;
      if (this.requestType != otherCommand.requestType) {
        return false;
      }

      if (!Arrays.equals(this.arguments, otherCommand.arguments)) {
        return false;
      }

      return true;
    }
    return false;
  }
}
