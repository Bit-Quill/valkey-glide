package glide.api.models.commands;

import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuperBuilder
public final class SortOptions {
    public static final String LIMIT_COMMAND_STRING = "LIMIT";
    public static final String ALPHA_COMMAND_STRING = "ALPHA";
    private final Limit limit;
    private final Order order;
    private final boolean alpha;

    @RequiredArgsConstructor
    public static final class Limit {
        private final Long offset;
        private final Long count;
    }

    @RequiredArgsConstructor
    public enum Order {
        ASC("ASC"),
        DESC("DESC");

        private final String redisApi;
    }

    public String[] toArgs() {
        List<String> optionArgs = new ArrayList<>();

        if (limit != null) {
            optionArgs.addAll(List.of(LIMIT_COMMAND_STRING, Long.toString(this.limit.offset), Long.toString(this.limit.count)));
        }

        if (order != null) {
            optionArgs.add(this.order.redisApi);
        }

        if (alpha) {
            optionArgs.add(ALPHA_COMMAND_STRING);
        }

        return optionArgs.toArray(new String[0]);
    }
}
