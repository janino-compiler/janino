
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.commons.compiler.util.resource.DirectoryResourceCreator;
import org.codehaus.commons.compiler.util.resource.DirectoryResourceFinder;
import org.codehaus.commons.compiler.util.resource.PathResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceCreator;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.util.ClassFile;

/**
 * A {@link org.codehaus.janino.JavaSourceClassLoader} that uses a resource storage provided by the application to
 * cache compiled classes and thus saving unnecessary recompilations.
 * <p>
 *   The application provides access to the resource storage through a pair of a {@link
 *   org.codehaus.commons.compiler.util.resource.ResourceFinder} and a {@link
 *   org.codehaus.commons.compiler.util.resource.ResourceCreator} (see {@link
 *   #CachingJavaSourceClassLoader(ClassLoader, ResourceFinder, String, ResourceFinder, ResourceCreator)}.
 * </p>
 * <p>
 *   See {@link org.codehaus.janino.JavaSourceClassLoader#main(String[])} for an example how to use this class.
 * </p>
 * <p>
 *   <b>Notice:</b> You must NOT rely on that this class stores some particular data in some particular resources
 *   through the given {@code classFileCacheResourceFinder/Creator}! These serve only as a means for the {@link
 *   CachingJavaSourceClassLoader} to persistently cache some data between invocations. In other words: If you want to
 *   compile {@code .java} files into {@code .class} files, then don't use <em>this</em> class but {@link Compiler}
 *   instead!
 * </p>
 */
public
class CachingJavaSourceClassLoader extends JavaSourceClassLoader {
    private final ResourceFinder  classFileCacheResourceFinder;
    private final ResourceCreator classFileCacheResourceCreator;
    private final ResourceFinder  sourceFinder;

    /**
     * See {@link #CachingJavaSourceClassLoader(ClassLoader, ResourceFinder, String, ResourceFinder, ResourceCreator)}.
     *
     * @param sourcePath Directories to scan for source files
     * @param cacheDirectory     Directory to use for caching generated class files (see class description)
     */
    public
    CachingJavaSourceClassLoader(
        ClassLoader             parentClassLoader,
        @Nullable File[]        sourcePath,
        @Nullable String        characterEncoding,
        File                    cacheDirectory
    ) {
        this(
            parentClassLoader,                           // parentClassLoader
            (                                            // sourceFinder
                sourcePath == null
                ? new DirectoryResourceFinder(new File("."))
                : new PathResourceFinder(sourcePath)
            ),
            characterEncoding,                           // characterEncoding
            new DirectoryResourceFinder(cacheDirectory), // classFileCacheResourceFinder
            new DirectoryResourceCreator(cacheDirectory) // classFileCacheResourceCreator
        );
    }

    /**
     * Notice that this class is thread-safe if and only if the <var>classFileCacheResourceCreator</var> stores its
     * data atomically, i.e. the <var>classFileCacheResourceFinder</var> sees the resource written by the {@code
     * classFileCacheResourceCreator} only after the {@link OutputStream} is closed.
     * <p>
     *   In order to make the caching scheme work, both the <var>classFileCacheResourceFinder</var> and the {@code
     *   sourceFinder} must support the {@link org.codehaus.commons.compiler.util.resource.Resource#lastModified()}
     *   method, so that the modification time of the source and the class files can be compared.
     * </p>
     *
     * @param parentClassLoader             Attempt to load classes through this one before looking for source files
     * @param sourceFinder                  Finds Java source for class {@code pkg.Cls} in resource {@code
     *                                      pkg/Cls.java}
     * @param characterEncoding     Encoding of Java source or {@code null} for platform default
     *                                      encoding
     * @param classFileCacheResourceFinder  Finds precompiled class {@code pkg.Cls} in resource {@code pkg/Cls.class}
     *                                      (see class description)
     * @param classFileCacheResourceCreator Stores compiled class {@code pkg.Cls} in resource {@code pkg/Cls.class} (see
     *                                      class description)
     */
    public
    CachingJavaSourceClassLoader(
        ClassLoader      parentClassLoader,
        ResourceFinder   sourceFinder,
        @Nullable String characterEncoding,
        ResourceFinder   classFileCacheResourceFinder,
        ResourceCreator  classFileCacheResourceCreator
    ) {
        super(parentClassLoader, sourceFinder, characterEncoding);
        this.classFileCacheResourceFinder  = classFileCacheResourceFinder;
        this.classFileCacheResourceCreator = classFileCacheResourceCreator;
        this.sourceFinder                  = sourceFinder;
    }

