
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

package util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import junit.framework.*;

import org.codehaus.janino.*;
import org.codehaus.janino.util.resource.MapResourceFinder;

public class JaninoTestSuite extends StructuredTestSuite {

    /** The test is expected to throw a ScanException */
    public static final CompileAndExecuteTest.Mode SCAN = new CompileAndExecuteTest.Mode();
    /** The test is expected to throw a ParseException */
    public static final CompileAndExecuteTest.Mode PARS = new CompileAndExecuteTest.Mode();
    /** The test is expected to throw a CompileException */
    public static final CompileAndExecuteTest.Mode COMP = new CompileAndExecuteTest.Mode();
    /** The test is expected to compile successfully, but is not executed */
    public static final CompileAndExecuteTest.Mode COOK = new CompileAndExecuteTest.Mode();
    /** The test is expected to compile and execute successfully */
    public static final CompileAndExecuteTest.Mode EXEC = new CompileAndExecuteTest.Mode();
    /** The test is expected to compile and execute successfully, and return <code>true</code> */
    public static final CompileAndExecuteTest.Mode TRUE = new CompileAndExecuteTest.Mode();

    /** The string is expected to contain exactly one scannable token. */
    public static final ScannerTest.Mode           VALI = new ScannerTest.Mode();
    /** Scanning the string is expected to throw a {@link org.codehaus.janino.Scanner.ScanException}. */
    public static final ScannerTest.Mode           INVA = new ScannerTest.Mode();

    public JaninoTestSuite(String name) {
        super(name);
    }

    /**
     * Add a test case that scans the given {@link String} into Java<sup>TM</sup> tokens.
     *
     * <table>
     *   <tr><th><code>mode</code></th><th>Meaning</th></tr>
     *   <tr><td>VALI</td><td>The string is expected to contain exactly one scannable token.</tr>
     *   <tr><td>INVA</td><td>Scanning the string is expected to throw a {@link org.codehaus.janino.Scanner.ScanException}.
     * </table>
     *
     * @param name The name of the JUnit test case
     */
    protected ScannerTest sca(final ScannerTest.Mode mode, String name, final String s) {
        ScannerTest tc = new ScannerTest(mode, name, s);
        addTest(tc);
        return tc;
    }
    protected static final class ScannerTest extends TestCase {
        private final Mode   mode;
        private final String s;

        protected static class Mode { private Mode() {} }

        public ScannerTest(Mode mode, String name, String s) {
            super(name);
            this.mode = mode;
            this.s    = s;
        }

        protected void runTest() throws Exception {
            if (this.mode == INVA) {
                try { new Scanner(null, new StringReader(this.s)).peek(); } catch (Scanner.ScanException ex) { return; }
                fail("Should have thrown ScanException");
            } else
            if (this.mode == VALI) {
                Scanner sc = new Scanner(null, new StringReader(this.s));
                sc.read();
                assertTrue("Only one token", sc.read().isEOF());
            } else
            {
                fail("Invalid mode \"" + this.mode + "\"");
            }
        }
    };

    /**
     * Add a test case that scans, parses and compiles an expression, and verifies that it
     * evaluates to <code>true</code>.
     *
     * <table>
     *   <tr><th><code>mode</code></th><th>Meaning</th></tr>
     *   <tr><td>SCAN</td><td>The test is expected to throw a ScanException</tr>
     *   <tr><td>PARS</td><td>The test is expected to throw a ParseException</tr>
     *   <tr><td>COMP</td><td>The test is expected to throw a CompileException</tr>
     *   <tr><td>COOK</td><td>The test is expected to compile successfully, but is not executed</tr>
     *   <tr><td>EXEC</td><td>The test is expected to compile and execute successfully</tr>
     *   <tr><td>TRUE</td><td>The test is expected to compile and execute successfully, and return <code>true</code></tr>
     * </table>
     *
     * @param name The name of the JUnit test case
     */
    protected ExpressionTest exp(ExpressionTest.Mode mode, String name, String expression) {
        ExpressionTest et = new ExpressionTest(mode, name, expression);
        addTest(et);
        return et;
    }
    static protected class ExpressionTest extends CompileAndExecuteTest {
        private final String              expression;
        private final ExpressionEvaluator expressionEvaluator;
    
        public ExpressionTest(Mode mode, String name, String expression) {
            super(name, mode);
            this.expression          = expression;
            this.expressionEvaluator = new ExpressionEvaluator();

            this.expressionEvaluator.setExpressionType(mode == TRUE ? boolean.class : ExpressionEvaluator.ANY_TYPE);
        }
        public ExpressionTest setDefaultImports(String[] defaultImports) { this.expressionEvaluator.setDefaultImports(defaultImports); return this; }
    
