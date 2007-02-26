
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2007, Arno Unkrig
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

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import junit.framework.*;

import org.codehaus.janino.*;
import org.codehaus.janino.Compiler; // To resolve ambiguity with "java.lang.Compiler".
import org.codehaus.janino.util.*;
import org.codehaus.janino.util.enumerator.*;
import org.codehaus.janino.util.resource.*;

public class CompilerTests extends TestCase {
    private static final String SRC = "src";

    public static Test suite() {
        TestSuite s = new TestSuite(Compiler.class.getName());
        s.addTest(new CompilerTests("testSelfCompile"));
        s.addTest(new CompilerTests("testCompileErrors"));
        return s;
    }

    public CompilerTests(String name) { super(name); }

    public void testSelfCompile() throws Exception {
        ClassLoader bootstrapClassLoader = SimpleCompiler.BOOT_CLASS_LOADER;
        File[] sourceFiles = new File[] {
            new File(SRC + "/org/codehaus/janino/Compiler.java"),
            new File(SRC + "/org/codehaus/janino/samples/ExpressionDemo.java"),
            new File(SRC + "/org/codehaus/janino/util/resource/MapResourceCreator.java"),
        };
        DirectoryResourceFinder sourceFinder = new DirectoryResourceFinder(new File(SRC));
        boolean verbose = false;

        Benchmark b = new Benchmark(true);
        b.beginReporting("Compile Janino from scratch");
        MapResourceCreator classFileResources1 = new MapResourceCreator();
        {
            Compiler c = new Compiler(
                sourceFinder,                                       // sourceFinder
                new ClassLoaderIClassLoader(bootstrapClassLoader),  // iClassLoader
                ResourceFinder.EMPTY_RESOURCE_FINDER,               // classFileFinder
                classFileResources1,                                // classFileCreator
                (String) null,                                      // optionalCharacterEncoding
                verbose,                                            // verbose
                DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION, // debuggingInformation
                (WarningHandler) null                               // optionalWarningHandler
            );
            c.setCompileErrorHandler(new UnitCompiler.ErrorHandler() {
                public void handleError(String message, Location optionalLocation) throws CompileException {
                    throw new CompileException(message, optionalLocation);
                }
            });
            c.compile(sourceFiles);
        }
        Map classFileMap1 = classFileResources1.getMap();
        b.endReporting("Generated " + classFileMap1.size() + " class files.");

        b.beginReporting("Compile Janino again, but with the class files created during the first compilation being available, i.e. only the explicitly given source files should be recompiled");
        MapResourceCreator classFileResources2 = new MapResourceCreator();
        MapResourceFinder classFileFinder = new MapResourceFinder(classFileMap1);
        classFileFinder.setLastModified(System.currentTimeMillis());
        {
            Compiler c = new Compiler(
                sourceFinder,                                       // sourceFinder
                new ClassLoaderIClassLoader(bootstrapClassLoader),  // iClassLoader
                classFileFinder,                                    // classFileFinder
                classFileResources2,                                // classFileCreator
                (String) null,                                      // optionalCharacterEncoding
                verbose,                                            // verbose
                DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION, // debuggingInformation
                (WarningHandler) null                               // optionalWarningHandler
            );
            c.setCompileErrorHandler(new UnitCompiler.ErrorHandler() {
                public void handleError(String message, Location optionalLocation) throws CompileException {
                    throw new CompileException(message, optionalLocation);
                }
            });
            c.compile(sourceFiles);
        }
        b.endReporting("Generated " + classFileResources2.getMap().size() + " class files.");
        assertTrue("Only few class files re-generated", classFileResources2.getMap().size() < 30);

        b.beginReporting("Compile Janino again, but this time using the classes generated by the first compilation. A.k.a., \"compile Janino with itself\"");

        // Set up a ClassLoader for the previously JANINO-compiled classes.
        final ClassLoader cl = new ResourceFinderClassLoader(new MapResourceFinder(classFileMap1), bootstrapClassLoader);

        // The helper object "l" accesses the previously JANINO-compiled classes.
        class Loader {
            Class  loadClass(Class c)
            throws ClassNotFoundException {
                return cl.loadClass(c.getName());
            }
            Object instantiate(Class c, Class[] parameterTypes, Object[] arguments)
            throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, ClassNotFoundException {
                return this.loadClass(c).getDeclaredConstructor(parameterTypes).newInstance(arguments);
            }
            Object getStaticField(Class c, String fieldName)
            throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
                return this.loadClass(c).getDeclaredField(fieldName).get(null);
            }
            Object invoke(Object object, String methodName, Class[] parameterTypes, Object[] arguments)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
                return object.getClass().getDeclaredMethod(methodName, parameterTypes).invoke(object, arguments);
            }
        };
        Loader l = new Loader();
        Map classFileMap3 = new HashMap();
        {
            Object sf   = l.instantiate(DirectoryResourceFinder.class, new Class[] { File.class }, new Object[] { new File(SRC) });
            Object icl  = l.instantiate(ClassLoaderIClassLoader.class, new Class[] { ClassLoader.class }, new Object[] { bootstrapClassLoader });
            Object cfrf = l.getStaticField(ResourceFinder.class, "EMPTY_RESOURCE_FINDER");
            Object cfrc = l.instantiate(MapResourceCreator.class, new Class[] { Map.class }, new Object[] { classFileMap3 });
            Object di   = l.getStaticField(DebuggingInformation.class, "DEFAULT_DEBUGGING_INFORMATION");

            Object compiler = l.instantiate(Compiler.class, new Class[] {
                l.loadClass(ResourceFinder.class),  // sourceFinder
                l.loadClass(IClassLoader.class),    // iClassLoader
                l.loadClass(ResourceFinder.class),  // classFileResourceFinder
                l.loadClass(ResourceCreator.class), // classFileResourceCreator
                String.class,                       // optionalCharacterEncoding
                boolean.class,                      // verbose
                l.loadClass(EnumeratorSet.class),   // debuggingInformation
                l.loadClass(WarningHandler.class),  // optionalWarningHandler
            }, new Object[] {
                sf,                                     // sourceFinder
                icl,                                    // iClassLoader
                cfrf,                                   // classFileResourceFinder
                cfrc,                                   // classFileResourceCreator
                (String) null,                          // optionalCharacterEncoding
                verbose ? Boolean.TRUE : Boolean.FALSE, // verbose
                di,                                     // debuggingInformation
                null,                                   // optionalWarningHandler
            });
            l.invoke(compiler, "compile", new Class[] { File[].class }, new Object[] { sourceFiles });
        }
        b.endReporting("Generated " + classFileMap3.size() + " class files.");

        // Compare "classFileMap1" and "classFileMap3". We cannot use "Map.equals()" because we
        // want to check byte-by-byte identity rather than reference identity.
        assertEquals(classFileMap1.size(), classFileMap3.size());
        for (Iterator it = classFileMap1.entrySet().iterator(); it.hasNext();) {
            Map.Entry me = (Map.Entry) it.next();
            assertTrue(Arrays.equals((byte[]) me.getValue(), (byte[]) classFileMap3.get(me.getKey())));
        }
    }

    public void testCompileErrors() throws Exception {
        Map sources = new HashMap();
        sources.put("pkg/A.java", ( // Class A uses class B, C, D.
            "package pkg;\n" +
            "public class A {\n" +
            "    void meth() {\n" +
            "        new B();\n" +
            "        new C();\n" +
            "        new D();\n" +
            "    }\n" +
            "}\n"
        ).getBytes());
        sources.put("pkg/B.java", (
            "package pkg;\n" +
            "public class B {\n" +
            "}\n"
        ).getBytes());
        sources.put("pkg/C.java", ( // Class C contains a compile error.
            "package pkg;\n" +
            "public class C extends E {\n" + // Compile error, because a class "E" is not defined.
            "}\n"
        ).getBytes());
        sources.put("pkg/D.java", (
            "package pkg;\n" +
            "public class D {\n" +
            "}\n"
        ).getBytes());
        ResourceFinder sourceFinder = new MapResourceFinder(sources);

        Map classes = new HashMap();
        Compiler compiler = new Compiler(
            sourceFinder,                                                  // sourceFinder
            new ClassLoaderIClassLoader(this.getClass().getClassLoader()), // iClassLoader
            ResourceFinder.EMPTY_RESOURCE_FINDER,                          // classFileFinder
            new MapResourceCreator(classes),                               // classFileCreator
            (String) null,                                                 // optionalCharacterEncoding
            false,                                                         // verbose
            DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION,            // debuggingInformation
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

        assertEquals(makeSet(new Object[] { "pkg/A.class", "pkg/B.class", }), classes.keySet());
    }

    private Set makeSet(Object[] elements) {
        Set s = new HashSet();
        for (int i = 0; i < elements.length; ++i) s.add(elements[i]);
        return s;
    }
}
