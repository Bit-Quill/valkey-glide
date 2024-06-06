package glide.api.models.commands;

import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;
import java.util.List;

@SuperBuilder
public final class SortStandaloneOptions extends SortOptions {
    public String[] toArgs() {
        List<String> optionArgs = Arrays.asList(super.toArgs());


        return optionArgs.toArray(new String[0]);
    }
}
