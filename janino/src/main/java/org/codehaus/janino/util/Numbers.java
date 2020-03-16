
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2019 Arno Unkrig. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino.util;

import org.codehaus.commons.nullanalysis.Nullable;

public final
class Numbers {

    private Numbers() {}

    /**
     * Counterpart of {@link Integer#parseInt(String, int)} for parsing <em>unsigned</em> integers.
     * <p>
     *   Redundant with {@code java.lang.Integer.parseUnsignedInt(String, int radix)}, which is available since Java 8.
     * </p>
     *
     * @return                       0 through 2<sup>32</sup> - 1
     * @throws NumberFormatException <var>s</var> is {@code null} or empty
     * @throws NumberFormatException <var>radix</var> is out of range (see {@link Character#digit(char, int)})
     * @throws NumberFormatException The value represented by <var>s</var> is larger than 2<sup>32</sup> - 1
     * @throws NumberFormatException <var>s</var> contains characters that are <em>not</em> valid digits
     *                               for the given <var>radix</var> (see {@link Character#digit(char, int)})
     */
    public static int
    parseUnsignedInt(@Nullable String s, int radix) throws NumberFormatException {

        if (s == null || s.isEmpty()) throw new NumberFormatException("null");

        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("Invalid radix " + radix);
        }

        int limit  = Numbers.INT_LIMITS[radix];
        int result = 0;
        for (int i = 0; i < s.length(); ++i) {

            if (result < 0 || (radix != 2 && result >= limit)) {
                throw new NumberFormatException("For input string \"" + s + "\" and radix " + radix);
            }

            int digitValue = Character.digit(s.charAt(i), radix);
            if (digitValue == -1) throw new NumberFormatException("For input string \"" + s + "\" and radix " + radix);

            int result2 = result * radix;
            result = result2 + digitValue;
            if (result2 < 0 && result >= 0) {
                throw new NumberFormatException("For input string \"" + s + "\" and radix " + radix);
            }
        }
        return result;
    }
    private static final int[] INT_LIMITS = {
        0,           // base 0 - not used
        0,           // base 1 - not used
        -2147483648, // base 2 - binary
        1431655766,
        1073741824,
        858993460,
        715827883,
        613566757,
        536870912,   // base 8 -- octal
        477218589,
        429496730,
        390451573,
        357913942,
        330382100,
        306783379,
        286331154,
        268435456,   // base 16 - hexadecimal
        252645136,
        238609295,
        226050911,
        214748365,
        204522253,
        195225787,
        186737709,
        178956971,
        171798692,
        165191050,
        159072863,
        153391690,
        148102321,
        143165577,
        138547333,
        134217728,
        130150525,
        126322568,
        122713352,
        119304648,   // base 36
    };

    /**
     * Counterpart of {@link Long#parseLong(String, int)} for parsing <em>unsigned</em> integers.
     * <p>
     *   Redundant with {@code java.lang.Long.parseUnsignedLong(String, int radix)}, which is available since Java 8.
     * </p>
     *
     * @return                       0 through 2<sup>64</sup> - 1
     * @throws NumberFormatException <var>s</var> is {@code null} or empty
     * @throws NumberFormatException <var>radix</var> is out of range (see {@link Character#digit(char, int)})
     * @throws NumberFormatException The value represented by <var>s</var> is larger than 2<sup>64</sup> - 1
     * @throws NumberFormatException <var>s</var> contains characters that are <em>not</em> valid digits
     *                               for the given <var>radix</var> (see {@link Character#digit(char, int)})
     */
    public static long
    parseUnsignedLong(@Nullable String s, int radix) throws NumberFormatException {

        if (s == null || s.isEmpty()) throw new NumberFormatException("null");

        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("Invalid radix " + radix);
        }

        long limit  = Numbers.LONG_LIMITS[radix];
        long result = 0L;
        for (int i = 0; i < s.length(); ++i) {

            if (result < 0 || (radix != 2 && result >= limit)) {
                throw new NumberFormatException("For input string \"" + s + "\" and radix " + radix);
            }

            int digitValue = Character.digit(s.charAt(i), radix);
            if (digitValue == -1) throw new NumberFormatException("For input string \"" + s + "\" and radix " + radix);

            long result2 = result * radix;
            result = result2 + digitValue;
            if (result2 < 0 && result >= 0) {
                throw new NumberFormatException("For input string \"" + s + "\" and radix " + radix);
            }
        }
        return result;
    }
    private static final long[] LONG_LIMITS = {
        0,                     // base 0 - not used
        0,                     // base 1 - not used
        -9223372036854775808L, // base 2 - binary
        6148914691236517206L,
        4611686018427387904L,
        3689348814741910324L,
        3074457345618258603L,
        2635249153387078803L,
        2305843009213693952L,  // base 8 -- octal
        2049638230412172402L,
        1844674407370955162L,
        1676976733973595602L,
        1537228672809129302L,
        1418980313362273202L,
        1317624576693539402L,
        1229782938247303442L,
        1152921504606846976L,  // base 16 - hexadecimal
        1085102592571150096L,
        1024819115206086201L,
        970881267037344822L,
        922337203685477581L,
        878416384462359601L,
        838488366986797801L,
        802032351030850071L,
        768614336404564651L,
        737869762948382065L,
        709490156681136601L,
        683212743470724134L,
        658812288346769701L,
        636094623231363849L,
        614891469123651721L,
        595056260442243601L,
        576460752303423488L,
        558992244657865201L,
        542551296285575048L,
        527049830677415761L,
        512409557603043101L,   // base 36
    };
}
