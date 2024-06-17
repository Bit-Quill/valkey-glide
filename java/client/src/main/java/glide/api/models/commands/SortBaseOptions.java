/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import lombok.experimental.SuperBuilder;

/**
 * Optional arguments to {@link glide.api.commands.GenericBaseCommands#sort(String,
 * SortBaseOptions)}, {@link glide.api.commands.GenericBaseCommands#sortReadOnly(String,
 * SortBaseOptions)}, and {@link glide.api.commands.GenericBaseCommands#sortWithStore(String,
 * String, SortBaseOptions)}
 */
@SuperBuilder
public class SortBaseOptions extends SortOptions {}
