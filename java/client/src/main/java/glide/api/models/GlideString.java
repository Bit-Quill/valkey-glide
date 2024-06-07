/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models;

import java.util.Arrays;
import lombok.Getter;

// TODO docs for the god of docs
public class GlideString {

    @Getter private byte[] bytes;
    private String string = null;

    /** Flag whether possibility to convert to string was checked. */
    private boolean conversionChecked = false;

    private GlideString() {}

    public static GlideString of(String string) {
        var res = new GlideString();
        res.string = string;
        res.bytes = string.getBytes();
        return res;
    }

    public static GlideString of(byte[] bytes) {
        var res = new GlideString();
        res.bytes = bytes;
        return res;
    }

    @Override
    public String toString() {
        return getString();
    }

    public String getString() {
        if (string != null) {
            return string;
        }

        assert canConvertToString() : "Value cannot be represented as a string";
        return string;
    }

    public synchronized boolean canConvertToString() {
        if (string != null) {
            return true;
        }

        if (conversionChecked) {
            return false;
        }

        // TODO find a better way to check this
        var tmpStr = new String(bytes);
        var tmpStrBytes = tmpStr.getBytes();
        if (tmpStrBytes.length != bytes.length) {
            conversionChecked = true;
            return false;
        }
        if (compareBytes(tmpStrBytes)) {
            string = tmpStr;
            return true;
        } else {
            conversionChecked = true;
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GlideString)) return false;
        GlideString that = (GlideString) o;

        if (string != null && string.equals(that.string)) {
            return true;
        }

        return compareBytes(that.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    private boolean compareBytes(byte[] other) {
        if (other.length != bytes.length) {
            return false;
        }

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != other[i]) {
                return false;
            }
        }
        return true;
    }
}
