
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
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

import junit.framework.*;

import org.codehaus.janino.*;

public class JaninoTestSuite extends StructuredTestSuite {
    /** The test is expected to throw a ScanException */
    public static final int SCAN = 0;
    /** The test is expected to throw a ParseException */
    public static final int PARS = 1;
    /** The test is expected to throw a CompileException */
    public static final int COMP = 2;
    /** The test is expected to compile successfully, but is not executed */
    public static final int COOK = 3;
    /** The test is expected to compile and execute successfully */
    public static final int EXEC = 4;
    /** The test is expected to compile and execute successfully, and return <code>true</code> */
    public static final int TRUE = 5;

    public JaninoTestSuite(String name) {
        super(name);
    }

    /**
     * Shorthand for "add expression test".
     *
     * @see ExpressionTest
     */
    protected ExpressionTest aet(int mode, String title, String expression) {
        ExpressionTest et = new ExpressionTest(mode, title, expression);
        addTest(et);
        return et;
    }

    /**
     * Shorthand for "add script test".
     *
     * @see ScriptTest
     */
    public ScriptTest ast(int mode, String title, String script) {
        ScriptTest st = new ScriptTest(mode, title, script);
        addTest(st);
        return st;
    }

                
    /**
     * Shorthand for "add class body test".
     *
     * @see ClassBodyTest
     */
    protected ClassBodyTest acbt(int mode, String title, String classBody) {
        ClassBodyTest cbt = new ClassBodyTest(title, mode, classBody);
        addTest(cbt);
        return cbt;
    }

    /**
     * Shorthand for "add simple compiler test".
     *
     * @see SimpleCompilerTest
     */
    protected SimpleCompilerTest asct(int mode, String title, String compilationUnit, String className) {
        SimpleCompilerTest sct = new SimpleCompilerTest(title, mode, compilationUnit, className);
        addTest(sct);
        return sct;
    }

    /**
     * A test case that compiles and evaluates a Janino expression, and verifies
     * that it evaluates to "true".
     */
    static protected class ExpressionTest extends CompileAndExecuteTest {
        private final String              expression;
        private final ExpressionEvaluator expressionEvaluator;
    
        public ExpressionTest(int mode, String name, String expression) {
            super(mode, name);
            this.expression = expression;
            this.expressionEvaluator = new ExpressionEvaluator();
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
     * A test case that compiles and runs a Janino script, and optionally checks
     * it boolean return value for thruthness.
     */
    static protected class ScriptTest extends CompileAndExecuteTest {
        private final String          script;
        private final ScriptEvaluator scriptEvaluator;
    
        public ScriptTest(int mode, String name, String script) {
            super(mode, name);
            this.script = script;
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
     * A test case that compiles a class body, calls its "static main()" method and
     * optionally verifies that it returns <code>true</code>.
     */
    static protected class ClassBodyTest extends CompileAndExecuteTest {
        private final String             classBody;
        private final ClassBodyEvaluator classBodyEvaluator;

        public ClassBodyTest(String name, int mode, String classBody) {
            super(mode, name);
            this.classBody = classBody;
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
     * A test case that compiles a compilation unit, optionally creates an instance of the compiled
     * class, calls its "void test()" method and optionally verifies that it returns
     * <code>true</code>.
     */
    static protected class SimpleCompilerTest extends CompileAndExecuteTest {
        private final String         compilationUnit;
        private final String         className;
        private final SimpleCompiler simpleCompiler;

        public SimpleCompilerTest(String name, int mode, String compilationUnit, String className) {
            super(mode, name);
            this.compilationUnit = compilationUnit;
            this.className       = className;
            this.simpleCompiler = new SimpleCompiler();
        }

        protected void compile() throws Exception {
            this.simpleCompiler.cook(this.compilationUnit);
        }

        protected Object execute() throws Exception {
            Class c = this.simpleCompiler.getClassLoader().loadClass(this.className);
            return c.getMethod("test", new Class[0]).invoke(c.newInstance(), new Object[0]);
        }
    }

    /**
     * A test case that calls its abstract methods {@link #compile()}, then {@link #execute()}, and
     * verifies that they throw exceptions and return results as expected.
     */
    static protected abstract class CompileAndExecuteTest extends TestCase {
        protected final int mode;
    
        public CompileAndExecuteTest(int mode, String name) {
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
