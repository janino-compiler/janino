
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;

import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.util.resource.Resource;

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

        private
        ResourceJavaFileObject(Resource resource, String className, Kind kind, Charset charset) {
            super(
                URI.create("bytearray:///" + className.replace('.', '/') + kind.extension),
                kind
            );
            this.resource = resource;
            this.charset  = charset;
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
                return Cookable.readString(r);
            } finally {
                r.close();
            }
        }

        @Override public long
        getLastModified() { return this.resource.lastModified(); }
    }

    /**
     * Wraps a {@link Resource} as a {@link JavaFileObject}.
     */
    public static JavaFileObject
    fromResource(Resource resource, String className, Kind kind, Charset charset) {
        return new ResourceJavaFileObject(resource, className, kind, charset);
    }
}
