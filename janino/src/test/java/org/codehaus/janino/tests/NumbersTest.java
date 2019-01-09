
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

package org.codehaus.janino.tests;

import java.math.BigInteger;

import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.util.Numbers;
import org.junit.Assert;
import org.junit.Test;

// SUPPRESS CHECKSTYLE JavadocMethod:9999

public
class NumbersTest {

    @Test public void
    testParseUnsignedInt() throws Exception {
        for (int radix = 5/*2*/; radix <= 36; radix++) {

            for (int x = 0; x <= 100; x++) {
                NumbersTest.assertParseUnsignedIntCompletesNormally(x, radix);
            }

            for (int delta = 100; delta >= 1; delta--) {
                NumbersTest.assertParseUnsignedIntCompletesNormally("delta=" + delta, (1L << 32) - delta, radix);
            }

            for (int delta = 0; delta <= 100; delta++) {
                NumbersTest.assertParseUnsignedIntThrowsNumberFormatException(
                    "delta=" + delta,
                    (1L << 32) + delta,
                    radix
                );
            }
        }
    }

    @Test public void
    testParseUnsignedLong() throws Exception {
        for (int radix = 2; radix <= 36; radix++) {
            for (int delta = 100; delta >= 0; delta--) {
                NumbersTest.assertParseUnsignedLongThrowsNumberFormatException(
                    "delta=" + delta,
                    BigInteger.valueOf(2).pow(64).add(BigInteger.valueOf(delta)),
                    radix
                );
            }
            for (int delta = 1; delta <= 100; delta++) {
                NumbersTest.assertParseUnsignedLongCompletesNormally(
                    "delta=" + delta,
                    BigInteger.valueOf(2).pow(64).subtract(BigInteger.valueOf(delta)),
                    radix
                );
            }
        }
    }

    /**
     * @throws AssertionError {@link Numbers#parseUnsignedInt(String, int)} does not complete normally
     */
    private static void
    assertParseUnsignedIntCompletesNormally(final long n, final int radix) {
        NumbersTest.assertParseUnsignedIntCompletesNormally(null, n, radix);
    }

    /**
     * @throws AssertionError {@link Numbers#parseUnsignedInt(String, int)} does not complete normally
     */
    private static void
    assertParseUnsignedIntCompletesNormally(@Nullable String message, final long n, final int radix) {
        final String s      = Long.toString(n, radix);
        final long[] result = new long[1];
        NumbersTest.assertCompletesNormally(message, new Runnable() {
            @Override public void run() { result[0] = Numbers.parseUnsignedInt(s, radix) & 0xffffffffL; }
        });
        Assert.assertEquals(n, result[0]);
    }

    /**
     * @throws AssertionError {@link Numbers#parseUnsignedInt(String, int)} does not throw a {@link
     *                        NumberFormatException}
     */
    private static void
    assertParseUnsignedIntThrowsNumberFormatException(@Nullable String message, long n, final int radix) {
        final String s = Long.toString(n, radix);
        NumbersTest.assertThrows(message, NumberFormatException.class, new Runnable() {
            @Override public void run() { Numbers.parseUnsignedInt(s, radix); }
        });
    }


    /**
     * @throws AssertionError {@link Numbers#parseUnsignedLong(String, int)} does not complete normally
     */
    private static void
    assertParseUnsignedLongCompletesNormally(@Nullable String message, final BigInteger n, final int radix) {
        final String s      = n.toString(radix);
        final long[] result = new long[1];
        NumbersTest.assertCompletesNormally(message, new Runnable() {
            @Override public void run() { result[0] = Numbers.parseUnsignedLong(s, radix); }
        });
        Assert.assertEquals(
            message,
            n,
            BigInteger.valueOf(result[0]).and(BigInteger.valueOf(2).pow(64).subtract(BigInteger.ONE))
        );
    }

    /**
     * @throws AssertionError {@link Numbers#parseUnsignedLong(String, int)} does not throw a {@link
     *                        NumberFormatException}
     */
    private static void
    assertParseUnsignedLongThrowsNumberFormatException(@Nullable String message, BigInteger n, final int radix) {
        final String s = n.toString(radix);
        NumbersTest.assertThrows(
            (message == null ? "" : message + ": ") + s + " (radix " + radix + ")",
            NumberFormatException.class,
            new Runnable() {
                @Override public void run() { Numbers.parseUnsignedLong(s, radix); }
            }
        );
    }

    /**
     * @throws AssertionError The <var>runnable</var> does not complete normally
     */
    private static void
    assertCompletesNormally(@Nullable String message, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            Assert.fail((message == null ? "" : message + ": ") + "Throws \"" + t + "\"");
        }
    }

    /**
     * @throws AssertionError The <var>runnable</var> completes normally
     * @throws AssertionError The <var>runnable</var> throws an exception other than <var>expected</var>
     */
    private static void
    assertThrows(@Nullable String message, Class<? extends Throwable> expected, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            if (t.getClass() != expected) {
                Assert.fail((
                    (message == null ? "" : message + ": ")
                    + expected.getName()
                    + " expected instead of \""
                    + t
                    + "\""
                ));
            }
            return;
        }
        Assert.fail((message == null ? "" : message + ": ") + expected.getName() + " expected");
    }
}
