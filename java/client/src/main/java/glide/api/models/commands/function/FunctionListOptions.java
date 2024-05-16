/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands.function;

/**
 * Option for <code>FUNCTION LIST</code> command.
 *
 * @see <a href="https://redis.io/docs/latest/commands/function-list/">redis.io</a>
 */
public class FunctionListOptions {
    /** Causes the server to include the libraries source implementation in the reply. */
    public static final String WITH_CODE_REDIS_API = "WITHCODE";

    /** REDIS API keyword followed by library name pattern. */
    public static final String LIBRARY_NAME_REDIS_API = "LIBRARYNAME";
}
