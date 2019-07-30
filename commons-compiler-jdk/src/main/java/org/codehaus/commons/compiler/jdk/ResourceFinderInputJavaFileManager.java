
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

package org.codehaus.commons.compiler.jdk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.util.resource.ListableResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.NotNullByDefault;

/**
 * A {@link ForwardingJavaFileManager} that maps accesses to a particular {@link Location} and {@link Kind} to a
 * search in a {@link ResourceFinder}.
 */
@NotNullByDefault(false) final
class ResourceFinderInputJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    private final Location         location;
    private final Kind             kind;
    private final ResourceFinder   resourceFinder;
    private final Charset          encoding;

    ResourceFinderInputJavaFileManager(
        JavaFileManager  delegate,
        Location         location,
        Kind             kind,
        ResourceFinder   resourceFinder,
        Charset          encoding
    ) {
        super(delegate);
        this.location       = location;
        this.kind           = kind;
        this.resourceFinder = resourceFinder;
        this.encoding       = encoding;
    }

    @Override public String
    inferBinaryName(Location location, JavaFileObject file) {

        if (file instanceof ResourceJavaFileObject) {
            String bn = ((ResourceJavaFileObject) file).getName();
            bn = bn.substring(1, bn.length() - 5);
            return bn;
        }

        return super.inferBinaryName(location, file);
    }

    @Override public boolean
    hasLocation(Location location) { return location == this.location || super.hasLocation(location); }

    // TODO: TMP
    @Override public Iterable<JavaFileObject>
    list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {

        if (location == this.location && kinds.contains(this.kind)) {
            if (this.resourceFinder instanceof ListableResourceFinder) {
                ListableResourceFinder lrf = (ListableResourceFinder) this.resourceFinder;

                Iterable<Resource> resources = lrf.list(packageName.replace('.', '/'), recurse);
                if (resources != null) {
                    List<JavaFileObject> result = new ArrayList<JavaFileObject>();
                    for (Resource r : resources) {
                        String className = r.getFileName().replace(File.separatorChar, '.');
                        className = className.substring(className.indexOf(packageName), className.length() - 5);
                        JavaFileObject jfo = this.getJavaFileForInput(location, className, this.kind);
                        if (jfo != null) {
                            result.add(jfo);
                        }
                    }
                    return result;
                }
            }
        }

        return super.list(location, packageName, kinds, recurse);
    }

    @Override public JavaFileObject
    getJavaFileForInput(Location location, String className, Kind kind)
    throws IOException {
        assert location != null;
        assert className != null;
        assert kind != null;

        if (location == this.location && kind == this.kind) {

            // Find the source file through the source path.
            final Resource
            resource = this.resourceFinder.findResource(className.replace('.', '/') + kind.extension);

            if (resource == null) return null;

            // Create and return a JavaFileObject.
            return new ResourceJavaFileObject(resource, className, kind);
        }

        return super.getJavaFileForInput(location, className, kind);
    }

    /**
     * Byte array-based implementation of {@link JavaFileObject}.
     */
    public
    class ResourceJavaFileObject extends SimpleJavaFileObject {

        private final Resource resource;

        public
        ResourceJavaFileObject(Resource resource, String className, Kind kind) {
            super(
                URI.create("bytearray:///" + className.replace('.', '/') + kind.extension),
                kind
            );
            this.resource = resource;
        }

        @Override public InputStream
        openInputStream() throws IOException { return this.resource.open(); }

        @Override public Reader
        openReader(boolean ignoreEncodingErrors) throws IOException {
            return (
                ResourceFinderInputJavaFileManager.this.encoding == null
                ? new InputStreamReader(this.resource.open())
                : new InputStreamReader(this.resource.open(), ResourceFinderInputJavaFileManager.this.encoding)
            );
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
    }
}
