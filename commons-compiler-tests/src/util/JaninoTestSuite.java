
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

package util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Method;

import org.codehaus.commons.compiler.AbstractJavaSourceClassLoader;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.IScriptEvaluator;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.janino.JavaSourceClassLoader;

/**
 * A base class for JUnit 4 test cases that provides easy-to-use functionality to test JANINO.
 */
public
class JaninoTestSuite {

    /** The test is expected to throw a CompileException. */
    public static final CompileAndExecuteTest.Mode COMP = new CompileAndExecuteTest.Mode();

    /** The test is expected to compile successfully, but is not executed. */
    public static final CompileAndExecuteTest.Mode COOK = new CompileAndExecuteTest.Mode();

    /** The test is expected to compile and execute successfully. */
    public static final CompileAndExecuteTest.Mode EXEC = new CompileAndExecuteTest.Mode();

    /** The test is expected to compile and execute successfully, and return {@code true}. */
    public static final CompileAndExecuteTest.Mode TRUE = new CompileAndExecuteTest.Mode();

    /** The {@link ICompilerFactory} in effect for this test execution. */
    protected final ICompilerFactory compilerFactory;

    public
    JaninoTestSuite(ICompilerFactory compilerFactory) {
        this.compilerFactory = compilerFactory;
    }

    /**
     * Add a test case that scans, parses and compiles an expression, and verifies that it
     * evaluates to {@code true}.
     *
     * <table>
     *   <tr><th>{@code mode}</th><th>Meaning</th></tr>
     *   <tr><td>COMP</td><td>The test is expected to throw a CompileException</td></tr>
     *   <tr><td>COOK</td><td>The test is expected to compile successfully, but is not executed</td></tr>
     *   <tr><td>EXEC</td><td>The test is expected to compile and execute successfully</td></tr>
     *   <tr><td>TRUE</td><td>The test is expected to compile and execute successfully, and return {@code true}</tr>
     * </table>
     */
    protected void
    exp(ExpressionTest.Mode mode, String expression) throws Exception {
        ExpressionTest et = new ExpressionTest(mode, expression);
        et.runTest();
    }

    /**
     * Like {@link #exp(util.JaninoTestSuite.CompileAndExecuteTest.Mode, String)}, but with additional default
     * imports, e.g. '{@code java.util.*}'.
     */
    protected void
    exp(ExpressionTest.Mode mode, String expression, String[] defaultImports) throws Exception {
        ExpressionTest et = new ExpressionTest(mode, expression);
        et.setDefaultImports(defaultImports);
        et.runTest();
    }

    private
    class ExpressionTest extends CompileAndExecuteTest {

        private final String               expression;
        private final IExpressionEvaluator expressionEvaluator;

        public
        ExpressionTest(Mode mode, String expression) throws Exception {
            super(mode);
            this.expression          = expression;
            this.expressionEvaluator = JaninoTestSuite.this.compilerFactory.newExpressionEvaluator();

            this.expressionEvaluator.setExpressionType(mode == TRUE ? boolean.class : IExpressionEvaluator.ANY_TYPE);
        }

        public ExpressionTest
        setDefaultImports(String[] defaultImports) {
            this.expressionEvaluator.setDefaultImports(defaultImports);
            return this;
        }

        protected void
        compile() throws Exception {
            this.expressionEvaluator.cook(this.expression);
        }

        protected Object
        execute() throws Exception {
            return this.expressionEvaluator.evaluate(new Object[0]);
        }
    }

    /**
     * Add a test case that scans, parses, compiles and executes a Janino script, and verifies
     * that it returns {@code true}.
     *
     * @param mode <table>
     *   <tr><td>{@link #COMP}</td><td>The test is expected to throw a CompileException</tr>
     *   <tr><td>{@link #COOK}</td><td>The test is expected to compile successfully, but is not executed</tr>
     *   <tr><td>{@link #EXEC}</td><td>The test is expected to compile and execute successfully</tr>
     *   <tr>
     *     <td>{@link #TRUE}</td>
     *     <td>The test is expected to compile and execute successfully, and return {@code true}</td>
     *   </tr>
     * </table>
     */
    protected void
    scr(ScriptTest.Mode mode, String script) throws Exception {
        ScriptTest st = new ScriptTest(mode, script);
        st.runTest();
    }

