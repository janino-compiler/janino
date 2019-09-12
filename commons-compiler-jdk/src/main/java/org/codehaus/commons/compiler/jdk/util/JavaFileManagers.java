
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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

import org.codehaus.commons.compiler.jdk.util.JavaFileObjects.ResourceJavaFileObject;
import org.codehaus.commons.compiler.util.reflect.ApiLog;
import org.codehaus.commons.compiler.util.resource.ListableResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceCreator;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.NotNullByDefault;

/**
 * Utility methods related to {@link JavaFileManager}s.
 */
public final
class JavaFileManagers {

    private JavaFileManagers() {}

    /**
     * A {@link ForwardingJavaFileManager} that maps accesses to a particular {@link Location} and {@link Kind} to a
     * search in a {@link ResourceFinder}.
     */
    public static <M extends JavaFileManager> ForwardingJavaFileManager<M>
    fromResourceFinder(
        final M              delegate,
        final Location       location,
        final Kind           kind,
        final ResourceFinder resourceFinder,
        final Charset        charset
    ) {

        class ResourceFinderInputJavaFileManager extends ForwardingJavaFileManager<M> {

            ResourceFinderInputJavaFileManager() { super(delegate); }

            @Override public String
            inferBinaryName(Location location, JavaFileObject file) {

                if (!(file instanceof ResourceJavaFileObject)) {
                    String result = super.inferBinaryName(location, file);
                    assert result != null;
                    return result;
                }

                // A [Java]FileObject's "name" looks like this: "/orc/codehaus/commons/compiler/Foo.java"
                String bn = file.getName();
                if (bn.startsWith("/")) bn = bn.substring(1);

                // Although not obvious from the documentation, binary names look like "java.lang.annotation.Retention".
                if (file.getKind() == Kind.SOURCE) {
                    assert bn.endsWith(".java") : bn;
                    bn = bn.substring(0, bn.length() - 5);
                    bn = bn.replace('/', '.');
                } else
                if (file.getKind() == Kind.CLASS) {
                    assert bn.endsWith(".class") : bn;
                    bn = bn.substring(0, bn.length() - 6);
                    bn = bn.replace('/', '.');
                }

                assert bn != null : file.toString();
                if (bn.startsWith(".")) {
                    System.currentTimeMillis();
                    file.getName();
                }

                return bn;
            }

            @Override public boolean
            hasLocation(Location location2) { return location2 == location || super.hasLocation(location2); }

            // Must implement "list()", otherwise we'd get "package xyz does not exist" compile errors
            @Override public Iterable<JavaFileObject>
            list(Location location2, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {

                if (location2 == location && kinds.contains(kind)) {

                    assert resourceFinder instanceof ListableResourceFinder : resourceFinder;
                    ListableResourceFinder lrf = (ListableResourceFinder) resourceFinder;

                    Iterable<Resource> resources = lrf.list(packageName.replace('.', '/'), recurse);
                    if (resources != null) {
                        List<JavaFileObject> result = new ArrayList<JavaFileObject>();
                        for (Resource r : resources) {

                            String fileName = r.getFileName();

                            fileName = fileName.replace(File.separatorChar, '.');
                            fileName = fileName.replace('/',                '.');

                            String className;
                            {
                                final int idx = fileName.lastIndexOf(packageName + ".");
                                assert idx != -1 : fileName + "//" + packageName;
                                className = fileName.substring(idx);
                            }

                            if (className.endsWith(".java")) {
                                className = className.substring(0, className.length() - 5);
                            } else
                            if (className.endsWith(".class") && kind == Kind.CLASS) {
                                className = className.substring(0, className.length() - 6);
                            }

                            JavaFileObject jfo = this.getJavaFileForInput(location2, className, kind);
                            if (jfo != null) {
                                result.add(jfo);
                            }
                        }
                        return result;
                    }
                }

                return super.list(location2, packageName, kinds, recurse);
            }

            @Override public JavaFileObject
            getJavaFileForInput(Location location2, String className, Kind kind2)
            throws IOException {

                assert location2 != null;
                assert className != null;
                assert kind2     != null;

                if (location2 == location && kind2 == kind) {

                    // Find the source file through the source path.
                    final Resource
                    resource = resourceFinder.findResource(className.replace('.', '/') + kind2.extension);

                    if (resource == null) return null;

                    // Create and return a JavaFileObject.
                    JavaFileObject result = JavaFileObjects.fromResource(resource, className, kind, charset);
                    result = (JavaFileObject) ApiLog.logMethodInvocations(result);
                    return result;
                }

                return super.getJavaFileForInput(location2, className, kind2);
            }
        }

        return new ResourceFinderInputJavaFileManager();
    }

    /**
     * @return A {@link ForwardingJavaFileManager} that stores {@link JavaFileObject}s through a {@link ResourceCreator}
     */
    public static ForwardingJavaFileManager<JavaFileManager>
    fromResourceCreator(JavaFileManager jfm, ResourceCreator fileCreator) {
        return new ResourceCreatorJavaFileManager<JavaFileManager>(Kind.CLASS, fileCreator, jfm);
    }

    private static final
    class ResourceCreatorJavaFileManager<T extends JavaFileManager> extends ForwardingJavaFileManager<T> {

        private final Kind            kind;
        private final ResourceCreator resourceCreator;

        ResourceCreatorJavaFileManager(Kind kind, ResourceCreator resourceCreator, T delegate) {
            super(delegate);
            this.kind            = kind;
            this.resourceCreator = resourceCreator;
        }

        @Override @NotNullByDefault(false) public JavaFileObject
        getJavaFileForOutput(
            Location     location,
            final String className,
            Kind         kind,
            FileObject   sibling
        ) throws IOException {

            if (kind != this.kind) {
                return super.getJavaFileForOutput(location, className, kind, sibling);
            }

            return new SimpleJavaFileObject(
                URI.create("bytearray:///" + className.replace('.', '/') + kind.extension),
                kind
            ) {

                @Override public OutputStream
                openOutputStream() throws IOException {
                    return ResourceCreatorJavaFileManager.this.resourceCreator.createResource(
                        className.replace('.', '/') + ".class"
                    );
                }

//                /**
//                 * @return The bytes that were previously written to this {@link JavaFileObject}
//                 */
//                public byte[]
//                toByteArray() { return this.buffer.toByteArray(); }
//
//                @Override public InputStream
//                openInputStream() throws IOException { return new ByteArrayInputStream(this.toByteArray()); }
            };
        }
    }
}
