/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands.geospatial;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/*
 * Enumeration representing distance units options for the `GEODIST` command.
 */
@RequiredArgsConstructor
@Getter
public enum GeoUnit {
    METERS("m"), // Represents distance in meters.
    KILOMETERS("km"), // Represents distance in kilometers.
    MILES("mi"), // Represents distance in miles.
    FEET("ft"); // Represents distance in feet.

    private final String redisApi;
}
