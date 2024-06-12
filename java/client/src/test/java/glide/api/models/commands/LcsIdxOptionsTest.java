/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands;

import static glide.api.models.commands.LcsIdxOptions.IDX_REDIS_API;
import static glide.api.models.commands.LcsIdxOptions.MINMATCHLEN_REDIS_API;
import static glide.api.models.commands.LcsIdxOptions.WITH_MATCHLEN_REDIS_API;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LcsIdxOptionsTest {

    @Test
    void toArgs() {
        // with both options
        LcsIdxOptions option =
                new LcsIdxOptions.IdxOptionBuilder().minMatchLen(3).withMatchLen(true).build();
        String[] args = option.toArgs();
        assertArrayEquals(
                args, new String[] {IDX_REDIS_API, MINMATCHLEN_REDIS_API, "3", WITH_MATCHLEN_REDIS_API});

        // only IDX
        LcsIdxOptions option2 = new LcsIdxOptions.IdxOptionBuilder().build();
        String[] args2 = option2.toArgs();
        assertArrayEquals(args2, new String[] {IDX_REDIS_API});
    }
}
