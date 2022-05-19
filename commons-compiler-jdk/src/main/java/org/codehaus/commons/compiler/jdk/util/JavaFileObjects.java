
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

package org.codehaus.commons.compiler.jdk.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

import org.codehaus.commons.compiler.io.Readers;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceCreator;
import org.codehaus.commons.nullanalysis.NotNullByDefault;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Utility methods related to {@link JavaFileObject}s.
 */
public final
class JavaFileObjects {

    private JavaFileObjects() {}

    /**
     * Byte array-based implementation of {@link JavaFileObject}.
     */
    public static final
    class ResourceJavaFileObject extends SimpleJavaFileObject {

        private final Resource resource;
        private final Charset  charset;
        private final String   name;

        private
        ResourceJavaFileObject(Resource resource, String className, Kind kind, Charset charset) {
            super(
                URI.create("bytearray:///" + className.replace('.', '/') + kind.extension),
                kind
            );
            this.resource = resource;
            this.charset  = charset;
            this.name     = "/" + className.replace('.', '/') + kind.extension;
        }

        @Override public boolean
        isNameCompatible(@Nullable String simpleName, @Nullable Kind kind) {
            return !"module-info".equals(simpleName);
        }

        @Override public String
        getName() {
            return this.name;
        }

        @Override public InputStream
        openInputStream() throws IOException { return this.resource.open(); }

        @Override public Reader
        openReader(boolean ignoreEncodingErrors) throws IOException {
            return new InputStreamReader(this.resource.open(), this.charset);
        }

        @Override public CharSequence
        getCharContent(boolean ignoreEncodingErrors) throws IOException {
            Reader r = this.openReader(true);
            try {
                return Readers.readAll(r);
            } finally {
                r.close();
            }
        }

        @Override public long
        getLastModified() { return this.resource.lastModified(); }

        public String
        getResourceFileName() { return this.resource.getFileName(); }
    }

    /**
     * Wraps a {@link Resource} as a {@link JavaFileObject}.
     */
    public static JavaFileObject
    fromResource(Resource resource, String className, Kind kind, Charset charset) {
        return new ResourceJavaFileObject(resource, className, kind, charset);
    }

    /**
     * @return The resource designated by the <var>url</var>, wrapped in a {@link JavaFileObject}
     */
    public static JavaFileObject
    fromUrl(final URL url, final String name, final Kind kind) {

        final URI subresourceUri;
        try {
            subresourceUri = url.toURI();
        } catch (URISyntaxException use) {
            throw new AssertionError(use);
        }

        // Cannot use "javax.tools.SimpleJavaFileObject" here, because the constructor requires a URI with a "path",
        // and URIs without a ":/" infix don't have a path.
        @NotNullByDefault(false) JavaFileObject result = new JavaFileObject() {

            @Override public URI
            toUri() { return subresourceUri; }

            @Override public String
            getName() { return name; }

            @Override public InputStream
            openInputStream() throws IOException { return url.openStream(); }

            @Override public Kind
            getKind() { return kind; }

            @Override public OutputStream openOutputStream()                             { throw new UnsupportedOperationException(); }
            @Override public Reader       openReader(boolean ignoreEncodingErrors)       { throw new UnsupportedOperationException(); }
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors)   { throw new UnsupportedOperationException(); }
            @Override public Writer       openWriter()                                   { throw new UnsupportedOperationException(); }
            @Override public long         getLastModified()                              { throw new UnsupportedOperationException(); }
            @Override public boolean      delete()                                       { throw new UnsupportedOperationException(); }
            @Override public boolean      isNameCompatible(String simpleName, Kind kind) { throw new UnsupportedOperationException(); }
            @Override public NestingKind  getNestingKind()                               { throw new UnsupportedOperationException(); }
            @Override public Modifier     getAccessLevel()                               { throw new UnsupportedOperationException(); }

            @Override public String
            toString() { return name + " from " + this.getClass().getSimpleName(); }
        };
        return result;
    }

    /**
     * @return A {@link JavaFileObject} that stores its data in an internal byte array
     */
    public static ByteArrayJavaFileObject
    inMemory(final String className, final Kind kind2, final Charset charset) {

        class MyJavaFileObject extends SimpleJavaFileObject implements ByteArrayJavaFileObject {

            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            MyJavaFileObject() {
                super(URI.create("bytearray:///" + className.replace('.', '/') + kind2.extension), kind2);
            }

            @Override public InputStream
            openInputStream() throws IOException { return new ByteArrayInputStream(this.toByteArray()); }

            @Override public OutputStream
            openOutputStream() throws IOException { return this.buffer; }

            @Override public Reader
            openReader(boolean ignoreEncodingErrors) throws IOException {
                return new InputStreamReader(this.openInputStream(), charset);
            }

            @Override public Writer
            openWriter() throws IOException { return new OutputStreamWriter(this.openOutputStream(), charset); }

            /**
             * @return The bytes that were previously written to this {@link JavaFileObject}
             */
            @Override
            public byte[]
            toByteArray() { return this.buffer.toByteArray(); }
        }

        return new MyJavaFileObject();
    }

    /**
     * Byte array-based implementation of {@link JavaFileObject}.
     */
    public
    interface ByteArrayJavaFileObject extends JavaFileObject {

        /**
         * @return The bytes that were previously written to this {@link JavaFileObject}
         */
        byte[] toByteArray();
    }

    /**
     * @param resourceName E.g. {@code "com/foo/pkg/Bar.class"}
     * @return             A {@link JavaFileObject} that stores data through the given <var>resourceCreator</var> and
     *                     <var>resourceName</var>
     */
    public static JavaFileObject
    fromResourceCreator(
        final ResourceCreator resourceCreator,
        final String          resourceName,
        Kind                  kind,
        final Charset         charset
    ) {

        return new SimpleJavaFileObject(URI.create("bytearray:///" + resourceName), kind) {

            @Override public OutputStream
            openOutputStream() throws IOException { return resourceCreator.createResource(resourceName); }

            @Override public Writer
            openWriter() throws IOException { return new OutputStreamWriter(this.openOutputStream(), charset); }
        };
    }
}
