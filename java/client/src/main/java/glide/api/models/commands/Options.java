package glide.api.models.commands;

import java.util.LinkedList;
import java.util.List;
import lombok.Builder;

/** Options base object to Options to a {@link Command} */
public abstract class Options {

    @Builder.Default protected List optionArgs = List.of();

    public String[] toArgs() {
        return toArgs(List.of());
    }

    public String[] toArgs(List<String> arguments) {
        List<String> args = new LinkedList<>(arguments);
        args.addAll(optionArgs);
        return args.toArray(new String[0]);
    }
}