        protected void compile() throws Exception {
            this.expressionEvaluator.cook(this.expression);
        }

        protected Object execute() throws Exception {
            return this.expressionEvaluator.evaluate(new Object[0]);
        }
    }
    
    /**
     * Add a test case that scans, parses, compiles and executes a Janino script, and verifies
     * that it returns <code>true</code>.
     *
     * <table>
     *   <tr><th><code>mode</code></th><th>Meaning</th></tr>
     *   <tr><td>SCAN</td><td>The test is expected to throw a ScanException</tr>
     *   <tr><td>PARS</td><td>The test is expected to throw a ParseException</tr>
     *   <tr><td>COMP</td><td>The test is expected to throw a CompileException</tr>
     *   <tr><td>COOK</td><td>The test is expected to compile successfully, but is not executed</tr>
     *   <tr><td>EXEC</td><td>The test is expected to compile and execute successfully</tr>
     *   <tr><td>TRUE</td><td>The test is expected to compile and execute successfully, and return <code>true</code></tr>
     * </table>
     *
     * @param name The name of the JUnit test case
     */
    protected ScriptTest scr(ScriptTest.Mode mode, String name, String script) {
        ScriptTest st = new ScriptTest(mode, name, script);
        addTest(st);
        return st;
    }
    static protected class ScriptTest extends CompileAndExecuteTest {
        private final String          script;
        private final ScriptEvaluator scriptEvaluator;
    
        public ScriptTest(Mode mode, String name, String script) {
            super(name, mode);
            this.script          = script;
            this.scriptEvaluator = new ScriptEvaluator();

            this.scriptEvaluator.setReturnType(mode == TRUE ? boolean.class : void.class);
        }
        public ScriptTest setDefaultImports(String[] defaultImports) { this.scriptEvaluator.setDefaultImports(defaultImports); return this; }

        protected void compile() throws Exception {
            this.scriptEvaluator.cook(this.script);
        }

        protected Object execute() throws Exception {
            return this.scriptEvaluator.evaluate(new Object[0]);
        }
    }
    
    /**
     * Add a test case that scans, parses and compiles a class body, invokes its
     * <code>public static boolean main()</code> method and verifies that it returns
     * <code>true</code>.
     *
     * <table>
     *   <tr><th><code>mode</code></th><th>Meaning</th></tr>
     *   <tr><td>SCAN</td><td>The test is expected to throw a ScanException</tr>
     *   <tr><td>PARS</td><td>The test is expected to throw a ParseException</tr>
     *   <tr><td>COMP</td><td>The test is expected to throw a CompileException</tr>
     *   <tr><td>COOK</td><td>The test is expected to compile successfully, but is not executed</tr>
     *   <tr><td>EXEC</td><td>The test is expected to compile and execute successfully</tr>
     *   <tr><td>TRUE</td><td>The test is expected to compile and execute successfully, and return <code>true</code></tr>
     * </table>
     *
     * @param name The name of the JUnit test case
     */
    protected ClassBodyTest clb(ClassBodyTest.Mode mode, String name, String classBody) {
        ClassBodyTest cbt = new ClassBodyTest(name, mode, classBody);
        addTest(cbt);
        return cbt;
    }
    static protected class ClassBodyTest extends CompileAndExecuteTest {
        private final String             classBody;
        private final ClassBodyEvaluator classBodyEvaluator;

        public ClassBodyTest(String name, Mode mode, String classBody) {
            super(name, mode);
            this.classBody          = classBody;
            this.classBodyEvaluator = new ClassBodyEvaluator();
        }
        public ClassBodyTest setDefaultImports(String[] defaultImports) { this.classBodyEvaluator.setDefaultImports(defaultImports); return this; }

        protected void compile() throws Exception {
            this.classBodyEvaluator.cook(this.classBody);
        }

        protected Object execute() throws Exception {
            return this.classBodyEvaluator.getClazz().getMethod("main", new Class[0]).invoke(null, new Object[0]);
        }
    }

