
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

package org.codehaus.janino.tests;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.janino.Java;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FloatingPointLiteral;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameters;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.PackageMemberClassDeclaration;
import org.codehaus.janino.Java.Primitive;
import org.codehaus.janino.Java.PrimitiveType;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.Statement;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Mod;
import org.codehaus.janino.SimpleCompiler;
import org.codehaus.janino.Unparser;
import org.junit.Assert;
import org.junit.Test;

// SUPPRESS CHECKSTYLE JavadocMethod:9999

/**
 * Tests for 'programmatically created' ASTs.
 */
public
class AstTest {

    private static Object
    compileAndEval(CompilationUnit cu) throws Exception {
        SimpleCompiler compiler = new SimpleCompiler();
        compiler.cook(cu);

        ClassLoader loader = compiler.getClassLoader();

        Class<?> handMadeClass = loader.loadClass("HandMade");

        Object handMade = handMadeClass.newInstance();
        Method calc     = handMadeClass.getMethod("calculate", new Class[0]);
        Object res      = calc.invoke(handMade, new Object[0]);
        return res;
    }

    private static ArrayType
    createByteArrayType() {
        return new Java.ArrayType(new Java.PrimitiveType(AstTest.getLocation(), Java.Primitive.BYTE));
    }

    private static PackageMemberClassDeclaration
    createClass(CompilationUnit cu) throws CompileException {
        PackageMemberClassDeclaration clazz = new PackageMemberClassDeclaration(
            AstTest.getLocation(),
            null,
            new Java.Modifiers(Mod.PUBLIC),
            "HandMade",
            null,         // optionalTypeParameters
            null,         // optionalExtendedType
            new Type[0]   // implementedTypes
        );
        cu.addPackageMemberTypeDeclaration(clazz);
        return clazz;
    }

    private static Type
    createDoubleType() { return new PrimitiveType(AstTest.getLocation(), Primitive.DOUBLE); }

    private static Java.BinaryOperation
    createOp(Rvalue l1, String op, Rvalue l2) { return new Java.BinaryOperation(AstTest.getLocation(), l1, op, l2); }

    private static IntegerLiteral
    createIntegerLiteral(String value) { return new IntegerLiteral(AstTest.getLocation(), value); }

    private static FloatingPointLiteral
    createFloatingPointLiteral(String value) { return new FloatingPointLiteral(AstTest.getLocation(), value); }

    private static void
    createMethod(PackageMemberClassDeclaration clazz, List<? extends Java.BlockStatement> statements, Type returnType) {
        MethodDeclarator method = new MethodDeclarator(
            AstTest.getLocation(),                       // location
            null,                                        // optionalDocComment
            new Java.Modifiers(Mod.PUBLIC),              // modifiers
            null,                                        // optionalTypeParameters
            returnType,                                  // type
            "calculate",                                 // name
            new FormalParameters(AstTest.getLocation()), // parameters
            new Type[0],                                 // thrownExceptions
            statements                                   // optionalStatements
        );
        clazz.addDeclaredMethod(method);
    }


    private static LocalVariableDeclarationStatement
    createVarDecl(String name, String fPValue) {
        return new Java.LocalVariableDeclarationStatement(
            AstTest.getLocation(),
            new Java.Modifiers(Mod.NONE),
            AstTest.createDoubleType(),
            new Java.VariableDeclarator[] {
                new Java.VariableDeclarator(
                    AstTest.getLocation(),
                    name,
                    0,
                    AstTest.createFloatingPointLiteral(fPValue)
                )
            }
        );
    }

    private static AmbiguousName
    createVariableRef(String name) {
        return new Java.AmbiguousName(AstTest.getLocation(), new String[] { name });
    }

    /**
     * A "Clever" method to get a location from a stack trace.
     */
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

        final PackageMemberClassDeclaration clazz = AstTest.createClass(cu);

        List<Java.Statement> body = new ArrayList<Statement>();

        Block sub = new Block(AstTest.getLocation());
        sub.addStatement(AstTest.createVarDecl("x", "2.0"));

        body.add(sub);
        body.add(
            new ReturnStatement(
                AstTest.getLocation(),
                new Java.BinaryOperation(
                    AstTest.getLocation(),
                    AstTest.createVariableRef("x"),
                    "*",
                    AstTest.createIntegerLiteral("3")
                )
            )
        );

        AstTest.createMethod(clazz, body, AstTest.createDoubleType());

