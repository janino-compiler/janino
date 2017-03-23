
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

package util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;

import org.codehaus.commons.compiler.AbstractJavaSourceClassLoader;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.IScriptEvaluator;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.nullanalysis.Nullable;
import org.junit.Assert;

/**
 * A base class for JUnit 4 test cases that provides easy-to-use functionality to test JANINO.
 */
public
class CommonsCompilerTestSuite {

    /**
     * The {@link ICompilerFactory} in effect for this test execution.
     */
    protected final ICompilerFactory compilerFactory;

    public
    CommonsCompilerTestSuite(ICompilerFactory compilerFactory) {
        this.compilerFactory = compilerFactory;
    }

    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Asserts that cooking the given <var>expression</var>, when cooked, issues an error.
     */
    protected void
    assertExpressionUncookable(String expression) throws Exception {
        new ExpressionTest(expression).assertUncookable();
    }

    /**
     * Asserts that cooking the given <var>expression</var>, when cooked, issues an error, and the error message
     * contains the <var>messageInfix</var>.
     */
    protected void
    assertExpressionUncookable(String expression, String messageInfix) throws Exception {
        new ExpressionTest(expression).assertUncookable(messageInfix);
    }

    /**
     * Asserts that cooking the given <var>expression</var>, when cooked, issues an error, and the error message
     * contains a match for <var>messageRegex</var>.
     */
    protected void
    assertExpressionUncookable(String expression, Pattern messageRegex) throws Exception {
        new ExpressionTest(expression).assertUncookable(messageRegex);
    }

    /**
     * Asserts that the given <var>expression</var> can be cooked without errors and warnings.
     */
    protected void
    assertExpressionCookable(String expression) throws Exception {
        new ExpressionTest(expression).assertCookable();
    }

    /**
     * Asserts that the given <var>expression</var> can be cooked and evaluated. (Its value is ignored.)
     */
    protected void
    assertExpressionEvaluatable(String expression) throws Exception {
        new ExpressionTest(expression).assertExecutable();
    }

    /**
     * Asserts that the given <var>expression</var> is cookable and evaluates to TRUE.
     */
    protected void
    assertExpressionEvaluatesTrue(String expression) throws Exception {
        new ExpressionTest(expression).assertResultTrue();
    }

    public
    class ExpressionTest extends CompileAndExecuteTest {

        private final String                 expression;
        protected final IExpressionEvaluator expressionEvaluator;

        public
        ExpressionTest(String expression) throws Exception {
            this.expression          = expression;
            this.expressionEvaluator = CommonsCompilerTestSuite.this.compilerFactory.newExpressionEvaluator();
        }

        @Override protected void
        cook() throws Exception { this.expressionEvaluator.cook(this.expression); }

