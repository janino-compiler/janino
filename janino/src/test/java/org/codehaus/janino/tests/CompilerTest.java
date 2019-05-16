
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved.
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

package org.codehaus.janino.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.ICookable;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.ByteArrayClassLoader;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Compiler;
import org.codehaus.janino.IClassLoader;
import org.codehaus.janino.Java;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.SimpleCompiler;
import org.codehaus.janino.UnitCompiler;
import org.codehaus.janino.util.Benchmark;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.ResourceFinderClassLoader;
import org.codehaus.janino.util.resource.DirectoryResourceFinder;
import org.codehaus.janino.util.resource.MapResourceCreator;
import org.codehaus.janino.util.resource.MapResourceFinder;
import org.codehaus.janino.util.resource.MultiResourceFinder;
import org.codehaus.janino.util.resource.Resource;
import org.codehaus.janino.util.resource.ResourceCreator;
import org.codehaus.janino.util.resource.ResourceFinder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

// SUPPRESS CHECKSTYLE JavadocMethod:9999

/**
 * Unit tests for the {@link SimpleCompiler}.
 */
public
class CompilerTest {

    private static final String JANINO_SRC           = "../janino/src/main/java";
    private static final String COMMONS_COMPILER_SRC = "../commons-compiler/src/main/java";
    private static final String RESOURCE_DIR         = "src/test/resources";

    @Before
    public void
    setUp() throws Exception {

        // Optionally print class file disassemblies to the console.
        if (Boolean.parseBoolean(System.getProperty("disasm"))) {
            Logger scl = Logger.getLogger(UnitCompiler.class.getName());
            for (Handler h : scl.getHandlers()) {
                h.setLevel(Level.FINEST);
            }
            scl.setLevel(Level.FINEST);
        }
    }

    /**
     * Another attempt to reproduce issue #32... still no success.
     */
    @Ignore
    @Test public void
    testSelfCompileParallel() throws Exception {

        final Throwable[] ex = new Throwable[1];

        Runnable r = new Runnable() {

            @Override public void
            run() {
                try {
                    for (int i = 0; i < 10; i++) {
                        System.out.printf("#%d%n", i);
                        CompilerTest.this.testSelfCompile();
                    }
                } catch (Throwable t) { // SUPPRESS CHECKSTYLE IllegalCatch
                    ex[0] = t;
                }
            }
        };

        Thread[] ts = new Thread[10];
        for (int i = 0; i < ts.length; i++) {
            (ts[i] = new Thread(r)).start();
        }
        for (int i = 0; i < ts.length && ex[0] == null; i++) {
            ts[i].join();
        }

        if (ex[0] != null) throw new AssertionError(ex[0]);
    }

