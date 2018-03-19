
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a "method descriptor" (JVMS 4.3.3).
 */
public
class MethodDescriptor {

    /**
     * The field descriptors of the method parameters.
     */
    public final String[] parameterFds;

    /**
     * The field descriptor of the method return value.
     */
    public final String   returnFd;

    /***/
    public
    MethodDescriptor(String[] parameterFds, String returnFd) {
        this.parameterFds = parameterFds;
        this.returnFd     = returnFd;
    }

    /**
     * Parses a method descriptor into parameter FDs and return FDs.
     */
    public
    MethodDescriptor(String s) {
        if (s.charAt(0) != '(') throw new InternalCompilerException();

        int          from         = 1;
        List<String> parameterFDs = new ArrayList<String>();
        while (s.charAt(from) != ')') {
            int to = from;
            while (s.charAt(to) == '[') ++to;
            if ("BCDFIJSZ".indexOf(s.charAt(to)) != -1) {
                ++to;
            } else
            if (s.charAt(to) == 'L') {
                for (++to; s.charAt(to) != ';'; ++to);
                ++to;
            } else {
                throw new InternalCompilerException();
            }
            parameterFDs.add(s.substring(from, to));
            from = to;
        }
        this.parameterFds = (String[]) parameterFDs.toArray(new String[parameterFDs.size()]);
        this.returnFd     = s.substring(++from);
    }

    /**
     * @return The number of slots required for invoking this method.
     */
    public int
    parameterSize() {
        int paramSize = 0;
        String[] paramFds = this.parameterFds;
        for (int i = 0; i < paramFds.length; i++) {
            paramSize += Descriptor.size(paramFds[i]);
        }
        return paramSize;
    }

    /**
     * @return The "method descriptor" (JVMS 4.3.3)
     */
    @Override public String
    toString() {
        StringBuilder sb = new StringBuilder("(");
        for (String parameterFd : this.parameterFds) sb.append(parameterFd);
        return sb.append(')').append(this.returnFd).toString();
    }

    /**
     * Patches an additional parameter into a given method descriptor.
     */
    public static String
    prependParameter(String md, String parameterFd) { return '(' + parameterFd + md.substring(1); }
}
