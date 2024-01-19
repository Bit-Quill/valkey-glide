package glide.api.models.commands;

import java.util.LinkedList;
import java.util.List;

/** Options base object to Options to a {@link glide.api.models.Command} */
public abstract class Options {

    protected List optionArgs;

    public String[] toArgs() {
        return toArgs(List.of());
    }

    public String[] toArgs(List<String> arguments) {
        List<String> args = new LinkedList<>(arguments);
        args.addAll(optionArgs);
        return args.toArray(new String[0]);
    }
}
