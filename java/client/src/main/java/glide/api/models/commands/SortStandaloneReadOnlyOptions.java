package glide.api.models.commands;

import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;
import java.util.List;

@SuperBuilder
public final class SortStandaloneReadOnlyOptions extends SortReadOnlyOptions {
    private final ByPattern byPattern;
    private final GetPattern getPattern;

    @RequiredArgsConstructor
    public static final class ByPattern {
        private final String pattern;
    }

    @RequiredArgsConstructor
    public static final class GetPattern {
        private final String[] patterns;
    }

    public String[] toArgs() {
        List<String> optionArgs = Arrays.asList(super.toArgs());

        if (byPattern != null) {
            optionArgs.addAll(List.of(BY_COMMAND_STRING, byPattern.pattern));
        }

        if (getPattern != null) {
            for (int i = 0; i < getPattern.patterns.length; i ++) {
                optionArgs.addAll(List.of(GET_COMMAND_STRING, byPattern.pattern));
            }
        }

        return optionArgs.toArray(new String[0]);
    }
}
