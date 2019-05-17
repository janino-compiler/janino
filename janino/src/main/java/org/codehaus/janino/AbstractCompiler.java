
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
import java.nio.charset.Charset;
import java.util.Arrays;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.util.StringUtil;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.util.resource.DirectoryResourceCreator;
import org.codehaus.janino.util.resource.DirectoryResourceFinder;
import org.codehaus.janino.util.resource.FileResource;
import org.codehaus.janino.util.resource.JarDirectoriesResourceFinder;
import org.codehaus.janino.util.resource.MultiResourceFinder;
import org.codehaus.janino.util.resource.PathResourceFinder;
import org.codehaus.janino.util.resource.Resource;
import org.codehaus.janino.util.resource.ResourceCreator;
import org.codehaus.janino.util.resource.ResourceFinder;

/**
 * A base class and wrapper for {@link Compiler} that implements all redundant API methods.
 */
public abstract
class AbstractCompiler implements ICompiler {

    /**
     * Special value for {@link #setClassFileFinder(ResourceFinder)}.
     *
     * @see #setClassFileFinder(ResourceFinder)
     */
    @Nullable public static final ResourceFinder FIND_NEXT_TO_SOURCE_FILE = null;

    /**
     * Special value for {@link #setClassFileCreator(ResourceCreator)}: Indicates that .class resources are to be
     * created in the directory of the .java resource from which they are generated.
     */
    @Nullable public static final ResourceCreator CREATE_NEXT_TO_SOURCE_FILE = null;

    @Override public void
    setSourcePath(File[] directoriesAndArchives) {
        this.setSourceFinder(new PathResourceFinder(directoriesAndArchives));
    }

    // System property "java.ext.dirs" is not set in JVM 9+, thus the default value "".
    private File[]
    extensionDirectories = StringUtil.parsePath(System.getProperty("java.ext.dirs", ""));

    private File[]
    classPath = StringUtil.parsePath(System.getProperty("java.class.path"));

    // System property "sun.boot.class.path" is not set in JVM 9+, so work around.
    private File[] bootClassPath;
    {
        this.bootClassPath = StringUtil.parsePath(System.getProperty("sun.boot.class.path"));

//        // The "sun.boot.class.path" system property exists only for Java 1.0 through 1.8.
//        String sbcp = System.getProperty("sun.boot.class.path");
//        if (sbcp != null) {
//            this.bootClassPath = StringUtil.parsePath(sbcp);
//        } else {
//            URL r = ClassLoader.getSystemClassLoader().getResource("java/lang/Object.class");
//            assert r != null;
//
//            assert "jar".equalsIgnoreCase(r.getProtocol());
//            JarURLConnection juc;
//            try {
//                juc = (JarURLConnection) r.openConnection();
//            } catch (IOException e) {
//                throw new AssertionError(e);
//            }
//            juc.setUseCaches(false);
//
//            JarFile jarFile;
//            try {
//                jarFile = juc.getJarFile();
//            } catch (IOException e) {
//                throw new AssertionError(e);
//            }
//
//            URL jarFileUrl = juc.getJarFileURL();
//
//
//            this.bootClassPath = new File[] {  };
//        }
    }

    @Override public final void
    setBootClassPath(File[] directoriesAndArchives) {
        this.bootClassPath = directoriesAndArchives;
        this.updateParentIClassLoader();
    }

    @Override public final void
    setExtensionDirectories(File[] directories) {
        this.extensionDirectories = directories;
        this.updateParentIClassLoader();
    }

    @Override public final void
    setClassPath(File[] directoriesAndArchives) {
        this.classPath = directoriesAndArchives;
        this.updateParentIClassLoader();
    }

    private void
    updateParentIClassLoader() {
        this.setIClassLoader(new ResourceFinderIClassLoader(
            new MultiResourceFinder(Arrays.asList(
                new PathResourceFinder(this.bootClassPath),
                new JarDirectoriesResourceFinder(this.extensionDirectories),
                new PathResourceFinder(this.classPath)
            )),
            null
        ));
    }

    // --------------- Destination directory + rebuild logic:

    @Nullable private File destinationDirectory;
    private boolean        rebuild;

    /**
     * @param destinationDirectory {@link #NO_DESTINATION_DIRECTORY} means "create .class resources in the directory
     *                             of the .java resource from which they are generated"
     */
    @Override public final void
    setDestinationDirectory(@Nullable File destinationDirectory) {
        this.destinationDirectory = destinationDirectory;
        this.updateDestinationDirectory();
    }

    @Override public final void
    setRebuild(boolean value) {
        this.rebuild = value;
        this.updateDestinationDirectory();
    }

    public void
    setCharacterEncoding(@Nullable String characterEncoding) {
        this.setEncoding(characterEncoding == null ? null : Charset.forName(characterEncoding));
    }

    @SuppressWarnings("null") private void
    updateDestinationDirectory() {

        File dd = this.destinationDirectory;

        this.setClassFileCreator(
            dd == ICompiler.NO_DESTINATION_DIRECTORY
            ? AbstractCompiler.CREATE_NEXT_TO_SOURCE_FILE
            : new DirectoryResourceCreator(dd)
        );
        this.setClassFileFinder(
            this.rebuild
            ? ResourceFinder.EMPTY_RESOURCE_FINDER
            : dd == ICompiler.NO_DESTINATION_DIRECTORY
            ? AbstractCompiler.FIND_NEXT_TO_SOURCE_FILE
            : new DirectoryResourceFinder(dd)
        );
    }

    /**
     * Finds more .java resources that need to be compiled, i.e. implements the "source path".
     * @param sourceFinder
     */
    public abstract void
    setSourceFinder(ResourceFinder sourceFinder);

    /**
     * This {@link ResourceFinder} is used to check whether a .class resource already exists and is younger than the
     * .java resource from which it was generated.
     * <p>
     *   If it is impossible to check whether an already-compiled class file exists, or if you want to enforce
     *   recompilation, pass {@link ResourceFinder#EMPTY_RESOURCE_FINDER} as the <var>classFileFinder</var>.
     * </p>
     *
     * @param resourceFinder          Special value {@link #FIND_NEXT_TO_SOURCE_FILE} means ".class file is next to
     *                                its source file, <em>not</em> in the destination directory"
     * @see #FIND_NEXT_TO_SOURCE_FILE
     */
    protected abstract void
    setClassFileFinder(@Nullable ResourceFinder resourceFinder);

    /**
     * Loads "auxiliary classes", typically from BOOTCLASSPATH + EXTDIR + CLASSPATH (but <em>not</em> from the
     * "destination directory"!).
     */
    public abstract void
    setIClassLoader(IClassLoader iClassLoader);

    /**
     * @param classFileCreator Sess {@link #CREATE_NEXT_TO_SOURCE_FILE}
     */
    public abstract void
    setClassFileCreator(@Nullable ResourceCreator classFileCreator);

    @Override public final boolean
    compile(File[] sourceFiles) throws CompileException, IOException {

        Resource[] sourceFileResources = new Resource[sourceFiles.length];
        for (int i = 0; i < sourceFiles.length; ++i) sourceFileResources[i] = new FileResource(sourceFiles[i]);
        this.compile(sourceFileResources);

        return true;
    }

    public abstract void
    compile(Resource[] sourceResources) throws CompileException, IOException;
}
