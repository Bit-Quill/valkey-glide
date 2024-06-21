/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.exceptions;

/** Errors that are thrown when a request cannot be completed in current configuration settings. */
public class WrongConfiguration extends RedisException {
    public WrongConfiguration(String message) {
        super(message);
    }
}
