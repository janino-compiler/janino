
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2016, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino.tools.jsh;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Provides basic functionality for jsh (globbing, etc.), and is designed for brevity, i.e. short method signatures.
 */
public final
class Brief {

    private Brief() {}

    /**
     * Prints the <var>subject</var> to {@code System.out}.
     */
    public static void
    out(@Nullable Object subject) { Brief.print(subject, System.out); }

    /**
     * Prints the <var>subject</var> to {@code System.err}.
     */
    public static void
    err(@Nullable Object subject) { Brief.print(subject, System.err); }

    /**
     * Implements the "jsh standard way" of printing objects.
     * <ul>
     *   <li>A {@code null} subject results in <em>nothing</em> being printed.</li>
     *   <li>Arrays (including primitive arrays) result in one line being printed per element.</li>
     *   <li>Collections result in one line being printed per element.</li>
     *   <li>All other subjects converted with {@link Object#toString()}, and then printed in one line.</li>
     * </ul>
     */
    public static void
    print(@Nullable Object subject, PrintStream ps) {

        if (subject == null) return;

        if (subject instanceof Object[]) {
            for (Object e : (Object[]) subject) {
                ps.println(e);
            }
            return;
        }

        if (subject.getClass().isArray()) {

            // "o" is a PRIMITIVE array. (Arrays of object references have already been handled.)
            int length = Array.getLength(subject);
            for (int i = 0; i < length; i++) {
                ps.println(Array.get(subject, i));
            }
            return;
        }

        if (subject instanceof Collection) {
            for (Object e : (Collection<?>) subject) ps.println(e);
            return;
        }

        ps.println(subject);
    }

    /**
     * Creates and returns a modifiable, empty {@link Map}. Shorthand for "{@code new HashMap()}".
     */
    @SuppressWarnings("rawtypes") public static Map
    map() { return new HashMap(); }

    /**
     * Creates and returns a modifiable, empty {@link List}. Shorthand for "{@code new ArrayList()}".
     */
    @SuppressWarnings("rawtypes") public static List
    list() { return new ArrayList(); }

    /**
     * Creates and returns a modifiable, empty {@link Set}. Shorthand for "{@code new HashSet()}".
     */
    @SuppressWarnings("rawtypes") public static Set
    set() { return new HashSet(); }

    /**
     * {@code null} <var>globs</var> results in a {@code null} return value.
     */
    @Nullable public static Collection<? extends File>
    glob(@Nullable String... globs) {

        if (globs == null) return null;

        Collection<File> result = new ArrayList<File>();

        for (String glob : globs) Brief.glob(null, glob, result);

        return result;
    }

    /**
     * A {@code null} parent is equivalent with {@code new File(".")}, except that the elements of the result have
     * a {@code null} parent.
     */
    public static void
    glob(@Nullable File parent, String glob, Collection<File> result) {

        final String prefix, suffix;
        {
            int idx = glob.indexOf('/');
            if (idx == -1) {
                prefix = glob;
                suffix = null;
            } else {
                prefix = glob.substring(0, idx);
                for (idx++; idx < glob.length() && glob.charAt(idx) == '/'; idx++);
                suffix = idx == glob.length() ? null : glob.substring(idx);
            }
        }

        File[] children;
        if (Brief.containsWildcards(glob)) {
            children = (parent == null ? new File(".") : parent).listFiles(new FilenameFilter() {

                @Override public boolean
                accept(@Nullable File dir, @Nullable String name) {
                    assert name != null;
                    return Brief.wildmatch(prefix, name);
                }
            });

            if (parent == null) {
                for (int i = 0; i < children.length; i++) {
                    children[i] = new File(children[i].getName());
                }
            }
        } else {
            children = new File[] { new File(parent, glob) };
        }

        if (suffix == null) {
            result.addAll(Arrays.asList(children));
        } else {
            for (File child : children) Brief.glob(child, suffix, result);
        }
    }

    public static boolean
    wildmatch(@Nullable String pattern, String text) {

        if (pattern == null) return true;

        int i;
        for (i = 0; i < pattern.length(); ++i) {
            char c = pattern.charAt(i);
            switch (c) {

            case '?':
                if (i == text.length()) return false;
                break;

            case '*':
                if (pattern.length() == i + 1) return true; // Optimization for trailing '*'.
                pattern = pattern.substring(i + 1);
                for (; i <= text.length(); ++i) {
                    if (Brief.wildmatch(pattern, text.substring(i))) return true;
                }
                return false;

            default:
                if (i == text.length()) return false;
                if (text.charAt(i) != c) return false;
                break;
            }
        }
        return text.length() == i;
    }

    public static boolean
    containsWildcards(@Nullable String pattern) {
        return pattern != null && (pattern.indexOf('*') != -1 || pattern.indexOf('?') != -1);
    }
}
