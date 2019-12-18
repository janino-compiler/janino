
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

package org.codehaus.commons.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

/**
 * Implements all methods of {@link IMultiCookable}, except for {@link IMultiCookable#cook(String[], Reader[])}.
 */
public abstract
class MultiCookable extends Cookable implements IMultiCookable {

    @Override public final void
    cook(InputStream... inputStreams) throws CompileException, IOException {
        this.cook(new String[inputStreams.length], inputStreams);
    }

    @Override public final void
    cook(String[] strings) throws CompileException {
        this.cook(new String[strings.length], strings);
    }

    @Override public final void
    cook(String[] fileNames, InputStream[] inputStreams) throws CompileException, IOException {
        this.cook(fileNames, inputStreams, new String[fileNames.length]);
    }

    @Override public final void
    cook(InputStream[] inputStreams, String[] encodings) throws CompileException, IOException {
        this.cook(new String[inputStreams.length], inputStreams, encodings);
    }

    @Override public final void
    cook(String[] fileNames, InputStream[] inputStreams, String[] encodings) throws CompileException, IOException {

        final int count = fileNames.length;

        Reader[] readers = new Reader[count];
        for (int i = 0; i < count; i++) {
            readers[i] = (
                encodings[i] == null
                ? new InputStreamReader(inputStreams[i])
                : new InputStreamReader(inputStreams[i], encodings[i])
            );
        }

        this.cook(fileNames, readers);
    }

    @Override public final void
    cook(String[] fileNames, String[] strings) throws CompileException {

        final int count = fileNames.length;

        Reader[] readers = new Reader[count];
        for (int i = 0; i < count; i++) readers[i] = new StringReader(strings[i]);

        try {
            this.cook(fileNames, readers);
        } catch (IOException ioe) {
            throw new InternalCompilerException("SNO: IOException despite StringReader", ioe);
        }
    }

    // The only method that must be implemented by a derived class.
    @Override public abstract void
    cook(String[] fileNames, Reader[] readers) throws CompileException, IOException;

    @Override public final void
    cookFiles(File[] files) throws CompileException, IOException {
        this.cookFiles(files, new String[files.length]);
    }

    @Override public final void
    cookFiles(File[] files, String[] encodings) throws CompileException, IOException {

        final int count = files.length;

        String[]      fileNames    = new String[count];
        InputStream[] inputStreams = new InputStream[count];
        try {
            for (int i = 0; i < count; i++) {
                final File file = files[i];

                fileNames[i]    = file.getPath();
                inputStreams[i] = new FileInputStream(file);
            }

            this.cook(inputStreams, encodings);

            for (int i = 0; i < count; i++) inputStreams[i].close();
        } finally {
            for (int i = 0; i < count; i++) {
                InputStream is = inputStreams[i];
                if (is != null) try { is.close(); } catch (Exception e) {}
            }
        }
    }

    @Override public final void
    cookFiles(String[] fileNames) throws CompileException, IOException {
        this.cook(fileNames, new String[fileNames.length]);
    }

    @Override public final void
    cookFiles(String[] fileNames, String[] encodings) throws CompileException, IOException {

        final int count = fileNames.length;

        File[] files = new File[count];
        for (int i = 0; i < count; i++) files[i] = new File(fileNames[i]);

        this.cookFiles(files, encodings);
    }

    @Override public final void
    cook(Reader... readers) throws CompileException, IOException {
        this.cook(new String[readers.length], readers);
    }
}
