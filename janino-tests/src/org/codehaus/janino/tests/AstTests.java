
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

package org.codehaus.janino.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.janino.Java;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.BasicType;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FloatingPointLiteral;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameters;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.PackageMemberClassDeclaration;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Mod;
import org.codehaus.janino.SimpleCompiler;
import org.codehaus.janino.UnparseVisitor;
import org.junit.Test;

// CHECKSTYLE JavadocMethod:OFF

/**
 * Tests for 'programmatically created' ASTs.
 */
public
class AstTests {

    private static Object
    compileAndEval(CompilationUnit cu) throws Exception {
        SimpleCompiler compiler = new SimpleCompiler();
        compiler.cook(cu);

        ClassLoader loader = compiler.getClassLoader();

        Class handMadeClass = loader.loadClass("HandMade");

        Object handMade = handMadeClass.newInstance();
        Method calc     = handMadeClass.getMethod("calculate", new Class[0]);
        Object res      = calc.invoke(handMade, new Object[0]);
        return res;
    }

    private static ArrayType
    createByteArrayType() {
        return new Java.ArrayType(new Java.BasicType(getLocation(), Java.BasicType.BYTE));
    }

    private static PackageMemberClassDeclaration
    createClass(CompilationUnit cu) throws CompileException {
        PackageMemberClassDeclaration clazz = new PackageMemberClassDeclaration(
            getLocation(),
            null,
            new Java.Modifiers(Mod.PUBLIC),
            "HandMade",
            null,
            new Type[0]
        );
        cu.addPackageMemberTypeDeclaration(clazz);
        return clazz;
    }

    private static Type
    createDoubleType() { return new BasicType(getLocation(), BasicType.DOUBLE); }

    private static Java.BinaryOperation
    createOp(Rvalue l1, String op, Rvalue l2) { return new Java.BinaryOperation(getLocation(), l1, op, l2); }

    private static IntegerLiteral
    createIntegerLiteral(String value) { return new IntegerLiteral(getLocation(), value); }

    private static FloatingPointLiteral
    createFloatingPointLiteral(String value) { return new FloatingPointLiteral(getLocation(), value); }

    private static void
    createMethod(PackageMemberClassDeclaration clazz, List/*<BlockStatement>*/ statements, Type returnType) {
        MethodDeclarator method = new MethodDeclarator(
            getLocation(),
            null,
            new Java.Modifiers(Mod.PUBLIC),
            returnType,
            "calculate",
            new FormalParameters(),
            new Type[0],
            statements
        );
        clazz.addDeclaredMethod(method);
    }


    private static LocalVariableDeclarationStatement
    createVarDecl(String name, String fPValue) {
        return new Java.LocalVariableDeclarationStatement(
            getLocation(),
            new Java.Modifiers(Mod.NONE),
            createDoubleType(),
            new Java.VariableDeclarator[] {
                new Java.VariableDeclarator(
                    getLocation(),
                    name,
                    0,
                    createFloatingPointLiteral(fPValue)
                )
            }
        );
    }

    private static AmbiguousName
    createVariableRef(String name) {
        return new Java.AmbiguousName(getLocation(), new String[] { name });
    }

    /** A "Clever" method to get a location from a stack trace. */
    private static Location
    getLocation() {
        Exception         e   = new Exception();
        StackTraceElement ste = e.getStackTrace()[1]; //we only care about our caller
        return new Location(
            ste.getFileName(),
            (short) ste.getLineNumber(),
            (short) 0
        );
    }

    @Test public void
    testBlock() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = createClass(cu);

        List/*<Statement>*/ body = new ArrayList();

        Block sub = new Block(getLocation());
        sub.addStatement(createVarDecl("x", "2.0"));

        body.add(sub);
        body.add(
            new ReturnStatement(
                getLocation(),
                new Java.BinaryOperation(
                    getLocation(),
                    createVariableRef("x"),
                    "*",
                    createIntegerLiteral("3")
                )
            )
        );

        createMethod(clazz, body, createDoubleType());

