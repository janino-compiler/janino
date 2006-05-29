
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino;

import java.io.*;
import java.util.*;

import org.codehaus.janino.util.*;
import org.codehaus.janino.util.enumerator.*;
import org.codehaus.janino.util.resource.*;

/**
 * A {@link org.codehaus.janino.JavaSourceClassLoader} that uses a
 * resource storage provided by the application to cache compiled
 * classes and thus saving unnecessary recompilations.
 * <p>
 * The application provides access to the resource storeage through a pair of
 * a {@link org.codehaus.janino.util.resource.ResourceFinder} and a
 * {@link org.codehaus.janino.util.resource.ResourceCreator} (see
 * {@link #CachingJavaSourceClassLoader(ClassLoader, ResourceFinder, String, ResourceFinder, ResourceCreator, EnumeratorSet)}.
 * <p>
 * See {@link org.codehaus.janino.JavaSourceClassLoader#main(String[])} for
 * an example how to use this class.
 */
public class CachingJavaSourceClassLoader extends JavaSourceClassLoader {
    private final ResourceFinder  classFileCacheResourceFinder;
    private final ResourceCreator classFileCacheResourceCreator;
    private final ResourceFinder  sourceFinder;

    /**
     * See {@link #CachingJavaSourceClassLoader(ClassLoader, ResourceFinder, String, ResourceFinder, ResourceCreator, EnumeratorSet)}.
     *
     * @param optionalSourcePath Directories to scan for source files
     * @param cacheDirectory Directory to use for caching generated class files
     */
    public CachingJavaSourceClassLoader(
        ClassLoader   parentClassLoader,
        File[]        optionalSourcePath,
        String        optionalCharacterEncoding,
        File          cacheDirectory,
        EnumeratorSet debuggingInformation
    ) {
        this(
            parentClassLoader,                            // parentClassLoader
            (                                             // sourceFinder
                optionalSourcePath == null ?
                (ResourceFinder) new DirectoryResourceFinder(new File(".")) :
                (ResourceFinder) new PathResourceFinder(optionalSourcePath)
            ),
            optionalCharacterEncoding,                    // optionalCharacterEncoding
            new DirectoryResourceFinder(cacheDirectory),  // classFileCacheResourceFinder
            new DirectoryResourceCreator(cacheDirectory), // classFileCacheResourceCreator
            debuggingInformation                          // debuggingInformation
        );
    }

    /**
     * Notice that this class is thread-safe if and only if the
     * <code>classFileCacheResourceCreator</code> stores its data atomically,
     * i.e. the <code>classFileCacheResourceFinder</code> sees the resource
     * written by the <code>classFileCacheResourceCreator</code> only after
     * the {@link OutputStream} is closed.
     * <p>
     * In order to make the caching scheme work, both the
     * <code>classFileCacheResourceFinder</code> and the
     * <code>sourceFinder</code> must support the {@link org.codehaus.janino.util.resource.Resource#lastModified()}
     * method, so that the modification time of the source and the class files
     * can be compared.
     * 
     * @param parentClassLoader             Attempt to load classes through this one before looking for source files
     * @param sourceFinder                  Finds Java<sup>TM</sup> source for class <code>pkg.Cls</code> in resource <code>pkg/Cls.java</code>
     * @param optionalCharacterEncoding     Encoding of Java<sup>TM</sup> source or <code>null</code> for platform default encoding
     * @param classFileCacheResourceFinder  Finds precompiled class <code>pkg.Cls</code> in resource <code>pkg/Cls.class</code>
     * @param classFileCacheResourceCreator Stores compiled class <code>pkg.Cls</code> in resource <code>pkg/Cls.class</code>
     * @param debuggingInformation          What debugging information to include into the generated class files
     */
    public CachingJavaSourceClassLoader(
        ClassLoader     parentClassLoader,
        ResourceFinder  sourceFinder,
        String          optionalCharacterEncoding,
        ResourceFinder  classFileCacheResourceFinder,
        ResourceCreator classFileCacheResourceCreator,
        EnumeratorSet   debuggingInformation
    ) {
        super(parentClassLoader, sourceFinder, optionalCharacterEncoding, debuggingInformation);
        this.classFileCacheResourceFinder  = classFileCacheResourceFinder;
        this.classFileCacheResourceCreator = classFileCacheResourceCreator;
        this.sourceFinder                  = sourceFinder;
    }

    /**
     * Override {@link JavaSourceClassLoader#findClass(String)} to implement
     * class file caching.
     */
    protected Class findClass(String className) throws ClassNotFoundException {

        // Check whether a class file resource exists in the cache.
        {
            Resource classFileResource = this.classFileCacheResourceFinder.findResource(ClassFile.getClassFileResourceName(className));
            if (classFileResource != null) {
    
                // Check whether a source file resource exists.
                Resource sourceResource = this.sourceFinder.findResource(ClassFile.getSourceResourceName(className));
                if (sourceResource == null) throw new ClassNotFoundException(className);
    
                // Check whether the class file is up-to-date.
                if (sourceResource.lastModified() < classFileResource.lastModified()) {

                    // Yes, it is... read the bytecode from the file and define the class.
                    byte[] bytecode;
                    InputStream is = null;
                    try {
                        is = classFileResource.open();
                        bytecode = CachingJavaSourceClassLoader.readInputStream(is);
                    } catch (IOException ex) {
                        throw new ClassNotFoundException("Reading class file from \"" + classFileResource + "\"", ex);
                    } finally {
                        try { is.close(); } catch (IOException ex) {}
                    }
                    return this.defineBytecode(className, bytecode);
                }
            }
        }

        // Cache miss... generate the bytecode from source.
        Map bytecodes = this.generateBytecodes(className);
        if (bytecodes == null) throw new ClassNotFoundException(className);

        // Write the generated bytecodes to the class file cache.
        for (Iterator it = bytecodes.entrySet().iterator(); it.hasNext();) {
            Map.Entry me = (Map.Entry) it.next();
            String className2 = (String) me.getKey();
            byte[] bytecode = (byte[]) me.getValue();

            OutputStream os = null;
            try {
                os = this.classFileCacheResourceCreator.createResource(ClassFile.getClassFileResourceName(className2));
                os.write(bytecode);
            } catch (IOException ex) {
                throw new ClassNotFoundException("Writing class file to \"" + ClassFile.getClassFileResourceName(className2) + "\"", ex);
            } finally {
                if (os != null) try { os.close(); } catch (IOException ex) {}
            }
        }

        Class clazz = this.defineBytecodes(className, bytecodes);
        if (clazz == null) throw new RuntimeException("Scanning, parsing and compiling class \"" + className + "\" did not create a class file!?");
        return clazz;
    }

    /**
     * Read data from <code>is</code> until EOF into a byte array.
     */
    private static byte[] readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CachingJavaSourceClassLoader.copy(is, baos);
        return baos.toByteArray();
    }

    /**
     * Copy data from <code>is</code> until EOF to <code>os</code>.
     */
    private static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[4096];
        for (;;) {
            int cnt = is.read(buffer);
            if (cnt == -1) return;
            os.write(buffer, 0, cnt);
        }
    }
}
