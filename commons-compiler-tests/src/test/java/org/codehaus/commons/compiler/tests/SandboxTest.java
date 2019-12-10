
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2017 Arno Unkrig. All rights reserved.
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

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedExceptionAction;
import java.util.List;

import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.Sandbox;
import org.codehaus.commons.nullanalysis.NotNullByDefault;
import org.codehaus.commons.nullanalysis.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.CommonsCompilerTestSuite;
import util.TestUtil;

/**
 * Test cases for the combination of JANINO with {@link Sandbox}.
 */
@RunWith(Parameterized.class) public
class SandboxTest extends CommonsCompilerTestSuite {

    private static final Permissions NO_PERMISSIONS = new Permissions();
    static {

        // Initialize a few classes before using NO_PERMISSIONS...
        try { InetAddress.getLocalHost(); } catch (UnknownHostException e) { throw new ExceptionInInitializerError(e); }
        new Socket();
    }

    private static final Permissions ALL_PERMISSIONS = new Permissions();
    static { SandboxTest.ALL_PERMISSIONS.add(new AllPermission()); }

    /**
     * Get all available compiler factories for the "CompilerFactory" JUnit parameter.
     */
    @Parameters(name = "CompilerFactory={0}") public static List<Object[]>
    compilerFactories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }

    public
    SandboxTest(ICompilerFactory compilerFactory) throws Exception { super(compilerFactory); }

    /**
     * Verifies that a trivial script works in the no-permissions sandbox.
     */
    @Test public void
    testReturnTrue() throws Exception {

        String script = "return true;";
        this.confinedScriptTest(script, SandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that it is not possible to retrieve a system property.
     */
    @Test(expected = AccessControlException.class) public void
    testGetSystemProperty() throws Exception {

        String script = "System.getProperty(\"foo\"); return true;";
        this.confinedScriptTest(script, SandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that it is not possible to delete a file.
     */
    @Test(expected = AccessControlException.class) public void
    testFileDelete() throws Exception {

        String script = "return new java.io.File(\"path/to/file.txt\").delete();";
        this.confinedScriptTest(script, SandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that it is forbidden to list a directory.
     */
    @Test(expected = AccessControlException.class) public void
    testFileList() throws Exception {

        String script = "return new java.io.File(\"path/to/dir\").list() != null;";
        this.confinedScriptTest(script, SandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that {@code .class} works in the no-permissions sandbox.
     */
    @Test public void
    testDotClass() throws Exception {

        String script = "return (System.class != null);";
        this.confinedScriptTest(script, SandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that {@link Class#forName(String)} is accessible in the no-permissions sandbox.
     */
    @Test public void
    testClassForName() throws Exception {

        String script = "return (System.class.forName(\"java.lang.String\") != null);";
        this.confinedScriptTest(script, SandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that {@link Class#getDeclaredField(String)} is forbidden in the no-permissions sandbox.
     */
    @Test(expected = AccessControlException.class) public void
    testDotClassGetDeclaredField() throws Exception {

        String script = "return (String.class.getDeclaredField(\"CASE_INSENSITIVE_ORDER\") != null);";
        this.confinedScriptTest(script, SandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that {@link Class#getDeclaredField(String)} and {@link Field#setAccessible(boolean)} <em>are</em>
     * allowed in an <em>all-permissions</em> sandbox.
     */
    @Test public void
    testDotClassGetDeclaredFieldAllPermissions() throws Exception {

        // JRE 9 throws
        //   java.lang.reflect.InaccessibleObjectException:
        //   Unable to make field private final byte[] java.lang.String.value accessible:
        //   module java.base does not "opens java.lang" to unnamed module @291ae
        // JREs 10, 11, 12, however, are not affected!?
        if (CommonsCompilerTestSuite.JVM_VERSION == 9) return;

        String script = "String.class.getDeclaredField(\"CASE_INSENSITIVE_ORDER\").setAccessible(true); return true;";
        this.confinedScriptTest(script, SandboxTest.ALL_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that creating an {@link URLConnection} is forbidden.
     */
    @Test(expected = AccessControlException.class) public void
    testUrlConnection1() throws Exception {

        // Java 7 and 8 have a design problem (or is it a bug?): The class initializer of
        // "sun.net.www.protocol.http.HttpURLConnection" runs a privileged action:
        //
        //   static {
        //       maxRedirects = ((Integer) AccessController.doPrivileged(
        //           new GetIntegerAction("http.maxRedirects", 20)
        //       )).intValue();
        //   }
        //
        // As a consequence, "URL.openConnection()" throws an InvocationTargetException, caused by
        // an ExceptionInInitializerError, caused by an AccessControlException (instead of an
        // "AccessControlException").
        // As a suitable workaround, we initialize the "sun.net.www.protocol.http.HttpURLConnection"
        // class HERE:
        new java.net.URL("http://localhost:65000").openConnection();

        // Now for the actual test case:
        String script = (
            "return new java.net.URL(\"http://localhost:65000\").openConnection().getInputStream() != null;"
        );
        this.confinedScriptTest(script, SandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that it is forbidden to resolve host names.
     */
    @Test(expected = AccessControlException.class) public void
    testSocketToHost() throws Exception {

        String script = "return new java.net.Socket(\"localhost\", 65000) != null;";
        this.confinedScriptTest(script, SandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that it is forbidden to connect to a numeric IPv4 address.
     */
    @Test(expected = AccessControlException.class) public void
    testSocketToIpAddress() throws Exception {

        String script = (
            "return new java.net.Socket(java.net.InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), 65000) != null;"
        );
        this.confinedScriptTest(script, SandboxTest.NO_PERMISSIONS).assertResultTrue();
    }

    /**
     * Verifies that also the {@link ISimpleCompiler} checks permissions.
     */
    @Test(expected = AccessControlException.class) public void
    testSimpleCompiler() throws Exception {

        this.confinedSimpleCompilerTest(
            "public class Foo { public static void main() { System.getProperty(\"foo\"); } }",
            "Foo",
            SandboxTest.NO_PERMISSIONS
        ).assertExecutable();
    }

    /**
     * Verifies that also the {@link IClassBodyEvaluator} checks permissions.
     */
    @Test(expected = AccessControlException.class) public void
    testClassBodyEvaluator() throws Exception {

        this.confinedClassBodyTest(
            "public static void main() { System.getProperty(\"foo\"); }",
            SandboxTest.NO_PERMISSIONS
        ).assertExecutable();
    }

    /**
     * Verifies that also the {@link IExpressionEvaluator} checks permissions.
     */
    @Test(expected = AccessControlException.class) public void
    testExpressionEvaluator() throws Exception {

        this.confinedExpressionTest(
            "System.getProperty(\"foo\")",
            SandboxTest.NO_PERMISSIONS
        ).assertExecutable();
    }

    /**
     * Verifies that subthreads can be created and execute successfully.
     */
    @Test public void
    testSubthreads() throws Exception {

        // "Thread()" does some REFLECTION, so we must allow that.
        Permissions permissions = new Permissions();
        permissions.add(new RuntimePermission("accessDeclaredMembers"));

        this.confinedScriptTest((
            ""
            + "final Object[] result = new Object[1];\n"
            + "Thread t = new Thread() {\n"
            + "    @Override public void run() { result[0] = \"howdy\"; }\n"
            + "};\n"
            + "t.start();\n"
            + "t.join();\n"
            + "return \"howdy\".equals(result[0]);\n"
        ), permissions).assertResultTrue();
    }

    /**
     * Verifies that also code declared in a subthread is subject to confinement.
     */
    @Test(expected = AccessControlException.class) public void
    testSubthreadConfinement() throws Exception {

        // "Thread()" does some REFLECTION, so we must allow that.
        Permissions permissions = new Permissions();
        permissions.add(new RuntimePermission("accessDeclaredMembers"));

        this.confinedScriptTest((
            ""
            + "final Object[] result = new Object[1];\n"
            + "Thread t = new Thread() {\n"
            + "    @Override public void run() {\n"
            + "        try {\n"
            + "            result[0] = new java.io.File(\"path/to/dir\").list();\n"
            + "        } catch (Exception e) {\n"
            + "            result[0] = e;\n"
            + "        }\n"
            + "    }\n"
            + "};\n"
            + "t.start();\n"
            + "t.join();\n"
            + "if (result[0] instanceof Exception) throw (Exception) result[0];\n"
            + "return result[0] == null;\n"
        ), permissions).assertResultTrue();
    }

    // ====================================== END OF TEST CASES ======================================

    /**
     * Creates and returns a {@link ScriptTest} object that executes scripts in a {@link Sandbox} with the given
     * <var>permissions</var>.
     */
    private ScriptTest
    confinedScriptTest(String script, final PermissionCollection permissions) throws Exception {

        final Sandbox sandbox = new Sandbox(permissions);

        return new ScriptTest(script) {

            @Override protected void
            cook() throws Exception {
                this.scriptEvaluator.setThrownExceptions(new Class<?>[] { Exception.class });
                super.cook();
            }

            @Override @Nullable protected Object
            execute() throws Exception {

                return sandbox.confine(new PrivilegedExceptionAction<Object>() {
                    @Override public Object run() throws Exception { return execute2(); }
                });
            }

            @NotNullByDefault(false) private Object
            execute2() throws Exception { return super.execute(); }
        };
    }

    /**
     * Creates and returns a {@link SimpleCompilerTest} object that executes the {@code public static void main()}
     * method of the named class in a {@link Sandbox} with the given <var>permissions</var>.
     */
    private SimpleCompilerTest
    confinedSimpleCompilerTest(
        String                     compilationUnit,
        String                     className,
        final PermissionCollection permissions
    ) throws Exception {

        final Sandbox sandbox = new Sandbox(permissions);

        return new SimpleCompilerTest(compilationUnit, className) {

            @NotNullByDefault(false) private Object
            execute2() throws Exception { return super.execute(); }

            @Override @Nullable protected Object
            execute() throws Exception {

                return sandbox.confine(new PrivilegedExceptionAction<Object>() {
                    @Override public Object run() throws Exception { return execute2(); }
                });
            }
        };
    }

    /**
     * Creates and returns a {@link ClassBodyTest} object that executes the {@code public
     * static void main()} method in a {@link Sandbox} with the given <var>permissions</var>.
     */
    private ClassBodyTest
    confinedClassBodyTest(String classBody, PermissionCollection permissions) throws Exception {

        final Sandbox sandbox = new Sandbox(permissions);

        return new ClassBodyTest(classBody) {

            @NotNullByDefault(false) private Object
            execute2() throws Exception { return super.execute(); }

            @Override @Nullable protected Object
            execute() throws Exception {

                return sandbox.confine(new PrivilegedExceptionAction<Object>() {
                    @Override public Object run() throws Exception { return execute2(); }
                });
            }
        };
    }

    /**
     * Creates and returns an {@link ExpressionTest} object that evaluates its subject expression
     * in a {@link Sandbox} with the given <var>permissions</var>.
     */
    private ExpressionTest
    confinedExpressionTest(String expression, PermissionCollection permissions) throws Exception {

        final Sandbox sandbox = new Sandbox(permissions);

        return new ExpressionTest(expression) {

            @NotNullByDefault(false) private Object
            execute2() throws Exception { return super.execute(); }

            @Override @Nullable protected Object
            execute() throws Exception {

                return sandbox.confine(new PrivilegedExceptionAction<Object>() {
                    @Override public Object run() throws Exception { return execute2(); }
                });
            }
        };
    }
}
