
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

package org.codehaus.commons.compiler.jdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.codehaus.commons.nullanalysis.NotNullByDefault;

/**
 * A {@link ForwardingJavaFileManager} that stores {@link JavaFileObject}s in byte arrays, i.e. in memory (as opposed
 * to the {@link StandardJavaFileManager}, which stores them in files).
 *
 * @param <M>
 */
@NotNullByDefault(false) public
class ByteArrayJavaFileManager<M extends JavaFileManager> extends ForwardingJavaFileManager<M> {

    private final Map<Location, Map<Kind, Map<String /*className*/, JavaFileObject>>> javaFiles = (
        new HashMap<Location, Map<Kind, Map<String, JavaFileObject>>>()
    );

    public
    ByteArrayJavaFileManager(M delegate) {
        super(delegate);
    }

    @Override public FileObject
    getFileForInput(Location location, String packageName, String relativeName) {
        throw new UnsupportedOperationException("getFileForInput");
    }

    @Override public FileObject
    getFileForOutput(
        Location   location,
        String     packageName,
        String     relativeName,
        FileObject sibling
    ) {
        throw new UnsupportedOperationException("getFileForInput");
    }

    @Override public JavaFileObject
    getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
        Map<Kind, Map<String, JavaFileObject>> locationJavaFiles = this.javaFiles.get(location);
        if (locationJavaFiles == null) return null;

        Map<String, JavaFileObject> kindJavaFiles = locationJavaFiles.get(kind);
        if (kindJavaFiles == null) return null;

        return kindJavaFiles.get(className);
    }

    @Override public JavaFileObject
    getJavaFileForOutput(
        Location     location,
        final String className,
        Kind         kind,
        FileObject   sibling
    ) throws IOException {

        /**
         * {@link StringWriter}-based implementation of {@link JavaFileObject}.
         * <p>
         *   Notice that {@link #getCharContent(boolean)} is much more efficient than {@link
         *   ByteArrayJavaFileObject#getCharContent(boolean)}. However, memory consumption is roughly double, and
         *   {@link #openInputStream()} and {@link #openOutputStream()} are not available.
         * </p>
         */
        class StringWriterJavaFileObject extends SimpleJavaFileObject {
            final StringWriter buffer = new StringWriter();

            StringWriterJavaFileObject(Kind kind) {
                super(
                    URI.create("stringbuffer:///" + className.replace('.', '/') + kind.extension),
                    kind
                );
            }

            @Override public Writer
            openWriter() throws IOException {
                return this.buffer;
            }

            @Override public Reader
            openReader(boolean ignoreEncodingErrors) throws IOException {
                return new StringReader(this.buffer.toString());
            }

            @Override public CharSequence
            getCharContent(boolean ignoreEncodingErrors) {
                return this.buffer.getBuffer();
            }
        }

        Map<Kind, Map<String, JavaFileObject>> locationJavaFiles = this.javaFiles.get(location);
        if (locationJavaFiles == null) {
            locationJavaFiles = new HashMap<Kind, Map<String, JavaFileObject>>();
            this.javaFiles.put(location, locationJavaFiles);
        }
        Map<String, JavaFileObject> kindJavaFiles = locationJavaFiles.get(kind);
        if (kindJavaFiles == null) {
            kindJavaFiles = new HashMap<String, JavaFileObject>();
            locationJavaFiles.put(kind, kindJavaFiles);
        }

        JavaFileObject fileObject = (
            kind == Kind.SOURCE
            ? new StringWriterJavaFileObject(kind)
            : new ByteArrayJavaFileObject(className, kind)
        );

        kindJavaFiles.put(className, fileObject);
        return fileObject;
    }

    @Override public Iterable<JavaFileObject>
    list(
        Location  location,
        String    packageName,
        Set<Kind> kinds,
        boolean   recurse
    ) throws IOException {
        Map<Kind, Map<String, JavaFileObject>> locationFiles = this.javaFiles.get(location);
        if (locationFiles == null) return super.list(location, packageName, kinds, recurse);

        String               prefix = packageName.length() == 0 ? "" : packageName + ".";
        int                  pl     = prefix.length();
        List<JavaFileObject> result = new ArrayList<JavaFileObject>();
        for (Kind kind : kinds) {
            Map<String, JavaFileObject> kindFiles = locationFiles.get(kind);
            if (kindFiles == null) continue;
            for (Entry<String, JavaFileObject> e : kindFiles.entrySet()) {
                String className = e.getKey();
                if (!className.startsWith(prefix)) continue;
                if (!recurse && className.indexOf('.', pl) != -1) continue;
                result.add(e.getValue());
            }
        }
        return result;
    }

    /**
     * Byte array-based implementation of {@link JavaFileObject}.
     */
    public static
    class ByteArrayJavaFileObject extends SimpleJavaFileObject {

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        public
        ByteArrayJavaFileObject(String className, Kind kind) {
            super(
                URI.create("bytearray:///" + className.replace('.', '/') + kind.extension),
                kind
            );
        }

        @Override public OutputStream
        openOutputStream() throws IOException { return this.buffer; }

        /**
         * @return The bytes that were previously written to this {@link JavaFileObject}
         */
        public byte[]
        toByteArray() { return this.buffer.toByteArray(); }

        @Override public InputStream
        openInputStream() throws IOException { return new ByteArrayInputStream(this.toByteArray()); }
    }
}
