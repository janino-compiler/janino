
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright 2004 Arno Unkrig
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.janino;

import java.util.ArrayList;
import java.util.List;

public class MethodDescriptor {
    public final String[] parameterFDs;
    public final String   returnFD;

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

    public String toString() {
        StringBuffer sb = new StringBuffer("(");
        for (int i = 0; i < this.parameterFDs.length; ++i) sb.append(this.parameterFDs[i]);
        return sb.append(')').append(this.returnFD).toString();
    }
}
