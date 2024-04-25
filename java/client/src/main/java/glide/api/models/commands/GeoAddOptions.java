/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import glide.api.commands.GeospatialIndicesBaseCommands;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * Optional arguments for {@link GeospatialIndicesBaseCommands#geoadd(String, Map, GeoAddOptions)}
 * command.
 *
 * @see <a href="https://redis.io/commands/geoadd/">redis.io</a>
 */
@Getter
@Builder
public class GeoAddOptions {
    /** Options for handling existing members. See {@link ConditionalChange} */
    private final ConditionalChange updateMode;

    /** If <code>true</code>, returns the count of changed elements instead of new elements added. */
    @Builder.Default private final boolean changed = false;
}
