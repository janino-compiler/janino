
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

package org.codehaus.janino;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Set;

import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.java9.java.lang.module.ModuleFinder;
import org.codehaus.commons.compiler.java9.java.lang.module.ModuleReference;
import org.codehaus.commons.compiler.util.StringUtil;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.util.resource.JarDirectoriesResourceFinder;
import org.codehaus.janino.util.resource.MultiResourceFinder;
import org.codehaus.janino.util.resource.PathResourceFinder;

/**
 * A base class and wrapper for {@link Compiler} that implements all redundant API methods.
 */
public abstract
class AbstractCompiler extends ICompiler {

    @Override public void
    setSourcePath(File[] directoriesAndArchives) {
        this.setSourceFinder(new PathResourceFinder(directoriesAndArchives));
    }

    // System property "java.ext.dirs" is not set in JVM 9+, thus the default value "".
    private File[]
    extensionDirectories = StringUtil.parsePath(System.getProperty("java.ext.dirs", ""));

    private File[]
    classPath = StringUtil.parsePath(System.getProperty("java.class.path"));

    /**
     * This is <em>always</em> non-{@code null} for JVMs that support BOOTCLASSPATH (1.0-1.8), and
     * this is <em>always</em> {@code null} for JVMs that don't (9+).
     */
    @Nullable private File[] bootClassPath = StringUtil.parseOptionalPath(System.getProperty("sun.boot.class.path"));
    { this.updateIClassLoader(); }

    @Override public final void
    setBootClassPath(File[] directoriesAndArchives) {

        if (this.bootClassPath == null) {
            throw new IllegalArgumentException("This JVM doese not support BOOTCLASSPATH; probably because it is 9+");
        }

        this.bootClassPath = directoriesAndArchives;
        this.updateIClassLoader();
    }

    @Override public final void
    setExtensionDirectories(File[] directories) {
        this.extensionDirectories = directories;
        this.updateIClassLoader();
    }

    @Override public final void
    setClassPath(File[] directoriesAndArchives) {
        this.classPath = directoriesAndArchives;
        this.updateIClassLoader();
    }

    private void
    updateIClassLoader() {

        File[] bcp = this.bootClassPath;

        if (bcp != null) {

            // JVM 1.0-1.8; BOOTCLASSPATH supported:
            this.setIClassLoader(new ResourceFinderIClassLoader(
                new MultiResourceFinder(Arrays.asList(
                    new PathResourceFinder(bcp),
                    new JarDirectoriesResourceFinder(this.extensionDirectories),
                    new PathResourceFinder(this.classPath)
                )),
                null
            ));
        } else {

            // JVM 9+: "Modules" replace the BOOTCLASSPATH:
            String sbcp = System.getProperty("sun.boot.class.path");
            if (sbcp != null) {
                this.bootClassPath = StringUtil.parsePath(sbcp);
            } else {
                URL r = ClassLoader.getSystemClassLoader().getResource("java/lang/Object.class");
                assert r != null;

                assert "jrt".equalsIgnoreCase(r.getProtocol()) : r.toString();

                ResourceFinder rf = new ResourceFinder() {

                    @Override @Nullable public Resource
                    findResource(final String resourceName) {

                        try {
                            final Set<ModuleReference> mrs = ModuleFinder.ofSystem().findAll();

                            for (final ModuleReference mr : mrs) {
                                final URI           moduleContentLocation = (URI) mr.location().get();
                                final URL           classFileUrl          = new URL(moduleContentLocation + "/" + resourceName);
                                final URLConnection uc                    = classFileUrl.openConnection();
                                try {
                                    uc.connect();
                                    return new Resource() {

                                        @Override public InputStream
                                        open() throws IOException {
                                            try {
                                                return uc.getInputStream();
                                            } catch (IOException ioe) {
                                                throw new IOException(moduleContentLocation + ", " + resourceName, ioe);
                                            }
                                        }
                                        @Override public String getFileName()  { return resourceName;         }
                                        @Override public long   lastModified() { return uc.getLastModified(); }
                                    };
                                } catch (IOException ioe) {
                                    ;
                                }
                            }
                            return null;
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    }
                };

                this.setIClassLoader(new ResourceFinderIClassLoader(
                    new MultiResourceFinder(Arrays.asList(
                        rf,
                        new JarDirectoriesResourceFinder(this.extensionDirectories),
                        new PathResourceFinder(this.classPath)
                    )),
                    null
                ));
            }

        }
    }

    // --------------- Destination directory + rebuild logic:

    /**
     * Loads "auxiliary classes", typically from BOOTCLASSPATH + EXTDIR + CLASSPATH (but <em>not</em> from the
     * "destination directory"!).
     */
    public abstract void
    setIClassLoader(IClassLoader iClassLoader);
}
