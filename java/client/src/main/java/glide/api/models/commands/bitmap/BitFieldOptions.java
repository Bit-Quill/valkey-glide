/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands.bitmap;

import glide.api.models.commands.bitmap.BitEncodingOption.BitEncoding;
import glide.api.models.commands.bitmap.BitOffsetOption.BitOffset;
import java.util.ArrayList;
import java.util.Arrays;
import lombok.NonNull;

public class BitFieldOptions {
    public interface BitFieldSubCommands {
        String[] createArgs();
    }

    public interface BitFieldReadOnlySubCommands extends BitFieldSubCommands {}

    public static class BitFieldGet implements BitFieldReadOnlySubCommands {
        private final String ENCODING;
        private final String OFFSET;

        public BitFieldGet(@NonNull BitEncoding encoding, @NonNull BitOffset offset) {
            ENCODING = encoding.getEncoding();
            OFFSET = offset.getOffset();
        }

        public String[] createArgs() {
            return new String[] {"GET", ENCODING, OFFSET};
        }
    }

    public static class BitFieldSet implements BitFieldSubCommands {
        private final String ENCODING;
        private final String OFFSET;
        private final String VALUE;

        public BitFieldSet(@NonNull BitEncoding encoding, @NonNull BitOffset offset, long value) {
            ENCODING = encoding.getEncoding();
            OFFSET = offset.getOffset();
            VALUE = Long.toString(value);
        }

        public String[] createArgs() {
            return new String[] {"SET", ENCODING, OFFSET, VALUE};
        }
    }

    public static class BitFieldIncrby implements BitFieldSubCommands {
        private final String ENCODING;
        private final String OFFSET;
        private final String INCREMENT;

        public BitFieldIncrby(@NonNull BitEncoding encoding, @NonNull BitOffset offset, long increment) {
            ENCODING = encoding.getEncoding();
            OFFSET = offset.getOffset();
            INCREMENT = Long.toString(increment);
        }

        public String[] createArgs() {
            return new String[] {"INCRBY", ENCODING, OFFSET, INCREMENT};
        }
    }

    public static class BitFieldOverflow implements BitFieldSubCommands {
        private final String overflowControl;

        public BitFieldOverflow(@NonNull BitOverflowControl overflowControl) {
            this.overflowControl = overflowControl.toString();
        }

        public String[] createArgs() {
            return new String[] {"OVERFLOW", overflowControl};
        }

        public enum BitOverflowControl {
            WRAP,
            SAT,
            FAIL
        }
    }

    public static String[] createBitFieldArgs(BitFieldSubCommands[] subCommands) {
        ArrayList<String> arguments = new ArrayList<>();

        Arrays.stream(subCommands)
                .forEach(subCommand -> arguments.addAll(Arrays.asList(subCommand.createArgs())));
        return arguments.toArray(new String[arguments.size()]);
    }
}
