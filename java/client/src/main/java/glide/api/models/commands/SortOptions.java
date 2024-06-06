package glide.api.models.commands;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;
import java.util.List;

@SuperBuilder
public class SortOptions extends SortReadOnlyOptions {
    public static final String STORE_COMMAND_STRING = "STORE";
//    public final Store store;

//    @RequiredArgsConstructor
//    public static final class Store {
//        private final String destination;
//    }

    public String[] toArgs() {
        List<String> optionArgs = Arrays.asList(super.toArgs());

//        if (store != null) {
//            optionArgs.addAll(List.of(STORE_COMMAND_STRING, store.destination));
//        }

        return optionArgs.toArray(new String[0]);
    }
}
