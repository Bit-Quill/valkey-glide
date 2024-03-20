/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.ffi.resolvers;

public class ScriptResolver {

    // TODO: consider lazy loading the glide_rs library
    static {
        System.loadLibrary("glide_rs");
    }

    /**
     * Resolves a Script object from code
     * @param code
     * @return String representing the saved hash
     */
    public static native String storeScript(String code);

    public static native void dropScript(String hash);
}