        try {
            AstTest.compileAndEval(cu);
            Assert.fail("Block must limit the scope of variables in it");
        } catch (CompileException ex) {
            Assert.assertTrue(ex.getMessage().endsWith("Expression \"x\" is not an rvalue"));
        }
    }

    @Test public void
    testByteArrayLiteral() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = AstTest.createClass(cu);

        final Byte           exp  = Byte.valueOf((byte) 1);
        List<Java.Statement> body = new ArrayList<Statement>();
        body.add(
            new ReturnStatement(
                AstTest.getLocation(),
                new Java.NewInitializedArray(
                    AstTest.getLocation(),
                    AstTest.createByteArrayType(),
                    new Java.ArrayInitializer(
                        AstTest.getLocation(),
                        new Java.Rvalue[] { AstTest.createIntegerLiteral("1") }
                    )
                )
            )
        );

        AstTest.createMethod(clazz, body, AstTest.createByteArrayType());

        Object res = AstTest.compileAndEval(cu);
        Assert.assertEquals(exp.byteValue(), ((byte[]) res)[0]);
    }

    @Test public void
    testLocalVariable() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        final PackageMemberClassDeclaration clazz = AstTest.createClass(cu);

        List<Java.Statement> body = new ArrayList<Statement>();
        body.add(AstTest.createVarDecl("x", "2.0"));
        body.add(
            new ReturnStatement(
                AstTest.getLocation(),
                new Java.BinaryOperation(
                    AstTest.getLocation(),
                    AstTest.createVariableRef("x"),
                    "*",
                    AstTest.createIntegerLiteral("3")
                )
            )
        );

        AstTest.createMethod(clazz, body, AstTest.createDoubleType());

        Object res = AstTest.compileAndEval(cu);
        Assert.assertTrue(res instanceof Double);
        Assert.assertEquals(Double.valueOf(6.0), res);
    }

    @Test public void
    testSimpleAst() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = AstTest.createClass(cu);

        List<Java.Statement> body = new ArrayList<Statement>();
        body.add(
            new ReturnStatement(
                AstTest.getLocation(),
                AstTest.createFloatingPointLiteral("3.0")
            )
        );

        AstTest.createMethod(clazz, body, AstTest.createDoubleType());

        Object res = AstTest.compileAndEval(cu);
        Assert.assertEquals(Double.valueOf(3.0), res);
    }

    @Test public void
    testClassRef() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = AstTest.createClass(cu);

        List<Java.Statement> body = new ArrayList<Statement>();

        body.add(
            new ReturnStatement(
                AstTest.getLocation(),
                new Java.ClassLiteral(
                    AstTest.getLocation(),
                    new Java.ReferenceType(
                        AstTest.getLocation(),       // location
                        new String[] { "HandMade" }, // identifiers
                        null                         // optionalTypeArguments
                    )
                )
            )
        );

        AstTest.createMethod(clazz, body, new Java.ReferenceType(
            AstTest.getLocation(),                    // location
            new String[] { "java", "lang", "Class" }, // identifiers
            null                                      // optionalTypeArguments
        ));

        SimpleCompiler compiler = new SimpleCompiler();
        compiler.cook(cu);

        ClassLoader loader        = compiler.getClassLoader();
        Class<?>    handMadeClass = loader.loadClass("HandMade");
        Method      calc          = handMadeClass.getMethod("calculate", new Class[0]);

        Object handMade = handMadeClass.newInstance();
        Object res      = calc.invoke(handMade, new Object[0]);
        Assert.assertEquals(handMadeClass, res);
    }

    @Test public void
    testPrecedence() throws Exception {
        ExpressionStatement es = new Java.ExpressionStatement(
            new Java.Assignment(
                AstTest.getLocation(),
                new Java.AmbiguousName(AstTest.getLocation(), new String[] { "x" }),
                "=",
                AstTest.createOp(
                    AstTest.createIntegerLiteral("1"),
                    "*",
                    AstTest.createOp(AstTest.createIntegerLiteral("2"), "+", AstTest.createIntegerLiteral("3"))
                )
            )
        );

        StringWriter sw = new StringWriter();
        Unparser     u  = new Unparser(sw);
        u.unparseBlockStatement(es);
        u.close();
        Assert.assertEquals("x = 1 * ((( 2 + 3 )));", sw.toString());
    }


    @Test public void
    testFullyQualifiedFieldRef() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = AstTest.createClass(cu);

        List<Java.Statement> body = new ArrayList<Statement>();
        body.add(new Java.ReturnStatement(
            AstTest.getLocation(),
            new Java.FieldAccessExpression(
                AstTest.getLocation(),
                new Java.AmbiguousName(
                    AstTest.getLocation(),
                    new String[] { "other_package2", "ScopingRules" }
                ),
                "publicStaticDouble"
            )
        ));

        AstTest.createMethod(clazz, body, AstTest.createDoubleType());

        Object res = AstTest.compileAndEval(cu);
        Assert.assertEquals(other_package2.ScopingRules.publicStaticDouble, res);
    }
}
