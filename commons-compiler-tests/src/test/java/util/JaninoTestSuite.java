
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
class JaninoTestSuite {

    /**
     * The {@link ICompilerFactory} in effect for this test execution.
     */
    protected final ICompilerFactory compilerFactory;

    public
    JaninoTestSuite(ICompilerFactory compilerFactory) {
        this.compilerFactory = compilerFactory;
    }

    /**
     * Asserts that cooking the given <var>expression</var> issues an error.
     */
    protected void
    assertExpressionUncookable(String expression) throws Exception {
        new ExpressionTest(expression).assertUncookable();
    }

    /**
     * Asserts that cooking the given <var>expression</var> issues an error, and the error message contains the
     * <var>messageInfix</var>.
     */
    protected void
    assertExpressionUncookable(String expression, String messageInfix) throws Exception {
        new ExpressionTest(expression).assertUncookable(messageInfix);
    }

    /**
     * Asserts that cooking the given <var>expression</var> issues an error, and the error message contains a match for
     * <var>messageRegex</var>.
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
     * Asserts that the given <var>expression</var> evaluates to TRUE.
     */
    protected void
    assertExpressionEvaluatesTrue(String expression) throws Exception {
        new ExpressionTest(expression).assertResultTrue();
    }

    private
    class ExpressionTest extends CompileAndExecuteTest {

        private final String               expression;
        private final IExpressionEvaluator expressionEvaluator;

        ExpressionTest(String expression) throws Exception {
            this.expression          = expression;
            this.expressionEvaluator = JaninoTestSuite.this.compilerFactory.newExpressionEvaluator();
        }

        @Override protected void
        compile() throws Exception { this.expressionEvaluator.cook(this.expression); }

        @Override @Nullable protected Object
        execute() throws Exception { return this.expressionEvaluator.evaluate(new Object[0]); }
    }

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
     * Asserts that the given <var>script</var> can be cooked and executed. (Its return value is ignored.)
     */
    protected void
    assertScriptExecutable(String script) throws Exception {
        new ScriptTest(script).assertExecutable();
    }

    /**
     * Asserts that the given <var>script</var> returns TRUE.
     */
    protected void
    assertScriptReturnsTrue(String script) throws Exception {
        new ScriptTest(script).assertResultTrue();
    }

    /**
     * Returns the result of a given <var>script</var>.
     */
    @Nullable protected Object
    getScriptResult(String script) throws Exception {
        return new ScriptTest(script).getResult();
    }

    public
    class ScriptTest extends CompileAndExecuteTest {

        private final String             script;
        protected final IScriptEvaluator scriptEvaluator;

        public ScriptTest(String script) throws Exception {
            this.script          = script;
            this.scriptEvaluator = JaninoTestSuite.this.compilerFactory.newScriptEvaluator();
        }

        @Override public void
        assertResultTrue() throws Exception {
            this.scriptEvaluator.setReturnType(boolean.class);
            super.assertResultTrue();
        }

        @Override @Nullable protected Object
        getResult() throws Exception {
            this.scriptEvaluator.setReturnType(Object.class);
            return super.getResult();
        }

        @Override protected void
        compile() throws Exception { this.scriptEvaluator.cook(this.script); }

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

    /**
     * Asserts that cooking the given <var>classBody</var> issues an error.
     */
    protected void
    assertClassBodyUncookable(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertUncookable();
    }

    /**
     * Asserts that the given <var>classBody</var> can be cooked without errors and warnings.
     */
    protected void
    assertClassBodyCookable(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertCookable();
    }

    /**
     * Asserts that the given <var>classBody</var> declares a method '{@code public static }<em>any-type</em> {@code
     * main()}' which executes and terminates normally. (The return value is ignored.)
     */
    protected void
    assertClassBodyExecutable(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertExecutable();
    }

    /**
     * Asserts that the given <var>classBody</var> declares a method {@code public static boolean main()} which
     * executesc and returns {@code true}.
     */
    protected void
    assertClassBodyMainReturnsTrue(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertResultTrue();
    }

