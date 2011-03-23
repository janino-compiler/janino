
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

package org.codehaus.janino;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.janino.Java.AlternateConstructorInvocation;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.AnonymousClassDeclaration;
import org.codehaus.janino.Java.ArrayAccessExpression;
import org.codehaus.janino.Java.ArrayInitializer;
import org.codehaus.janino.Java.ArrayInitializerOrRvalue;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.Assignment;
import org.codehaus.janino.Java.Atom;
import org.codehaus.janino.Java.BasicType;
import org.codehaus.janino.Java.BinaryOperation;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.BooleanLiteral;
import org.codehaus.janino.Java.BreakStatement;
import org.codehaus.janino.Java.Cast;
import org.codehaus.janino.Java.CatchClause;
import org.codehaus.janino.Java.CharacterLiteral;
import org.codehaus.janino.Java.ClassDeclaration;
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.CompilationUnit.ImportDeclaration;
import org.codehaus.janino.Java.ConditionalExpression;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.ContinueStatement;
import org.codehaus.janino.Java.Crement;
import org.codehaus.janino.Java.DoStatement;
import org.codehaus.janino.Java.EmptyStatement;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FieldAccessExpression;
import org.codehaus.janino.Java.FieldDeclaration;
import org.codehaus.janino.Java.FloatingPointLiteral;
import org.codehaus.janino.Java.ForStatement;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.IfStatement;
import org.codehaus.janino.Java.Initializer;
import org.codehaus.janino.Java.Instanceof;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.InterfaceDeclaration;
import org.codehaus.janino.Java.LabeledStatement;
import org.codehaus.janino.Java.LocalClassDeclaration;
import org.codehaus.janino.Java.LocalClassDeclarationStatement;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.Lvalue;
import org.codehaus.janino.Java.MemberClassDeclaration;
import org.codehaus.janino.Java.MemberInterfaceDeclaration;
import org.codehaus.janino.Java.MemberTypeDeclaration;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.MethodInvocation;
import org.codehaus.janino.Java.NamedClassDeclaration;
import org.codehaus.janino.Java.NewAnonymousClassInstance;
import org.codehaus.janino.Java.NewArray;
import org.codehaus.janino.Java.NewClassInstance;
import org.codehaus.janino.Java.NewInitializedArray;
import org.codehaus.janino.Java.NullLiteral;
import org.codehaus.janino.Java.PackageDeclaration;
import org.codehaus.janino.Java.PackageMemberClassDeclaration;
import org.codehaus.janino.Java.PackageMemberInterfaceDeclaration;
import org.codehaus.janino.Java.PackageMemberTypeDeclaration;
import org.codehaus.janino.Java.ParenthesizedExpression;
import org.codehaus.janino.Java.QualifiedThisReference;
import org.codehaus.janino.Java.ReferenceType;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.RvalueMemberType;
import org.codehaus.janino.Java.Statement;
import org.codehaus.janino.Java.StringLiteral;
import org.codehaus.janino.Java.SuperConstructorInvocation;
import org.codehaus.janino.Java.SuperclassFieldAccessExpression;
import org.codehaus.janino.Java.SuperclassMethodInvocation;
import org.codehaus.janino.Java.SwitchStatement;
import org.codehaus.janino.Java.SynchronizedStatement;
import org.codehaus.janino.Java.ThisReference;
import org.codehaus.janino.Java.ThrowStatement;
import org.codehaus.janino.Java.TryStatement;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.Java.VariableDeclarator;
import org.codehaus.janino.Java.WhileStatement;
import org.codehaus.janino.Scanner.Token;
import org.codehaus.janino.util.enumerator.Enumerator;

/**
 * A parser for the Java&trade; programming language.
 */
public class Parser {
    private final Scanner scanner;

    public Parser(Scanner scanner) {
        this.scanner = scanner;
    }

    public Scanner getScanner() {
        return this.scanner;
    }

    /**
     * <pre>
     *   CompilationUnit := [ PackageDeclaration ]
     *                      { ImportDeclaration }
     *                      { TypeDeclaration }
     * </pre>
     */
    public CompilationUnit parseCompilationUnit() throws CompileException, IOException {
        CompilationUnit compilationUnit = new CompilationUnit(this.location().getFileName());

        if (this.peek("package")) {
            compilationUnit.setPackageDeclaration(this.parsePackageDeclaration());
        }

        while (this.peek("import")) {
            compilationUnit.addImportDeclaration(this.parseImportDeclaration());
        }

        while (!this.peekEOF()) {
            if (this.peekRead(";")) continue;

            compilationUnit.addPackageMemberTypeDeclaration(this.parsePackageMemberTypeDeclaration());
        }

        return compilationUnit;
    }

    /**
     * <pre>
     *   PackageDeclaration := 'package' QualifiedIdentifier ';'
     * </pre>
     */
    public PackageDeclaration parsePackageDeclaration() throws CompileException, IOException {
        this.read("package");
        Location loc = this.location();
        String packageName = Parser.join(this.parseQualifiedIdentifier(), ".");
        this.read(";");
        this.verifyStringIsConventionalPackageName(packageName, loc);
        return new PackageDeclaration(loc, packageName);
    }

    /**
     * <pre>
     *   ImportDeclaration := 'import' ImportDeclarationBody ';'
     * </pre>
     */
    public CompilationUnit.ImportDeclaration parseImportDeclaration() throws CompileException, IOException {
        this.read("import");
        CompilationUnit.ImportDeclaration importDeclaration = this.parseImportDeclarationBody();
        this.read(";");
        return importDeclaration;
    }

    /**
     * <pre>
     *   ImportDeclarationBody := [ 'static' ] Identifier { '.' Identifier } [ '.' '*' ]
     * </pre>
     */
    public CompilationUnit.ImportDeclaration parseImportDeclarationBody() throws CompileException, IOException {
        Location loc = this.location();
        boolean isStatic;
        if (this.peekRead("static")) {
            isStatic = true;
        } else
        {
            isStatic = false;
        }
        List l = new ArrayList();
        l.add(this.readIdentifier());
        for (;;) {
            if (!this.peek(".")) {
                String[] identifiers = (String[]) l.toArray(new String[l.size()]);
                return (
                    isStatic
                    ? (ImportDeclaration) new CompilationUnit.SingleStaticImportDeclaration(loc, identifiers)
                    : (ImportDeclaration) new CompilationUnit.SingleTypeImportDeclaration(loc, identifiers)
                );
            }
            this.read(".");
            if (this.peekRead("*")) {
                String[] identifiers = (String[]) l.toArray(new String[l.size()]);
                return (
                    isStatic
                    ? (ImportDeclaration) new CompilationUnit.StaticImportOnDemandDeclaration(loc, identifiers)
                    : (ImportDeclaration) new CompilationUnit.TypeImportOnDemandDeclaration(loc, identifiers)
                );
            }
            l.add(this.readIdentifier());
        }
    }

    /**
     * QualifiedIdentifier := Identifier { '.' Identifier }
     */
    public String[] parseQualifiedIdentifier() throws CompileException, IOException {
        List l = new ArrayList();
        l.add(this.readIdentifier());
        while (this.peek(".") && this.peekNextButOne().type == Token.IDENTIFIER) {
            this.read();
            l.add(this.readIdentifier());
        }
        return (String[]) l.toArray(new String[l.size()]);
    }

