package glide.api.models.commands;

import glide.api.models.exceptions.RequestException;
import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;

@Builder
@NonNull
public class Options {

  protected List optionArgs;

  public String[] toArgs() {
    return toArgs(List.of());
  }

  public String[] toArgs(List<String> arguments) {
    List<String> args = new LinkedList<>(arguments);
    arguments.addAll(optionArgs);
    return arguments.toArray(new String[0]);
  }
}