    /**
     * Add a test case that scans, parses, compiles and executes a Janino script, and verifies
     * that it returns {@code true}.
     *
     * @param mode <table>
     *   <tr><td>{@link #COMP}</td><td>The test is expected to throw a CompileException</tr>
     *   <tr><td>{@link #COOK}</td><td>The test is expected to compile successfully, but is not executed</tr>
     *   <tr><td>{@link #EXEC}</td><td>The test is expected to compile and execute successfully</tr>
     *   <tr>
     *     <td>{@link #TRUE}</td>
     *     <td>The test is expected to compile and execute successfully, and return {@code true}</td>
     *   </tr>
     * </table>
     * @param defaultImports E.g. '<code> new String[] { "java.util.List" }</code>'
     */
    protected void
    scr(ScriptTest.Mode mode, String script, String[] defaultImports) throws Exception {
        ScriptTest st = new ScriptTest(mode, script);
        st.setDefaultImports(defaultImports);
        st.runTest();
    }

    private
    class ScriptTest extends CompileAndExecuteTest {

        private final String           script;
        private final IScriptEvaluator scriptEvaluator;

        public
        ScriptTest(Mode mode, String script) throws Exception {
            super(mode);
            this.script          = script;
            this.scriptEvaluator = JaninoTestSuite.this.compilerFactory.newScriptEvaluator();

            this.scriptEvaluator.setReturnType(mode == TRUE ? boolean.class : void.class);
        }

        public ScriptTest
        setDefaultImports(String[] defaultImports) {
            this.scriptEvaluator.setDefaultImports(defaultImports);
            return this;
        }

        protected void
        compile() throws Exception {
            this.scriptEvaluator.cook(this.script);
        }

        protected Object
        execute() throws Exception {
            return this.scriptEvaluator.evaluate(new Object[0]);
        }
    }

    /**
     * Add a test case that scans, parses and compiles a class body, invokes its {@code public static boolean main()}
     * method and verifies that it returns {@code true}.
     *
     * <table>
     *   <tr><th>{@code mode}</th><th>Meaning</th></tr>
     *   <tr><td>COMP</td><td>The test is expected to throw a CompileException</tr>
     *   <tr><td>COOK</td><td>The test is expected to compile successfully, but is not executed</tr>
     *   <tr><td>EXEC</td><td>The test is expected to compile and execute successfully</tr>
     *   <tr><td>TRUE</td><td>The test is expected to compile and execute successfully, and return {@code true}</tr>
     * </table>
     */
    protected void
    clb(ClassBodyTest.Mode mode, String classBody) throws Exception {
        ClassBodyTest cbt = new ClassBodyTest(mode, classBody);
        cbt.runTest();
    }

    /**
     * Like {@link #clb(util.JaninoTestSuite.CompileAndExecuteTest.Mode, String)}, but with additional default
     * imports, e.g. '{@code java.util.*}'.
     */
    protected void
    clb(ClassBodyTest.Mode mode, String classBody, String[] defaultImports) throws Exception {
        ClassBodyTest cbt = new ClassBodyTest(mode, classBody);
        cbt.setDefaultImports(defaultImports);
        cbt.runTest();
    }

    private
    class ClassBodyTest extends CompileAndExecuteTest {
        private final String              classBody;
        private final IClassBodyEvaluator classBodyEvaluator;

        public
        ClassBodyTest(Mode mode, String classBody) throws Exception {
            super(mode);
            this.classBody          = classBody;
            this.classBodyEvaluator = JaninoTestSuite.this.compilerFactory.newClassBodyEvaluator();
        }
        public ClassBodyTest
        setDefaultImports(String[] defaultImports) {
            this.classBodyEvaluator.setDefaultImports(defaultImports);
            return this;
        }