    private
    class ClassBodyTest extends CompileAndExecuteTest {
        private final String              classBody;
        private final IClassBodyEvaluator classBodyEvaluator;

        ClassBodyTest(String classBody) throws Exception {
            this.classBody          = classBody;
            this.classBodyEvaluator = JaninoTestSuite.this.compilerFactory.newClassBodyEvaluator();
        }

        @Override protected void
        compile() throws Exception {
            this.classBodyEvaluator.cook(this.classBody);
        }

        @Override protected Object
        execute() throws Exception {
            return this.classBodyEvaluator.getClazz().getMethod("main").invoke(null);
        }
    }

    /**
     * Asserts that cooking the given <var>compilationUnit</var> with the {@link ISimpleCompiler} issues an error.
     */
    protected void
    assertCompilationUnitUncookable(String compilationUnit) throws Exception {
        new SimpleCompilerTest(compilationUnit, "Xxx").assertUncookable();
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

    private
    class SimpleCompilerTest extends CompileAndExecuteTest {

        private final String          compilationUnit;
        private final String          className;
        private final ISimpleCompiler simpleCompiler;

        SimpleCompilerTest(String compilationUnit, String className) throws Exception {
            this.compilationUnit = compilationUnit;
            this.className       = className;
            this.simpleCompiler  = JaninoTestSuite.this.compilerFactory.newSimpleCompiler();
        }

        @Override protected void
        compile() throws Exception {
            this.simpleCompiler.cook(this.compilationUnit);
        }

        @Override protected Object
        execute() throws Exception {
            return (
                this.simpleCompiler.getClassLoader()
                .loadClass(this.className)
                .getMethod("main", new Class[0])
                .invoke(null, new Object[0])
            );
        }
    }

    /**
     * Loads the class with the given <var>className</var> from the given <var>sourceDirectory</var>.
     */
    protected void
    assertJavaSourceLoadable(final File sourceDirectory, final String className) throws Exception {
        AbstractJavaSourceClassLoader loader = this.compilerFactory.newJavaSourceClassLoader();
        loader.setSourcePath(new File[] { sourceDirectory });
        loader.loadClass(className);
    }

    /**
     * A test case that calls its abstract methods {@link #compile()}, then {@link #execute()}, and verifies that they
     * throw exceptions and return results as expected.
     */
    abstract static
    class CompileAndExecuteTest {

        /**
         * @see CompileAndExecuteTest
         */
        protected abstract void compile() throws Exception;

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
                this.compile();
            } catch (CompileException ce) {
                return ce.getMessage();
            }

            try {
                Assert.fail("Should have issued an error, but compiled successfully, and evaluated to \"" + this.execute() + "\"");
            } catch (Exception e) {
                Assert.fail("Should have issued an error, but compiled successfully");
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
                Assert.fail("Error message '" + errorMessage + "' does not contain '" + messageInfix + "'");
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
                Assert.fail("Error message '" + errorMessage + "' does not contain a match of '" + messageRegex + "'");
            }
        }

        /**
         * Asserts that cooking completes without errors.
         */
        protected void
        assertCookable() throws Exception {
            this.compile();
        }

        /**
         * Asserts that cooking and executing completes normally.
         */
        protected void
        assertExecutable() throws Exception {
            this.compile();
            this.execute();
        }

        /**
         * @return The result of the execution
         */
        @Nullable protected Object
        getResult() throws Exception {
            this.compile();
            return this.execute();
        }

        /**
         * Asserts that cooking completes normally and executing returns {@link Boolean#TRUE}.
         */
        protected void
        assertResultTrue() throws Exception {
            this.compile();
            Object result = this.execute();
            Assert.assertNotNull("Test result not NULL", result);
            Assert.assertSame(String.valueOf(result), Boolean.class, result.getClass());
            Assert.assertTrue("Test result is FALSE", (Boolean) result);
        }
    }
}