    /**
     * <pre>
     *   PackageMemberTypeDeclaration :=
     *             ModifiersOpt 'class' ClassDeclarationRest |
     *             ModifiersOpt 'interface' InterfaceDeclarationRest
     * </pre>
     */
    public PackageMemberTypeDeclaration parsePackageMemberTypeDeclaration() throws CompileException, IOException {
        String optionalDocComment = this.scanner.doc();

        short modifiers = this.parseModifiersOpt();

        switch (this.read(new String[] { "class", "interface" })) {
        case 0:
            if (optionalDocComment == null) this.warning("CDCM", "Class doc comment missing", this.location());
            return (PackageMemberClassDeclaration) this.parseClassDeclarationRest(
                optionalDocComment,                      // optionalDocComment
                modifiers,                               // modifiers
                ClassDeclarationContext.COMPILATION_UNIT // context
            );
        case 1:
            if (optionalDocComment == null) this.warning("IDCM", "Interface doc comment missing", this.location());
            return (PackageMemberInterfaceDeclaration) this.parseInterfaceDeclarationRest(
                optionalDocComment,                          // optionalDocComment
                modifiers,                                   // modifiers
                InterfaceDeclarationContext.COMPILATION_UNIT // context
            );
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * <pre>
     *   ModifiersOpt := { 'public' | 'protected' | 'private' | 'static' |
     *           'abstract' | 'final' | 'native' | 'synchronized' |
     *           'transient' | 'volatile' | 'strictfp'
     * </pre>
     */
    public short parseModifiersOpt() throws CompileException, IOException {
        short mod = 0;
        for (;;) {
            int idx = this.peekRead(MODIFIER_NAMES);
            if (idx == -1) break;
            String kw = MODIFIER_NAMES[idx];
            short x = MODIFIER_CODES[idx];

            if ((mod & x) != 0) throw this.compileException("Duplicate modifier \"" + kw + "\"");
            for (int i = 0; i < Parser.MUTUALLY_EXCLUSIVE_MODIFIER_CODES.length; ++i) {
                short m = Parser.MUTUALLY_EXCLUSIVE_MODIFIER_CODES[i];
                if ((x & m) != 0 && (mod & m) != 0) {
                    throw this.compileException("Only one of '" + Mod.shortToString(m) + "' allowed");
                }
            }
            mod |= x;
        }
        return mod;
    }
    private static final String[] MODIFIER_NAMES = {
        "public", "protected", "private", "static", "abstract", "final", "native", "synchronized",
        "transient", "volatile", "strictfp"
    };
    private static final short[] MODIFIER_CODES = {
        Mod.PUBLIC, Mod.PROTECTED, Mod.PRIVATE, Mod.STATIC, Mod.ABSTRACT, Mod.FINAL, Mod.NATIVE, Mod.SYNCHRONIZED,
        Mod.TRANSIENT, Mod.VOLATILE, Mod.STRICTFP
    };
    private static final short[] MUTUALLY_EXCLUSIVE_MODIFIER_CODES = {
        Mod.PUBLIC | Mod.PROTECTED | Mod.PRIVATE,
        Mod.ABSTRACT | Mod.FINAL,
    };

    /**
     * <pre>
     *   ClassDeclarationRest :=
     *        Identifier
     *        [ 'extends' ReferenceType ]
     *        [ 'implements' ReferenceTypeList ]
     *        ClassBody
     * </pre>
     */
    public NamedClassDeclaration parseClassDeclarationRest(
        String                  optionalDocComment,
        short                   modifiers,
        ClassDeclarationContext context
    ) throws CompileException, IOException {
        Location location = this.location();
        String className = this.readIdentifier();
        this.verifyIdentifierIsConventionalClassOrInterfaceName(className, location);

        if (this.peek("<")) throw this.compileException("JANINO does not support generics");

        ReferenceType optionalExtendedType = null;
        if (this.peekRead("extends")) {
            optionalExtendedType = this.parseReferenceType();
        }

        ReferenceType[] implementedTypes = new ReferenceType[0];
        if (this.peekRead("implements")) {
            implementedTypes = this.parseReferenceTypeList();
        }

        NamedClassDeclaration namedClassDeclaration;
        if (context == ClassDeclarationContext.COMPILATION_UNIT) {
            namedClassDeclaration = new PackageMemberClassDeclaration(
                location,                              // location
                optionalDocComment,                    // optionalDocComment
                modifiers,                             // modifiers
                className,                             // name
                optionalExtendedType,                  // optinalExtendedType
                implementedTypes                       // implementedTypes
            );
        } else
        if (context == ClassDeclarationContext.TYPE_DECLARATION) {
            namedClassDeclaration = new MemberClassDeclaration(
                location,             // location
                optionalDocComment,   // optionalDocComment
                modifiers,            // modifiers
                className,            // name
                optionalExtendedType, // optionalExtendedType
                implementedTypes      // implementedTypes
            );
        } else
        if (context == ClassDeclarationContext.BLOCK) {
            namedClassDeclaration = new LocalClassDeclaration(
                location,             // location
                optionalDocComment,   // optionalDocComment
                modifiers,            // modifiers
                className,            // name
                optionalExtendedType, // optionalExtendedType
                implementedTypes      // implementedTypes
            );
        } else
        {
            throw new JaninoRuntimeException("SNO: Class declaration in unexpected context " + context);
        }

        this.parseClassBody(namedClassDeclaration);
        return namedClassDeclaration;
    }
    public static final class ClassDeclarationContext extends Enumerator {
        public static final ClassDeclarationContext BLOCK            = new ClassDeclarationContext("block");
        public static final ClassDeclarationContext TYPE_DECLARATION = new ClassDeclarationContext("type_declaration");
        public static final ClassDeclarationContext COMPILATION_UNIT = new ClassDeclarationContext("compilation_unit");
        private ClassDeclarationContext(String name) { super(name); }
    }

    /**
     * <pre>
     *   ClassBody := '{' { ClassBodyDeclaration } '}'
     * </pre>
     */
    public void parseClassBody(ClassDeclaration classDeclaration) throws CompileException, IOException {
        this.read("{");

        for (;;) {
            if (this.peekRead("}")) return;

            this.parseClassBodyDeclaration(classDeclaration);
        }
    }

    /**
     * <pre>
     *   ClassBodyDeclaration :=
     *     ';' |
     *     ModifiersOpt (
     *       Block |                                    // Instance (JLS2 8.6) or static initializer (JLS2 8.7)
     *       'void' Identifier MethodDeclarationRest |
     *       'class' ClassDeclarationRest |
     *       'interface' InterfaceDeclarationRest |
     *       ConstructorDeclarator |
     *       Type Identifier (
     *         MethodDeclarationRest |
     *         FieldDeclarationRest ';'
     *       )
     *     )
     *
     * </pre>
     */
    public void parseClassBodyDeclaration(ClassDeclaration classDeclaration) throws CompileException, IOException {
        if (this.peekRead(";")) return;

        String optionalDocComment = this.scanner.doc();
        short modifiers = this.parseModifiersOpt();

        // Initializer?
        if (this.peek("{")) {
            if ((modifiers & ~Mod.STATIC) != 0) {
                throw this.compileException("Only modifier \"static\" allowed on initializer");
            }

            Initializer initializer = new Initializer(
                this.location(),               // location
                (modifiers & Mod.STATIC) != 0, // statiC
                this.parseBlock()              // block
            );

            classDeclaration.addVariableDeclaratorOrInitializer(initializer);
            return;
        }

        // "void" method declaration.
        if (this.peekRead("void")) {
            Location location = this.location();
            if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", location);
            String name = this.readIdentifier();
            classDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                optionalDocComment,                      // declaringType
                modifiers,                               // optionalDocComment
                new BasicType(location, BasicType.VOID), // modifiers
                name                                     // name
            ));
            return;
        }

        // Member class.
        if (this.peekRead("class")) {
            if (optionalDocComment == null) this.warning("MCDCM", "Member class doc comment missing", this.location());
            classDeclaration.addMemberTypeDeclaration((MemberTypeDeclaration) this.parseClassDeclarationRest(
                optionalDocComment,                      // optionalDocComment
                modifiers,                               // modifiers
                ClassDeclarationContext.TYPE_DECLARATION // context
            ));
            return;
        }

        // Member interface.
        if (this.peekRead("interface")) {
            if (optionalDocComment == null) {
                this.warning("MIDCM", "Member interface doc comment missing", this.location());
            }
            classDeclaration.addMemberTypeDeclaration((MemberTypeDeclaration) this.parseInterfaceDeclarationRest(
                optionalDocComment,                                // optionalDocComment
                (short) (modifiers | Mod.STATIC),                  // modifiers
                InterfaceDeclarationContext.NAMED_TYPE_DECLARATION // context
            ));
            return;
        }

        // Constructor.
        if (
            classDeclaration instanceof NamedClassDeclaration
            && this.peek().value.equals(((NamedClassDeclaration) classDeclaration).getName())
            && this.peekNextButOne("(")
        ) {
            if (optionalDocComment == null) this.warning("CDCM", "Constructor doc comment missing", this.location());
            classDeclaration.addConstructor(this.parseConstructorDeclarator(
                optionalDocComment, // declaringClass
                modifiers           // modifiers
            ));
            return;
        }

        // Member method or field.
        Type memberType = this.parseType();
        Location location = this.location();
        String memberName = this.readIdentifier();

        // Method declarator.
        if (this.peek("(")) {
            if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.location());
            classDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                optionalDocComment,   // declaringType
                modifiers, // optionalDocComment
                memberType,          // modifiers
                memberName          // name
            ));
            return;
        }

        // Field declarator.
        if (optionalDocComment == null) this.warning("FDCM", "Field doc comment missing", this.location());
        FieldDeclaration fd = new FieldDeclaration(
            location,                                  // location
            optionalDocComment,                        // optionalDocComment
            modifiers,                                 // modifiers
            memberType,                                // type
            this.parseFieldDeclarationRest(memberName) // variableDeclarators
        );
        this.read(";");
        classDeclaration.addVariableDeclaratorOrInitializer(fd);
    }

    /**
     * <pre>
     *   InterfaceDeclarationRest :=
     *     Identifier
     *     [ 'extends' ReferenceTypeList ]
     *     InterfaceBody
     * </pre>
     */
    public InterfaceDeclaration parseInterfaceDeclarationRest(
        String                      optionalDocComment,
        short                       modifiers,
        InterfaceDeclarationContext context
    ) throws CompileException, IOException {
        Location location = this.location();
        String interfaceName = this.readIdentifier();
        this.verifyIdentifierIsConventionalClassOrInterfaceName(interfaceName, location);

        ReferenceType[] extendedTypes = new ReferenceType[0];
        if (this.peekRead("extends")) {
            extendedTypes = this.parseReferenceTypeList();
        }

        InterfaceDeclaration interfaceDeclaration;
        if (context == InterfaceDeclarationContext.COMPILATION_UNIT) {
            interfaceDeclaration = new PackageMemberInterfaceDeclaration(
                location,                              // location
                optionalDocComment,                    // optionalDocComment
                modifiers,                             // modifiers
                interfaceName,                         // name
                extendedTypes                          // extendedTypes
            );
        } else
        if (context == InterfaceDeclarationContext.NAMED_TYPE_DECLARATION) {
            interfaceDeclaration = new MemberInterfaceDeclaration(
                location,                                   // location
                optionalDocComment,                         // optionalDocComment
                modifiers,                                  // modifiers
                interfaceName,                              // name
                extendedTypes                               // extendedTypes
            );
        } else
        {
            throw new JaninoRuntimeException("SNO: Interface declaration in unexpected context " + context);
        }

        this.parseInterfaceBody(interfaceDeclaration);
        return interfaceDeclaration;
    }
    public static final class InterfaceDeclarationContext extends Enumerator {
        // CHECKSTYLE(LineLengthCheck):OFF
        public static final InterfaceDeclarationContext NAMED_TYPE_DECLARATION = new InterfaceDeclarationContext("named_type_declaration");
        public static final InterfaceDeclarationContext COMPILATION_UNIT       = new InterfaceDeclarationContext("compilation_unit");
        // CHECKSTYLE(LineLengthCheck):ON

        InterfaceDeclarationContext(String name) { super(name); }
    }

    /**
     * <pre>
     *   InterfaceBody := '{' {
     *     ';' |
     *     ModifiersOpt (
     *       'void' Identifier MethodDeclarationRest |
     *       'class' ClassDeclarationRest |
     *       'interface' InterfaceDeclarationRest |
     *       Type Identifier (
     *         MethodDeclarationRest |
     *         FieldDeclarationRest
     *       )
     *     )
     *   } '}'
     * </pre>
     */
    public void parseInterfaceBody(InterfaceDeclaration interfaceDeclaration) throws CompileException, IOException {
        this.read("{");

        while (!this.peekRead("}")) {

            if (this.peekRead(";")) continue;

            String optionalDocComment = this.scanner.doc();
            short modifiers = this.parseModifiersOpt();

            // "void" method declaration.
            if (this.peekRead("void")) {
                if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.location());
                Location location = this.location();
                String name = this.readIdentifier();
                interfaceDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                    optionalDocComment,                              // declaringType
                    (short) (modifiers | Mod.ABSTRACT | Mod.PUBLIC), // optionalDocComment
                    new BasicType(location, BasicType.VOID),         // modifiers
                    name                                             // name
                ));
            } else

            // Member class.
            if (this.peekRead("class")) {
                if (optionalDocComment == null) {
                    this.warning("MCDCM", "Member class doc comment missing", this.location());
                }
                interfaceDeclaration.addMemberTypeDeclaration(
                    (MemberTypeDeclaration) this.parseClassDeclarationRest(
                        optionalDocComment,                            // optionalDocComment
                        (short) (modifiers | Mod.STATIC | Mod.PUBLIC), // modifiers
                        ClassDeclarationContext.TYPE_DECLARATION       // context
                    )
                );
            } else

            // Member interface.
            if (this.peekRead("interface")) {
                if (optionalDocComment == null) {
                    this.warning("MIDCM", "Member interface doc comment missing", this.location());
                }
                interfaceDeclaration.addMemberTypeDeclaration(
                    (MemberTypeDeclaration) this.parseInterfaceDeclarationRest(
                        optionalDocComment,                                // optionalDocComment
                        (short) (modifiers | Mod.STATIC | Mod.PUBLIC),     // modifiers
                        InterfaceDeclarationContext.NAMED_TYPE_DECLARATION // context
                    )
                );
            } else

            // Member method or field.
            {
                Type     memberType = this.parseType();
                String   memberName = this.readIdentifier();
                Location location = this.location();

                // Method declarator.
                if (this.peek("(")) {
                    if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.location());
                    interfaceDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                        optionalDocComment,                              // declaringType
                        (short) (modifiers | Mod.ABSTRACT | Mod.PUBLIC), // optionalDocComment
                        memberType,                                      // modifiers
                        memberName                                       // name
                    ));
                } else

                // Field declarator.
                {
                    if (optionalDocComment == null) this.warning("FDCM", "Field doc comment missing", this.location());
                    FieldDeclaration fd = new FieldDeclaration(
                        location,                                  // location
                        optionalDocComment,                        // optionalDocComment
                        (short) (                                  // modifiers
                            modifiers |
                            Mod.PUBLIC | Mod.STATIC | Mod.FINAL
                        ),
                        memberType,                                // type
                        this.parseFieldDeclarationRest(memberName) // variableDeclarators
                    );
                    interfaceDeclaration.addConstantDeclaration(fd);
                }
            }
        }
    }

    /**
     * <pre>
     *   ConstructorDeclarator :=
     *     Identifier
     *     FormalParameters
     *     [ 'throws' ReferenceTypeList ]
     *     '{'
     *       [ 'this' Arguments ';' | 'super' Arguments ';' | Primary '.' 'super' Arguments ';' ]
     *       BlockStatements
     *     '}'
     * </pre>
     */
    public ConstructorDeclarator parseConstructorDeclarator(
        String optionalDocComment,
        short  modifiers
    ) throws CompileException, IOException {
        Location location = this.location();
        this.readIdentifier();  // Class name

        // Parse formal parameters.
        FunctionDeclarator.FormalParameter[] formalParameters = this.parseFormalParameters();

        // Parse "throws" clause.
        ReferenceType[] thrownExceptions;
        if (this.peekRead("throws")) {
            thrownExceptions = this.parseReferenceTypeList();
        } else {
            thrownExceptions = new ReferenceType[0];
        }

        // Parse constructor body.
        location = this.location();
        this.read("{");

        // Special treatment for the first statement of the constructor body: If this is surely an
        // expression statement, and if it could be a "ConstructorInvocation", then parse the
        // expression and check if it IS a ConstructorInvocation.
        ConstructorInvocation    optionalConstructorInvocation = null;
        List/*<BlockStatement>*/ statements = new ArrayList();
        if (
            this.peek(new String[] {
                "this", "super", "new", "void",
                "byte", "char", "short", "int", "long", "float", "double", "boolean",
            }) != -1
            || this.peekLiteral()
            || this.peekIdentifier() != null
        ) {
            Atom a = this.parseExpression();
            if (a instanceof ConstructorInvocation) {
                this.read(";");
                optionalConstructorInvocation = (ConstructorInvocation) a;
            } else {
                Statement s;
                if (this.peekIdentifier() != null) {
                    Type variableType = a.toTypeOrPE();
                    s = new LocalVariableDeclarationStatement(
                        a.getLocation(),                     // location
                        (short) 0,                           // modifiers
                        variableType,                        // type
                        this.parseLocalVariableDeclarators() // variableDeclarators
                    );
                    this.read(";");
                } else {
                    s = new ExpressionStatement(a.toRvalueOrPE());
                    this.read(";");
                }
                statements.add(s);
            }
        }
        statements.addAll(this.parseBlockStatements());

        this.read("}");

        return new ConstructorDeclarator(
            location,                      // location
            optionalDocComment,            // optionalDocComment
            modifiers,                     // modifiers
            formalParameters,              // formalParameters
            thrownExceptions,              // thrownExceptions
            optionalConstructorInvocation, // optionalConstructorInvocationStatement
            statements                     // statements
        );
    }

    /**
     * <pre>
     *   MethodDeclarationRest :=
     *     FormalParameters
     *     { '[' ']' }
     *     [ 'throws' ReferenceTypeList ]
     *     ( ';' | MethodBody )
     * </pre>
     */
    public MethodDeclarator parseMethodDeclarationRest(
        String optionalDocComment,
        short  modifiers,
        Type   type,
        String name
    ) throws CompileException, IOException {
        Location location = this.location();

        this.verifyIdentifierIsConventionalMethodName(name, location);

        FunctionDeclarator.FormalParameter[] formalParameters = this.parseFormalParameters();

        for (int i = this.parseBracketsOpt(); i > 0; --i) type = new ArrayType(type);

        ReferenceType[] thrownExceptions;
        if (this.peekRead("throws")) {
            thrownExceptions = this.parseReferenceTypeList();
        } else {
            thrownExceptions = new ReferenceType[0];
        }

        List/*<BlockStatement>*/ optionalStatements;
        if (this.peekRead(";")) {
            if ((modifiers & (Mod.ABSTRACT | Mod.NATIVE)) == 0) {
                throw this.compileException("Non-abstract, non-native method must have a body");
            }
            optionalStatements = null;
        } else {
            if ((modifiers & (Mod.ABSTRACT | Mod.NATIVE)) != 0) {
                throw this.compileException("Abstract or native method must not have a body");
            }
            this.read("{");
            optionalStatements = this.parseBlockStatements();
            this.read("}");
        }
        return new MethodDeclarator(
            location,           // location
            optionalDocComment, // optionalDocComment
            modifiers,          // modifiers
            type,               // type
            name,               // name
            formalParameters,   // formalParameters
            thrownExceptions,   // thrownExceptions
            optionalStatements  // optionalStatements
        );
    }

    /**
     * <pre>
     *   VariableInitializer :=
     *     ArrayInitializer |
     *     Expression
     * </pre>
     */
    public ArrayInitializerOrRvalue parseVariableInitializer() throws CompileException, IOException {
        if (this.peek("{")) {
            return this.parseArrayInitializer();
        } else
        {
            return this.parseExpression().toRvalueOrPE();
        }
    }

    /**
     * <pre>
     *   ArrayInitializer :=
     *     '{' [ VariableInitializer { ',' VariableInitializer } [ ',' ] '}'
     * </pre>
     */
    public ArrayInitializer parseArrayInitializer() throws CompileException, IOException {
        Location location = this.location();
        this.read("{");
        List l = new ArrayList(); // ArrayInitializerOrRvalue
        while (!this.peekRead("}")) {
            l.add(this.parseVariableInitializer());
            if (this.peekRead("}")) break;
            this.read(",");
        }
        return new ArrayInitializer(
            location,
            (ArrayInitializerOrRvalue[]) l.toArray(new ArrayInitializerOrRvalue[l.size()])
        );
    }

    /**
     * <pre>
     *   FormalParameters := '(' [ FormalParameter { ',' FormalParameter } ] ')'
     * </pre>
     */
    public FunctionDeclarator.FormalParameter[] parseFormalParameters() throws CompileException, IOException {
        this.read("(");
        if (this.peekRead(")")) return new FunctionDeclarator.FormalParameter[0];

        List l = new ArrayList(); // FormalParameter
        do {
            l.add(this.parseFormalParameter());
        } while (this.read(new String[] { ",", ")" }) == 0);
        return (FunctionDeclarator.FormalParameter[]) l.toArray(new FunctionDeclarator.FormalParameter[l.size()]);
    }

    /**
     * <pre>
     *   FormalParameter := [ 'final' ] Type Identifier BracketsOpt
     * </pre>
     */
    public FunctionDeclarator.FormalParameter parseFormalParameter() throws CompileException, IOException {
        boolean finaL = this.peekRead("final");

        Type type = this.parseType();

        Location location = this.location();
        String name = this.readIdentifier();
        this.verifyIdentifierIsConventionalLocalVariableOrParameterName(name, location);

        for (int i = this.parseBracketsOpt(); i > 0; --i) type = new ArrayType(type);
        return new FunctionDeclarator.FormalParameter(location, finaL, type, name);
    }

    /**
     * <pre>
     *   BracketsOpt := { '[' ']' }
     * </pre>
     */
    int parseBracketsOpt() throws CompileException, IOException {
        int res = 0;
        while (this.peek("[") && this.peekNextButOne("]")) {
            this.read();
            this.read();
            ++res;
        }
        return res;
    }

    /**
     * <pre>
     *   MethodBody := Block
     * </pre>
     */
    public Block parseMethodBody() throws CompileException, IOException {
        return this.parseBlock();
    }

    /**
     * <pre>
     *   '{' BlockStatements '}'
     * </pre>
     */
    public Block parseBlock() throws CompileException, IOException {
        Block block = new Block(this.location());
        this.read("{");
        block.addStatements(this.parseBlockStatements());
        this.read("}");
        return block;
    }

    /**
     * <pre>
     *   BlockStatements := { BlockStatement }
     * </pre>
     */
    public List parseBlockStatements() throws CompileException, IOException {
        List l = new ArrayList();
        while (
            !this.peek("}") &&
            !this.peek("case") &&
            !this.peek("default")
        ) l.add(this.parseBlockStatement());
        return l;
    }

    /**
     * <pre>
     *   BlockStatement := { Identifier ':' } (
     *     ( Modifiers Type | ModifiersOpt BasicType ) LocalVariableDeclarators ';' |
     *     'class' ... |
     *     Statement |
     *     'final' Type LocalVariableDeclarators ';' |
     *     Expression ';' |
     *     Expression LocalVariableDeclarators ';'   (1)
     *   )
     * </pre>
     *
     * (1) "Expression" must pose a type, and has optional trailing brackets.
     */
    public BlockStatement parseBlockStatement() throws CompileException, IOException {

        // Statement?
        if (
            (
                this.peekIdentifier() != null
                && this.peekNextButOne(":")
            )
            || this.peek(new String[] {
                "if", "for", "while", "do", "try", "switch", "synchronized",
                "return", "throw", "break", "continue"
            }) != -1
            || this.peek(new String[] { "{", ";" }) != -1
        ) return this.parseStatement();

        // Local class declaration?
        if (this.peekRead("class")) {
            // JAVADOC[TM] ignores doc comments for local classes, but we
            // don't...
            String optionalDocComment = this.scanner.doc();
            if (optionalDocComment == null) this.warning("LCDCM", "Local class doc comment missing", this.location());

            final LocalClassDeclaration lcd = (LocalClassDeclaration) this.parseClassDeclarationRest(
                optionalDocComment,           // optionalDocComment
                Mod.NONE,                     // modifiers
                ClassDeclarationContext.BLOCK // context
            );
            return new LocalClassDeclarationStatement(lcd);
        }

        // 'final' Type LocalVariableDeclarators ';'
        if (this.peekRead("final")) {
            Location location = this.location();
            Type variableType = this.parseType();
            LocalVariableDeclarationStatement lvds = new LocalVariableDeclarationStatement(
                location,                            // location
                Mod.FINAL,                           // modifiers
                variableType,                        // type
                this.parseLocalVariableDeclarators() // variableDeclarators
            );
            this.read(";");
            return lvds;
        }

        // It's either a non-final local variable declaration or an expression statement. We can
        // only tell after parsing an expression.
        Atom a = this.parseExpression();

        // Expression ';'
        if (this.peekRead(";")) {
            return new ExpressionStatement(a.toRvalueOrPE());
        }

        // Expression LocalVariableDeclarators ';'
        Type variableType = a.toTypeOrPE();
        LocalVariableDeclarationStatement lvds = new LocalVariableDeclarationStatement(
            a.getLocation(),                     // location
            Mod.NONE,                            // modifiers
            variableType,                        // type
            this.parseLocalVariableDeclarators() // variableDeclarators
        );
        this.read(";");
        return lvds;
    }

    /**
     * <pre>
     *   LocalVariableDeclarators := VariableDeclarator { ',' VariableDeclarator }
     * </pre>
     */
    public VariableDeclarator[] parseLocalVariableDeclarators() throws CompileException, IOException {
        List l = new ArrayList();
        do {
            VariableDeclarator vd = this.parseVariableDeclarator();
            this.verifyIdentifierIsConventionalLocalVariableOrParameterName(vd.name, vd.getLocation());
            l.add(vd);
        } while (this.peekRead(","));
        return (VariableDeclarator[]) l.toArray(new VariableDeclarator[l.size()]);
    }

    /**
     * <pre>
     *   FieldDeclarationRest :=
     *     VariableDeclaratorRest
     *     { ',' VariableDeclarator }
     * </pre>
     */
    public VariableDeclarator[] parseFieldDeclarationRest(String name) throws CompileException, IOException {
        List l = new ArrayList();

        VariableDeclarator vd = this.parseVariableDeclaratorRest(name);
        this.verifyIdentifierIsConventionalFieldName(vd.name, vd.getLocation());
        l.add(vd);

        while (this.peekRead(",")) {

            vd = this.parseVariableDeclarator();
            this.verifyIdentifierIsConventionalFieldName(vd.name, vd.getLocation());
            l.add(vd);
        }
        return (VariableDeclarator[]) l.toArray(new VariableDeclarator[l.size()]);
    }

    /**
     * <pre>
     *   VariableDeclarator := Identifier VariableDeclaratorRest
     * </pre>
     */
    public VariableDeclarator parseVariableDeclarator() throws CompileException, IOException {
        return this.parseVariableDeclaratorRest(this.readIdentifier());
    }

    /**
     * <pre>
     *   VariableDeclaratorRest := { '[' ']' } [ '=' VariableInitializer ]
     * </pre>
     * Used by field declarations and local variable declarations.
     */
    public VariableDeclarator parseVariableDeclaratorRest(String name) throws CompileException, IOException  {
        Location loc = this.location();
        int brackets = this.parseBracketsOpt();
        ArrayInitializerOrRvalue initializer = null;
        if (this.peekRead("=")) {
            initializer = this.parseVariableInitializer();
        }
        return new VariableDeclarator(loc, name, brackets, initializer);
    }

    /**
     * <pre>
     *   Statement :=
     *     LabeledStatement |
     *     Block |
     *     IfStatement |
     *     ForStatement |
     *     WhileStatement |
     *     DoStatement |
     *     TryStatement |
     *     'switch' ... |
     *     'synchronized' ... |
     *     ReturnStatement |
     *     ThrowStatement |
     *     BreakStatement |
     *     ContinueStatement |
     *     EmptyStatement |
     *     ExpressionStatement
     * </pre>
     */
    public Statement parseStatement() throws CompileException, IOException {
        if (this.peekIdentifier() != null && this.peekNextButOne(":")) {
            return this.parseLabeledStatement();
        }

        Statement stmt = (
            this.peek("{")            ? this.parseBlock() :
            this.peek("if")           ? this.parseIfStatement() :
            this.peek("for")          ? this.parseForStatement() :
            this.peek("while")        ? this.parseWhileStatement() :
            this.peek("do")           ? this.parseDoStatement() :
            this.peek("try")          ? this.parseTryStatement() :
            this.peek("switch")       ? this.parseSwitchStatement() :
            this.peek("synchronized") ? this.parseSynchronizedStatement() :
            this.peek("return")       ? this.parseReturnStatement() :
            this.peek("throw")        ? this.parseThrowStatement() :
            this.peek("break")        ? this.parseBreakStatement() :
            this.peek("continue")     ? this.parseContinueStatement() :
            this.peek(";")            ? this.parseEmptyStatement() :
            this.parseExpressionStatement()
        );
        if (stmt == null) throw this.compileException("'" + this.peek().value + "' NYI");

        return stmt;
    }

    /**
     * <pre>
     *   LabeledStatement := Identifier ':' Statement
     * </pre>
     */
    public Statement parseLabeledStatement() throws CompileException, IOException {
        String label = this.readIdentifier();
        this.read(":");
        return new LabeledStatement(
            this.location(),        // location
            label,                  // label
            this.parseStatement()   // body
        );
    }

    /**
     * <pre>
     *   IfStatement := 'if' '(' Expression ')' Statement [ 'else' Statement ]
     * </pre>
     */
    public Statement parseIfStatement() throws CompileException, IOException {
        Location location = this.location();
        this.read("if");
        this.read("(");
        final Rvalue condition = this.parseExpression().toRvalueOrPE();
        this.read(")");

        Statement thenStatement = this.parseStatement();

        Statement optionalElseStatement = null;
        if (this.peekRead("else")) {
            optionalElseStatement = this.parseStatement();
        }

        return new IfStatement(
            location,             // location
            condition,            // condition
            thenStatement,        // thenStatement
            optionalElseStatement // optionalElseStatement
        );
    }

    /**
     * <pre>
     *   ForStatement :=
     *     'for' '('
     *       [ ForInit ] ';'
     *       [ Expression ] ';'
     *       [ ExpressionList ]
     *     ')' Statement
     * </pre>
     */
    public Statement parseForStatement() throws CompileException, IOException {
        Location location = this.location();
        this.read("for");

        this.read("(");

        BlockStatement optionalInit = null;
        if (!this.peek(";")) optionalInit = this.parseForInit();

        this.read(";");

        Rvalue optionalCondition = null;
        if (!this.peek(";")) optionalCondition = this.parseExpression().toRvalueOrPE();

        this.read(";");

        Rvalue[] optionalUpdate = null;
        if (!this.peek(")")) optionalUpdate = this.parseExpressionList();

        this.read(")");

        return new ForStatement(
            location,             // location
            optionalInit,         // optionalInit
            optionalCondition,    // optionalCondition
            optionalUpdate,       // optionalUpdate
            this.parseStatement() // body
        );
    }

    /**
     * <pre>
     *   ForInit :=
     *     Modifiers Type LocalVariableDeclarators |
     *     ModifiersOpt BasicType LocalVariableDeclarators |
     *     Expression (
     *       LocalVariableDeclarators |       (1)
     *       { ',' Expression }
     *     )
     * </pre>
     *
     * (1) "Expression" must pose a type.
     */
    private BlockStatement parseForInit() throws CompileException, IOException {

        // Modifiers Type LocalVariableDeclarators
        // ModifiersOpt BasicType LocalVariableDeclarators
        if (this.peek(new String[] {
            "final", "byte", "short", "char", "int", "long", "float", "double", "boolean"
        }) != -1) {
            short modifiers = this.parseModifiersOpt();
            Type variableType = this.parseType();
            return new LocalVariableDeclarationStatement(
                this.location(),                     // location
                modifiers,                           // modifiers
                variableType,                        // type
                this.parseLocalVariableDeclarators() // variableDeclarators
            );
        }

        Atom a = this.parseExpression();

        // Expression LocalVariableDeclarators
        if (this.peekIdentifier() != null) {
            Type variableType = a.toTypeOrPE();
            return new LocalVariableDeclarationStatement(
                a.getLocation(),                     // location
                Mod.NONE,                            // modifiers
                variableType,                        // type
                this.parseLocalVariableDeclarators() // variableDeclarators
            );
        }

        // Expression { ',' Expression }
        if (!this.peekRead(",")) {
            return new ExpressionStatement(a.toRvalueOrPE());
        }
        List l = new ArrayList();
        l.add(new ExpressionStatement(a.toRvalueOrPE()));
        do {
            l.add(new ExpressionStatement(this.parseExpression().toRvalueOrPE()));
        } while (this.peekRead(","));

        Block b = new Block(a.getLocation());
        b.addStatements(l);
        return b;
    }

    /**
     * <pre>
     *   WhileStatement := 'while' '(' Expression ')' Statement
     * </pre>
     */
    public Statement parseWhileStatement() throws CompileException, IOException {
        Location location = this.location();
        this.read("while");

        this.read("(");
        Rvalue condition = this.parseExpression().toRvalueOrPE();
        this.read(")");

        return new WhileStatement(
            location,             // location
            condition,            // condition
            this.parseStatement() // body
        );
    }

    /**
     * <pre>
     *   DoStatement := 'do' Statement 'while' '(' Expression ')' ';'
     * </pre>
     */
    public Statement parseDoStatement() throws CompileException, IOException {
        Location location = this.location();
        this.read("do");

        Statement body = this.parseStatement();

        this.read("while");
        this.read("(");
        Rvalue condition = this.parseExpression().toRvalueOrPE();
        this.read(")");
        this.read(";");

        return new DoStatement(
            location, // location
            body,     // body
            condition // condition
        );
    }

    /**
     * <pre>
     *   TryStatement :=
     *     'try' Block Catches [ Finally ] |
     *     'try' Block Finally
     *
     *   Catches := CatchClause { CatchClause }
     *
     *   CatchClause := 'catch' '(' FormalParameter ')' Block
     *
     *   Finally := 'finally' Block
     * </pre>
     */
    public Statement parseTryStatement() throws CompileException, IOException {
        Location location = this.location();
        this.read("try");

        Block body = this.parseBlock();

        // { CatchClause }
        List ccs = new ArrayList();
        while (this.peekRead("catch")) {
            Location loc = this.location();
            this.read("(");
            FunctionDeclarator.FormalParameter caughtException = this.parseFormalParameter();
            this.read(")");
            ccs.add(new CatchClause(
                loc,              // location
                caughtException,  // caughtException
                this.parseBlock() // body
            ));
        }
        Block optionalFinally = null;
        if (this.peekRead("finally")) {
            optionalFinally = this.parseBlock();
        }
        if (ccs.size() == 0 && optionalFinally == null) {
            throw this.compileException(
                "\"try\" statement must have at least one \"catch\" clause or a \"finally\" clause"
            );
        }

        return new TryStatement(
            location,       // location
            body,           // body
            ccs,            // catchClauses
            optionalFinally // optionalFinally
        );
    }

    /**
     * <pre>
     *   SwitchStatement :=
     *     'switch' '(' Expression ')' '{' { SwitchLabels BlockStatements } '}'
     *
     *   SwitchLabels := SwitchLabels { SwitchLabels }
     *
     *   SwitchLabel := 'case' Expression ':' | 'default' ':'
     * </pre>
     */
    public Statement parseSwitchStatement() throws CompileException, IOException {
        Location location = this.location();
        this.read("switch");

        this.read("(");
        Rvalue condition = this.parseExpression().toRvalueOrPE();
        this.read(")");

        this.read("{");
        List sbsgs = new ArrayList();
        while (!this.peekRead("}")) {
            Location location2 = this.location();
            boolean hasDefaultLabel = false;
            List caseLabels = new ArrayList();
            do {
                if (this.peekRead("case")) {
                    caseLabels.add(this.parseExpression().toRvalueOrPE());
                } else
                if (this.peekRead("default")) {
                    if (hasDefaultLabel) throw this.compileException("Duplicate \"default\" label");
                    hasDefaultLabel = true;
                } else {
                    throw this.compileException("\"case\" or \"default\" expected");
                }
                this.read(":");
            } while (this.peek(new String[] { "case", "default" }) != -1);

            SwitchStatement.SwitchBlockStatementGroup sbsg = new SwitchStatement.SwitchBlockStatementGroup(
                location2, // location
                caseLabels, // caseLabels
                hasDefaultLabel, // hasDefaultLabel
                this.parseBlockStatements() // blockStatements
            );
            sbsgs.add(sbsg);
        }
        return new SwitchStatement(
            location,  // location
            condition, // condition
            sbsgs      // sbsgs
        );
    }

    /**
     * <pre>
     *   SynchronizedStatement :=
     *     'synchronized' '(' expression ')' Block
     * </pre>
     */
    public Statement parseSynchronizedStatement() throws CompileException, IOException {
        Location location = this.location();
        this.read("synchronized");
        this.read("(");
        Rvalue expression = this.parseExpression().toRvalueOrPE();
        this.read(")");
        return new SynchronizedStatement(
            location,         // location
            expression,       // expression
            this.parseBlock() // body
        );
    }

    /**
     * <pre>
     *   ReturnStatement := 'return' [ Expression ] ';'
     * </pre>
     */
    public Statement parseReturnStatement() throws CompileException, IOException {
        Location location = this.location();
        this.read("return");
        Rvalue returnValue = this.peek(";") ? null : this.parseExpression().toRvalueOrPE();
        this.read(";");
        return new ReturnStatement(location, returnValue);
    }

    /**
     * <pre>
     *   ThrowStatement := 'throw' Expression ';'
     * </pre>
     */
    public Statement parseThrowStatement() throws CompileException, IOException {
        Location location = this.location();
        this.read("throw");
        final Rvalue expression = this.parseExpression().toRvalueOrPE();
        this.read(";");

        return new ThrowStatement(location, expression);
    }

    /**
     * <pre>
     *   BreakStatement := 'break' [ Identifier ] ';'
     * </pre>
     */
    public Statement parseBreakStatement() throws CompileException, IOException {
        Location location = this.location();
        this.read("break");
        String optionalLabel = null;
        if (this.peekIdentifier() != null) optionalLabel = this.readIdentifier();
        this.read(";");
        return new BreakStatement(location, optionalLabel);
    }

    /**
     * <pre>
     *   ContinueStatement := 'continue' [ Identifier ] ';'
     * </pre>
     */
    public Statement parseContinueStatement() throws CompileException, IOException {
        Location location = this.location();
        this.read("continue");
        String optionalLabel = null;
        if (this.peekIdentifier() != null) optionalLabel = this.readIdentifier();
        this.read(";");
        return new ContinueStatement(location, optionalLabel);
    }

    /**
     * <pre>
     *   EmptyStatement := ';'
     * </pre>
     */
    public Statement parseEmptyStatement() throws CompileException, IOException {
        Location location = this.location();
        this.read(";");
        return new EmptyStatement(location);
    }

    /**
     * <pre>
     *   ExpressionList := Expression { ',' Expression }
     * </pre>
     */
    public Rvalue[] parseExpressionList() throws CompileException, IOException {
        List l = new ArrayList();
        do {
            l.add(this.parseExpression().toRvalueOrPE());
        } while (this.peekRead(","));
        return (Rvalue[]) l.toArray(new Rvalue[l.size()]);
    }

    /**
     * <pre>
     *   Type := (
     *     'byte' | 'short' | 'char' | 'int' | 'long' |
     *     'float' | 'double' | 'boolean' |
     *     ReferenceType
     *   ) { '[' ']' }
     * </pre>
     */
    public Type parseType() throws CompileException, IOException {
        int idx = this.peekRead(BASIC_TYPE_NAMES);
        Type res = idx != -1 ? (Type) new BasicType(this.location(), BASIC_TYPE_CODES[idx]) : this.parseReferenceType();
        for (int i = this.parseBracketsOpt(); i > 0; --i) res = new ArrayType(res);
        return res;
    }
    private static final String[] BASIC_TYPE_NAMES = {
        "byte", "short", "char", "int", "long", "float",
        "double", "boolean"
    };
    private static final int[] BASIC_TYPE_CODES = {
        BasicType.BYTE, BasicType.SHORT, BasicType.CHAR, BasicType.INT, BasicType.LONG, BasicType.FLOAT,
        BasicType.DOUBLE, BasicType.BOOLEAN
    };

    /**
     * <pre>
     *   ReferenceType := QualifiedIdentifier
     * </pre>
     */
    public ReferenceType parseReferenceType() throws CompileException, IOException {
        String[] identifiers = this.parseQualifiedIdentifier();
        if (this.peek("<")) throw this.compileException("JANINO does not support generics");
        return new ReferenceType(this.location(), identifiers);
    }

    /**
     * <pre>
     *   ReferenceTypeList := ReferenceType { ',' ReferenceType }
     * </pre>
     */
    public ReferenceType[] parseReferenceTypeList() throws CompileException, IOException {
        List l = new ArrayList();
        l.add(this.parseReferenceType());
        while (this.peekRead(",")) {
            l.add(this.parseReferenceType());
        }
        return (ReferenceType[]) l.toArray(new ReferenceType[l.size()]);
    }

    /**
     * <pre>
     *   Expression := AssignmentExpression
     * </pre>
     */
    public Atom parseExpression() throws CompileException, IOException  {
        return this.parseAssignmentExpression();
    }

    /**
     * <pre>
     *   AssignmentExpression :=
     *     ConditionalExpression [ AssignmentOperator AssignmentExpression ]
     *
     *   AssignmentOperator :=
     *     '=' | '*=' | '/=' | '%=' | '+=' | '-=' | '<<=' |
     *     '>>=' | '>>>=' | '&=' | '^=' | '|='
     * </pre>
     */
    public Atom parseAssignmentExpression() throws CompileException, IOException  {
        Atom a = this.parseConditionalExpression();
        if (this.peek(
            new String[] { "=", "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", "<<=", ">>=", ">>>=" }
        ) != -1) {
            final Lvalue lhs = a.toLvalueOrPE();
            Location location = this.location();
            String operator = this.readOperator();
            final Rvalue rhs = this.parseAssignmentExpression().toRvalueOrPE();
            return new Assignment(location, lhs, operator, rhs);
        }
        return a;
    }

    /**
     * <pre>
     *   ConditionalExpression :=
     *     ConditionalOrExpression [ '?' Expression ':' ConditionalExpression ]
     * </pre>
     */
    public Atom parseConditionalExpression() throws CompileException, IOException  {
        Atom a = this.parseConditionalOrExpression();
        if (!this.peekRead("?")) return a;
        Location location = this.location();

        Rvalue lhs = a.toRvalueOrPE();
        Rvalue mhs = this.parseExpression().toRvalueOrPE();
        this.read(":");
        Rvalue rhs = this.parseConditionalExpression().toRvalueOrPE();
        return new ConditionalExpression(location, lhs, mhs, rhs);
    }

    /**
     * <pre>
     *   ConditionalOrExpression :=
     *     ConditionalAndExpression { '||' ConditionalAndExpression ]
     * </pre>
     */
    public Atom parseConditionalOrExpression() throws CompileException, IOException  {
        Atom a = this.parseConditionalAndExpression();
        while (this.peekRead("||")) {
            Location location = this.location();
            a = new BinaryOperation(
                location,
                a.toRvalueOrPE(),
                "||",
                this.parseConditionalAndExpression().toRvalueOrPE()
            );
        }
        return a;
    }

    /**
     * <pre>
     *   ConditionalAndExpression :=
     *     InclusiveOrExpression { '&&' InclusiveOrExpression }
     * </pre>
     */
    public Atom parseConditionalAndExpression() throws CompileException, IOException  {
        Atom a = this.parseInclusiveOrExpression();
        while (this.peekRead("&&")) {
            Location location = this.location();
            a = new BinaryOperation(
                location,
                a.toRvalueOrPE(),
                "&&",
                this.parseInclusiveOrExpression().toRvalueOrPE()
            );
        }
        return a;
    }

    /**
     * <pre>
     *   InclusiveOrExpression :=
     *     ExclusiveOrExpression { '|' ExclusiveOrExpression }
     * </pre>
     */
    public Atom parseInclusiveOrExpression() throws CompileException, IOException  {
        Atom a = this.parseExclusiveOrExpression();
        while (this.peekRead("|")) {
            Location location = this.location();
            a = new BinaryOperation(
                location,
                a.toRvalueOrPE(),
                "|",
                this.parseExclusiveOrExpression().toRvalueOrPE()
            );
        }
        return a;
    }

    /**
     * <pre>
     *   ExclusiveOrExpression :=
     *     AndExpression { '^' AndExpression }
     * </pre>
     */
    public Atom parseExclusiveOrExpression() throws CompileException, IOException  {
        Atom a = this.parseAndExpression();
        while (this.peekRead("^")) {
            Location location = this.location();
            a = new BinaryOperation(
                location,
                a.toRvalueOrPE(),
                "^",
                this.parseAndExpression().toRvalueOrPE()
            );
        }
        return a;
    }

    /**
     * <pre>
     *   AndExpression :=
     *     EqualityExpression { '&' EqualityExpression }
     * </pre>
     */
    public Atom parseAndExpression() throws CompileException, IOException  {
        Atom a = this.parseEqualityExpression();
        while (this.peekRead("&")) {
            Location location = this.location();
            a = new BinaryOperation(
                location,
                a.toRvalueOrPE(),
                "&",
                this.parseEqualityExpression().toRvalueOrPE()
            );
        }
        return a;
    }

    /**
     * <pre>
     *   EqualityExpression :=
     *     RelationalExpression { ( '==' | '!=' ) RelationalExpression }
     * </pre>
     */
    public Atom parseEqualityExpression() throws CompileException, IOException  {
        Atom a = this.parseRelationalExpression();

        while (this.peek(new String[] { "==", "!=" }) != -1) {
            a = new BinaryOperation(
                this.location(),                                // location
                a.toRvalueOrPE(),                               // lhs
                this.read().value,                              // op
                this.parseRelationalExpression().toRvalueOrPE() // rhs
            );
        }
        return a;
    }

    /**
     * <pre>
     *   RelationalExpression :=
     *     ShiftExpression {
     *       ( ( '<' | '>' | '<=' | '>=' ) ShiftExpression ) |
     *       ( 'instanceof' ReferenceType )
     *     }
     * </pre>
     */
    public Atom parseRelationalExpression() throws CompileException, IOException  {
        Atom a = this.parseShiftExpression();

        for (;;) {
            if (this.peekRead("instanceof")) {
                Location location = this.location();
                a = new Instanceof(
                    location,
                    a.toRvalueOrPE(),
                    this.parseType()
                );
            } else
            if (this.peek(new String[] { "<", ">", "<=", ">=" }) != -1) {
                a = new BinaryOperation(
                    this.location(),                           // location
                    a.toRvalueOrPE(),                          // lhs
                    this.read().value,                         // op
                    this.parseShiftExpression().toRvalueOrPE() // rhs
                );
            } else {
                return a;
            }
        }
    }

    /**
     * <pre>
     *   ShiftExpression :=
     *     AdditiveExpression { ( '<<' | '>>' | '>>>' ) AdditiveExpression }
     * </pre>
     */
    public Atom parseShiftExpression() throws CompileException, IOException  {
        Atom a = this.parseAdditiveExpression();

        while (this.peek(new String[] { "<<", ">>", ">>>" }) != -1) {
            a = new BinaryOperation(
                this.location(),                              // location
                a.toRvalueOrPE(),                             // lhs
                this.read().value,                            // op
                this.parseAdditiveExpression().toRvalueOrPE() // rhs
            );
        }
        return a;
    }

    /**
     * <pre>
     *   AdditiveExpression :=
     *     MultiplicativeExpression { ( '+' | '-' ) MultiplicativeExpression }
     * </pre>
     */
    public Atom parseAdditiveExpression() throws CompileException, IOException  {
        Atom a = this.parseMultiplicativeExpression();

        while (this.peek(new String[] { "+", "-" }) != -1) {
            a = new BinaryOperation(
                this.location(),                                    // location
                a.toRvalueOrPE(),                                   // lhs
                this.read().value,                                  // op
                this.parseMultiplicativeExpression().toRvalueOrPE() // rhs
            );
        }
        return a;
    }

    /**
     * <pre>
     *   MultiplicativeExpression :=
     *     UnaryExpression { ( '*' | '/' | '%' ) UnaryExpression }
     * </pre>
     */
    public Atom parseMultiplicativeExpression() throws CompileException, IOException {
        Atom a = this.parseUnaryExpression();

        while (this.peek(new String[] { "*", "/", "%" }) != -1) {
            a = new BinaryOperation(
                this.location(),                           // location
                a.toRvalueOrPE(),                          // lhs
                this.read().value,                         // op
                this.parseUnaryExpression().toRvalueOrPE() // rhs
            );
        }
        return a;
    }

    /**
     * <pre>
     *   UnaryExpression :=
     *     { PrefixOperator } Primary { Selector } { PostfixOperator }
     *
     *   PrefixOperator := '++' | '--' | '+' | '-' | '~' | '!'
     *
     *   PostfixOperator := '++' | '--'
     * </pre>
     */
    public Atom parseUnaryExpression() throws CompileException, IOException {
        if (this.peek(new String[] { "++", "--" }) != -1) {
            return new Crement(
                this.location(),                           // location
                this.read().value,                         // operator
                this.parseUnaryExpression().toLvalueOrPE() // operand
            );
        }

        if (this.peek(new String[] { "+", "-", "~", "!" }) != -1) {
            return new UnaryOperation(
                this.location(),                           // location
                this.read().value,                         // operator
                this.parseUnaryExpression().toRvalueOrPE() // operand
            );
        }

        Atom a = this.parsePrimary();

        while (this.peek(new String[] { ".", "[" }) != -1) {
            a = this.parseSelector(a);
        }

        while (this.peek(new String[] { "++", "--" }) != -1) {
            a = new Crement(
                this.location(),   // location
                a.toLvalueOrPE(),  // operand
                this.read().value  // operator
            );
        }

        return a;
    }

    /**
     * <pre>
     *   Primary :=
     *     CastExpression |                        // CastExpression 15.16
     *     '(' Expression ')' |                    // ParenthesizedExpression 15.8.5
     *     Literal |                               // Literal 15.8.1
     *     Name |                                  // AmbiguousName
     *     Name Arguments |                        // MethodInvocation
     *     Name '[]' { '[]' } |                    // ArrayType 10.1
     *     Name '[]' { '[]' } '.' 'class' |        // ClassLiteral 15.8.2
     *     'this' |                                // This 15.8.3
     *     'this' Arguments |                      // Alternate constructor invocation 8.8.5.1
     *     'super' Arguments |                     // Unqualified superclass constructor invocation 8.8.5.1
     *     'super' '.' Identifier |                // SuperclassFieldAccess 15.11.2
     *     'super' '.' Identifier Arguments |      // SuperclassMethodInvocation 15.12.4.9
     *     NewClassInstance |
     *     NewAnonymousClassInstance |             // ClassInstanceCreationExpression 15.9
     *     NewArray |                              // ArrayCreationExpression 15.10
     *     NewInitializedArray |                   // ArrayInitializer 10.6
     *     BasicType { '[]' } |                    // Type
     *     BasicType { '[]' } '.' 'class' |        // ClassLiteral 15.8.2
     *     'void' '.' 'class'                      // ClassLiteral 15.8.2
     *
     *   CastExpression :=
     *     '(' PrimitiveType { '[]' } ')' UnaryExpression |
     *     '(' Expression ')' UnaryExpression
     *
     *   NewClassInstance := 'new' ReferenceType Arguments
     *
     *   NewAnonymousClassInstance := 'new' ReferenceType Arguments [ ClassBody ]
     *
     *   NewArray := 'new' Type DimExprs { '[]' }
     *
     *   NewInitializedArray := 'new' ArrayType ArrayInitializer
     * </pre>
     */
    public Atom parsePrimary() throws CompileException, IOException {
        if (this.peekRead("(")) {
            if (
                this.peek(new String[] { "boolean", "char", "byte", "short", "int", "long", "float", "double", }) != -1
            ) {
                // '(' PrimitiveType { '[]' } ')' UnaryExpression
                Type type = this.parseType();
                int brackets = this.parseBracketsOpt();
                this.read(")");
                for (int i = 0; i < brackets; ++i) type = new ArrayType(type);
                return new Cast(
                    this.location(),                           // location
                    type,                                      // targetType
                    this.parseUnaryExpression().toRvalueOrPE() // value
                );
            }
            Atom a = this.parseExpression();
            this.read(")");

            if (
                this.peekLiteral()
                || this.peekIdentifier() != null
                || this.peek(new String[] { "(", "~", "!", }) != -1
                || this.peek(new String[] { "this", "super", "new", }) != -1
            ) {
                // '(' Expression ')' UnaryExpression
                return new Cast(
                    this.location(),                           // location
                    a.toTypeOrPE(),                            // targetType
                    this.parseUnaryExpression().toRvalueOrPE() // value
                );
            }

            // '(' Expression ')'
            return new ParenthesizedExpression(a.getLocation(), a.toRvalueOrPE());
        }

        if (this.peekLiteral()) {

            // Literal
            return this.parseLiteral();
        }

        if (this.peekIdentifier() != null) {
            Location location = this.location();
            String[] qi = this.parseQualifiedIdentifier();
            if (this.peek("(")) {
                // Name Arguments
                return new MethodInvocation(
                    this.location(),                           // location
                    qi.length == 1 ? null : new AmbiguousName( // optionalTarget
                        location,       // location
                        qi,             // identifiers
                        qi.length - 1   // n
                    ),
                    qi[qi.length - 1],                         // methodName
                    this.parseArguments()                      // arguments
                );
            }
            if (this.peek("[") && this.peekNextButOne("]")) {
                // Name '[]' { '[]' }
                // Name '[]' { '[]' } '.' 'class'
                Type res = new ReferenceType(
                    location, // location
                    qi        // identifiers
                );
                int brackets = this.parseBracketsOpt();
                for (int i = 0; i < brackets; ++i) res = new ArrayType(res);
                if (this.peek(".") && this.peekNextButOne("class")) {
                    this.read();
                    Location location2 = this.location();
                    this.read();
                    return new ClassLiteral(location2, res);
                } else {
                    return res;
                }
            }
            // Name
            return new AmbiguousName(
                this.location(), // location
                qi               // identifiers
            );
        }

        if (this.peekRead("this")) {
            Location location = this.location();
            if (this.peek("(")) {

                // 'this' Arguments
                // Alternate constructor invocation (JLS 8.8.5.1)
                return new AlternateConstructorInvocation(
                    location,             // location
                    this.parseArguments() // arguments
                );
            } else
            {

                // 'this'
                return new ThisReference(location);
            }
        }

        if (this.peekRead("super")) {
            if (this.peek("(")) {

                // 'super' Arguments
                // Unqualified superclass constructor invocation (JLS 8.8.5.1)
                return new SuperConstructorInvocation(
                    this.location(),      // location
                    (Rvalue) null,        // optionalQualification
                    this.parseArguments() // arguments
                );
            }
            this.read(".");
            String name = this.readIdentifier();
            if (this.peek("(")) {

                // 'super' '.' Identifier Arguments
                return new SuperclassMethodInvocation(
                    this.location(),      // location
                    name,                 // methodName
                    this.parseArguments() // arguments
                );
            } else {

                // 'super' '.' Identifier
                return new SuperclassFieldAccessExpression(
                    this.location(), // location
                    (Type) null,     // optionalQualification
                    name             // fieldName
                );
            }
        }

        // 'new'
        if (this.peekRead("new")) {
            Location location = this.location();
            Type type = this.parseType();
            if (type instanceof ArrayType) {
                // 'new' ArrayType ArrayInitializer
                return new NewInitializedArray(location, (ArrayType) type, this.parseArrayInitializer());
            }
            if (type instanceof ReferenceType && this.peek("(")) {
                // 'new' ReferenceType Arguments [ ClassBody ]
                Rvalue[] arguments = this.parseArguments();
                if (this.peek("{")) {
                    // 'new' ReferenceType Arguments ClassBody
                    final AnonymousClassDeclaration anonymousClassDeclaration = new AnonymousClassDeclaration(
                        this.location(), // location
                        type             // baseType
                    );
                    this.parseClassBody(anonymousClassDeclaration);
                    return new NewAnonymousClassInstance(
                        location,                  // location
                        (Rvalue) null,             // optionalQualification
                        anonymousClassDeclaration, // anonymousClassDeclaration
                        arguments                  // arguments
                    );
                } else {
                    // 'new' ReferenceType Arguments
                    return new NewClassInstance(
                        location,      // location
                        (Rvalue) null, // optionalQualification
                        type,          // type
                        arguments      // arguments
                    );
                }
            }
            // 'new' Type DimExprs { '[]' }
            return new NewArray(
                location,               // location
                type,                   // type
                this.parseDimExprs(),   // dimExprs
                this.parseBracketsOpt() // dims
            );
        }

        // BasicType
        if (this.peek(new String[] { "boolean", "char", "byte", "short", "int", "long", "float", "double", }) != -1) {
            Type res = this.parseType();
            int brackets = this.parseBracketsOpt();
            for (int i = 0; i < brackets; ++i) res = new ArrayType(res);
            if (this.peek(".") && this.peekNextButOne("class")) {
                // BasicType { '[]' } '.' 'class'
                this.read();
                Location location = this.location();
                this.read();
                return new ClassLiteral(location, res);
            }
            // BasicType { '[]' }
            return res;
        }

        // 'void'
        if (this.peekRead("void")) {
            if (this.peek(".") && this.peekNextButOne("class")) {
                // 'void' '.' 'class'
                this.read();
                Location location = this.location();
                this.read();
                return new ClassLiteral(location, new BasicType(location, BasicType.VOID));
            }
            throw this.compileException("\"void\" encountered in wrong context");
        }

        throw this.compileException("Unexpected token \"" + this.peek().value + "\" in primary");
    }

    /**
     * <pre>
     *   Selector :=
     *     '.' Identifier |                       // FieldAccess 15.11.1
     *     '.' Identifier Arguments |             // MethodInvocation
     *     '.' 'this'                             // QualifiedThis 15.8.4
     *     '.' 'super' Arguments                  // Qualified superclass constructor invocation (JLS 8.8.5.1)
     *     '.' 'super' '.' Identifier |           // SuperclassFieldReference (JLS 15.11.2)
     *     '.' 'super' '.' Identifier Arguments | // SuperclassMethodInvocation (JLS 15.12.4.9)
     *     '.' 'new' Identifier Arguments [ ClassBody ] | // QualifiedClassInstanceCreationExpression  15.9
     *     '.' 'class'
     *     '[' Expression ']'                     // ArrayAccessExpression 15.13
     * </pre>
     */
    public Atom parseSelector(Atom atom) throws CompileException, IOException {
        if (this.peekRead(".")) {
            if (this.peek().type == Token.IDENTIFIER) {
                String identifier = this.readIdentifier();
                if (this.peek("(")) {
                    // '.' Identifier Arguments
                    return new MethodInvocation(
                        this.location(),      // location
                        atom.toRvalueOrPE(),  // optionalTarget
                        identifier,           // methodName
                        this.parseArguments() // arguments
                    );
                }
                // '.' Identifier
                return new FieldAccessExpression(
                    this.location(),     // location
                    atom.toRvalueOrPE(), // lhs
                    identifier           // fieldName
                );
            }
            if (this.peekRead("this")) {
                // '.' 'this'
                Location location = this.location();
                return new QualifiedThisReference(
                    location,                // location
                    atom.toTypeOrPE()        // qualification
                );
            }
            if (this.peekRead("super")) {
                Location location = this.location();
                if (this.peek("(")) {

                    // '.' 'super' Arguments
                    // Qualified superclass constructor invocation (JLS 8.8.5.1) (LHS is an Rvalue)
                    return new SuperConstructorInvocation(
                        location,             // location
                        atom.toRvalueOrPE(),  // optionalQualification
                        this.parseArguments() // arguments
                    );
                }
                this.read(".");
                String identifier = this.readIdentifier();

                if (this.peek("(")) {

                    // '.' 'super' '.' Identifier Arguments
                    // Qualified superclass method invocation (JLS 15.12) (LHS is a ClassName)
                    // TODO: Qualified superclass method invocation
                    throw this.compileException("Qualified superclass method invocation NYI");
                } else {

                    // '.' 'super' '.' Identifier
                    // Qualified superclass field access (JLS 15.11.2) (LHS is an Rvalue)
                    return new SuperclassFieldAccessExpression(
                        location,          // location
                        atom.toTypeOrPE(), // optionalQualification
                        identifier         // fieldName
                    );
                }
            }
            if (this.peekRead("new")) {
                // '.' 'new' Identifier Arguments [ ClassBody ]
                Rvalue lhs = atom.toRvalue();
                Location location = this.location();
                String identifier = this.readIdentifier();
                Type type = new RvalueMemberType(
                    location,  // location
                    lhs,       // rValue
                    identifier // identifier
                );
                Rvalue[] arguments = this.parseArguments();
                if (this.peek("{")) {
                    // '.' 'new' Identifier Arguments ClassBody (LHS is an Rvalue)
                    final AnonymousClassDeclaration anonymousClassDeclaration = new AnonymousClassDeclaration(
                        this.location(), // location
                        type             // baseType
                    );
                    this.parseClassBody(anonymousClassDeclaration);
                    return new NewAnonymousClassInstance(
                        location,                  // location
                        lhs,                       // optionalQualification
                        anonymousClassDeclaration, // anonymousClassDeclaration
                        arguments                  // arguments
                    );
                } else {
                    // '.' 'new' Identifier Arguments (LHS is an Rvalue)
                    return new NewClassInstance(
                        location, // location
                        lhs,      // optionalQualification
                        type,     // referenceType
                        arguments // arguments
                    );
                }
            }
            if (this.peekRead("class")) {
                // '.' 'class'
                Location location = this.location();
                return new ClassLiteral(location, atom.toTypeOrPE());
            }
            throw this.compileException("Unexpected selector '" + this.peek().value + "' after \".\"");
        }
        if (this.peekRead("[")) {
            // '[' Expression ']'
            Location location = this.location();
            Rvalue index = this.parseExpression().toRvalueOrPE();
            this.read("]");
            return new ArrayAccessExpression(
                location,            // location
                atom.toRvalueOrPE(), // lhs
                index                // index
            );
        }
        throw this.compileException("Unexpected token '" + this.peek().value + "' in selector");
    }

    /**
     * <pre>
     *   DimExprs := DimExpr { DimExpr }
     * </pre>
     */
    public Rvalue[] parseDimExprs() throws CompileException, IOException {
        List l = new ArrayList();
        l.add(this.parseDimExpr());
        while (this.peek("[") && !this.peekNextButOne("]")) {
            l.add(this.parseDimExpr());
        }
        return (Rvalue[]) l.toArray(new Rvalue[l.size()]);
    }

    /**
     * <pre>
     *   DimExpr := '[' Expression ']'
     * </pre>
     */
    public Rvalue parseDimExpr() throws CompileException, IOException {
        this.read("[");
        Rvalue res = this.parseExpression().toRvalueOrPE();
        this.read("]");
        return res;
    }

    /**
     * <pre>
     *   Arguments := '(' [ ArgumentList ] ')'
     * </pre>
     */
    public Rvalue[] parseArguments() throws CompileException, IOException {
        this.read("(");
        if (this.peekRead(")")) return new Rvalue[0];
        Rvalue[] arguments = this.parseArgumentList();
        this.read(")");
        return arguments;
    }

    /**
     * <pre>
     *   ArgumentList := Expression { ',' Expression }
     * </pre>
     */
    public Rvalue[] parseArgumentList() throws CompileException, IOException {
        List l = new ArrayList();
        do {
            l.add(this.parseExpression().toRvalueOrPE());
        } while (this.peekRead(","));
        return (Rvalue[]) l.toArray(new Rvalue[l.size()]);
    }

    public Rvalue parseLiteral() throws CompileException, IOException {
        Token t = this.read();
        switch (t.type) {
        case Token.INTEGER_LITERAL:        return new IntegerLiteral(t.getLocation(), t.value);
        case Token.FLOATING_POINT_LITERAL: return new FloatingPointLiteral(t.getLocation(), t.value);
        case Token.BOOLEAN_LITERAL:        return new BooleanLiteral(t.getLocation(), t.value);
        case Token.CHARACTER_LITERAL:      return new CharacterLiteral(t.getLocation(), t.value);
        case Token.STRING_LITERAL:         return new StringLiteral(t.getLocation(), t.value);
        case Token.NULL_LITERAL:           return new NullLiteral(t.getLocation(), t.value);
        default:
            throw this.compileException("Literal expected");
        }
    }

    /**
     * <pre>
     *   ExpressionStatement := Expression ';'
     * </pre>
     */
    public Statement parseExpressionStatement() throws CompileException, IOException {
        Rvalue rv = this.parseExpression().toRvalueOrPE();
        this.read(";");

        return new ExpressionStatement(rv);
    }

    public Location location() {
        return this.scanner.location();
    }

    private Token nextToken, nextButOneToken;

    // Token-level methods.

    public Token peek() throws CompileException, IOException {
        if (this.nextToken == null) this.nextToken = this.scanner.produce();
        return this.nextToken;
    }

    public Token peekNextButOne() throws CompileException, IOException {
        if (this.nextToken == null) this.nextToken = this.scanner.produce();
        if (this.nextButOneToken == null) this.nextButOneToken = this.scanner.produce();
        return this.nextButOneToken;
    }

    public Token read() throws CompileException, IOException {
        if (this.nextToken == null) return this.scanner.produce();
        Token result = this.nextToken;
        this.nextToken = this.nextButOneToken;
        this.nextButOneToken = null;
        return result;
    }

    // Peek/read/peekRead convenience methods.

    public boolean peek(String value) throws CompileException, IOException {
        return this.peek().value.equals(value);
    }

    public int peek(String[] values) throws CompileException, IOException {
        return indexOf(values, this.peek().value);
    }

    public int peek(int[] types) throws CompileException, IOException {
        return indexOf(types, this.peek().type);
    }

    public boolean peekNextButOne(String value) throws CompileException, IOException {
        return this.peekNextButOne().value.equals(value);
    }

    public void read(String value) throws CompileException, IOException {
        String s = this.read().value;
        if (!s.equals(value)) throw this.compileException("'" + value + "' expected instead of '" + s + "'");
    }

    public int read(String[] values) throws CompileException, IOException {
        String s = this.read().value;
        int idx = indexOf(values, s);
        if (idx == -1) {
            throw this.compileException("One of '" + join(values, " ") + "' expected instead of '" + s + "'");
        }
        return idx;
    }

    public boolean peekRead(String value) throws CompileException, IOException {
        if (this.nextToken == null) {
            Token t = this.scanner.produce();
            if (t.value.equals(value)) return true;
            this.nextToken = t;
            return false;
        }
        if (!this.nextToken.value.equals(value)) return false;
        this.nextToken = this.nextButOneToken;
        this.nextButOneToken = null;
        return true;
    }

    /** @return -1 iff the next token is none of <code>values</code> */
    public int peekRead(String[] values) throws CompileException, IOException {
        if (this.nextToken == null) {
            Token t = this.scanner.produce();
            int idx = indexOf(values, t.value);
            if (idx != -1) return idx;
            this.nextToken = t;
            return -1;
        }
        int idx = indexOf(values, this.nextToken.value);
        if (idx == -1) return -1;
        this.nextToken = this.nextButOneToken;
        this.nextButOneToken = null;
        return idx;
    }

    public boolean peekEOF() throws CompileException, IOException {
        return this.peek().type == Token.EOF;
    }

    public String peekIdentifier() throws CompileException, IOException {
        Token t = this.peek();
        return t.type == Token.IDENTIFIER ? t.value : null;
    }

    public boolean peekLiteral() throws CompileException, IOException {
        return this.peek(new int[] {
            Token.INTEGER_LITERAL, Token.FLOATING_POINT_LITERAL, Token.BOOLEAN_LITERAL, Token.CHARACTER_LITERAL,
            Token.STRING_LITERAL, Token.NULL_LITERAL,
       }) != -1;
    }

    public String readIdentifier() throws CompileException, IOException {
        Token t = this.read();
        if (t.type != Token.IDENTIFIER) throw this.compileException("Identifier expected instead of '" + t.value + "'");
        return t.value;
    }

    public String readOperator() throws CompileException, IOException {
        Token t = this.read();
        if (t.type != Token.OPERATOR) throw this.compileException("Operator expected instead of '" + t.value + "'");
        return t.value;
    }

    private static int indexOf(String[] strings, String subject) {
        for (int i = 0; i < strings.length; ++i) {
            if (strings[i].equals(subject)) return i;
        }
        return -1;
    }

    private static int indexOf(int[] values, int subject) {
        for (int i = 0; i < values.length; ++i) {
            if (values[i] == subject) return i;
        }
        return -1;
    }

    /**
     * Issue a warning if the given string does not comply with the package naming conventions
     * (JLS2 6.8.1).
     */
    private void verifyStringIsConventionalPackageName(String s, Location loc) {
        if (!Character.isLowerCase(s.charAt(0))) {
            this.warning(
                "UPN",
                "Package name \"" + s + "\" does not begin with a lower-case letter (see JLS2 6.8.1)",
                loc
            );
            return;
        }

        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (!Character.isLowerCase(c) && c != '_' && c != '.') {
                this.warning("PPN", "Poorly chosen package name \"" + s + "\" contains bad character '" + c + "'", loc);
                return;
            }
        }
    }

    /**
     * Issue a warning if the given identifier does not comply with the class and interface type
     * naming conventions (JLS2 6.8.2).
     */
    private void verifyIdentifierIsConventionalClassOrInterfaceName(String id, Location loc) {
        if (!Character.isUpperCase(id.charAt(0))) {
            this.warning(
                "UCOIN1",
                "Class or interface name \"" + id + "\" does not begin with an upper-case letter (see JLS2 6.8.2)",
                loc
            );
            return;
        }
        for (int i = 0; i < id.length(); ++i) {
            char c = id.charAt(i);
            if (!Character.isLetter(c) && !Character.isDigit(c)) {
                this.warning("UCOIN", (
                    "Class or interface name \""
                    + id
                    + "\" contains unconventional character \""
                    + c
                    + "\" (see JLS2 6.8.2)"
                ), loc);
                return;
            }
        }
    }

    /**
     * Issue a warning if the given identifier does not comply with the method naming conventions
     * (JLS2 6.8.3).
     */
    private void verifyIdentifierIsConventionalMethodName(String id, Location loc) {
        if (!Character.isLowerCase(id.charAt(0))) {
            this.warning(
                "UMN1",
                "Method name \"" + id + "\" does not begin with a lower-case letter (see JLS2 6.8.3)",
                loc
            );
            return;
        }
        for (int i = 0; i < id.length(); ++i) {
            char c = id.charAt(i);
            if (!Character.isLetter(c) && !Character.isDigit(c)) {
                this.warning(
                    "UMN",
                    "Method name \"" + id + "\" contains unconventional character \"" + c + "\" (see JLS2 6.8.3)",
                    loc
                );
                return;
            }
        }
    }

    /**
     * Issue a warning if the given identifier does not comply with the field naming conventions
     * (JLS2 6.8.4) and constant naming conventions (JLS2 6.8.5).
     */
    private void verifyIdentifierIsConventionalFieldName(String id, Location loc) {

        // In practice, a field is not always a constant iff it is static-final. So let's
        // always tolerate both field and constant names.

        if (Character.isUpperCase(id.charAt(0))) {
            for (int i = 0; i < id.length(); ++i) {
                char c = id.charAt(i);
                if (!Character.isUpperCase(c) && !Character.isDigit(c) && c != '_') {
                    this.warning(
                        "UCN",
                        "Constant name \"" + id + "\" contains unconventional character \"" + c + "\" (see JLS2 6.8.5)",
                        loc
                    );
                    return;
                }
            }
        } else
        if (Character.isLowerCase(id.charAt(0))) {
            for (int i = 0; i < id.length(); ++i) {
                char c = id.charAt(i);
                if (!Character.isLetter(c) && !Character.isDigit(c)) {
                    this.warning(
                        "UFN",
                        "Field name \"" + id + "\" contains unconventional character \"" + c + "\" (see JLS2 6.8.4)",
                        loc
                    );
                    return;
                }
            }
        } else {
            this.warning("UFN1", (
                "\""
                + id
                + "\" is neither a conventional field name (JLS2 6.8.4) nor a conventional constant name (JLS2 6.8.5)"
            ), loc);
        }
    }

    /**
     * Issue a warning if the given identifier does not comply with the local variable and
     * parameter naming conventions (JLS2 6.8.6).
     */
    private void verifyIdentifierIsConventionalLocalVariableOrParameterName(String id, Location loc) {
        if (!Character.isLowerCase(id.charAt(0))) {
            this.warning(
                "ULVN1",
                "Local variable name \"" + id + "\" does not begin with a lower-case letter (see JLS2 6.8.6)",
                loc
            );
            return;
        }
        for (int i = 0; i < id.length(); ++i) {
            char c = id.charAt(i);
            if (!Character.isLetter(c) && !Character.isDigit(c)) {
                this.warning("ULVN", (
                    "Local variable name \""
                    + id
                    + "\" contains unconventional character \""
                    + c
                    + "\" (see JLS2 6.8.6)"
                ), loc);
                return;
            }
        }
    }

    /**
     * By default, warnings are discarded, but an application my install a {@link WarningHandler}.
     * <p>
     * Notice that there is no <code>Parser.setErrorHandler()</code> method, but parse errors always throw a {@link
     * CompileException}. The reason being is that there is no reasonable way to recover from parse errors and continue
     * parsing, so there is no need to install a custom parse error handler.
     *
     * @param optionalWarningHandler <code>null</code> to indicate that no warnings be issued
     */
    public void setWarningHandler(WarningHandler optionalWarningHandler) {
        this.optionalWarningHandler = optionalWarningHandler;
    }

    // Used for elaborate warning handling.
    private WarningHandler optionalWarningHandler = null;

    /**
     * Issues a warning with the given message and location and returns. This is done through
     * a {@link WarningHandler} that was installed through
     * {@link #setWarningHandler(WarningHandler)}.
     * <p>
     * The <code>handle</code> argument qulifies the warning and is typically used by
     * the {@link WarningHandler} to suppress individual warnings.
     */
    private void warning(String handle, String message, Location optionalLocation) {
        if (this.optionalWarningHandler != null) {
            this.optionalWarningHandler.handleWarning(handle, message, optionalLocation);
        }
    }

    /**
     * Convenience method for throwing a CompileException.
     */
    protected final CompileException compileException(String message) {
        return new CompileException(message, this.location());
    }

    private static String join(String[] sa, String separator) {
        if (sa == null) return ("(null)");
        if (sa.length == 0) return ("(zero length array)");
        StringBuffer sb = new StringBuffer(sa[0]);
        for (int i = 1; i < sa.length; ++i) {
            sb.append(separator).append(sa[i]);
        }
        return sb.toString();
    }
}
