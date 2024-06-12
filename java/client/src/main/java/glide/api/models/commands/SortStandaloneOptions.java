/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class SortStandaloneOptions {
    public static final String BY_COMMAND_STRING = "BY";
    public static final String GET_COMMAND_STRING = "GET";
    private final String byPattern;
    private final String[] getPatterns;

    public String[] toArgs() {
        List<String> optionArgs = new ArrayList<>();

        if (byPattern != null) {
            optionArgs.addAll(List.of(BY_COMMAND_STRING, byPattern));
        }

        if (getPatterns != null) {
            for (int i = 0; i < getPatterns.length; i++) {
                optionArgs.addAll(List.of(GET_COMMAND_STRING, getPatterns[i]));
            }
        }

        return optionArgs.toArray(new String[0]);
    }
}
