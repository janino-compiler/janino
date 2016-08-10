
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

import org.codehaus.janino.tools.jsh.Brief;

/**
 * Changes the current working directory.
 */
public final
class Cd {

    private Cd() {}

    /**
     * Changes the current working directory to the user's home directory.
     */
    public static void
    cd() { Cd.cd(new File(System.getProperty("user.home"))); }

    /**
     * Sets a new current working directory.
     */
    public static void
    cd(String dirName) { Cd.cd(new File(dirName)); }

    /**
     * Sets a new current working directory.
     */
    public static void
    cd(File dir) { System.setProperty("user.dir", dir.getAbsolutePath()); }

    /**
     * Prints the a listing of the <var>files</var> to {@code System.out}, or, iff <var>files</var> {@code == null},
     * of the current working directory.
     */
    public static void
    pwd() { Brief.print(System.getProperty("user.dir"), System.out); }
}
