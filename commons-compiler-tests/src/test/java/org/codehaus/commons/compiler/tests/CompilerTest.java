
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
import java.util.regex.Pattern;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.lang.ClassLoaders;
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
    private final ICompilerFactory                    compilerFactory;
    private final String                              compilerFactoryId;
    private final boolean                             isJdk;
    @SuppressWarnings("unused") private final boolean isJanino;

    @Parameters(name = "CompilerFactory={0}") public static Collection<Object[]>
    compilerFactories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }

    public
    CompilerTest(ICompilerFactory compilerFactory) {

        this.compilerFactory = compilerFactory;

        this.compilerFactoryId = compilerFactory.getId();
        this.isJdk             = this.compilerFactoryId.equals("org.codehaus.commons.compiler.jdk");
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
            new File(CompilerTest.COMMONS_COMPILER_SRC     + "/org/codehaus/commons/compiler/samples/ExpressionDemo.java"),
            new File(CompilerTest.COMMONS_COMPILER_SRC     + "/org/codehaus/commons/compiler/util/resource/MapResourceCreator.java"),
            new File(CompilerTest.COMMONS_COMPILER_SRC     + "/org/codehaus/commons/compiler/util/resource/MapResourceFinder.java"),
        };

        Benchmark b = new Benchmark(true);

        // -------------------- PHASE 1

        SortedMap<String, byte[]> classFileMap1;
        {
            b.beginReporting("Compile " + this.compilerFactoryId + " from scratch");
            classFileMap1 = new TreeMap<>(
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
            SortedMap<String, byte[]> classFileMap2 = new TreeMap<>(
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
                ClassLoaders.BOOTCLASSPATH_CLASS_LOADER
            );

            // Load the ICompiler from that class loader.
            b.beginReporting(
                "Compile " + this.compilerFactoryId + " again, but this time using the classes generated by the first "
                + "compilation, a.k.a. \"compile Janino with itself\""
            );

            Object iCompiler = cl.loadClass(
                this.compilerFactory.getId() + ".Compiler"
            ).getDeclaredConstructor().newInstance();

            SortedMap<String, byte[]> classFileMap3 = new TreeMap<>(CompilerTest.compileJanino(
                sourceFiles,
                iCompiler,
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

        final Class<?> DirectoryResourceFinder_class = cl.loadClass("org.codehaus.commons.compiler.util.resource.DirectoryResourceFinder"); // SUPPRESS CHECKSTYLE LocalFinalVariableName:6
        final Class<?> MapResourceCreator_class      = cl.loadClass("org.codehaus.commons.compiler.util.resource.MapResourceCreator");
        final Class<?> MapResourceFinder_class       = cl.loadClass("org.codehaus.commons.compiler.util.resource.MapResourceFinder");
        final Class<?> MultiResourceFinder_class     = cl.loadClass("org.codehaus.commons.compiler.util.resource.MultiResourceFinder");
        final Class<?> ResourceCreator_class         = cl.loadClass("org.codehaus.commons.compiler.util.resource.ResourceCreator");
        final Class<?> ResourceFinder_class          = cl.loadClass("org.codehaus.commons.compiler.util.resource.ResourceFinder");

//        compiler.setSourceFinder(new MultiResourceFinder(new ResourceFinder[] {
//             new DirectoryResourceFinder(new File(CompilerTest.JANINO_SRC)),
//             new DirectoryResourceFinder(new File(CompilerTest.COMMONS_COMPILER_SRC)),
//             new DirectoryResourceFinder(new File(CompilerTest.COMMONS_COMPILER_JDK_SRC)),
//         }));
        {
            final Object[] rfs = CompilerTest.newArray(
                ResourceFinder_class,
                CompilerTest.newInstance1(DirectoryResourceFinder_class, File.class, new File(CompilerTest.JANINO_SRC)),
                CompilerTest.newInstance1(DirectoryResourceFinder_class, File.class, new File(CompilerTest.COMMONS_COMPILER_SRC)),
                CompilerTest.newInstance1(DirectoryResourceFinder_class, File.class, new File(CompilerTest.COMMONS_COMPILER_JDK_SRC))
            );
            CompilerTest.invoke1(
                compiler,                  // target
                "setSourceFinder",         // methodName
                ResourceFinder_class,      // parameterType
                CompilerTest.newInstance1( // argument
                    MultiResourceFinder_class, // clasS
                    rfs.getClass(),            // parameterType
                    rfs                        // arguments
                )
            );
        }

//        compiler.setClassPath(new File[0]);
        CompilerTest.invoke1(compiler, "setClassPath", new File[0].getClass(), new File[0]);

        final Map<String, byte[]> result = new HashMap<>();

//        compiler.setClassFileCreator(new MapResourceCreator(result));
        CompilerTest.invoke1(
            compiler,
            "setClassFileCreator",
            ResourceCreator_class,
            CompilerTest.newInstance1(MapResourceCreator_class, Map.class, result)
        );

        if (precompiledClasses != null) {

//            MapResourceFinder classFileFinder = new MapResourceFinder(precompiledClasses);
            Object classFileFinder = CompilerTest.newInstance1(MapResourceFinder_class, Map.class, precompiledClasses);

//            classFileFinder.setLastModified(System.currentTimeMillis());
            CompilerTest.invoke1(classFileFinder, "setLastModified", long.class, System.currentTimeMillis());

//            compiler.setClassFileFinder(classFileFinder);
            CompilerTest.invoke1(compiler, "setClassFileFinder", ResourceFinder_class, classFileFinder);
        } else {

//            compiler.setClassFileFinder(ResourceFinder.EMPTY_RESOURCE_FINDER);
            CompilerTest.invoke1(
                compiler,
                "setClassFileFinder",
                ResourceFinder_class,
                CompilerTest.getStaticField(ResourceFinder_class, "EMPTY_RESOURCE_FINDER")
            );
        }

        // compiler.setDebugLines(true);
        // compiler.setDebugSource(true);
        // compiler.setDebugVars(true);
//        invoke1(compiler, "setDebugLines",  boolean.class, true);
//        invoke1(compiler, "setDebugSource", boolean.class, true);
//        invoke1(compiler, "setDebugVars",   boolean.class, true);

//        compiler.compile(sourceFiles);
        CompilerTest.invoke1(compiler, "compile", File[].class, sourceFiles);

        return result;
    }

    private static Object
    getStaticField(Class<?> clasS, String fieldName) throws Exception {
        return clasS.getDeclaredField(fieldName).get(null);
    }

    /**
     * Creates and returns an object instance through a one-argument constructor.
     */
    private static Object
    newInstance1(
        Class<?> clasS,
        Class<?> parameterType,
        Object   argument
    ) throws Exception {
        return CompilerTest.newInstance(clasS, new Class[] { parameterType }, new Object[] { argument });
    }

    private static Object
    newInstance(
        Class<?>   clasS,
        Class<?>[] parameterTypes,
        Object[]   arguments
    ) throws Exception {
        assert arguments.length == parameterTypes.length;
        return clasS.getDeclaredConstructor(parameterTypes).newInstance(arguments);
    }

    private static void
    invoke1(
        Object   target,
        String   methodName,
        Class<?> parameterType,
        Object   argument
    ) throws Exception {
        CompilerTest.invoke(target, methodName, new Class[] { parameterType }, new Object[] { argument });
    }

    private static void
    invoke(
        Object     target,
        String     methodName,
        Class<?>[] parameterTypes,
        Object[]   arguments
    ) throws Exception {
        assert arguments.length == parameterTypes.length;
        target.getClass().getMethod(methodName, parameterTypes).invoke(target, arguments);
    }

    private static Object[]
    newArray(Class<?> elementClass, Object... initializers) {
        final Object[] result = (Object[]) Array.newInstance(elementClass, initializers.length);
        for (int i = 0; i < initializers.length; i++) {
            result[i] = initializers[i];
        }
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
                handleError(String message, @Nullable Location location) throws CompileException {
                    throw new CompileException(message, location);
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

        this.assertUncompilable("cannot.*\\bE\\b", sourceFinder);
    }

    @Test public void
    testErrorHandler() throws Exception {
        MapResourceFinder sourceFinder = new MapResourceFinder();

        sourceFinder.addResource("pkg/A.java", (
            ""
            + "package pkg;\n"
            + "public class A {\n"
            + "    void meth() {\n"
            + "        return 1;\n"
            + "    }\n"
            + "}\n"
        ));

        // Default error handling.
        this.assertUncompilable(
            (
                ""
                + "Method must not return a value"
                + "|"
                + "unexpected return value"
                + "|"
                + "compiler\\.err\\.cant\\.ret\\.val\\.from\\.meth\\.decl\\.void"
                + "|"
                + "cannot return a value from method whose result type is void"
            ),
            sourceFinder
        );

        // Error handler that throws a CompileException.
        {
            ICompiler compiler = this.compilerFactory.newCompiler();

            final int[] count = new int[1];
            compiler.setCompileErrorHandler(new ErrorHandler() {
                @Override public void handleError(String message, @Nullable Location location) throws CompileException {
                    count[0]++;
                    throw new CompileException(message, location);
                }
            });

            CompilerTest.assertUncompilable(
                (
                    ""
                    + "Method must not return a value"
                    + "|"
                    + "unexpected return value"
                    + "|"
                    + "cannot return a value from method whose result type is void"
                ),
                compiler,
                sourceFinder
            );
            Assert.assertEquals(1, count[0]);
        }

        // Error handler that does *not* throw a CompileException.
        {
            ICompiler compiler = this.compilerFactory.newCompiler();

            final int[] count = new int[1];
            compiler.setCompileErrorHandler(new ErrorHandler() {
                @Override public void handleError(String message, @Nullable Location location) { count[0]++; }
            });

            CompilerTest.assertUncompilable("Compilation failed with 1 errors|error.*while compiling", compiler, sourceFinder);
            Assert.assertEquals(1, count[0]);
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

        // Set up the compiler.
        ICompiler compiler = this.compilerFactory.newCompiler();
        compiler.setSourceFinder(sourceFinder);
        return CompilerTest.compile(compiler, sourceFinder);
    }

    private static Map<String, byte[]>
    compile(ICompiler compiler, MapResourceFinder sourceFinder) throws CompileException, IOException {
//        compiler.setIClassLoader(new ClassLoaderIClassLoader(CompilerTest.class.getClassLoader()));

        // Storage for generated bytecode.
        final Map<String, byte[]> classes = new HashMap<>();
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
        Map<String, byte[]> classes = new HashMap<>();
        compiler.setClassFileCreator(new MapResourceCreator(classes));

        // Now compile two units with different package declarations.
        compiler.compile(new Resource[] {
            new StringResource("pkg1/A.java", "package pkg1; public class A { public static int meth() { return pkg2.B.meth(); } }"),
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
    assertFind(final String regex, final String actual) {
        Assert.assertTrue(
            "Expected that \"" + actual + "\" contain match of regex \"" + regex + "\"",
            Pattern.compile(regex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(actual).find()
        );
    }

    private void
    assertUncompilable(String messageRegex, MapResourceFinder sourceFinder) throws IOException {
        try {
            this.compile(sourceFinder);
            Assert.fail("CompileException expected");
        } catch (CompileException ex) {
            CompilerTest.assertFind(messageRegex, ex.getMessage());
        }
    }

    private static void
    assertUncompilable(String messageRegex, ICompiler compiler, MapResourceFinder sourceFinder) throws IOException {
        try {
            CompilerTest.compile(compiler, sourceFinder);
            Assert.fail("CompileException expected");
        } catch (CompileException ex) {
            CompilerTest.assertFind(messageRegex, ex.getMessage());
        }
    }
}
