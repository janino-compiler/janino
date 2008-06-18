
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

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.codehaus.janino.CompileException;
import org.codehaus.janino.Java;
import org.codehaus.janino.Location;
import org.codehaus.janino.Mod;
import org.codehaus.janino.SimpleCompiler;
import org.codehaus.janino.UnparseVisitor;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.BasicType;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.Literal;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.PackageMemberClassDeclaration;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameter;
import org.codehaus.janino.Parser.ParseException;

public class AstTests extends TestCase {
    public static Test suite() {
        TestSuite s = new TestSuite(AstTests.class.getName());
        s.addTest(new AstTests("testSimpleAst"));
        s.addTest(new AstTests("testLocalVariable"));
        s.addTest(new AstTests("testBlock"));
        s.addTest(new AstTests("testByteArrayLiteral"));
        s.addTest(new AstTests("testClassRef"));
        s.addTest(new AstTests("testPrecedence"));
        return s;
    }

    public AstTests(String name) { super(name); }

    private static Object compileAndEval(CompilationUnit cu) throws CompileException,
    ClassNotFoundException,
    InstantiationException, IllegalAccessException,
    NoSuchMethodException, InvocationTargetException {
        SimpleCompiler compiler = new SimpleCompiler();
        compiler.cook(cu);

        ClassLoader loader = compiler.getClassLoader(); 

        Class handMadeClass = loader.loadClass("HandMade");

        Object handMade = handMadeClass.newInstance();
        Method calc = handMadeClass.getMethod("calculate", null);
        Object res = calc.invoke(handMade, null);
        return res;
    }

    private static ArrayType createByteArrayType() {
        return new Java.ArrayType(
            new Java.BasicType(
                getLocation(), 
                Java.BasicType.BYTE
            )
        );
    };

    private static PackageMemberClassDeclaration createClass(CompilationUnit cu)
    throws ParseException {
        PackageMemberClassDeclaration clazz = new PackageMemberClassDeclaration(
            getLocation(),
            null,
            Mod.PUBLIC,
            "HandMade",
            null,
            new Type[]{}
        );
        cu.addPackageMemberTypeDeclaration(clazz);
        return clazz;
    }

    private static Type createDoubleType() {
        return new BasicType(getLocation(), BasicType.DOUBLE);
    }

    private static Java.BinaryOperation createOp(Rvalue l1, String op, Rvalue l2) {
        return new Java.BinaryOperation(getLocation(), l1, op, l2);
    }

    private static Literal createLiteral(double d) {
        return createLiteral(new Double(d));
    }


    private static Literal createLiteral(Object o) {
        return new Literal( getLocation(), o );
    }

    private static void createMethod(PackageMemberClassDeclaration clazz, Block body, Type returnType) {
        MethodDeclarator method = new MethodDeclarator(
            getLocation(),
            null,
            (short)(Mod.PUBLIC),
            returnType,
            "calculate",
            new FormalParameter[0],
            new Type[0],
            body
        );
        clazz.addDeclaredMethod(method);
    }


    private static LocalVariableDeclarationStatement createVarDecl(String name, double value) {
        return new Java.LocalVariableDeclarationStatement(
            getLocation(),
            (short)0,
            createDoubleType(),
            new Java.VariableDeclarator[] {
                new Java.VariableDeclarator(
                    getLocation(),
                    name,
                    0,
                    createLiteral(value)
                )
            }
        );
    }

    private static AmbiguousName createVariableRef(String name) {
        return new Java.AmbiguousName(
            getLocation(),
            new String[] { name }
        );
    }

    /**
     * "Clever" method to get a location from a stack trace
     */
    static private Location getLocation() {
        Exception e = new Exception();
        StackTraceElement ste = e.getStackTrace()[1];//we only care about our caller
        return new Location( 
            ste.getFileName(), 
            (short)ste.getLineNumber(), 
            (short)0
        );
    }


    public void testBlock() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = createClass(cu);

        Block body = new Block(getLocation());

