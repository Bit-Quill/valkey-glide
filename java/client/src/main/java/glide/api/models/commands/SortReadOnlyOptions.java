package glide.api.models.commands;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@SuperBuilder
public class SortReadOnlyOptions {
    public static final String LIMIT_COMMAND_STRING = "LIMIT";
    public Limit limit;
    public SortBy sortBy;

    @RequiredArgsConstructor
    public enum SortBy {
        ASC("ASC"),
        DESC("DESC");

        private final String redisApi;
    }

    @RequiredArgsConstructor
    public static final class Limit {
        private final Long offset;
        private final Long count;
    }

    @RequiredArgsConstructor
    public static final class ByPattern {
        private final String pattern;
    }

    public String[] toArgs() {
        List<String> optionArgs = new ArrayList<>();
        if (sortBy != null) {
            optionArgs.add(this.sortBy.redisApi);
        }

        if (limit != null) {
            optionArgs.addAll(List.of(LIMIT_COMMAND_STRING, Long.toString(this.limit.offset), Long.toString(this.limit.count)));
        }

        return optionArgs.toArray(new String[0]);
    }
}
