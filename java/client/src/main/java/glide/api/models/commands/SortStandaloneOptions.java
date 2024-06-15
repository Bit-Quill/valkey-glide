/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

/**
 * Optional arguments to {@link glide.api.commands.GenericCommands#sort(String,
 * SortStandaloneOptions)}, {@link glide.api.commands.GenericCommands#sort(String, SortOptions,
 * SortStandaloneOptions)}, {@link glide.api.commands.GenericCommands#sortReadOnly(String,
 * SortStandaloneOptions)}, {@link glide.api.commands.GenericCommands#sortReadOnly(String,
 * SortOptions, SortStandaloneOptions)}, {@link
 * glide.api.commands.GenericCommands#sortWithStore(String, String, SortStandaloneOptions)}, and
 * {@link glide.api.commands.GenericCommands#sortWithStore(String, String, SortOptions,
 * SortStandaloneOptions)}
 *
 * @see <a href="https://redis.io/commands/sort/">redis.io</a> and <a
 *     href="https://redis.io/docs/latest/commands/sort_ro/">redis.io</a>
 */
@Builder
public class SortStandaloneOptions {
    /**
     * <code>BY</code> subcommand string to include in the <code>SORT</code> and <code>SORT_RO</code>
     * commands.
     */
    public static final String BY_COMMAND_STRING = "BY";

    /**
     * <code>GET</code> subcommand string to include in the <code>SORT</code> and <code>SORT_RO</code>
     * commands.
     */
    public static final String GET_COMMAND_STRING = "GET";

    /**
     * A pattern to sort by external keys instead of by the elements stored at the key themselves. The
     * pattern should contain an asterisk (*) as a placeholder for the element values, where the value
     * from the key replaces the asterisk to create the key name. For example, if <code>key</code>
     * contains IDs of objects, <code>byPattern</code> can be used to sort these IDs based on an
     * attribute of the objects, like their weights or timestamps.
     */
    private final String byPattern;

    /**
     * A pattern used to retrieve external keys' values, instead of the elements at <code>key</code>.
     * The pattern should contain an asterisk (*) as a placeholder for the element values, where the
     * value from <code>key</code> replaces the asterisk to create the <code>key</code> name. This
     * allows the sorted elements to be transformed based on the related keys values. For example, if
     * <code>key</code> contains IDs of users, <code>getPatterns</code> can be used to retrieve
     * specific attributes of these users, such as their names or email addresses. E.g., if <code>
     * getPatterns</code> is <code>name_*</code>, the command will return the values of the keys
     * <code>name_&lt;element&gt;</code> for each sorted element. Multiple <code>getPatterns</code>
     * arguments can be provided to retrieve multiple attributes. The special value <code>#</code> can
     * be used to include the actual element from `key` being sorted. If not provided, only the sorted
     * elements themselves are returned.
     */
    private final String[] getPatterns;

    /**
     * Creates the arguments to be used in {@link glide.api.commands.GenericCommands#sort(String,
     * SortStandaloneOptions)}, {@link glide.api.commands.GenericCommands#sort(String, SortOptions,
     * SortStandaloneOptions)}, {@link glide.api.commands.GenericCommands#sortReadOnly(String,
     * SortStandaloneOptions)}, {@link glide.api.commands.GenericCommands#sortReadOnly(String,
     * SortOptions, SortStandaloneOptions)}, {@link
     * glide.api.commands.GenericCommands#sortWithStore(String, String, SortStandaloneOptions)}, and
     * {@link glide.api.commands.GenericCommands#sortWithStore(String, String, SortOptions,
     * SortStandaloneOptions)}.
     *
     * @return a String array that holds the sub commands and their arguments.
     */
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
