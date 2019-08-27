
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved. // CHECKSTYLE:OFF CHECKSTYLE:ON
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

package org.codehaus.commons.compiler.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.ICookable;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.util.Benchmark;
import org.codehaus.commons.compiler.util.Disassembler;
import org.codehaus.commons.compiler.util.ResourceFinderClassLoader;
import org.codehaus.commons.compiler.util.reflect.ByteArrayClassLoader;
import org.codehaus.commons.compiler.util.resource.DirectoryResourceFinder;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.MapResourceFinder;
import org.codehaus.commons.compiler.util.resource.MultiResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.codehaus.commons.nullanalysis.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.TestUtil;

// SUPPRESS CHECKSTYLE JavadocMethod:9999

/**
 * Unit tests for the {@link SimpleCompiler}.
 */
@RunWith(Parameterized.class) public
class CompilerTest {

    private static final String COMMONS_COMPILER_SRC     = "../commons-compiler/src/main/java";
    private static final String COMMONS_COMPILER_JDK_SRC = "../commons-compiler-jdk/src/main/java";
    private static final String JANINO_SRC               = "../janino/src/main/java";
    private static final String RESOURCE_DIR             = "src/test/resources";

    /**
     * The {@link ICompilerFactory} in effect for this test execution.
     */
    private final ICompilerFactory compilerFactory;
    private final String           compilerFactoryId;
    private final boolean          isJdk;
    private final boolean          isJanino;

