
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino;

import java.util.*;

/**
 * Representation of a "method descriptor" (JVMS 4.3.3).
 */
public class MethodDescriptor {

    /** The field descriptors of the method parameters. */
    public final String[] parameterFDs;

    /** The field descriptor of the method return value. */
    public final String   returnFD;

    /** */
    public MethodDescriptor(String[] parameterFDs, String returnFD) {
        this.parameterFDs = parameterFDs;
        this.returnFD     = returnFD;
    }

    /**
     * Parse a method descriptor into parameter FDs and return FDs.
     */
    public MethodDescriptor(String s) {
        if (s.charAt(0) != '(') throw new RuntimeException();

        int from = 1;
        List parameterFDs = new ArrayList(); // String
        while (s.charAt(from) != ')') {
            int to = from;
            while (s.charAt(to) =='[') ++to;
            if ("BCDFIJSZ".indexOf(s.charAt(to)) != -1) {
                ++to;
            } else
            if (s.charAt(to) == 'L') {
                for (++to; s.charAt(to) != ';'; ++to);
                ++to;
            } else {
                throw new RuntimeException();
            }
            parameterFDs.add(s.substring(from, to));
            from = to;
        }
        this.parameterFDs = (String[]) parameterFDs.toArray(new String[parameterFDs.size()]);
        this.returnFD = s.substring(++from);
    }

    /**
     * Returns the "method descriptor" (JVMS 4.3.3).
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("(");
        for (int i = 0; i < this.parameterFDs.length; ++i) sb.append(this.parameterFDs[i]);
        return sb.append(')').append(this.returnFD).toString();
    }

    /**
     * Patch an additional parameter into a given method descriptor.
     */
    public static String prependParameter(String md, String parameterFD) {
        return '(' + parameterFD + md.substring(1);
    }
}