        protected void
        compile() throws Exception {
            this.classBodyEvaluator.cook(this.classBody);
        }

        protected Object
        execute() throws Exception {
            @SuppressWarnings("unchecked") Method method = this.classBodyEvaluator.getClazz().getMethod(
                "main",
                new Class[0]
            );
            return method.invoke(null, new Object[0]);
        }
    }

    /**
     * Add a test case that scans, parses and compiles a compilation unit, then calls the {@code public static boolean
     * test()} method of the named class, and verifies that it returns {@code true}.
     *
     * <table>
     *   <tr><th>{@code mode}</th><th>Meaning</th></tr>
     *   <tr><td>COMP</td><td>The test is expected to throw a CompileException</tr>
     *   <tr><td>COOK</td><td>The test is expected to compile successfully, but is not executed</tr>
     *   <tr><td>EXEC</td><td>The test is expected to compile and execute successfully</tr>
     *   <tr><td>TRUE</td><td>The test is expected to compile and execute successfully, and return {@code true}</tr>
     * </table>
     * @param className The name of the class with the {@code public static boolean test()} method
     */
    protected void
    sim(SimpleCompilerTest.Mode mode, String compilationUnit, String className) throws Exception {
        SimpleCompilerTest sct = new SimpleCompilerTest(mode, compilationUnit, className);
        sct.runTest();
    }

    private
    class SimpleCompilerTest extends CompileAndExecuteTest {

        private final String          compilationUnit;
        private final String          className;
        private final ISimpleCompiler simpleCompiler;

        public
        SimpleCompilerTest(Mode mode, String compilationUnit, String className) throws Exception {
            super(mode);
            this.compilationUnit = compilationUnit;
            this.className       = className;
            this.simpleCompiler  = JaninoTestSuite.this.compilerFactory.newSimpleCompiler();
        }

        protected void
        compile() throws Exception {
            this.simpleCompiler.cook(this.compilationUnit);
        }

        protected Object
        execute() throws Exception {
            return (
                this.simpleCompiler.getClassLoader()
                .loadClass(this.className)
                .getMethod("test", new Class[0])
                .invoke(null, new Object[0])
            );
        }
    }

    /**
     * Create and return a test case that sets up a {@link JavaSourceClassLoader} that reads sources from a given
     * directory, and then loads the named class.
     *
     * @param className The name of the class to be loaded from the {@link JavaSourceClassLoader}
     */
    protected void
    jscl(String testCaseName, final File sourceDirectory, final String className) throws Exception {
        AbstractJavaSourceClassLoader loader = this.compilerFactory.newJavaSourceClassLoader();
        loader.setSourcePath(new File[] { sourceDirectory });
        loader.loadClass(className);
    }

    /**
     * A test case that calls its abstract methods {@link #compile()}, then {@link #execute()}, and
     * verifies that they throw exceptions and return results as expected.
     */
    abstract static
    class CompileAndExecuteTest {

        private final Mode mode;

        public static final class Mode { private Mode() {} }

        public
        CompileAndExecuteTest(Mode mode) {
            this.mode = mode;
        }

        /** @see CompileAndExecuteTest */
        protected abstract void compile() throws Exception;

        /** @see CompileAndExecuteTest */
        protected abstract Object execute() throws Exception;

        /**
         * Invoke {@link #compile()}, then {@link #execute()}, and verify that they throw exceptions and return
         * results as expected.
         */
        protected void
        runTest() throws Exception {
            if (this.mode == COMP) {
                try {
                    this.compile();
                } catch (CompileException ex) {
                    return;
                }
                fail("Should have issued an error, but compiled successfully");
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
                assertNotNull("Test result not NULL", result);
                assertSame("Test return type is BOOLEAN", Boolean.class, result.getClass());
                assertEquals("Test result is TRUE", true, ((Boolean) result).booleanValue());
            } else
            {
                fail("Invalid mode \"" + this.mode + "\"");
            }
        }
    }
}
