
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2020 Arno Unkrig. All rights reserved.
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

package org.codehaus.janino;

import java.util.Arrays;

import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute.VerificationTypeInfo;

class StackMap {

    /**
     * Elements are never changed.
     */
    private final VerificationTypeInfo[] locals, operands;

    StackMap(VerificationTypeInfo[] locals, VerificationTypeInfo[] operands) {
        this.locals    = (VerificationTypeInfo[]) locals.clone();
        this.operands  = (VerificationTypeInfo[]) operands.clone();
    }

    // -----------------------

    /**
     * @return A {@link StackMap} with a local variable stack that is extended by <var>local</var>, and the same
     *         operand stack
     */
    StackMap
    pushLocal(VerificationTypeInfo local) {
        return new StackMap(StackMap.addToArray(this.locals, local), this.operands);
    }

    /**
     * @return A {@link StackMap} with a local variable stack with one element less, and the same operand stack
     */
    StackMap
    popLocal() { return new StackMap(StackMap.removeLastFromArray(this.locals), this.operands); }

    /**
     * @return The top element of the local variable stack
     */
    VerificationTypeInfo
    peekLocal() { return this.locals[this.locals.length - 1]; }

    VerificationTypeInfo[]
    locals() { return (VerificationTypeInfo[]) this.locals.clone(); }

    // -----------------------

    /**
     * @return A {@link StackMap} with the same local variable stack, and an operand stack that is extended by
     *         <var>operand</var>
     */
    StackMap
    pushOperand(VerificationTypeInfo operand) {
        return new StackMap(this.locals, StackMap.addToArray(this.operands, operand));
    }

    /**
     * @return A {@link StackMap} with the same local variable stack, and an operand stack with one element less
     */
    StackMap
    popOperand() { return new StackMap(this.locals, StackMap.removeLastFromArray(this.operands)); }

    /**
     * @return The top element of the operand stack
     */
    VerificationTypeInfo
    peekOperand() { return this.operands[this.operands.length - 1]; }

    VerificationTypeInfo[]
    operands() { return (VerificationTypeInfo[]) this.operands.clone(); }

    // -----------------------

    private static VerificationTypeInfo[]
    addToArray(VerificationTypeInfo[] original, VerificationTypeInfo value) {
        int                    l      = original.length;
        VerificationTypeInfo[] result = new VerificationTypeInfo[l + 1];

        int i;
        for (i = 0; i < l; i++) result[i] = original[i];
        result[i] = value;

        return result;
    }

    private static VerificationTypeInfo[]
    removeLastFromArray(VerificationTypeInfo[] original) {
        int                    l      = original.length - 1;
        VerificationTypeInfo[] result = new VerificationTypeInfo[l];

        for (int i = 0; i < l; i++) result[i] = original[i];

        return result;
    }

    @Override public String
    toString() { return "locals=" + Arrays.toString(this.locals) + ", stack=" + Arrays.toString(this.operands); }

    @Override public int
    hashCode() { return Arrays.hashCode(this.locals) ^ Arrays.hashCode(this.operands); }

    @Override public boolean
    equals(@Nullable Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof StackMap)) return false;
        StackMap that = (StackMap) obj;
        return Arrays.equals(this.locals, that.locals) && Arrays.equals(this.operands, that.operands);
    }
}
