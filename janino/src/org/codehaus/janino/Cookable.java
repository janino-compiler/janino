
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

import java.io.*;

/**
 * "Cooking" means scanning a sequence of Java<sup>TM</sup> tokens with a
 * {@link org.codehaus.janino.Scanner}. This class declares numerous <code>cook*()</code> methods
 * that use a {@link java.lang.String}, a {@link java.io.File}, an {@link java.io.InputStream} or
 * a {@link java.io.Reader} as the source of characters for scanning.
 * <p>
 * The <code>cook*()</code> methods eventually invoke the abstract {@link #internalCook(Scanner)}
 * method with a correctly configured {@link org.codehaus.janino.Scanner}.
 */
public abstract class Cookable {

    /**
     * To be implemented by the derived classes.
     */
    protected abstract void internalCook(Scanner scanner)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException;

    // The "cook()" method family.

    public final void cook(Scanner scanner)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.internalCook(scanner);
    }
    public final void cook(Reader r)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.cook(null, r);
    }

    /**
     * @param optionalFileName Used when reporting errors and warnings.
     */
    public final void cook(String optionalFileName, Reader r)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.cook(new Scanner(optionalFileName, r));
    }

    /**
     * Cook tokens from an {@link InputStream}, encoded in the "platform default encoding".
     */
    public final void cook(InputStream is)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.cook(null, is);
    }

    /**
     * Cook tokens from an {@link InputStream}, encoded in the "platform default encoding".
     *
     * @param optionalFileName Used when reporting errors and warnings.
     */
    public final void cook(String optionalFileName, InputStream is)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.cook(optionalFileName, is, null);
    }
    public final void cook(InputStream is, String optionalEncoding)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.cook(new Scanner(null, is, optionalEncoding));
    }

    /**
     * @param optionalFileName Used when reporting errors and warnings.
     */
    public final void cook(String optionalFileName, InputStream is, String optionalEncoding)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.cook(new Scanner(optionalFileName, is, optionalEncoding));
    }

    /**
     * Cook tokens from the given {@link File}, encoded in the "platform default encoding".
     */
    public final void cookFile(File file)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.cookFile(file, null);
    }
    public final void cookFile(File file, String optionalEncoding)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        InputStream is = new FileInputStream(file);
        try {
            this.internalCook(new Scanner(file.getAbsolutePath(), is, optionalEncoding));
            is.close();
            is = null;
        } finally {
            if (is != null) try { is.close(); } catch (IOException ex) {}
        }
    }

    /**
     * Cook tokens from the named file, encoded in the "platform default encoding".
     */
    public final void cookFile(String fileName)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.cookFile(fileName, null);
    }
    public final void cookFile(String fileName, String optionalEncoding)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.cookFile(new File(fileName), optionalEncoding);
    }

    /**
     * Cook tokens from a {@link java.lang.String}.
     */
    public final void cook(String s)
    throws CompileException, Parser.ParseException, Scanner.ScanException {
        try {
            this.cook(new StringReader(s));
        } catch (IOException ex) {
            throw new RuntimeException("SNO: IOException despite StringReader");
        }
    }
}
