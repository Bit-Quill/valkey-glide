/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LmPopOptions {
    LEFT("LEFT"),
    RIGHT("RIGHT"),
    COUNT("COUNT");

    private final String redisApi;
}
