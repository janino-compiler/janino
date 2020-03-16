
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

package org.codehaus.commons.compiler.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.commons.nullanalysis.Nullable;

public final
class StringUtil {

    private StringUtil() {}

    /**
     * Breaks a given string up by the system-dependent path-separator character (on UNIX systems, this character is
     * ":"; on Microsoft Windows systems it is ";"). Empty components are ignored.
     * <p>
     *   UNIX Examples:
     * </p>
     * <dl style="border: 1px solid; padding: 6px">
     *   <dt>A:B:C          <dd>A, B, C
     *   <dt>::B:           <dd>B
     *   <dt>:A             <dd>A
     *   <dt>(Empty string) <dd>(Zero components)
     * </dl>
     *
     * @see File#pathSeparatorChar
     */
    public static File[]
    parsePath(String s) {
        int        from = 0;
        List<File> l    = new ArrayList<File>();
        for (;;) {
            int to = s.indexOf(File.pathSeparatorChar, from);
            if (to == -1) {
                if (from != s.length()) l.add(new File(s.substring(from)));
                break;
            }
            if (to != from) l.add(new File(s.substring(from, to)));
            from = to + 1;
        }
        return (File[]) l.toArray(new File[l.size()]);
    }

    /**
     * Same as {@link #parsePath(String)}, but returns {@code null} iff <em>s</em> {@code == null}.
     */
    @Nullable public static File[]
    parseOptionalPath(@Nullable String s) { return s == null ? null : StringUtil.parsePath(s); }
}
