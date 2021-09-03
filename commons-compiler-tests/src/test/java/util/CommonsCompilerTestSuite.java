
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.commons.compiler.AbstractJavaSourceClassLoader;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.IScriptEvaluator;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.nullanalysis.Nullable;
import org.junit.Assert;

/**
 * A base class for JUnit 4 test cases that provides easy-to-use functionality to test JANINO.
 */
public
class CommonsCompilerTestSuite {

    /**
     * The version of the running JVM (6, 7, 8, ...). The version of the JRE on the bootstrap classpath may be
     * different from (i.e. smaller than) the JVM version!
     */
    public static final int JVM_VERSION;

    static {
        String  jv = System.getProperty("java.specification.version");
        Matcher m  = Pattern.compile("(?:1\\.)?(\\d+)").matcher(jv);
        Assert.assertTrue(m.matches());
        JVM_VERSION = Integer.parseInt(m.group(1));
    }

    public final boolean isJdk, isJanino;

    /**
     * The {@link ICompilerFactory} in effect for this test execution.
     */
    protected final ICompilerFactory compilerFactory;

    public
    CommonsCompilerTestSuite(ICompilerFactory compilerFactory) {

        this.compilerFactory = compilerFactory;

        String compilerFactoryId = compilerFactory.getId();
        this.isJdk    = compilerFactoryId.equals("org.codehaus.commons.compiler.jdk"); // SUPPRESS CHECKSTYLE EqualsAvoidNull|LineLength
        this.isJanino = compilerFactoryId.equals("org.codehaus.janino");
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
     * contains a match of the <var>messageRegex</var>.
     */
    protected void
    assertExpressionUncookable(String expression, String messageRegex) throws Exception {
        new ExpressionTest(expression).assertUncookable(messageRegex);
    }

    /**
     * Asserts that cooking the given <var>expression</var> issues an error, and the error is located at the given
     * <var>messageLineNumber</var>.
     */
    protected void
    assertExpressionUncookable(String expression, int messageLineNumber) throws Exception {
        new ExpressionTest(expression).assertUncookable(messageLineNumber);
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

        @Override public void setSourceVersion(int sourceVersion) { this.expressionEvaluator.setSourceVersion(sourceVersion); }
        @Override public void setTargetVersion(int targetVersion) { this.expressionEvaluator.setTargetVersion(targetVersion); }

        @Override protected void
        cook() throws Exception {
            this.expressionEvaluator.cook(this.expression);

            // Enable assertions on the new class loader.
            this.expressionEvaluator.getMethod().getDeclaringClass().getClassLoader().setDefaultAssertionStatus(true);
        }

        @Override @Nullable protected Object
        execute() throws Exception {
            try {
                return this.expressionEvaluator.evaluate();
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
     * Asserts that cooking the given <var>script</var> issues an error, and the error message contains a match for
     * <var>messageRegex</var>.
     */
    protected void
    assertScriptUncookable(String script, String messageRegex) throws Exception {
        new ScriptTest(script).assertUncookable(messageRegex);
    }

    /**
     * Asserts that cooking the given <var>script</var> issues an error, and the error is located at the given
     * <var>messageLineNumber</var>.
     */
    protected void
    assertScriptUncookable(String script, int messageLineNumber) throws Exception {
        new ScriptTest(script).assertUncookable(messageLineNumber);
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
     * Returns silently if cooking fails with a message that contains{@code "NYI"}.
     */
    protected void
    assertScriptExecutable(String script) throws Exception {
        new ScriptTest(script).assertExecutable();
    }

    /**
     * Asserts that the given <var>script</var> can be cooked and executed.
     *
     * @return The value returned by the script, or {@code null} if cooking failed with a message that contains
     *         {@code "NYI"}
     */
    protected <T> T
    assertScriptExecutable(String script, Class<T> returnType) throws Exception {

        final ScriptTest st = new ScriptTest(script);
        st.scriptEvaluator.setReturnType(returnType);

        @SuppressWarnings("unchecked") final T result = (T) st.assertExecutable();
        return result;
    }

    /**
     * Asserts that the given <var>script</var> is cookable and returns TRUE.
     */
    protected void
    assertScriptReturnsTrue(String script) throws Exception { new ScriptTest(script).assertResultTrue(); }

    protected void
    assertScriptReturnsNull(String script) throws Exception { new ScriptTest(script).assertResultNull(); }

    public
    class ScriptTest extends CompileAndExecuteTest {

        private final String             script;
        protected final IScriptEvaluator scriptEvaluator;

        public
        ScriptTest(String script) throws Exception {
            this.script          = script;
            this.scriptEvaluator = CommonsCompilerTestSuite.this.compilerFactory.newScriptEvaluator();
        }

        @Override public void setSourceVersion(int sourceVersion) { this.scriptEvaluator.setSourceVersion(sourceVersion); }
        @Override public void setTargetVersion(int targetVersion) { this.scriptEvaluator.setTargetVersion(targetVersion); }

        @Override public void
        assertResultTrue() throws Exception {
            this.scriptEvaluator.setReturnType(boolean.class);
            super.assertResultTrue();
        }

        @Override public void
        assertResultNull() throws Exception {
            this.scriptEvaluator.setReturnType(Object.class);
            super.assertResultNull();
        }

        @Override protected void
        cook() throws Exception {
            this.scriptEvaluator.cook(this.script);

            // Enable assertions on the new class loader.
            this.scriptEvaluator.getMethod().getDeclaringClass().getClassLoader().setDefaultAssertionStatus(true);
        }

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
     * Asserts that cooking the given <var>classBody</var> issues an error, and the error message contains a match for
     * <var>messageRegex</var>.
     */
    protected void
    assertClassBodyUncookable(String classBody, String messageRegex) throws Exception {
        new ClassBodyTest(classBody).assertUncookable(messageRegex);
    }

    /**
     * Asserts that cooking the given <var>classBody</var> issues an error, and the error is located at the given
     * <var>messageLineNumber</var>.
     */
    protected void
    assertClassBodyUncookable(String classBody, int... messageLineNumber) throws Exception {
        new ClassBodyTest(classBody).assertUncookable(messageLineNumber);
    }

    /**
     * Asserts that the given <var>classBody</var> can be cooked without errors and warnings.
     */
    protected void
    assertClassBodyCookable(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertCookable();
    }

    /**
     * Asserts that the given <var>classBody</var> is cookable and declares a method "{@code public [ static ]
     * }<em>any-type</em> {@code main()}" which executes and terminates normally. (The return value is ignored.)
     */
    protected void
    assertClassBodyExecutable(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertExecutable();
    }

    /**
     * Asserts that the given <var>classBody</var> is cookable and declares a method "{@code public [ static ] boolean
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

        @Override public void setSourceVersion(int sourceVersion) { this.classBodyEvaluator.setSourceVersion(sourceVersion); }
        @Override public void setTargetVersion(int targetVersion) { this.classBodyEvaluator.setTargetVersion(targetVersion); }

        @Override protected void
        cook() throws Exception {
            this.classBodyEvaluator.cook(this.classBody);

            // Enable assertions on the new class loader.
            this.classBodyEvaluator.getClazz().getClassLoader().setDefaultAssertionStatus(true);
        }

        @Override @Nullable protected Object
        execute() throws Exception {
            try {
                Class<?> cbeClass   = this.classBodyEvaluator.getClazz();
                Method   mainMethod = cbeClass.getMethod("main");
                return mainMethod.invoke(Modifier.isStatic(mainMethod.getModifiers()) ? null : cbeClass.newInstance());
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
     * the error message contains a match for <var>messageRegex</var>.
     */
    protected void
    assertCompilationUnitUncookable(String compilationUnit, String messageRegex) throws Exception {
        new SimpleCompilerTest(compilationUnit, "Xxx").assertUncookable(messageRegex);
    }

    /**
     * Asserts that cooking the given <var>compilationUnit</var> with the {@link ISimpleCompiler} issues an error, and
     * the error is located at the given <var>messageLineNumber</var>.
     */
    protected void
    assertCompilationUnitUncookable(String compilationUnit, int messageLineNumber) throws Exception {
        new SimpleCompilerTest(compilationUnit, "Xxx").assertUncookable(messageLineNumber);
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
     * Asserts that the given <var>compilationUnit</var> can be cooked by the {@link ISimpleCompiler} without errors
     * and warnings, <em>or</em> issues an error that matches the <var>messageRegex</var>.
     */
    protected void
    assertCompilationUnitCookable(String compilationUnit, String messageRegex) throws Exception {
        new SimpleCompilerTest(compilationUnit, "Xxx").assertCookable(messageRegex);
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

    /**
     * Asserts that the given <var>compilationUnit</var> can be cooked by the {@link ISimpleCompiler} and its {@code
     * public static boolean }<em>className</em>{@code .main()} method returns TRUE, <em>or</em> issues an error that matches
     * the <var>messageRegex</var>.
     */
    protected void
    assertCompilationUnitMainReturnsTrue(String compilationUnit, String className, String messageRegex) throws Exception {
        new SimpleCompilerTest(compilationUnit, className).assertResultTrue(messageRegex);
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

        @Override public void setSourceVersion(int sourceVersion) { this.simpleCompiler.setSourceVersion(sourceVersion); }
        @Override public void setTargetVersion(int targetVersion) { this.simpleCompiler.setTargetVersion(targetVersion); }

        @Override protected void
        cook() throws Exception {
            this.simpleCompiler.cook(this.compilationUnit);

            // Enable assertions on the new class loader.
            this.simpleCompiler.getClassLoader().setDefaultAssertionStatus(true);
        }

        @Override @Nullable protected Object
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
    private abstract
    class CompileAndExecuteTest {

        public abstract void setSourceVersion(int sourceVersion);
        public abstract void setTargetVersion(int targetVersion);

        /**
         * @see CompileAndExecuteTest
         */
        protected abstract void cook() throws Exception;

        /**
         * @return The value produced by the execution
         * @see    CompileAndExecuteTest
         */
        @Nullable protected abstract Object execute() throws Exception;

        /**
         * Asserts that cooking issues an error.
         *
         * @return The thrown {@link CompileException}
         */
        public CompileException
        assertUncookable() throws Exception {

            try {
                this.cook();
            } catch (CompileException ce) {
                return ce;
            }

            try {
                CommonsCompilerTestSuite.this.fail((
                    "Should have issued an error, but compiled successfully, and evaluated to \""
                    + this.execute()
                    + "\""
                ));
                throw new AssertionError();
            } catch (Exception e) {
                CommonsCompilerTestSuite.this.fail("Should have issued an error, but compiled successfully");
                throw new AssertionError();
            }
        }

        /**
         * Asserts that cooking issues an error, and that the error message contains the
         * <var>messageInfix</var>.
         */
        public void
        assertUncookable(@Nullable String messageRegex) throws Exception {

            CompileException ce           = this.assertUncookable();
            String           errorMessage = ce.getMessage();

            if (messageRegex != null && !Pattern.compile(messageRegex).matcher(errorMessage).find()) {
                CommonsCompilerTestSuite.this.fail((
                    "Error message '"
                    + errorMessage
                    + "' does not contain a match of '"
                    + messageRegex
                    + "'"
                ), ce);
            }
        }

        /**
         * Asserts that cooking issues an error, and that the error is located at the given
         * <var>expectedLineNumber</var>.
         */
        protected void
        assertUncookable(int... expectedLineNumber) throws Exception {

            CompileException ce = this.assertUncookable();

            Location loc = ce.getLocation();
            if (loc == null) throw new AssertionError("No location!?");

            if (CommonsCompilerTestSuite.indexOf(expectedLineNumber, loc.getLineNumber()) == -1) {
                CommonsCompilerTestSuite.this.fail((
                    "Compilation error (\""
                    + ce.getMessage()
                    + "\") expected in line "
                    + expectedLineNumber
                    + ", but was in line "
                    + loc.getLineNumber()
                ), ce);
            }
        }

        /**
         * Asserts that cooking completes without errors.
         */
        public void
        assertCookable() throws Exception {
            try {
                this.cook();
            } catch (CompileException ce) {
                throw new AssertionError(ce);
            }
        }

        /**
         * Asserts that cooking completes without errors, <em>or</em> issues an error that matches the
         * <var>messageRegex</var>.
         */
        protected void
        assertCookable(@Nullable String messageRegex) throws Exception {
            try {
                this.cook();
            } catch (CompileException ce) {
                String errorMessage = ce.getMessage();

                if (messageRegex != null && !Pattern.compile(messageRegex).matcher(errorMessage).find()) {
                    CommonsCompilerTestSuite.this.fail((
                        "Compilation error message \""
                        + errorMessage
                        + "\" does not contain a match of \""
                        + messageRegex
                        + "\""
                    ));
                }
            }
        }

        /**
         * Asserts that cooking and executing completes normally.
         *
         * @return The value produced by the execution, or {@code null} if cooking failed with a message that contains
         *         {@code "NYI"}
         */
        public Object
        assertExecutable() throws Exception {

            try {
                this.cook();
            } catch (CompileException ce) {

                // Have mercy with compile exceptions that have "NYI" ("not yet implemented") in their message.
                if (ce.getMessage().indexOf("NYI") != -1) return null;
                throw ce;
            }

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

        /**
         * Asserts that cooking completes normally and executing returns {@link Boolean#TRUE}, <em>or</em> issues an error that
         * matches the <var>messageRegex</var>.
         *
         * @param messageRegex {@code null} means "any error message is acceptable"
         */
        public void
        assertResultTrue(@Nullable String messageRegex) throws Exception {
            try {
                this.cook();
            } catch (CompileException ce) {
                String errorMessage = ce.getMessage();

                if (messageRegex != null && !Pattern.compile(messageRegex).matcher(errorMessage).find()) {
                    CommonsCompilerTestSuite.this.fail((
                        "Compilation error message \""
                        + errorMessage
                        + "\" does not contain a match of \""
                        + messageRegex
                        + "\""
                    ), ce);
                }
                return;
            }

            Object result = this.execute();
            Assert.assertNotNull("Test result not NULL", result);
            Assert.assertSame(String.valueOf(result), Boolean.class, result.getClass());
            Assert.assertTrue("Test result is FALSE", (Boolean) result);
        }

        /**
         * Asserts that cooking completes normally and executing returns {@code null}.
         */
        public void
        assertResultNull() throws Exception {
            this.cook();
            Object result = this.execute();
            Assert.assertNull(String.valueOf(result), result);
        }
    }

    private void
    fail(@Nullable String message) { this.fail(message, null); }

    private void
    fail(@Nullable String message, @Nullable Throwable cause) {

        AssertionError ae = new AssertionError((
            message
            + " (implementation="
            + this.compilerFactory.getId()
            + ", java.version="
            + System.getProperty("java.version")
            + ")"
        ));

        if (cause != null) ae.initCause(cause);

        throw ae;
    }

    private static int
    indexOf(int[] ia, int subject) {
        for (int i = 0; i < ia.length; i++) {
            if (ia[i] == subject) return i;
        }
        return -1;
    }
}