    /**
     * Overrides {@link JavaSourceClassLoader#generateBytecodes(String)} to implement class file caching.
     *
     * @return                        String name =&gt; byte[] bytecode, or {@code null} if no source code could be
     *                                found
     * @throws ClassNotFoundException Compilation problems or class file cache I/O problems
     */
    @Override @Nullable protected Map<String /*name*/, byte[] /*bytecode*/>
    generateBytecodes(String className) throws ClassNotFoundException {
        // Check whether a class file resource exists in the cache.
        {
            Resource classFileResource = this.classFileCacheResourceFinder.findResource(
                ClassFile.getClassFileResourceName(className)
            );
            if (classFileResource != null) {

                // Check whether a source file resource exists.
                Resource sourceResource = this.sourceFinder.findResource(ClassFile.getSourceResourceName(className));
                if (sourceResource == null) return null;

                // Check whether the class file is up-to-date.
                if (sourceResource.lastModified() < classFileResource.lastModified()) {

                    // Yes, it is... read the bytecode from the file and define the class.
                    byte[] bytecode;
                    try {
                        bytecode = CachingJavaSourceClassLoader.readResource(classFileResource);
                    } catch (IOException ex) {
                        throw new ClassNotFoundException("Reading class file from \"" + classFileResource + "\"", ex);
                    }
                    Map<String /*name*/, byte[] /*bytecode*/> m = new HashMap<>();
                    m.put(className, bytecode);
                    return m;
                }
            }
        }

        // Cache miss... generate the bytecode from source.
        Map<String /*name*/, byte[] /*bytecode*/> bytecodes = super.generateBytecodes(className);
        if (bytecodes == null) return null;

        // Write the generated bytecodes to the class file cache.
        for (Map.Entry<String, byte[]> me : bytecodes.entrySet()) {
            String className2 = (String) me.getKey();
            byte[] bytecode   = (byte[]) me.getValue();

            try {
                CachingJavaSourceClassLoader.writeResource(
                    this.classFileCacheResourceCreator,
                    ClassFile.getClassFileResourceName(className2),
                    bytecode
                );
            } catch (IOException ex) {
                throw new ClassNotFoundException(
                    "Writing class file to \"" + ClassFile.getClassFileResourceName(className2) + "\"",
                    ex
                );
            }
        }

        return bytecodes;
    }

    /**
     * Reads all bytes from the given resource.
     */
    private static byte[]
    readResource(Resource r) throws IOException {
        ByteArrayOutputStream baos   = new ByteArrayOutputStream();
        byte[]                buffer = new byte[4096];

        InputStream is = r.open();
        try {
            for (;;) {
                int cnt = is.read(buffer);
                if (cnt == -1) break;
                baos.write(buffer, 0, cnt);
            }
        } finally {
            try { is.close(); } catch (IOException ex) {}
        }

        return baos.toByteArray();
    }

    /**
     * Creates a resource with the given name and store the data in it.
     */
    private static void
    writeResource(ResourceCreator resourceCreator, String resourceName, byte[] data) throws IOException {
        OutputStream os = resourceCreator.createResource(resourceName);
        try {
            os.write(data);
        } finally {
            try { os.close(); } catch (IOException ex) {}
        }
    }
}