        @Override @Nullable protected Object
        execute() throws Exception {
            try {
                return this.expressionEvaluator.evaluate(new Object[0]);
            } catch (InvocationTargetException ite) {
                Throwable te = ite.getTargetException();
                throw te instanceof Exception ? (Exception) te : ite;
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Asserts that cooking the given <var>script</var> issues an error.
     */
    protected void
    assertScriptUncookable(String script) throws Exception {
        new ScriptTest(script).assertUncookable();
    }

    /**
     * Asserts that cooking the given <var>script</var> issues an error, and the error message contains the
     * <var>messageInfix</var>.
     */
    protected void
    assertScriptUncookable(String script, String messageInfix) throws Exception {
        new ScriptTest(script).assertUncookable(messageInfix);
    }

    /**
     * Asserts that cooking the given <var>script</var> issues an error, and the error message contains a match for
     * <var>messageRegex</var>.
     */
    protected void
    assertScriptUncookable(String script, Pattern messageRegex) throws Exception {
        new ScriptTest(script).assertUncookable(messageRegex);
    }

    /**
     * Asserts that the given <var>script</var> can be cooked without errors and warnings.
     */
    protected void
    assertScriptCookable(String script) throws Exception {
        new ScriptTest(script).assertCookable();
    }

    /**
     * Asserts that the given <var>script</var> can be cooked and executed. The return type of the scipt is {@code
     * void}.
     */
    protected void
    assertScriptExecutable(String script) throws Exception {
        new ScriptTest(script).assertExecutable();
    }

    /**
     * Asserts that the given <var>script</var> can be cooked and executed.
     *
     * @param returnType The return type of the script
     * @return           The return value of the script execution
     */
    @Nullable protected <T> T
    assertScriptExecutable(String script, Class<T> returnType) throws Exception {

        ScriptTest st = new ScriptTest(script);
        st.scriptEvaluator.setReturnType(returnType);

        @SuppressWarnings("unchecked") T result = (T) st.assertExecutable();
        return result;
    }

    /**
     * Asserts that the given <var>script</var> is cookable and returns TRUE.
     */
    protected void
    assertScriptReturnsTrue(String script) throws Exception { new ScriptTest(script).assertResultTrue(); }

    public
    class ScriptTest extends CompileAndExecuteTest {

        private final String             script;
        protected final IScriptEvaluator scriptEvaluator;

        public
        ScriptTest(String script) throws Exception {
            this.script          = script;
            this.scriptEvaluator = CommonsCompilerTestSuite.this.compilerFactory.newScriptEvaluator();
        }

        @Override public void
        assertResultTrue() throws Exception {
            this.scriptEvaluator.setReturnType(boolean.class);
            super.assertResultTrue();
        }

        @Override @Nullable public <T> T
        assertExecutable(Class<T> returnType) throws Exception {

            this.scriptEvaluator.setReturnType(returnType);

            @SuppressWarnings("unchecked") T result = (T) super.assertExecutable();
            return result;
        }

        @Override protected void
        cook() throws Exception { this.scriptEvaluator.cook(this.script); }

        @Override @Nullable protected Object
        execute() throws Exception {
            try {
                return this.scriptEvaluator.evaluate(new Object[0]);
            } catch (InvocationTargetException ite) {
                Throwable te = ite.getTargetException();
                throw te instanceof Exception ? (Exception) te : ite;
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Asserts that cooking the given <var>classBody</var> issues an error.
     */
    protected void
    assertClassBodyUncookable(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertUncookable();
    }

    /**
     * Asserts that cooking the given <var>classBody</var> issues an error, and the error message contains the
     * <var>messageInfix</var>.
     */
    protected void
    assertClassBodyUncookable(String classBody, String messageInfix) throws Exception {
        new ClassBodyTest(classBody).assertUncookable(messageInfix);
    }

    /**
     * Asserts that cooking the given <var>classBody</var> issues an error, and the error message contains a match for
     * <var>messageRegex</var>.
     */
    protected void
    assertClassBodyUncookable(String classBody, Pattern messageRegex) throws Exception {
        new ClassBodyTest(classBody).assertUncookable(messageRegex);
    }

    /**
     * Asserts that the given <var>classBody</var> can be cooked without errors and warnings.
     */
    protected void
    assertClassBodyCookable(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertCookable();
    }

    /**
     * Asserts that the given <var>classBody</var> is cookable and declares a method "{@code public static
     * }<em>any-type</em> {@code main()}" which executes and terminates normally. (The return value is ignored.)
     */
    protected void
    assertClassBodyExecutable(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertExecutable();
    }

    /**
     * Asserts that the given <var>classBody</var> is cookable and declares a method "{@code public static boolean
     * main()}" which executes and returns {@code true}.
     */
    protected void
    assertClassBodyMainReturnsTrue(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertResultTrue();
    }

    public
    class ClassBodyTest extends CompileAndExecuteTest {

        private final String                classBody;
        protected final IClassBodyEvaluator classBodyEvaluator;

        public
        ClassBodyTest(String classBody) throws Exception {
            this.classBody          = classBody;
            this.classBodyEvaluator = CommonsCompilerTestSuite.this.compilerFactory.newClassBodyEvaluator();
        }

        @Override protected void
        cook() throws Exception {
            this.classBodyEvaluator.cook(this.classBody);
        }

        @Override protected Object
        execute() throws Exception {
            try {
                return this.classBodyEvaluator.getClazz().getMethod("main").invoke(null);
            } catch (InvocationTargetException ite) {
                Throwable te = ite.getTargetException();
                throw te instanceof Exception ? (Exception) te : ite;
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Asserts that cooking the given <var>compilationUnit</var> with the {@link ISimpleCompiler} issues an error.
     */
    protected void
    assertCompilationUnitUncookable(String compilationUnit) throws Exception {
        new SimpleCompilerTest(compilationUnit, "Xxx").assertUncookable();
    }

    /**
     * Asserts that cooking the given <var>compilationUnit</var> with the {@link ISimpleCompiler} issues an error, and
     * the error message contains the <var>messageInfix</var>.
     */
    protected void
    assertCompilationUnitUncookable(String compilationUnit, String messageInfix) throws Exception {
        new SimpleCompilerTest(compilationUnit, "Xxx").assertUncookable(messageInfix);
    }

    /**
     * Asserts that cooking the given <var>classBody</var> with the {@link ISimpleCompiler} issues an error, and the
     * error message contains a match for <var>messageRegex</var>.
     */
    protected void
    assertCompilationUnitUncookable(String compilationUnit, Pattern messageRegex) throws Exception {
        new SimpleCompilerTest(compilationUnit, "Xxx").assertUncookable(messageRegex);
    }

    /**
     * Asserts that the given <var>compilationUnit</var> can be cooked by the {@link ISimpleCompiler} without errors
     * and warnings.
     */
    protected void
    assertCompilationUnitCookable(String compilationUnit) throws Exception {
        new SimpleCompilerTest(compilationUnit, "Xxx").assertCookable();
    }

    /**
     * Asserts that the given <var>compilationUnit</var> can be cooked by the {@link ISimpleCompiler} and its '{@code
     * public static }<em>any-type className</em>{@code .main()}' method completes without exceptions. (The return
     * value is ignored.)
     */
    protected void
    assertCompilationUnitMainExecutable(String compilationUnit, String className) throws Exception {
        new SimpleCompilerTest(compilationUnit, className).assertExecutable();
    }

    /**
     * Asserts that the given <var>compilationUnit</var> can be cooked by the {@link ISimpleCompiler} and its {@code
     * public static boolean }<em>className</em>{@code .main()} method returns TRUE.
     */
    protected void
    assertCompilationUnitMainReturnsTrue(String compilationUnit, String className) throws Exception {
        new SimpleCompilerTest(compilationUnit, className).assertResultTrue();
    }

    public
    class SimpleCompilerTest extends CompileAndExecuteTest {

        private final String            compilationUnit;
        private final String            className;
        protected final ISimpleCompiler simpleCompiler;

        public
        SimpleCompilerTest(String compilationUnit, String className) throws Exception {
            this.compilationUnit = compilationUnit;
            this.className       = className;
            this.simpleCompiler  = CommonsCompilerTestSuite.this.compilerFactory.newSimpleCompiler();
        }

        @Override protected void
        cook() throws Exception {
            this.simpleCompiler.cook(this.compilationUnit);
        }

        @Override protected Object
        execute() throws Exception {
            try {
                return (
                    this.simpleCompiler.getClassLoader()
                    .loadClass(this.className)
                    .getMethod("main", new Class[0])
                    .invoke(null, new Object[0])
                );
            } catch (InvocationTargetException ite) {
                Throwable te = ite.getTargetException();
                throw te instanceof Exception ? (Exception) te : ite;
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Scans, parses, compiles and loads the class with the given <var>className</var> from the given
     * <var>sourceDirectory</var>.
     */
    protected void
    assertJavaSourceLoadable(final File sourceDirectory, final String className) throws Exception {
        AbstractJavaSourceClassLoader loader = this.compilerFactory.newJavaSourceClassLoader();
        loader.setSourcePath(new File[] { sourceDirectory });
        loader.loadClass(className);
    }

    // ----------------------------------------------------------------------------------------------------------------

    /**
     * A test case that calls its abstract methods {@link #cook()}, then {@link #execute()}, and verifies that they
     * throw exceptions or return results as expected.
     */
    private abstract static
    class CompileAndExecuteTest {

        /**
         * @see CompileAndExecuteTest
         */
        protected abstract void cook() throws Exception;

        /**
         * @see CompileAndExecuteTest
         */
        @Nullable protected abstract Object execute() throws Exception;

        /**
         * Asserts that cooking issues an error.
         *
         * @return The compilation error message
         */
        protected String
        assertUncookable() throws Exception {

            try {
                this.cook();
            } catch (CompileException ce) {
                return ce.getMessage();
            }

            try {
                CommonsCompilerTestSuite.fail((
                    "Should have issued an error, but compiled successfully, and evaluated to \""
                    + this.execute()
                    + "\""
                ));
            } catch (Exception e) {
                CommonsCompilerTestSuite.fail("Should have issued an error, but compiled successfully");
            }

            return "S.N.O.";
        }

        /**
         * Asserts that cooking issues an error, and that the error message contains the
         * <var>messageInfix</var>.
         */
        protected void
        assertUncookable(String messageInfix) throws Exception {

            String errorMessage = this.assertUncookable();

            if (!errorMessage.contains(messageInfix)) {
                CommonsCompilerTestSuite.fail((
                    "Error message '"
                    + errorMessage
                    + "' does not contain '"
                    + messageInfix
                    + "'"
                ));
            }
        }

        /**
         * Asserts that cooking issues an error, and that the error message contains a match of
         * <var>messageRegex</var>.
         */
        protected void
        assertUncookable(@Nullable Pattern messageRegex) throws Exception {

            String errorMessage = this.assertUncookable();

            if (messageRegex != null && !messageRegex.matcher(errorMessage).find()) {
                CommonsCompilerTestSuite.fail((
                    "Error message '"
                    + errorMessage
                    + "' does not contain a match of '"
                    + messageRegex
                    + "'"
                ));
            }
        }

        /**
         * Asserts that cooking completes without errors.
         */
        protected void
        assertCookable() throws Exception {
            this.cook();
        }

        /**
         * Asserts that cooking and executing completes normally.
         *
         * @return The execution result
         */
        @Nullable public Object
        assertExecutable() throws Exception {
            this.cook();
            return this.execute();
        }

        /**
         * @return The result of the execution
         */
        @Nullable protected <T> T
        assertExecutable(Class<T> returnType) throws Exception {
            this.cook();

            @SuppressWarnings("unchecked") T result = (T) this.execute();
            return result;
        }

        /**
         * Asserts that cooking completes normally and executing returns {@link Boolean#TRUE}.
         */
        public void
        assertResultTrue() throws Exception {
            this.cook();
            Object result = this.execute();
            Assert.assertNotNull("Test result not NULL", result);
            Assert.assertSame(String.valueOf(result), Boolean.class, result.getClass());
            Assert.assertTrue("Test result is FALSE", (Boolean) result);
        }
    }

    private static void
    fail(String message) {

        Assert.fail((
            message
            + " (java.version="
            + System.getProperty("java.version")
            + ", java.specification.version="
            + System.getProperty("java.specification.version)")
            + "+ "
        ));
    }
}
