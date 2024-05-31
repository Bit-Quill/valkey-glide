/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Supports commands and transactions for the "Scripting and Function" group for standalone and
 * cluster clients.
 *
 * @see <a href="https://redis.io/docs/latest/commands/?group=scripting">Scripting and Function
 *     Commands</a>
 */
public interface ScriptingAndFunctionsCommands {

    /**
     * Loads a library to Redis unless a library with the same name exists. Use {@link
     * #functionLoadReplace} to replace existing libraries.
     *
     * @since Redis 7.0 and above.
     * @see <a href="https://redis.io/docs/latest/commands/function-load/">redis.io</a> for details.
     * @param libraryCode The source code that implements the library.
     * @return The library name that was loaded.
     * @example
     *     <pre>{@code
     * String code = "#!lua name=mylib \n redis.register_function('myfunc', function(keys, args) return args[1] end)";
     * String response = client.functionLoad(code).get();
     * assert response.equals("mylib");
     * }</pre>
     */
    CompletableFuture<String> functionLoad(String libraryCode);

    /**
     * Loads a library to Redis and overwrites a library with the same name if it exists.
     *
     * @since Redis 7.0 and above.
     * @see <a href="https://redis.io/docs/latest/commands/function-load/">redis.io</a> for details.
     * @param libraryCode The source code that implements the library.
     * @return The library name that was loaded.
     * @example
     *     <pre>{@code
     * String code = "#!lua name=mylib \n redis.register_function('myfunc', function(keys, args) return args[1] end)";
     * String response = client.functionLoadReplace(code).get();
     * assert response.equals("mylib");
     * }</pre>
     */
    CompletableFuture<String> functionLoadReplace(String libraryCode);

    /**
     * Kills a function that is currently executing.<br>
     * <code>FUNCTION KILL</code> terminates read-only functions only.
     *
     * @since Redis 7.0 and above.
     * @see <a href="https://redis.io/docs/latest/commands/function-kill/">redis.io</a> for details.
     * @return <code>OK</code>.
     * @example
     *     <pre>{@code
     * String response = client.functionKill().get();
     * assert response.equals("OK");
     * }</pre>
     */
    CompletableFuture<String> functionKill();

    /**
     * Returns information about the function that's currently running and information about the
     * available execution engines.
     *
     * @since Redis 7.0 and above.
     * @see <a href="https://redis.io/docs/latest/commands/function-stats/">redis.io</a> for details.
     * @return A <code>Map</code> with two keys:
     *     <ul>
     *       <li><code>running_script</code> with information about the running script.
     *       <li><code>engines</code> with information about available engines and theirs stats.
     *     </ul>
     *     See example for more details.
     * @example
     *     <pre>{@code
     * Map<String, Map<String, Object>> response = client.functionStats().get();
     * Map<String, Object> running_script_info = response.get("running_script");
     * if (running_script_info != null) {
     *   Object[] command = (Object[]) running_script_info.get("command");
     *   String command_line = Arrays.stream(command).map(String.class::cast).collect(Collectors.joining(" "));
     *   System.out.printf("Server is currently running function '%s' with command line '%s', which runs for %d ms%n",
     *       running_script_info.get("name"), command_line, (long)running_script_info.get("duration_ms"));
     * }
     * Map<String, Object> engines_info = response.get("engines");
     * for (String engine_name : engines_info.keySet()) {
     *   Map<String, Long> engine = (Map<String, Long>) engines_info.get(engine_name);
     *   System.out.printf("Server supports engine '%s', which has %d libraries and %d functions in total%n",
     *       engine_name, engine.get("libraries_count"), engine.get("functions_count"));
     * }
     * }</pre>
     */
    CompletableFuture<Map<String, Map<String, Object>>> functionStats();
}
