
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

package org.codehaus.janino.tools.jsh.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.tools.jsh.Brief;

/**
 * Prints the name of files, or the names of the members of directories.
 */
public final
class Ls {

    private Ls() {}

    /**
     * Lists the current working directory.
     */
    public static void
    ls() { Ls.ls((String[]) null); }

    /**
     * {@code null} <var>globs</var> lists the current directory.
     */
    public static void
    ls(@Nullable String... globs) {
        Collection<? extends File> files = Brief.glob(globs);
        Ls.ls(files);
    }

    /**
     * Prints a listing of the <var>files</var> to {@code System.out}, or, iff <var>files</var> {@code == null},
     * of the current working directory.
     */
    public static void
    ls(@Nullable Collection<? extends File> files) {

        if (files == null) {
            for (String memberName : new File(".").list()) System.out.println(memberName);
            return;
        }

        if (files.size() == 1) {
            Ls.ls(files.iterator().next());
            return;
        }

        // Iff there is more than one file: Print the non-directory files first, then list the directories.

        List<File> directories = new ArrayList<File>();
        for (File file : files) {
            if (file.isDirectory()) {
                directories.add(file);
            } else {
                System.out.println(file);
            }
        }

        for (File d : directories) {
            System.out.println();
            System.out.println(d + ":");
            for (String memberName : d.list()) System.out.println(memberName);
        }
    }

    /**
     * A {@code null} <var>file</var> results in the current working directory being listed.
     * @param file
     */
    public static void
    ls(@Nullable File file) {

        if (file == null) {
            for (String memberName : new File(".").list()) System.out.println(memberName);
            return;
        }

        if (file.isDirectory()) {
            for (File member : file.listFiles()) System.out.println(member);
        } else {
            System.out.println(file);
        }
    }
}