    /**
     * Add a test case that scans, parses and compiles a compilation unit, then calls the
     * <code>public static boolean test()</code> method of the named class, and verifies that it
     * returns <code>true</code>.
     *
     * <table>
     *   <tr><th><code>mode</code></th><th>Meaning</th></tr>
     *   <tr><td>SCAN</td><td>The test is expected to throw a ScanException</tr>
     *   <tr><td>PARS</td><td>The test is expected to throw a ParseException</tr>
     *   <tr><td>COMP</td><td>The test is expected to throw a CompileException</tr>
     *   <tr><td>COOK</td><td>The test is expected to compile successfully, but is not executed</tr>
     *   <tr><td>EXEC</td><td>The test is expected to compile and execute successfully</tr>
     *   <tr><td>TRUE</td><td>The test is expected to compile and execute successfully, and return <code>true</code></tr>
     * </table>
     *
     * @param name The name of the JUnit test case
     * @param className The name of the class with the <code>public static boolean test()</code> method
     */
    protected SimpleCompilerTest sim(SimpleCompilerTest.Mode mode, String name, String compilationUnit, String className) {
        SimpleCompilerTest sct = new SimpleCompilerTest(name, mode, compilationUnit, className);
        addTest(sct);
        return sct;
    }
    static protected class SimpleCompilerTest extends CompileAndExecuteTest {
        private final String         compilationUnit;
        private final String         className;
        private final SimpleCompiler simpleCompiler;

        public SimpleCompilerTest(String name, Mode mode, String compilationUnit, String className) {
            super(name, mode);
            this.compilationUnit = compilationUnit;
            this.className       = className;
            this.simpleCompiler = new SimpleCompiler();
        }

        protected void compile() throws Exception {
            this.simpleCompiler.cook(this.compilationUnit);
        }

        protected Object execute() throws Exception {
            return (
                this.simpleCompiler.getClassLoader()
                .loadClass(this.className)
                .getMethod("test", new Class[0])
                .invoke(null, new Object[0])
            );
        }
    }

    /**
     * Create and return a test case that sets up a {@link JavaSourceClassLoader} that accesses
     * resources given by name and contents, and then loads the named class.
     *
     * @param testCaseName The name of the test case
     * @param resourceNamesAndContents Pairs of resource name and resource contents
     * @param className The name of the class to be loaded from the {@link JavaSourceClassLoader}
     */
    protected void jscl(String testCaseName, final String[] resourceNamesAndContents, final String className) {
        TestCase testCase = new TestCase(testCaseName) {
            protected void runTest() throws Throwable {
                Map sources = new HashMap();
                for (int i = 0; i < resourceNamesAndContents.length; i += 2) {
                    sources.put(resourceNamesAndContents[i], resourceNamesAndContents[i + 1].getBytes());
                }
                ClassLoader loader = new JavaSourceClassLoader(
                    this.getClass().getClassLoader(),
                    new MapResourceFinder(sources),
                    null,
                    DebuggingInformation.NONE
                );
    
                loader.loadClass(className);
            }
        };
        addTest(testCase);
    }

    /**
     * A test case that calls its abstract methods {@link #compile()}, then {@link #execute()}, and
     * verifies that they throw exceptions and return results as expected.
     */
    static abstract class CompileAndExecuteTest extends TestCase {
        protected final Mode mode;
    
        public static class Mode { private Mode() {}}

        public CompileAndExecuteTest(String name, Mode mode) {
            super(name);
            // Notice: JUnit 3.8.1 gets confused if the name contains "(" and/or ",".
            if (name.indexOf('(') != -1) throw new RuntimeException("Parentheses in test name not permitted");
            if (name.indexOf(',') != -1) throw new RuntimeException("Comma in test name not permitted");
            this.mode = mode;
        }

        protected abstract void   compile() throws Exception;
        protected abstract Object execute() throws Exception;

        /**
         * Invoke {@link #compile()}, then {@link #execute()}, and verify that they throw exceptions and return
         * results as expected.
         */
        protected void runTest() throws Exception {
            if (this.mode == SCAN) {
                try { this.compile(); } catch (Scanner.ScanException ex) { return; }
                fail("Should have thrown ScanException");
            } else
            if (this.mode == PARS) {
                try { this.compile(); } catch (Parser.ParseException ex) { return; }
                fail("Should have thrown ParseException");
            } else
            if (this.mode == COMP) {
                try { this.compile(); } catch (CompileException ex) { return; }
                fail("Should have thrown CompileException");
            } else
            if (this.mode == COOK) {
                this.compile();
            } else
            if (this.mode == EXEC) {
                this.compile();
                this.execute();
            } else
            if (this.mode == TRUE) {
                this.compile();
                Object result = this.execute();
                assertNotNull("Test result", result);
                assertSame("Test return type", Boolean.class, result.getClass());
                assertEquals("Test result", true, ((Boolean) result).booleanValue());
            } else
            {
                fail("Invalid mode \"" + this.mode + "\"");
            }
        }
    }
}
