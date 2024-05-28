/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands.bitmap;

import lombok.Getter;

public class BitEncodingOption {
    public interface BitEncoding {
        String getEncoding();
    }

    public static class SignedEncoding implements BitEncoding {
        @Getter
        private final String encoding;

        public SignedEncoding(long encodingLength) {
            encoding = "i".concat(Long.toString(encodingLength));
        }
    }

    public static class UnsignedEncoding implements BitEncoding {
        @Getter
        private final String encoding;

        public UnsignedEncoding(long encodingLength) {
            encoding = "u".concat(Long.toString(encodingLength));
        }
    }
}
