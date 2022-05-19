
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

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

            @Override @NotNullByDefault(false) public String
            inferBinaryName(Location location, JavaFileObject jfo) {

                if (!(jfo instanceof ResourceJavaFileObject)) {
                    String result = super.inferBinaryName(location, jfo);
                    assert result != null;
                    return result;
                }

                // A [Java]FileObject's "name" looks like this: "/org/codehaus/commons/compiler/Foo.java".
                // A [Java]FileObject's "binary name" looks like "java.lang.annotation.Retention".

                String bn = jfo.getName();
                if (bn.startsWith("/")) bn = bn.substring(1);

                if (!bn.endsWith(jfo.getKind().extension)) {
                    throw new AssertionError(
                        "Name \"" + jfo.getName() + "\" does not match kind \"" + jfo.getKind() + "\""
                    );
                }
                bn = bn.substring(0, bn.length() - jfo.getKind().extension.length());

                bn = bn.replace('/', '.');

                return bn;
            }

            @Override @NotNullByDefault(false) public boolean
            hasLocation(Location location2) { return location2 == location || super.hasLocation(location2); }

            // Must implement "list()", otherwise we'd get "package xyz does not exist" compile errors
            @Override @NotNullByDefault(false) public Iterable<JavaFileObject>
            list(Location location2, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {

                Iterable<JavaFileObject> delegatesJfos = super.list(location2, packageName, kinds, recurse);

                if (location2 == location && kinds.contains(kind)) {

                    assert resourceFinder instanceof ListableResourceFinder : resourceFinder;
                    ListableResourceFinder lrf = (ListableResourceFinder) resourceFinder;

                    Iterable<Resource> resources = lrf.list(packageName.replace('.', '/'), recurse);
                    if (resources != null) {
                        List<JavaFileObject> result = new ArrayList<>();
                        for (Resource r : resources) {

                            String className = r.getFileName();

                            if (!className.endsWith(kind.extension)) continue;
                            className = className.substring(0, className.length() - kind.extension.length());

                            className = className.replace(File.separatorChar, '.');
                            className = className.replace('/',                '.');

                            {
                                final int idx = className.lastIndexOf(packageName + ".");
                                assert idx != -1 : className + "//" + packageName;
                                className = className.substring(idx);
                            }


                            JavaFileObject jfo = this.getJavaFileForInput(location2, className, kind);
                            if (jfo != null) {
                                result.add(jfo);
                            }
                        }

                        // Add JFOs of delegate file manager.
                        for (JavaFileObject jfo : delegatesJfos) result.add(jfo);
                        return result;
                    }
                }

                return delegatesJfos;
            }

            @Override @NotNullByDefault(false) public JavaFileObject
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

            @Override @NotNullByDefault(false) public boolean
            isSameFile(FileObject a, FileObject b) {

                if (a instanceof ResourceJavaFileObject && b instanceof ResourceJavaFileObject) {
                    return a.getName().contentEquals(b.getName());
                }

                return super.isSameFile(a, b);
            }
        }

        return new ResourceFinderInputJavaFileManager();
    }

    /**
     * @return A {@link ForwardingJavaFileManager} that stores {@link JavaFileObject}s through a {@link ResourceCreator}
     */
    public static <M extends JavaFileManager> ForwardingJavaFileManager<M>
    fromResourceCreator(
        M                     delegate,
        final Location        location,
        final Kind            kind,
        final ResourceCreator resourceCreator,
        final Charset         charset
    ) {

        return new ForwardingJavaFileManager<M>(delegate) {

            @Override @NotNullByDefault(false) public JavaFileObject
            getJavaFileForOutput(
                Location     location2,
                final String className,
                Kind         kind2,
                FileObject   sibling
            ) throws IOException {

                if (kind2 == kind && location2 == location) {
                    final String resourceName = className.replace('.', '/') + ".class";
                    return JavaFileObjects.fromResourceCreator(resourceCreator, resourceName, kind, charset);
                } else {
                    return super.getJavaFileForOutput(location2, className, kind2, sibling);
                }
            }
        };
    }

    /**
     * @return A {@link ForwardingJavaFileManager} that stores {@link JavaFileObject}s in byte arrays, i.e. in memory
     *         (as opposed to the {@link StandardJavaFileManager}, which stores them in files)
     */
    public static <M extends JavaFileManager> ForwardingJavaFileManager<M>
    inMemory(M delegate, final Charset charset) {

        return new ForwardingJavaFileManager<M>(delegate) {

            private final Map<Location, Map<Kind, Map<String /*className*/, JavaFileObject>>>
            javaFiles = new HashMap<>();

            @Override @NotNullByDefault(false) public FileObject
            getFileForInput(Location location, String packageName, String relativeName) {
                throw new UnsupportedOperationException("getFileForInput");
            }

            @Override @NotNullByDefault(false) public FileObject
            getFileForOutput(
                Location   location,
                String     packageName,
                String     relativeName,
                FileObject sibling
            ) {
                throw new UnsupportedOperationException("getFileForOutput");
            }

            @Override @NotNullByDefault(false) public JavaFileObject
            getJavaFileForInput(Location location, String className, Kind kind) throws IOException {

                Map<Kind, Map<String, JavaFileObject>> locationJavaFiles = this.javaFiles.get(location);
                if (locationJavaFiles != null) {
                    Map<String, JavaFileObject> kindJavaFiles = locationJavaFiles.get(kind);
                    if (kindJavaFiles != null) return kindJavaFiles.get(className);
                }

                return super.getJavaFileForInput(location, className, kind);
            }

            @Override @NotNullByDefault(false) public JavaFileObject
            getJavaFileForOutput(
                Location     location,
                final String className,
                Kind         kind,
                FileObject   sibling
            ) throws IOException {

                Map<Kind, Map<String, JavaFileObject>> locationJavaFiles = this.javaFiles.get(location);
                if (locationJavaFiles == null) {
                    locationJavaFiles = new HashMap<>();
                    this.javaFiles.put(location, locationJavaFiles);
                }
                Map<String, JavaFileObject> kindJavaFiles = locationJavaFiles.get(kind);
                if (kindJavaFiles == null) {
                    kindJavaFiles = new HashMap<>();
                    locationJavaFiles.put(kind, kindJavaFiles);
                }

                JavaFileObject fileObject = JavaFileObjects.inMemory(className, kind, charset);

                kindJavaFiles.put(className, fileObject);

                return fileObject;
            }

            @Override @NotNullByDefault(false) public Iterable<JavaFileObject>
            list(
                Location  location,
                String    packageName,
                Set<Kind> kinds,
                boolean   recurse
            ) throws IOException {

                Map<Kind, Map<String, JavaFileObject>> locationFiles = this.javaFiles.get(location);
                if (locationFiles == null) return super.list(location, packageName, kinds, recurse);

                String               prefix = packageName.isEmpty() ? "" : packageName + ".";
                int                  pl     = prefix.length();
                List<JavaFileObject> result = new ArrayList<>();
                for (Kind kind : kinds) {
                    Map<String, JavaFileObject> kindFiles = locationFiles.get(kind);
                    if (kindFiles == null) continue;
                    for (Entry<String, JavaFileObject> e : kindFiles.entrySet()) {
                        final String         className      = e.getKey();
                        final JavaFileObject javaFileObject = e.getValue();

                        if (!className.startsWith(prefix)) continue;
                        if (!recurse && className.indexOf('.', pl) != -1) continue;
                        result.add(javaFileObject);
                    }
                }
                return result;
            }
        };
    }
}
