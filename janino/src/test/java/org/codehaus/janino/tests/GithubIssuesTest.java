
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2017 Arno Unkrig. All rights reserved.
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

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.util.reflect.ByteArrayClassLoader;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.MapResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.ExpressionEvaluator;
import org.codehaus.janino.IClassLoader;
import org.codehaus.janino.Java;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.SimpleCompiler;
import org.codehaus.janino.UnitCompiler;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.DeepCopier;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests that reproduce <a href="https://github.com/janino-compiler/janino/issues">the issues reported on GITHUB</a>.
 */
public
class GithubIssuesTest {

    /**
     * @see <a href="https://github.com/janino-compiler/janino/issues/19">GITHUB issue #19: Get bytecode after
     *      compile</a>
     */
    @Test public void
    testCompileToBytecode() throws CompileException {

        // Set up an ExpressionCompiler and cook the expression.
        ExpressionEvaluator ee = new ExpressionEvaluator();
        ee.setExpressionType(int.class);

        ee.cook("7");

        // Retrieve the generated bytecode from the ExpressionCompiler. The result is a map from class name
        // to the class's bytecode.
        Map<String, byte[]> result = ee.getBytecodes();
        Assert.assertNotNull(result);

        // verify that exactly _one_ class was generated.
        Assert.assertEquals(1, result.size());

        // Verify the class's name.
        byte[] ba = result.get(IClassBodyEvaluator.DEFAULT_CLASS_NAME);
        Assert.assertNotNull(ba);

        // Verify that the generated bytecode looks "reasonable", i.e. starts with the charcteristic
        // "magic bytes" and has an approximate size.
        Assert.assertArrayEquals(
            new byte[] { (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe },
            Arrays.copyOf(ba, 4)
        );
        Assert.assertTrue(Integer.toString(ba.length), ba.length > 150);
        Assert.assertTrue(Integer.toString(ba.length), ba.length < 300);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" }) @Test public void
    testExpressionCompilerMethods() throws CompileException, IOException, InvocationTargetException {

        Class<Comparable> interfaceToImplement = Comparable.class;
        String[]          parameterNames       = { "o" };
        Class<?>[]        parameterTypes       = { Object.class };
        String            e                    = "7";
        final Object[]    arguments            = { "" };

        CASES: for (int i = 0;; i++) {

            ExpressionEvaluator ee = new ExpressionEvaluator();

//            try {
                switch (i) {

//                 All these invocations are expected to throw an IllegalStateException.
                // SUPPRESS CHECKSTYLE LineLength:8
                case 0: Assert.assertEquals(7, ee.createFastEvaluator(new StringReader(e), interfaceToImplement, parameterNames).compareTo("foo"));                    break;
                case 1: Assert.assertEquals(7, ee.createFastEvaluator(new Scanner(null, new StringReader(e)), interfaceToImplement, parameterNames).compareTo("foo")); break;
                case 2: Assert.assertEquals(7, ee.createFastEvaluator(e, interfaceToImplement, parameterNames).compareTo("foo"));                                      break;
//                case 3: ee.setExpressionType(int.class); ee.cook(e); Assert.assertEquals(7, ee.evaluate(arguments));                                                                               break;
//                case 4: ee.setExpressionType(int.class); ee.cook(e); Assert.assertEquals(7, ee.evaluate(0, arguments));                                                                            break;
//                case 5: ee.setExpressionType(int.class); ee.cook(e); Assert.assertNotNull(ee.getClazz());                                                                                        break;
//                case 6: ee.setExpressionType(int.class); ee.cook(e); Assert.assertNotNull(ee.getMethod());                                                                                       break;
//                case 7: ee.setExpressionType(int.class); ee.cook(e); Assert.assertNotNull(ee.getMethod(0));                                                                                      break;

                case /*8*/3: break CASES;

                default: continue;
                }
//                Assert.fail("Exception expected (case " + i + ")");
//            } catch (IllegalStateException ise) {
//                ;
//            }
        }

        {
            ExpressionEvaluator ee = new ExpressionEvaluator();
            ee.setExpressionType(int.class);
            ee.setParameters(parameterNames, parameterTypes);
            ee.cook(e);

            Assert.assertEquals(7, ee.evaluate(arguments));
            Assert.assertEquals(7, ee.evaluate(0, arguments));
            Assert.assertNotNull(ee.getClazz());
            Assert.assertNotNull(ee.getMethod());
            Assert.assertNotNull(ee.getMethod(0));
        }
    }

    @Test public void
    testIssue91() throws Exception {

        // Parse the framework code.
        CompilationUnit cu = (CompilationUnit) new Parser(new Scanner(
            "user expression", // This will appear in stack traces as "file name".
            new StringReader(
                ""
                + "package com.acme.framework;\n"
                + "\n"
                + "public\n"
                + "class Framework {\n"
                + "\n"
                + "    public static void\n"
                + "    frameworkMethod() {\n"
                + "        System.out.println(\"frameworkMethod()\");\n"
                + "    }\n"
                + "\n"
                + "    public static void\n"
                + "    userMethod() {\n"
                + "        System.out.println(77);\n" // <= "77" will be replaced with the user expression!
                + "    }\n"
                + "}\n"
            )
        )).parseAbstractCompilationUnit();

        // Parse the "user expression".
        final Rvalue userExpression = (
            new Parser(new Scanner(
                null,
                new StringReader( // Line numbers will appear in stack traces.
                    ""
                    + "\n"
                    + "\n"
                    + "java.nio.charset.Charset.forName(\"kkk\")\n" // <= Causes an UnsupportedCharsetException
                )
            ))
            .parseExpression()
            .toRvalueOrCompileException()
        );

        // Merge the framework code with the user expression.
        cu = new DeepCopier() {

            @Override public Rvalue
            copyIntegerLiteral(IntegerLiteral subject) throws CompileException {
                if ("77".equals(subject.value)) return userExpression;
                return super.copyIntegerLiteral(subject);
            }
        }.copyCompilationUnit(cu);

        // Compile the code into a ClassLoader.
        SimpleCompiler sc = new SimpleCompiler();
        sc.setDebuggingInformation(true, true, true);
        sc.cook(cu);
        ClassLoader cl = sc.getClassLoader();

        // Find the generated class by name.
        Class<?> c = cl.loadClass("com.acme.framework.Framework");

        // Invoke the class's methods.
        c.getMethod("frameworkMethod").invoke(null);
        try {
            c.getMethod("userMethod").invoke(null);
            Assert.fail("InvocationTargetException expected");
        } catch (InvocationTargetException ite) {
            Throwable te = ite.getTargetException();
            Assert.assertEquals(UnsupportedCharsetException.class, te.getClass());
            StackTraceElement top = te.getStackTrace()[1];
            Assert.assertEquals("userMethod", top.getMethodName());
            Assert.assertEquals("user expression", top.getFileName());
            Assert.assertEquals(3, top.getLineNumber());
        }
    }

    // This is currently failing
    // https://github.com/codehaus/janino/issues/4
    @Ignore
    @Test public void
    testReferenceQualifiedSuper() throws Exception {
        GithubIssuesTest.doCompile(true, true, false, GithubIssuesTest.RESOURCE_DIR + "/a/Test.java");
    }
    private static final String RESOURCE_DIR = "../commons-compiler-tests/src/test/resources";

    @Test public void
    testIssue102() throws Exception {
        CompileUnit unit1 = new CompileUnit("demo.pkg1", "A$$1", (
            ""
            + "package demo.pkg1;\n"
            + "import demo.pkg2.*;\n"
            + "public class A$$1 {\n"
            + "    public static String main() { return B$1.hello(); }\n"
            + "    public static String hello() { return \"HELLO\"; }\n"
            + "}"
        ));
        CompileUnit unit2 = new CompileUnit("demo.pkg2", "B$1", (
            ""
            + "package demo.pkg2;\n"
            + "import demo.pkg1.*;\n"
            + "public class B$1 {\n"
            + "    public static String hello() { return \"HELLO\" /*A$$1.hello()*/; }\n"
            + "}"
        ));

        ClassLoader
        classLoader = this.compile(Thread.currentThread().getContextClassLoader(), unit1, unit2);

        Assert.assertEquals(
            "HELLO",
            classLoader.loadClass("demo.pkg1.A$$1").getMethod("main").invoke(null)
        );
    }

    public ClassLoader
    compile(ClassLoader parentClassLoader, CompileUnit... compileUnits) {

        MapResourceFinder sourceFinder = new MapResourceFinder();
        for (CompileUnit unit : compileUnits) {
            String stubFileName = unit.pkg.replace(".", "/") + "/" + unit.mainClassName + ".java";
            sourceFinder.addResource(stubFileName, unit.getCode());
        }

        // Storage for generated bytecode
        final Map<String, byte[]> classes = new HashMap<String, byte[]>();

        // Set up the compiler.
        org.codehaus.janino.Compiler compiler = new org.codehaus.janino.Compiler();
        compiler.setSourceFinder(sourceFinder);
        compiler.setIClassLoader(new ClassLoaderIClassLoader(parentClassLoader));
        compiler.setClassFileCreator(new MapResourceCreator(classes));
        compiler.setClassFileFinder(new MapResourceFinder(classes));

        // Compile all sources
        try {
            compiler.compile(sourceFinder.resources().toArray(new Resource[0]));
        } catch (CompileException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Set up a class loader that finds and defined the generated classes.
        return new ByteArrayClassLoader(classes, parentClassLoader);
    }

    interface Supplier<T> { T get(); }

    public
    class CompileUnit {
        String                             pkg;
        String                             mainClassName;
        @Nullable private String           code;
        @Nullable private Supplier<String> genCodeFunc;

        public
        CompileUnit(String pkg, String mainClassName, String code) {
            this.pkg           = pkg;
            this.mainClassName = mainClassName;
            this.code          = code;
        }

        public
        CompileUnit(String pkg, String mainClassName, Supplier<String> genCodeFunc) {
            this.pkg           = pkg;
            this.mainClassName = mainClassName;
            this.genCodeFunc   = genCodeFunc;
        }

        public String
        getCode() {
            if (this.code != null) return this.code;

//            Preconditions.checkNotNull(this.genCodeFunc);
            assert this.genCodeFunc != null;
            this.code = this.genCodeFunc.get();
            return this.code;
        }

        @Override public String
        toString() { return "CompileUnit{ pkg=" + this.pkg + ", mainClassName='" + this.mainClassName + " }"; }
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
        final IClassLoader                  cl   = new ClassLoaderIClassLoader(GithubIssuesTest.class.getClassLoader());
        List<ClassFile>                     cfs  = new LinkedList<ClassFile>();
        for (String fileName : fileNames) {

            FileReader r = new FileReader(fileName);
            try {
                Java.AbstractCompilationUnit acu = new Parser(new Scanner(fileName, r)).parseAbstractCompilationUnit();
                acus.add(acu);

                // Compile them.
                cfs.addAll(Arrays.asList(new UnitCompiler(acu, cl).compileUnit(debugSource, debugLines, debugVars)));
            } finally {
                try { r.close(); } catch (Exception e) {}
            }
        }
        return cfs;
    }

}