        try {
            compileAndEval(cu);
            fail("Block must limit the scope of variables in it");
        } catch (CompileException ex) {
            assertTrue(ex.getMessage().endsWith("Expression \"x\" is not an rvalue"));
        }
    }

    @Test public void
    testByteArrayLiteral() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = createClass(cu);

        Byte                exp  = Byte.valueOf((byte) 1);
        List/*<Statement>*/ body = new ArrayList();
        body.add(
            new ReturnStatement(
                getLocation(),
                new Java.NewInitializedArray(
                    getLocation(),
                    createByteArrayType(),
                    new Java.ArrayInitializer(
                        getLocation(),
                        new Java.Rvalue[] {
                            createIntegerLiteral("1")
                        }
                    )
                )
            )
        );

        createMethod(clazz, body, createByteArrayType());

        Object res = compileAndEval(cu);
        assertEquals(exp.byteValue(), ((byte[]) res)[0]);
    }

    @Test public void
    testLocalVariable() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = createClass(cu);

        List/*<Statement>*/ body = new ArrayList();
        body.add(createVarDecl("x", "2.0"));
        body.add(
            new ReturnStatement(
                getLocation(),
                new Java.BinaryOperation(
                    getLocation(),
                    createVariableRef("x"),
                    "*",
                    createIntegerLiteral("3")
                )
            )
        );

        createMethod(clazz, body, createDoubleType());

        Object res = compileAndEval(cu);
        assertTrue(res instanceof Double);
        assertEquals(Double.valueOf(6.0), res);
    }

    @Test public void
    testSimpleAst() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = createClass(cu);

        List/*<Statement>*/ body = new ArrayList();
        body.add(
            new ReturnStatement(
                getLocation(),
                createFloatingPointLiteral("3.0")
            )
        );

        createMethod(clazz, body, createDoubleType());

        Object res = compileAndEval(cu);
        assertEquals(Double.valueOf(3.0), res);
    }

    @Test public void
    testClassRef() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = createClass(cu);

        List/*<Statement>*/ body = new ArrayList();

        body.add(
            new ReturnStatement(
                getLocation(),
                new Java.ClassLiteral(
                    getLocation(),
                    new Java.ReferenceType(
                        getLocation(),
                        new String[] {
                            "HandMade"
                        },
                        null // optionalTypeArguments
                    )
                )
            )
        );

        createMethod(clazz, body, new Java.ReferenceType(
            getLocation(),
            new String[] { "java", "lang", "Class" },
            null // optionalTypeArguments
        ));

        SimpleCompiler compiler = new SimpleCompiler();
        compiler.cook(cu);

        ClassLoader loader        = compiler.getClassLoader();
        Class       handMadeClass = loader.loadClass("HandMade");
        Method      calc          = handMadeClass.getMethod("calculate", new Class[0]);

        Object handMade = handMadeClass.newInstance();
        Object res      = calc.invoke(handMade, new Object[0]);
        assertEquals(handMadeClass, res);
    }

    @Test public void
    testPrecedence() throws Exception {
        ExpressionStatement es = new Java.ExpressionStatement(
            new Java.Assignment(
                getLocation(),
                new Java.AmbiguousName(
                    getLocation(),
                    new String[] { "x" }
                ),
                "=",
                createOp(
                    createIntegerLiteral("1"), "*",
                    createOp(createIntegerLiteral("2"), "+", createIntegerLiteral("3"))
                )
            )
        );

        StringWriter   sw = new StringWriter();
        UnparseVisitor uv = new UnparseVisitor(sw);
        uv.visitExpressionStatement(es);
        uv.close();
        assertEquals("x = 1 * ((( 2 + 3 )));", sw.toString());
    }


    @Test public void
    testFullyQualifiedFieldRef() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = createClass(cu);

        List/*<Statement>*/ body = new ArrayList();
        body.add(new Java.ReturnStatement(
            getLocation(),
            new Java.FieldAccessExpression(
                getLocation(),
                new Java.AmbiguousName(
                    getLocation(),
                    new String[] { "other_package2", "ScopingRules" }
                ),
                "publicStaticDouble"
            )
        ));

        createMethod(clazz, body, createDoubleType());

        Object res = compileAndEval(cu);
        assertEquals(other_package2.ScopingRules.publicStaticDouble, res);
    }
}
