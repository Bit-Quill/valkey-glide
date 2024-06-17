/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api;

import java.util.List;
import org.mockito.ArgumentMatcher;

/**
 * Argument matcher for comparing lists of byte arrays.
 *
 * <p>It is used in Mockito verifications to assert that a method call was made with a specific list
 * of byte arrays as arguments.
 */
public class ByteArrayArgumentMatcher implements ArgumentMatcher<List<byte[]>> {
    List<byte[]> arguments;

    /**
     * Constructs a new ByteArrayArgumentMatcher with the provided list of byte arrays.
     *
     * @param arguments The list of byte arrays to match against.
     */
    public ByteArrayArgumentMatcher(List<byte[]> arguments) {
        this.arguments = arguments;
    }

    /**
     * Matches the provided list of byte arrays against the stored arguments.
     *
     * @param t The list of byte arrays to match
     * @return boolean - true if the provided list of byte arrays matches the stored arguments, false
     *     Sotherwise.
     */
    @Override
    public boolean matches(List<byte[]> t) {
        int length = t.size();

        for (int index = 0; index < length; index++) {
            if (!(new String(t.get(index)).equals(new String(arguments.get(index))))) return false;
        }
        return true;
    }
}
