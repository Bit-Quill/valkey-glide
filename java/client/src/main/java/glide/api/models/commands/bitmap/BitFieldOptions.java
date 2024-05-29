/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands.bitmap;

import static glide.utils.ArrayTransformUtils.concatenateArrays;

import glide.api.commands.BitmapBaseCommands;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Subcommand arguments for {@link BitmapBaseCommands#bitfield(String, BitFieldSubCommands[])} and
 * {@link BitmapBaseCommands#bitfieldReadOnly(String, BitFieldReadOnlySubCommands[])}. Specifies
 * subcommands, bit-encoding type, and offset type.
 *
 * @see <a href="https://redis.io/commands/bitfield/">redis.io</a>
 */
public class BitFieldOptions {
    /** Subcommands for {@link BitmapBaseCommands#bitfield(String, BitFieldSubCommands[])}. */
    public interface BitFieldSubCommands {
        String[] createArgs();
    }

    /**
     * Subcommands for {@link BitmapBaseCommands#bitfieldReadOnly(String,
     * BitFieldReadOnlySubCommands[])}.
     */
    public interface BitFieldReadOnlySubCommands extends BitFieldSubCommands {}

    /**
     * GET subcommand for {@link BitmapBaseCommands#bitfield(String, BitFieldSubCommands[])} and
     * {@link BitmapBaseCommands#bitfieldReadOnly(String, BitFieldReadOnlySubCommands[])}.
     */
    @RequiredArgsConstructor
    public static class BitFieldGet implements BitFieldReadOnlySubCommands {
        private final BitEncoding encoding;
        private final BitOffset offset;

        /**
         * Creates the GET subcommand arguments.
         *
         * @return a String array with GET arguments.
         */
        public String[] createArgs() {
            return new String[] {"GET", encoding.getEncoding(), offset.getOffset()};
        }
    }

    /** SET subcommand for {@link BitmapBaseCommands#bitfield(String, BitFieldSubCommands[])}. */
    @RequiredArgsConstructor
    public static class BitFieldSet implements BitFieldSubCommands {
        private final BitEncoding encoding;
        private final BitOffset offset;
        private final long value;

        /**
         * Creates the SET subcommand arguments.
         *
         * @return a String array with SET arguments.
         */
        public String[] createArgs() {
            return new String[] {"SET", encoding.getEncoding(), offset.getOffset(), Long.toString(value)};
        }
    }

    /** INCRBY subcommand for {@link BitmapBaseCommands#bitfield(String, BitFieldSubCommands[])}. */
    @RequiredArgsConstructor
    public static class BitFieldIncrby implements BitFieldSubCommands {
        private final BitEncoding encoding;
        private final BitOffset offset;
        private final long increment;

        /**
         * Creates the INCRBY subcommand arguments.
         *
         * @return a String array with INCRBY arguments.
         */
        public String[] createArgs() {
            return new String[] {
                "INCRBY", encoding.getEncoding(), offset.getOffset(), Long.toString(increment)
            };
        }
    }

    /** OVERFLOW subcommand for {@link BitmapBaseCommands#bitfield(String, BitFieldSubCommands[])}. */
    @RequiredArgsConstructor
    public static class BitFieldOverflow implements BitFieldSubCommands {
        private final BitOverflowControl overflowControl;

        /**
         * Creates the OVERFLOW subcommand arguments.
         *
         * @return a String array with OVERFLOW arguments.
         */
        public String[] createArgs() {
            return new String[] {"OVERFLOW", overflowControl.toString()};
        }

        /** Supported overflow controls */
        public enum BitOverflowControl {
            WRAP,
            SAT,
            FAIL
        }
    }

    private interface BitEncoding {
        String getEncoding();
    }

    /** Specifies that the argument is a signed encoding */
    public static final class SignedEncoding implements BitEncoding {
        @Getter private final String encoding;

        /**
         * Constructor that prepends the number with "i" to specify that it is in signed encoding.
         *
         * @param encodingLength bit size of encoding.
         */
        public SignedEncoding(long encodingLength) {
            encoding = "i".concat(Long.toString(encodingLength));
        }
    }

    /** Specifies that the argument is a signed encoding */
    public static final class UnsignedEncoding implements BitEncoding {
        @Getter private final String encoding;

        /**
         * Constructor that prepends the number with "u" to specify that it is in unsigned encoding.
         *
         * @param encodingLength bit size of encoding.
         */
        public UnsignedEncoding(long encodingLength) {
            encoding = "u".concat(Long.toString(encodingLength));
        }
    }

    private interface BitOffset {
        String getOffset();
    }

    /** Offset in the array of bits. */
    public static final class Offset implements BitOffset {
        @Getter private final String offset;

        /**
         * Constructor for Offset.
         *
         * @param offset element in the array of bits.
         */
        public Offset(long offset) {
            this.offset = Long.toString(offset);
        }
    }

    /** Offset in the array of bits multiplied by the encoding value. */
    public static final class OffsetMultiplier implements BitOffset {
        @Getter private final String offset;

        /**
         * Constructor that prepends the offset value with "#" to specify that it is a multiplier.
         *
         * @param offset element multiplied by the encoding value in the array of bits.
         */
        public OffsetMultiplier(long offset) {
            this.offset = "#".concat(Long.toString(offset));
        }
    }

    /**
     * Creates the arguments to be used in {@link BitmapBaseCommands#bitfield(String,
     * BitFieldSubCommands[])} and {@link BitmapBaseCommands#bitfieldReadOnly(String,
     * BitFieldReadOnlySubCommands[])}.
     *
     * @param subCommands commands that holds arguments to be included in the argument String array.
     * @return a String array that holds the sub commands and their arguments.
     */
    public static String[] createBitFieldArgs(BitFieldSubCommands[] subCommands) {
        String[] arguments = {};

        for (int i = 0; i < subCommands.length; i++) {
            arguments = concatenateArrays(arguments, subCommands[i].createArgs());
        }

        return arguments;
    }
}
