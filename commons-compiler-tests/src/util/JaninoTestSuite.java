
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

/** A base class for JUnit 4 test cases that provides easy-to-use functionality to test JANINO. */
public
class JaninoTestSuite {

    /** The {@link ICompilerFactory} in effect for this test execution. */
    protected final ICompilerFactory compilerFactory;

    public
    JaninoTestSuite(ICompilerFactory compilerFactory) {
        this.compilerFactory = compilerFactory;
    }

    /**
     * Asserts that cooking the given {@code expression} issues an error.
     */
    protected void
    assertExpressionUncookable(String expression) throws Exception {
        new ExpressionTest(expression).assertUncookable();
    }

    /**
     * Asserts that the given {@code expression} can be cooked without errors and warnings.
     */
    protected void
    assertExpressionCookable(String expression) throws Exception {
        new ExpressionTest(expression).assertCookable();
    }
    
    /**
     * Asserts that the given {@code expression} can be cooked and evaluated. (Its value is ignored.)
     */
    protected void
    assertExpressionEvaluatable(String expression) throws Exception {
        new ExpressionTest(expression).assertExecutable();
    }
    
    /**
     * Asserts that the given {@code expression} evaluates to TRUE.
     */
    protected void
    assertExpressionEvaluatesTrue(String expression) throws Exception {
        new ExpressionTest(expression).assertResultTrue();
    }

    private
    class ExpressionTest extends CompileAndExecuteTest {

        private final String               expression;
        private final IExpressionEvaluator expressionEvaluator;

        public
        ExpressionTest(String expression) throws Exception {
            this.expression          = expression;
            this.expressionEvaluator = JaninoTestSuite.this.compilerFactory.newExpressionEvaluator();
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
     * Asserts that cooking the given {@code script} issues an error.
     */
    protected void
    assertScriptUncookable(String script) throws Exception {
        new ScriptTest(script).assertUncookable();
    }

    /**
     * Asserts that the given {@code script} can be cooked without errors and warnings.
     */
    protected void
    assertScriptCookable(String script) throws Exception {
        new ScriptTest(script).assertCookable();
    }
    
    /**
     * Asserts that the given {@code script} can be cooked and executed. (Its return value is ignored.)
     */
    protected void
    assertScriptExecutable(String script) throws Exception {
        new ScriptTest(script).assertExecutable();
    }
    
    /**
     * Asserts that the given {@code script} returns TRUE.
     */
    protected void
    assertScriptReturnsTrue(String script) throws Exception {
        new ScriptTest(script).assertResultTrue();
    }

    private
    class ScriptTest extends CompileAndExecuteTest {

        private final String           script;
        private final IScriptEvaluator scriptEvaluator;

        public
        ScriptTest(String script) throws Exception {
            this.script          = script;
            this.scriptEvaluator = JaninoTestSuite.this.compilerFactory.newScriptEvaluator();
        }

        @Override protected void
        assertExecutable() throws Exception {
            this.scriptEvaluator.setReturnType(void.class);
            super.assertExecutable();
        }

        @Override protected void
        assertResultTrue() throws Exception {
            this.scriptEvaluator.setReturnType(boolean.class);
            super.assertResultTrue();
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
     * Asserts that cooking the given {@code classBody} issues an error.
     */
    protected void
    assertClassBodyUncookable(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertUncookable();
    }

    /**
     * Asserts that the given {@code classBody} can be cooked without errors and warnings.
     */
    protected void
    assertClassBodyCookable(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertCookable();
    }
    
    /**
     * Asserts that the given {@code classBody} declares a method '{@code public static }<i>any-type</i> {@code
     * main()}' which executes and terminates normally. (The return value is ignored.)
     */
    protected void
    assertClassBodyExecutable(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertExecutable();
    }
    
    /**
     * Asserts that the given {@code classBody} declares a method {@code public static boolean main()} which executes
     * and returns {@code true}.
     */
    protected void
    assertClassBodyMainReturnsTrue(String classBody) throws Exception {
        new ClassBodyTest(classBody).assertResultTrue();
    }
    
    private
    class ClassBodyTest extends CompileAndExecuteTest {
        private final String              classBody;
        private final IClassBodyEvaluator classBodyEvaluator;

        public
        ClassBodyTest(String classBody) throws Exception {
            this.classBody          = classBody;
            this.classBodyEvaluator = JaninoTestSuite.this.compilerFactory.newClassBodyEvaluator();
        }

        protected void
        compile() throws Exception {
            this.classBodyEvaluator.cook(this.classBody);
        }

        protected Object
        execute() throws Exception {

            @SuppressWarnings("unchecked") Method
            method = this.classBodyEvaluator.getClazz().getMethod("main", new Class[0]);

            return method.invoke(null, new Object[0]);
        }
    }

    /**
     * Asserts that cooking the given {@code compilationUnit} with the {@link ISimpleCompiler} issues an error.
     */
    protected void
    assertCompilationUnitUncookable(String compilationUnit) throws Exception {
        new SimpleCompilerTest(compilationUnit, "Xxx").assertUncookable();
    }

    /**
     * Asserts that the given {@code compilationUnit} can be cooked by the {@link ISimpleCompiler} without errors and
     * warnings.
     */
    protected void
    assertCompilationUnitCookable(String compilationUnit) throws Exception {
        new SimpleCompilerTest(compilationUnit, "Xxx").assertCookable();
    }

    /**
     * Asserts that the given {@code compilationUnit} can be cooked by the {@link ISimpleCompiler} and its '{@code
     * public static }<i>any-type className</i>{@code .main()}' method completes without exceptions. (The return value
     * is ignored.)
     */
    protected void
    assertCompilationUnitMainExecutable(String compilationUnit, String className) throws Exception {
        new SimpleCompilerTest(compilationUnit, className).assertExecutable();
    }
    
    /**
     * Asserts that the given {@code compilationUnit} can be cooked by the {@link ISimpleCompiler} and its {@code public
     * static boolean }<i>className</i>{@code .main()} method returns TRUE.
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

        public
        SimpleCompilerTest(String compilationUnit, String className) throws Exception {
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
                .getMethod("main", new Class[0])
                .invoke(null, new Object[0])
            );
        }
    }

    /**
     * Loads the class with the given {@code className} from the given {@code sourceDirectory}.
     */
    protected void
    assertJavaSourceLoadable(final File sourceDirectory, final String className) throws Exception {
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

        /** @see CompileAndExecuteTest */
        protected abstract void compile() throws Exception;

        /** @see CompileAndExecuteTest */
        protected abstract Object execute() throws Exception;

        /**
         * Assert that cooking issues an error with the given {@code message}.
         */
        protected void
        assertUncookable() throws Exception {
            try {
                this.compile();
            } catch (CompileException ce) {
                return; // SUPPRESS CHECKSTYLE AvoidHidingCause
            }
            fail("Should have issued an error, but compiled successfully");
        }
        
        /**
         * Assert that cooking completes without errors.
         */
        protected void
        assertCookable() throws Exception {
            this.compile();
        }

        /**
         * Assert that cooking and executing completes normally.
         */
        protected void
        assertExecutable() throws Exception {
            this.compile();
            this.execute();
        }
        
        /**
         * Assert that cooking completes normally and executing returns TRUE.
         */
        protected void
        assertResultTrue() throws Exception {
            this.compile();
            Object result = this.execute();
            assertNotNull("Test result not NULL", result);
            assertSame("Test return type is BOOLEAN", Boolean.class, result.getClass());
            assertEquals("Test result is TRUE", true, ((Boolean) result).booleanValue());
        }
    }
}