        Block sub = new Block(getLocation());
        sub.addStatement( createVarDecl("x", 2.0) );                         

        body.addStatement(sub);
        body.addStatement(
            new ReturnStatement(
                getLocation(),
                new Java.BinaryOperation(
                    getLocation(),
                    createVariableRef("x"),
                    "*",
                    createLiteral(3)
                )
            )
        );

        createMethod(clazz, body, createDoubleType());

        try {
            compileAndEval(cu);
            fail("Block must limit the scope of variables in it");
        } catch(CompileException ex) {
            assertTrue(ex.getMessage().endsWith("Expression \"x\" is not an rvalue"));
        }
    }

    public void testByteArrayLiteral() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = createClass(cu);

        Integer exp = new Integer(1);
        Block body = new Block(getLocation());
        body.addStatement(
            new ReturnStatement(
                getLocation(),
                new Java.NewInitializedArray(
                    getLocation(),
                    createByteArrayType(),
                    new Java.ArrayInitializer(
                        getLocation(),
                        new Java.Rvalue[] {
                            createLiteral(exp)
                        }
                    )
                )
            )
        );

        createMethod(clazz, body, 
            createByteArrayType()
        );

        Object res = compileAndEval(cu);
        assertEquals(exp.byteValue(), ((byte[])res)[0]);
    }

    public void testLocalVariable() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = createClass(cu);

        Block body = new Block(getLocation());
        body.addStatement( createVarDecl("x", 2.0) );                         
        body.addStatement(
            new ReturnStatement(
                getLocation(),
                new Java.BinaryOperation(
                    getLocation(),
                    createVariableRef("x"),
                    "*",
                    createLiteral(3)
                )
            )
        );

        createMethod(clazz, body, createDoubleType());

        Object res = compileAndEval(cu);
        assertTrue(res instanceof Double);
        assertEquals(new Double(6.0), res);
    }

    public void testSimpleAst() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = createClass(cu);

        Block body = new Block(getLocation());
        body.addStatement(
            new ReturnStatement(
                getLocation(),
                createLiteral(3.0)
            )
        );

        createMethod(clazz, body, createDoubleType());

        Object res = compileAndEval(cu);
        assertEquals(new Double(3.0), res);
    }

    public void testClassRef() throws Exception {
        CompilationUnit cu = new CompilationUnit("AstTests.java");

        PackageMemberClassDeclaration clazz = createClass(cu);

        Block body = new Block(getLocation());

        body.addStatement(
            new ReturnStatement(
                getLocation(),
                new Java.ClassLiteral(
                    getLocation(),
                    new Java.ReferenceType(
                        getLocation(),
                        new String[] {
                            "HandMade"
                        }
                    )
                )
            )
        );

        createMethod(clazz, body, 
            new Java.ReferenceType(
                getLocation(),
                new String[] { "java", "lang", "Class" }
            )
        );

        SimpleCompiler compiler = new SimpleCompiler();
        compiler.cook(cu);

        ClassLoader loader = compiler.getClassLoader(); 
        Class handMadeClass = loader.loadClass("HandMade");
        Method calc = handMadeClass.getMethod("calculate", null);

        Object handMade = handMadeClass.newInstance();
        Object res = calc.invoke(handMade, null);
        assertEquals(handMadeClass, res);
    }

    public void testPrecedence() throws Exception {
        ExpressionStatement es = new Java.ExpressionStatement(
            new Java.Assignment(
                getLocation(),
                new Java.AmbiguousName(
                    getLocation(),
                    new String[] { "x" }
                ),
                "=",
                createOp(
                    createLiteral(1), "*",
                    createOp(createLiteral(2), "+", createLiteral(3))
                )
            )
        );

        StringWriter sw = new StringWriter();
        UnparseVisitor uv = new UnparseVisitor(sw);
        uv.visitExpressionStatement(es);
        assertEquals("x = 1.0D * ((( 2.0D + 3.0D )));", sw.toString());
    }
}