    @Test public void
    testSelfCompile() throws Exception {

        ClassLoader bootstrapClassLoader = ICookable.BOOT_CLASS_LOADER;
        File[]      sourceFiles          = {
            new File(CompilerTest.JANINO_SRC + "/org/codehaus/janino/Compiler.java"),
            new File(CompilerTest.COMMONS_COMPILER_SRC + "/org/codehaus/commons/compiler/samples/ExpressionDemo.java"),
            new File(CompilerTest.JANINO_SRC + "/org/codehaus/janino/ClassLoaderIClassLoader.java"),
            new File(CompilerTest.JANINO_SRC + "/org/codehaus/janino/util/resource/MapResourceCreator.java"),
        };
        ResourceFinder sourceFinder = new MultiResourceFinder(Arrays.asList(new ResourceFinder[] {
            new DirectoryResourceFinder(new File(CompilerTest.JANINO_SRC)),
            new DirectoryResourceFinder(new File(CompilerTest.COMMONS_COMPILER_SRC)),
        }));

        // --------------------

        Benchmark b = new Benchmark(true);
        b.beginReporting("Compile Janino from scratch");
        MapResourceCreator classFileResources1 = new MapResourceCreator();
        {
            Compiler c = new Compiler(
                sourceFinder,                                     // sourceFinder
                new ClassLoaderIClassLoader(bootstrapClassLoader) // iClassLoader
            );

            c.setClassFileCreator(classFileResources1);
            c.setCompileErrorHandler(new ErrorHandler() {

                @Override public void
                handleError(String message, @Nullable Location optionalLocation) throws CompileException {
                    throw new CompileException(message, optionalLocation);
                }
            });
            c.compile(sourceFiles);
        }
        Map<String, byte[]> classFileMap1 = classFileResources1.getMap();
        b.endReporting("Generated " + classFileMap1.size() + " class files.");
        CompilerTest.assertMoreThan("Number of generated classes", 200, classFileResources1.getMap().size());

        // --------------------

        b.beginReporting(
            "Compile Janino again, but with the class files created during the first compilation being available, "
            + "i.e. only the explicitly given source files should be recompiled"
        );
        MapResourceCreator classFileResources2 = new MapResourceCreator();
        MapResourceFinder  classFileFinder     = new MapResourceFinder(classFileMap1);
        classFileFinder.setLastModified(System.currentTimeMillis());
        {
            Compiler c = new Compiler(
                sourceFinder,                                     // sourceFinder
                new ClassLoaderIClassLoader(bootstrapClassLoader) // iClassLoader
            );
            c.setClassFileFinder(classFileFinder);
            c.setClassFileCreator(classFileResources2);
            c.setCompileErrorHandler(new ErrorHandler() {

                @Override public void
                handleError(String message, @Nullable Location optionalLocation) throws CompileException {
                    throw new CompileException(message, optionalLocation);
                }
            });
            c.compile(sourceFiles);
        }
        b.endReporting("Generated " + classFileResources2.getMap().size() + " class files.");
        CompilerTest.assertLessThan("Number of generated classes", 30, classFileResources2.getMap().size());

        // --------------------

        b.beginReporting(
            "Compile Janino again, but this time using the classes generated by the first compilation, "
            + "a.k.a. \"compile Janino with itself\""
        );

        // Set up a ClassLoader for the previously JANINO-compiled classes.
        final ClassLoader cl = new ResourceFinderClassLoader(
            new MultiResourceFinder(Arrays.asList(
                new MapResourceFinder(classFileMap1),
                new DirectoryResourceFinder(new File("../de.unkrig.jdisasm/bin"))
            )),
            bootstrapClassLoader
        );

        // The helper object "l" accesses the previously JANINO-compiled classes.
        class Loader {

            Class<?>
            loadClass(Class<?> c) throws ClassNotFoundException { return cl.loadClass(c.getName()); }

            Object
            instantiate(Class<?> c, Class<?>[]  parameterTypes, Object[] arguments) throws Exception {
                return this.loadClass(c).getDeclaredConstructor(parameterTypes).newInstance(arguments);
            }

            Object
            getStaticField(Class<?> c, String fieldName)
            throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
                return this.loadClass(c).getDeclaredField(fieldName).get(null);
            }

            Object
            invoke(Object object, String methodName, Class<?>[] parameterTypes, Object[] arguments)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
                return object.getClass().getMethod(methodName, parameterTypes).invoke(object, arguments);
            }
        }
        Loader              l             = new Loader();
        Map<String, byte[]> classFileMap3 = new HashMap<String, byte[]>();
        {
            Object sf = l.instantiate(MultiResourceFinder.class, new Class[] { Collection.class }, new Object[] {
                Arrays.asList(new Object[] {
                    l.instantiate(DirectoryResourceFinder.class, new Class[] { File.class }, new Object[] {
                        new File(CompilerTest.JANINO_SRC),
                    }),
                    l.instantiate(DirectoryResourceFinder.class, new Class[] { File.class }, new Object[] {
                        new File(CompilerTest.COMMONS_COMPILER_SRC),
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
                l.loadClass(ResourceFinder.class), // sourceFinder
                l.loadClass(IClassLoader.class)    // iClassLoader
            }, new Object[] {
                sf, // sourceFinder
                icl // iClassLoader
            });

            l.invoke(
                compiler,
                "setClassFileFinder",
                new Class[] { l.loadClass(ResourceFinder.class) },
                new Object[] { cfrf }
            );
            l.invoke(
                compiler,
                "setClassFileCreator",
                new Class[] { l.loadClass(ResourceCreator.class) },
                new Object[] { cfrc }
            );

            l.invoke(
                compiler,
                "compile",
                new Class[] { File[].class },
                new Object[] { sourceFiles }
            );
        }
        b.endReporting("Generated " + classFileMap3.size() + " class files.");

        // Compare "classFileMap1" and "classFileMap3". We cannot use "Map.equals()" because we
        // want to check byte-by-byte identity rather than reference identity.
        Assert.assertEquals(classFileMap1.size(), classFileMap3.size());
        for (Map.Entry<String, byte[]> me : classFileMap1.entrySet()) {
            Assert.assertTrue(Arrays.equals(me.getValue(), classFileMap3.get(me.getKey())));
        }
    }

    @Test public void
    testCompileErrors() throws Exception {

        MapResourceFinder sourceFinder = new MapResourceFinder();

        sourceFinder.addResource("pkg/A.java", ( // Class A uses class B, C, D.
            ""
            + "package pkg;\n"
            + "public class A {\n"
            + "    void meth() {\n"
            + "        new B();\n"
            + "        new C();\n"
            + "        new D();\n"
            + "    }\n"
            + "}\n"
        ));
        sourceFinder.addResource("pkg/B.java", (
            ""
            + "package pkg;\n"
            + "public class B {\n"
            + "}\n"
        ));
        sourceFinder.addResource("pkg/C.java", ( // Class C contains a compile error.
            ""
            + "package pkg;\n"
            + "public class C extends E {\n" // Compile error, because a class "E" is not defined.
            + "}\n"
        ));
        sourceFinder.addResource("pkg/D.java", (
            ""
            + "package pkg;\n"
            + "public class D {\n"
            + "}\n"
        ));

        try {
            CompilerTest.compile(sourceFinder);
            Assert.fail("CompileException expected");
        } catch (CompileException ex) {
            Assert.assertTrue(
                ex.getMessage(),
                ex.getMessage().contains("Cannot determine simple type name \"E\"")
            );
        }
    }

    @Test public void
    testInMemoryCompilation() throws Exception {

        // Set of compilation units.
        MapResourceFinder sourceFinder = new MapResourceFinder();
        sourceFinder.addResource("pkg1/A.java", (
            ""
            + "package pkg1;\n"
            + "\n"
            + "import pkg2.*;\n"
            + "\n"
            + "public\n"
            + "class A {\n"
            + "    public static String main() { return B.meth(); }\n"
            + "    public static String meth() { return \"HELLO\"; }\n"
            + "}\n"
        ));
        sourceFinder.addResource("pkg2/B.java", (
            ""
            + "package pkg2;\n"
            + "\n"
            + "import pkg1.*;\n"
            + "\n"
            + "public\n"
            + "class B {\n"
            + "    public static String meth() { return A.meth(); }\n"
            + "}\n"
        ));

        final Map<String, byte[]> classes = CompilerTest.compile(sourceFinder);

        // Set up a class loader that finds and defined the generated classes.
        ClassLoader cl = new ByteArrayClassLoader(classes);

        // Now invoke "pkg1.A.main()" and assert that it returns "HELLO".
        Assert.assertEquals("HELLO", cl.loadClass("pkg1.A").getMethod("main").invoke(null));
    }

    private static Map<String, byte[]>
    compile(MapResourceFinder sourceFinder) throws CompileException, IOException {

        // Set up the compiler.
        Compiler compiler = new Compiler(
            sourceFinder,                                                    // sourceFinder
            new ClassLoaderIClassLoader(CompilerTest.class.getClassLoader()) // parentIClassLoader
        );

        // Storage for generated bytecode.
        final Map<String, byte[]> classes = new HashMap<String, byte[]>();
        compiler.setClassFileCreator(new MapResourceCreator(classes));
        compiler.setClassFileFinder(new MapResourceFinder(classes));

        // Compile all sources.
        compiler.compile(sourceFinder.resources().toArray(new Resource[0]));

        return classes;
    }

    @Test public void
    testImplicitCastTernaryOperator() throws Exception {

        String cu = (
            ""
            + "package pkg;\n"
            + "public class A {\n"
            + "    public static final Boolean wrap(boolean b) { return Boolean.valueOf(b); }\n"
            + "    public java.lang.Object one() { \n"
            + "       return this.f == (byte) 2 ? null : wrap(this.f == (byte) 1);\n"
            + "    }\n"
            + ""
            + "    public void two() {\n"
            + "       byte b = (byte) ((((byte) 2 == (byte) 2 ? (byte) 2 : (byte) 1 ^ (byte) 2)));\n"
            + "    }\n"
            + ""
            + "    public byte f = (byte) 2;\n"
            + "}\n"
        );

        ISimpleCompiler sc = new SimpleCompiler();
        sc.cook(cu);
    }


    // This is currently failing
    // https://github.com/codehaus/janino/issues/4
    @Ignore
    @Test public void
    testReferenceQualifiedSuper() throws Exception {
        CompilerTest.doCompile(true, true, false, CompilerTest.RESOURCE_DIR + "/a/Test.java");
    }

    // https://github.com/codehaus/janino/issues/5
    @Test public void
    testLocalVarTableGeneration() throws Exception {
        SimpleCompiler s = new SimpleCompiler();
        s.setDebuggingInformation(true, true, true);
        s.cook(new FileInputStream(CompilerTest.RESOURCE_DIR + "/a/TestLocalVarTable.java"));
        s.getClassLoader().loadClass("a.TestLocalVarTable");
    }

    public static List<ClassFile>
    doCompile(
        boolean   debugSource,
        boolean   debugLines,
        boolean   debugVars,
        String... fileNames
    ) throws Exception {

        // Parse each compilation unit.
        final List<AbstractCompilationUnit> acus = new LinkedList<AbstractCompilationUnit>();
        final IClassLoader                  cl   = new ClassLoaderIClassLoader(CompilerTest.class.getClassLoader());
        List<ClassFile>                     cfs  = new LinkedList<ClassFile>();
        for (String fileName : fileNames) {

            FileReader r = new FileReader(fileName);
            try {
                Java.AbstractCompilationUnit acu = new Parser(new Scanner(fileName, r)).parseAbstractCompilationUnit();
                acus.add(acu);

                // Compile them.
                ClassFile[] compiled = new UnitCompiler(acu, cl).compileUnit(debugSource, debugLines, debugVars);
                for (ClassFile cf : compiled) {
                    cfs.add(cf);
                }
            } finally {
                try { r.close(); } catch (Exception e) {}
            }
        }
        return cfs;

    }

    private static void
    assertLessThan(@Nullable String message, int expected, int actual) {
        Assert.assertTrue(
            (message == null ? "" : message + ": ") + "Expected less than " + expected + ", but were " + actual,
            actual < expected
        );
    }

    private static void
    assertMoreThan(@Nullable String message, int expected, int actual) {
        Assert.assertTrue(
            (message == null ? "" : message + ": ") + "Expected more than " + expected + ", but were " + actual,
            actual > expected
        );
    }
}
