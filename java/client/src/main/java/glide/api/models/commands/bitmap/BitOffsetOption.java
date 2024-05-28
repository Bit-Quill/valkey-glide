/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands.bitmap;

import lombok.Getter;

public class BitOffsetOption {
    public interface BitOffset {
        String getOffset();
    }

    public static class Offset implements BitOffset {
        @Getter
        private final String offset;

        public Offset(long offset) {
            this.offset = Long.toString(offset);
        }
    }

    public static class OffsetMultiplier implements BitOffset {
        @Getter
        private final String offset;

        public OffsetMultiplier(long offset) {
            this.offset = "#".concat(Long.toString(offset));
        }
    }
}