    @Parameters(name = "CompilerFactory={0}") public static Collection<Object[]>
    compilerFactories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }

    public
    CompilerTest(ICompilerFactory compilerFactory) {

        this.compilerFactory = compilerFactory;

        this.compilerFactoryId = compilerFactory.getId();
        this.isJdk             = this.compilerFactoryId.equals("org.codehaus.commons.compiler.jdk"); // SUPPRESS CHECKSTYLE EqualsAvoidNull|LineLength
        this.isJanino          = this.compilerFactoryId.equals("org.codehaus.janino");
    }

    @Before
    public void
    setUp() throws Exception {
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

        File[] sourceFiles  = {
            (
                this.isJdk
                ? new File(CompilerTest.COMMONS_COMPILER_JDK_SRC + "/org/codehaus/commons/compiler/jdk/Compiler.java")
                : new File(CompilerTest.JANINO_SRC               +               "/org/codehaus/janino/Compiler.java")
            ),
            new File(CompilerTest.COMMONS_COMPILER_SRC     + "/org/codehaus/commons/compiler/samples/ExpressionDemo.java"),  // SUPPRESS CHECKSTYLE LineLength:2
            new File(CompilerTest.COMMONS_COMPILER_SRC     + "/org/codehaus/commons/compiler/util/resource/MapResourceCreator.java"),
            new File(CompilerTest.COMMONS_COMPILER_SRC     + "/org/codehaus/commons/compiler/util/resource/MapResourceFinder.java"),
        };

        Benchmark b = new Benchmark(true);

        // -------------------- PHASE 1

        SortedMap<String, byte[]> classFileMap1;
        {
            b.beginReporting("Compile " + this.compilerFactoryId + " from scratch");
            classFileMap1 = new TreeMap<String, byte[]>(
                CompilerTest.compileJanino(sourceFiles, this.compilerFactory.newCompiler(), null)
            );
            b.endReporting("Generated " + classFileMap1.size() + " class files.");

            CompilerTest.assertMoreThan("Number of generated classes", 70, classFileMap1.size());
        }

        // -------------------- PHASE 2

        {
            b.beginReporting(
                "Compile " + this.compilerFactoryId + " again, but with the class files created during the first "
                + "compilation being available, i.e. only the explicitly given source files should be recompiled"
            );
            SortedMap<String, byte[]> classFileMap2 = new TreeMap<String, byte[]>(
                CompilerTest.compileJanino(sourceFiles, this.compilerFactory.newCompiler(), classFileMap1)
            );
            b.endReporting("Generated " + classFileMap2.size() + " class files.");

            CompilerTest.assertLessThan("Number of generated classes", 20, classFileMap2.size());
        }

        // -------------------- PHASE 3

        {

            // Set up a ClassLoader for the classes generated in phase 1.
            final ClassLoader cl = new ResourceFinderClassLoader(
                new MultiResourceFinder(
                    new MapResourceFinder(classFileMap1),
                    new DirectoryResourceFinder(new File("../de.unkrig.jdisasm/bin"))
                ),
                ICookable.BOOT_CLASS_LOADER
            );

            b.beginReporting(
                "Compile " + this.compilerFactoryId + " again, but this time using the classes generated by the first "
                + "compilation, a.k.a. \"compile Janino with itself\""
            );
            SortedMap<String, byte[]> classFileMap3 = new TreeMap<String, byte[]>(CompilerTest.compileJanino(
                sourceFiles,
                cl.loadClass(this.compilerFactory.getId() + ".Compiler").getDeclaredConstructor().newInstance(),
                null // precompiledClasses
            ));
            b.endReporting("Generated " + classFileMap3.size() + " class files.");

            // Compare "classFileMap1" and "classFileMap3". We cannot use "Map.equals()" because we
            // want to check byte-by-byte identity rather than reference identity.
            Assert.assertEquals(classFileMap1.keySet(), classFileMap3.keySet());
            for (Map.Entry<String, byte[]> me : classFileMap1.entrySet()) {
                String resourceName = me.getKey();

                byte[] expectedClassFileBytes = me.getValue();
                byte[] actualClassFileBytes   = classFileMap3.get(resourceName);
                if (!Arrays.equals(expectedClassFileBytes, actualClassFileBytes)) {
                    System.out.println("Expected:");
                    Disassembler.disassembleToStdout(expectedClassFileBytes);
                    System.out.println("Actual:");
                    Disassembler.disassembleToStdout(actualClassFileBytes);
                }
                Assert.assertArrayEquals(resourceName, expectedClassFileBytes, actualClassFileBytes);
            }
        }
    }

    /**
     * Compiles the current source of JANINO with the <var>compiler</var>.
     * The <var>compiler</var> may originate from a different class loader; all accesses to it are made through
     * reflection.
     *
     * @return The generated class files
     */
    private static Map<String, byte[]>
    compileJanino(File[] sourceFiles, Object compiler, @Nullable Map<String, byte[]> precompiledClasses)
    throws Exception {

        final ClassLoader cl = compiler.getClass().getClassLoader();

        final Class<?> DirectoryResourceFinder_class = cl.loadClass("org.codehaus.commons.compiler.util.resource.DirectoryResourceFinder"); // SUPPRESS CHECKSTYLE LocalFinalVariableName|LineLength:6
        final Class<?> ICompiler_class               = cl.loadClass("org.codehaus.commons.compiler.ICompiler");
        final Class<?> MapResourceCreator_class      = cl.loadClass("org.codehaus.commons.compiler.util.resource.MapResourceCreator");
        final Class<?> MapResourceFinder_class       = cl.loadClass("org.codehaus.commons.compiler.util.resource.MapResourceFinder");
        final Class<?> MultiResourceFinder_class     = cl.loadClass("org.codehaus.commons.compiler.util.resource.MultiResourceFinder");
        final Class<?> ResourceCreator_class         = cl.loadClass("org.codehaus.commons.compiler.util.resource.ResourceCreator");
        final Class<?> ResourceFinder_class          = cl.loadClass("org.codehaus.commons.compiler.util.resource.ResourceFinder");

        final Map<String, byte[]> result = new HashMap<String, byte[]>();

        // ResourceFinder[] rfs = {
        //     new DirectoryResourceFinder(new File(JANINO_SRC),
        //     new DirectoryResourceFinder(new File(COMMONS_COMPILER_SRC),
        //     new DirectoryResourceFinder(new File(COMMONS_COMPILER_JDK_SRC),
        // };
        // compiler.setSourceFinder(new MultiResourceFinder(rfs));
        {
            final Object[] rfs = (Object[]) Array.newInstance(ResourceFinder_class, 3);
            rfs[0] = DirectoryResourceFinder_class.getDeclaredConstructor(File.class).newInstance(new File(CompilerTest.JANINO_SRC)); // SUPPRESS CHECKSTYLE LineLength:2
            rfs[1] = DirectoryResourceFinder_class.getDeclaredConstructor(File.class).newInstance(new File(CompilerTest.COMMONS_COMPILER_SRC));
            rfs[2] = DirectoryResourceFinder_class.getDeclaredConstructor(File.class).newInstance(new File(CompilerTest.COMMONS_COMPILER_JDK_SRC));
            ICompiler_class.getMethod("setSourceFinder", ResourceFinder_class).invoke(
                compiler,
                MultiResourceFinder_class.getDeclaredConstructor(rfs.getClass()).newInstance((Object) rfs)
            );
        }

        // compiler.setClassPath(new File[0])
        ICompiler_class.getMethod("setClassPath", File[].class).invoke(compiler, (Object) new File[0]);

        // compiler.setClassFileCreator(new MapResourceCreator(result));
        ICompiler_class.getMethod("setClassFileCreator", ResourceCreator_class).invoke(
            compiler,
            MapResourceCreator_class.getDeclaredConstructor(Map.class).newInstance(result)
        );

        // if (precompiledClasses != null) {
        //     MapResourceFinder classFileFinder = new MapResourceFinder(precompiledClasses);
        //     classFileFinder.setLastModified(System.currentTimeMillis());
        //     compiler.setClassFileFinder(classFileFinder);
        // } else {
        //     compiler.setClassFileFinder(ResourceFinder.EMPTY_RESOURCE_FINDER);
        // }
        if (precompiledClasses != null) {
            Object classFileFinder = MapResourceFinder_class.getDeclaredConstructor(Map.class).newInstance(
                precompiledClasses
            );
            MapResourceFinder_class.getDeclaredMethod("setLastModified", long.class).invoke(
                classFileFinder,
                System.currentTimeMillis()
            );
            ICompiler_class.getDeclaredMethod("setClassFileFinder", ResourceFinder_class).invoke(
                compiler,
                classFileFinder
            );
        } else {
            ICompiler_class.getMethod("setClassFileFinder", ResourceFinder_class).invoke(
                compiler,
                ResourceFinder_class.getDeclaredField("EMPTY_RESOURCE_FINDER").get(null)
            );
        }

        // compiler.setDebugLines(true);
        // compiler.setDebugSource(true);
        // compiler.setDebugVars(true);
//        ICompiler_class.getDeclaredMethod("setDebugLines",  boolean.class).invoke(compiler, true);
//        ICompiler_class.getDeclaredMethod("setDebugSource", boolean.class).invoke(compiler, true);
//        ICompiler_class.getDeclaredMethod("setDebugVars",   boolean.class).invoke(compiler, true);

        // compiler.compile(sourceFiles);
        ICompiler_class.getDeclaredMethod("compile", File[].class).invoke(compiler, (Object) sourceFiles);

        return result;
    }

    @Test public void
    testTypeBug() throws Exception {

        File[] sourceFiles = {
            new File(
                CompilerTest.COMMONS_COMPILER_SRC + "/org/codehaus/commons/compiler/java8/java/util/stream/Stream.java"
            ),
        };
        ResourceFinder sourceFinder = new MultiResourceFinder(
            new DirectoryResourceFinder(new File(CompilerTest.JANINO_SRC)),
            new DirectoryResourceFinder(new File(CompilerTest.COMMONS_COMPILER_SRC))
        );

        // --------------------

        Benchmark b = new Benchmark(true);
        b.beginReporting("Compile Stream.java");
        MapResourceCreator classFileResources1 = new MapResourceCreator();
        {
            ICompiler c = this.compilerFactory.newCompiler();
            c.setSourceFinder(sourceFinder);
            c.setClassPath(new File[0]);
            c.setClassFileCreator(classFileResources1);
//            c.setDebugLines(true);
//            c.setDebugSource(true);
//            c.setDebugVars(true);
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
            this.compile(sourceFinder);
            Assert.fail("CompileException expected");
        } catch (CompileException ex) {
            CompilerTest.assertMatches("(?si).*cannot.*\\bE\\b.*", ex.getMessage());
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

        final Map<String, byte[]> classes = this.compile(sourceFinder);
        Assert.assertEquals(2, classes.size());

        // Set up a class loader that finds and defined the generated classes.
        ClassLoader cl = new ByteArrayClassLoader(classes);

        // Now invoke "pkg1.A.main()" and assert that it returns "HELLO".
        Assert.assertEquals("HELLO", cl.loadClass("pkg1.A").getMethod("main").invoke(null));
    }

    private Map<String, byte[]>
    compile(MapResourceFinder sourceFinder) throws CompileException, IOException {

        // Storage for generated bytecode.
        final Map<String, byte[]> classes = new HashMap<String, byte[]>();

        // Set up the compiler.
        ICompiler compiler = this.compilerFactory.newCompiler();
        compiler.setSourceFinder(sourceFinder);
//        compiler.setIClassLoader(new ClassLoaderIClassLoader(CompilerTest.class.getClassLoader()));
        compiler.setClassFileFinder(new MapResourceFinder(classes));
        compiler.setClassFileCreator(new MapResourceCreator(classes));
//        compiler.setDebugLines(true);
//        compiler.setDebugSource(true);
//        compiler.setDebugVars(true);

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

        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(cu);
    }

    // https://github.com/codehaus/janino/issues/5
    @Test public void
    testLocalVarTableGeneration() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.setDebuggingInformation(true, true, true);
        sc.cook(new FileInputStream(CompilerTest.RESOURCE_DIR + "/a/TestLocalVarTable.java"));
        sc.getClassLoader().loadClass("a.TestLocalVarTable");
    }

    @Test public void
    testIssue98() throws Exception {

        ICompiler compiler = this.compilerFactory.newCompiler();

        // Here's the magic: Configure a custom "resource creator", so the .class files are stored in a Map, and no
        // files are created.
        Map<String, byte[]> classes = new HashMap<String, byte[]>();
        compiler.setClassFileCreator(new MapResourceCreator(classes));

        // Now compile two units with different package declarations.
        compiler.compile(new Resource[] {
            new StringResource("pkg1/A.java", "package pkg1; public class A { public static int meth() { return pkg2.B.meth(); } }"), // SUPPRESS CHECKSTYLE LineLength:4
            new StringResource("pkg2/B.java", "package pkg2; public class B { public static int meth() { return 77;            } }"),
        });

        // Set up a class loader that uses the generated .class files.
        ClassLoader cl = new ResourceFinderClassLoader(
            new MapResourceFinder(classes),    // resourceFinder
            ClassLoader.getSystemClassLoader() // parent
        );

        // Invoke "pkg1.A.meth()" and verify that the return value is correct.
        Assert.assertEquals(77, cl.loadClass("pkg1.A").getDeclaredMethod("meth").invoke(null));
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

    private static void
    assertMatches(final String regex, final String actual) {
        Assert.assertTrue(
            "Expected that \"" + actual + "\" would match regex \"" + regex + "\"",
            actual.matches(regex)
        );
    }
}
