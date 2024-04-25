/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a geographic position defined by longitude and latitude.<br>
 * <br>
 * The exact limits, as specified by EPSG:900913 / EPSG:3785 / OSGEO:41001 are the following:
 *
 * <ul>
 *   <li>Valid longitudes are from -180 to 180 degrees.
 *   <li>Valid latitudes are from -85.05112878 to 85.05112878 degrees.
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public class GeospatialData {
    /** The longitude coordinate. */
    private final double longitude;

    /** The latitude coordinate. */
    private final double latitude;
}
