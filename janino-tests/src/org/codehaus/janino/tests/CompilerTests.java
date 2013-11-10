
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.ICookable;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Compiler;
import org.codehaus.janino.IClassLoader;
import org.codehaus.janino.SimpleCompiler;
import org.codehaus.janino.util.Benchmark;
import org.codehaus.janino.util.ResourceFinderClassLoader;
import org.codehaus.janino.util.resource.DirectoryResourceFinder;
import org.codehaus.janino.util.resource.MapResourceCreator;
import org.codehaus.janino.util.resource.MapResourceFinder;
import org.codehaus.janino.util.resource.MultiResourceFinder;
import org.codehaus.janino.util.resource.Resource;
import org.codehaus.janino.util.resource.ResourceCreator;
import org.codehaus.janino.util.resource.ResourceFinder;
import org.junit.Test;

// CHECKSTYLE JavadocMethod:OFF

/** Unit tests for the {@link SimpleCompiler}. */
public
class CompilerTests {

    private static final String JANINO_SRC           = "../janino/src";
    private static final String COMMONS_COMPILER_SRC = "../commons-compiler/src";

    @Test public void
    testSelfCompile() throws Exception {

        ClassLoader bootstrapClassLoader = ICookable.BOOT_CLASS_LOADER;
        File[]      sourceFiles          = new File[] {
            new File(JANINO_SRC + "/org/codehaus/janino/Compiler.java"),
            new File(COMMONS_COMPILER_SRC + "/org/codehaus/commons/compiler/samples/ExpressionDemo.java"),
            new File(JANINO_SRC + "/org/codehaus/janino/ClassLoaderIClassLoader.java"),
            new File(JANINO_SRC + "/org/codehaus/janino/util/resource/MapResourceCreator.java"),
        };
        ResourceFinder sourceFinder = new MultiResourceFinder(Arrays.asList(new ResourceFinder[] {
            new DirectoryResourceFinder(new File(JANINO_SRC)),
            new DirectoryResourceFinder(new File(COMMONS_COMPILER_SRC)),
        }));
        boolean verbose     = false;
        boolean debugSource = true, debugLines = true, debugVars = false;

        Benchmark b = new Benchmark(true);
        b.beginReporting("Compile Janino from scratch");
        MapResourceCreator classFileResources1 = new MapResourceCreator();
        {
            Compiler c = new Compiler(
                sourceFinder,                                      // sourceFinder
                new ClassLoaderIClassLoader(bootstrapClassLoader), // iClassLoader
                ResourceFinder.EMPTY_RESOURCE_FINDER,              // classFileFinder
                classFileResources1,                               // classFileCreator
                (String) null,                                     // optionalCharacterEncoding
                verbose,                                           // verbose
                debugSource,                                       // debugSource
                debugLines,                                        // debugLines
                debugVars,                                         // debugVars
                (WarningHandler) null                              // optionalWarningHandler
            );
            c.setCompileErrorHandler(new ErrorHandler() {

                @Override public void
                handleError(String message, Location optionalLocation) throws CompileException {
                    throw new CompileException(message, optionalLocation);
                }
            });
            c.compile(sourceFiles);
        }
        Map/*<String, byte[]>*/ classFileMap1 = classFileResources1.getMap();
        b.endReporting("Generated " + classFileMap1.size() + " class files.");

        b.beginReporting(
            "Compile Janino again, but with the class files created during the first compilation being available, "
            + "i.e. only the explicitly given source files should be recompiled"
        );
        MapResourceCreator classFileResources2 = new MapResourceCreator();
        MapResourceFinder  classFileFinder     = new MapResourceFinder(classFileMap1);
        classFileFinder.setLastModified(System.currentTimeMillis());
        {
            Compiler c = new Compiler(
                sourceFinder,                                       // sourceFinder
                new ClassLoaderIClassLoader(bootstrapClassLoader),  // iClassLoader
                classFileFinder,                                    // classFileFinder
                classFileResources2,                                // classFileCreator
                (String) null,                                      // optionalCharacterEncoding
                verbose,                                            // verbose
                true,                                               // debugSource
                true,                                               // debugLines
                false,                                              // debugVars
                (WarningHandler) null                               // optionalWarningHandler
            );
            c.setCompileErrorHandler(new ErrorHandler() {

                @Override public void
                handleError(String message, Location optionalLocation) throws CompileException {
                    throw new CompileException(message, optionalLocation);
                }
            });
            c.compile(sourceFiles);
        }
        b.endReporting("Generated " + classFileResources2.getMap().size() + " class files.");
        assertTrue("Only few class files re-generated", classFileResources2.getMap().size() < 30);

        b.beginReporting(
            "Compile Janino again, but this time using the classes generated by the first compilation. "
            + "A.k.a., \"compile Janino with itself\""
        );

        // Set up a ClassLoader for the previously JANINO-compiled classes.
        final ClassLoader cl = new ResourceFinderClassLoader(
            new MapResourceFinder(classFileMap1),
            bootstrapClassLoader
        );

        // The helper object "l" accesses the previously JANINO-compiled classes.
        class Loader {

            Class
            loadClass(Class c) throws ClassNotFoundException { return cl.loadClass(c.getName()); }

            Object
            instantiate(Class c, Class[]  parameterTypes, Object[] arguments) throws Exception {
                return this.loadClass(c).getDeclaredConstructor(parameterTypes).newInstance(arguments);
            }

            Object
            getStaticField(Class c, String fieldName)
            throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
                return this.loadClass(c).getDeclaredField(fieldName).get(null);
            }

            Object
            invoke(Object object, String methodName, Class[] parameterTypes, Object[] arguments)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
                return object.getClass().getDeclaredMethod(methodName, parameterTypes).invoke(object, arguments);
            }
        }
        Loader                  l             = new Loader();
        Map/*<String, byte[]>*/ classFileMap3 = new HashMap();
        {
            Object sf = l.instantiate(MultiResourceFinder.class, new Class[] { Collection.class }, new Object[] {
                Arrays.asList(new Object[] {
                    l.instantiate(DirectoryResourceFinder.class, new Class[] { File.class }, new Object[] {
                        new File(JANINO_SRC),
                    }),
                    l.instantiate(DirectoryResourceFinder.class, new Class[] { File.class }, new Object[] {
                        new File(COMMONS_COMPILER_SRC),
                    }),
                }),
            });
            Object icl = l.instantiate(ClassLoaderIClassLoader.class, new Class[] { ClassLoader.class }, new Object[] {
                bootstrapClassLoader,
            });
            Object cfrf = l.getStaticField(ResourceFinder.class, "EMPTY_RESOURCE_FINDER");
            Object cfrc = l.instantiate(
                MapResourceCreator.class,
                new Class[] { Map.class },
                new Object[] { classFileMap3 }
            );

            Object compiler = l.instantiate(Compiler.class, new Class[] {
                l.loadClass(ResourceFinder.class),  // sourceFinder
                l.loadClass(IClassLoader.class),    // iClassLoader
                l.loadClass(ResourceFinder.class),  // classFileResourceFinder
                l.loadClass(ResourceCreator.class), // classFileResourceCreator
                String.class,                       // optionalCharacterEncoding
                boolean.class,                      // verbose
                boolean.class,                      // debugSource
                boolean.class,                      // debugLines
                boolean.class,                      // debugVars
                l.loadClass(WarningHandler.class),  // optionalWarningHandler
            }, new Object[] {
                sf,                                     // sourceFinder
                icl,                                    // iClassLoader
                cfrf,                                   // classFileResourceFinder
                cfrc,                                   // classFileResourceCreator
                (String) null,                          // optionalCharacterEncoding
                verbose ? Boolean.TRUE : Boolean.FALSE, // verbose
                new Boolean(debugSource),               // debugSource
                new Boolean(debugLines),                // debugLines
                new Boolean(debugVars),                 // debugVars
                null,                                   // optionalWarningHandler
            });
            l.invoke(compiler, "compile", new Class[] { File[].class }, new Object[] { sourceFiles });
        }
        b.endReporting("Generated " + classFileMap3.size() + " class files.");

        // Compare "classFileMap1" and "classFileMap3". We cannot use "Map.equals()" because we
        // want to check byte-by-byte identity rather than reference identity.
        assertEquals(classFileMap1.size(), classFileMap3.size());
        for (Iterator/*<Entry<String, byte[]>>*/ it = classFileMap1.entrySet().iterator(); it.hasNext();) {
            Map.Entry/*<String, byte[]>*/ me = (Map.Entry) it.next();
            assertTrue(Arrays.equals((byte[]) me.getValue(), (byte[]) classFileMap3.get(me.getKey())));
        }
    }

    @Test public void
    testCompileErrors() throws Exception {
        Map/*<String, String>*/ sources = new HashMap();
        sources.put("pkg/A.java", ( // Class A uses class B, C, D.
            ""
            + "package pkg;\n"
            + "public class A {\n"
            + "    void meth() {\n"
            + "        new B();\n"
            + "        new C();\n"
            + "        new D();\n"
            + "    }\n"
            + "}\n"
        ).getBytes());
        sources.put("pkg/B.java", (
            ""
            + "package pkg;\n"
            + "public class B {\n"
            + "}\n"
        ).getBytes());
        sources.put("pkg/C.java", ( // Class C contains a compile error.
            ""
            + "package pkg;\n"
            + "public class C extends E {\n" // Compile error, because a class "E" is not defined.
            + "}\n"
        ).getBytes());
        sources.put("pkg/D.java", (
            ""
            + "package pkg;\n"
            + "public class D {\n"
            + "}\n"
        ).getBytes());
        ResourceFinder sourceFinder = new MapResourceFinder(sources);

        Map/*<String, byte[]>*/ classes  = new HashMap();
        Compiler                compiler = new Compiler(
            sourceFinder,                                                  // sourceFinder
            new ClassLoaderIClassLoader(this.getClass().getClassLoader()), // iClassLoader
            ResourceFinder.EMPTY_RESOURCE_FINDER,                          // classFileFinder
            new MapResourceCreator(classes),                               // classFileCreator
            (String) null,                                                 // optionalCharacterEncoding
            false,                                                         // verbose
            true,                                                          // debugSource
            true,                                                          // debugLines
            false,                                                         // debugVars
            (WarningHandler) null                                          // optionalWarningHandler
        );
        COMPILE: {
            try {
                compiler.compile(new Resource[] { sourceFinder.findResource("pkg/A.java") });
            } catch (CompileException ex) {
                break COMPILE;
            }
            fail("CompileException expected");
        }

        assertEquals(new HashSet(Arrays.asList(new Object[] { "pkg/A.class", "pkg/B.class", })), classes.keySet());
    }

    /**
     * JANINO (as of now) does not support generics, and should clearly state the fact instead of throwing
     * mysterious {@link CompileException}s like '"{" expected at start of class body'.
     */
    @Test public void
    testGenerics() {
        try {
            new SimpleCompiler().cook("class Foo<K, V> {}");
        } catch (CompileException ce) {
            if (ce.getMessage().contains("does not support generics")) return;
            fail("Unexpected CompileException message '" + ce.getMessage() + "'");
        }
        fail("Usage of generics should cause a CompileException");

    }
}
