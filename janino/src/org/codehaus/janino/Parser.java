
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

package org.codehaus.janino;

import java.io.*;
import java.util.*;

import org.codehaus.janino.util.enumerator.Enumerator;

/**
 * A parser for the Java<sup>TM</sup> programming language.
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
    public Java.CompilationUnit parseCompilationUnit() throws ParseException, Scanner.ScanException, IOException {
        Java.CompilationUnit compilationUnit = new Java.CompilationUnit(this.location().getFileName());

        if (this.peekKeyword("package")) {
            compilationUnit.setPackageDeclaration(this.parsePackageDeclaration());
        }

        while (this.peekKeyword("import")) {
            compilationUnit.addImportDeclaration(this.parseImportDeclaration());
        }

        while (!this.scanner.peek().isEOF()) {
            if (this.peekOperator(";")) {
                this.eatToken();
            } else
            {
                compilationUnit.addPackageMemberTypeDeclaration(this.parsePackageMemberTypeDeclaration());
            }
        }

        return compilationUnit;
    }

    /**
     * <pre>
     *   PackageDeclaration := 'package' QualifiedIdentifier ';'
     * </pre>
     */
    public Java.PackageDeclaration parsePackageDeclaration() throws Parser.ParseException, Scanner.ScanException, IOException {
        this.readKeyword("package");
        Location loc = this.location();
        String packageName = Parser.join(this.parseQualifiedIdentifier(), ".");
        this.readOperator(";");
        this.verifyStringIsConventionalPackageName(packageName, loc);
        return new Java.PackageDeclaration(loc, packageName);
    }

    /**
     * <pre>
     *   ImportDeclaration := 'import' ImportDeclarationBody ';'
     * </pre>
     */
    public Java.CompilationUnit.ImportDeclaration parseImportDeclaration() throws ParseException, Scanner.ScanException, IOException {
        this.readKeyword("import");
        Java.CompilationUnit.ImportDeclaration importDeclaration = this.parseImportDeclarationBody();
        this.readOperator(";");
        return importDeclaration;
    }

    /**
     * <pre>
     *   ImportDeclarationBody := Identifier { '.' Identifier } [ '.' '*' ]
     * </pre>
     */
    public Java.CompilationUnit.ImportDeclaration parseImportDeclarationBody() throws ParseException, Scanner.ScanException, IOException {
        Location loc = this.location();
        List l = new ArrayList();
        l.add(this.readIdentifier());
        for (;;) {
            if (!this.peekOperator(".")) {
                return new Java.CompilationUnit.SingleTypeImportDeclaration(loc, (String[]) l.toArray(new String[l.size()]));
            }
            this.readOperator(".");
            if (this.peekOperator("*")) {
                this.eatToken();
                return new Java.CompilationUnit.TypeImportOnDemandDeclaration(loc, (String[]) l.toArray(new String[l.size()]));
            }
            l.add(this.readIdentifier());
        }
    }

    /**
     * QualifiedIdentifier := Identifier { '.' Identifier }
     */
    public String[] parseQualifiedIdentifier() throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isIdentifier()) this.throwParseException("Identifier expected");
        List l = new ArrayList();
        l.add(this.readIdentifier());
        while (this.peekOperator(".") && this.scanner.peekNextButOne().isIdentifier()) {
            this.eatToken();
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
    public Java.PackageMemberTypeDeclaration parsePackageMemberTypeDeclaration() throws ParseException, Scanner.ScanException, IOException {
        String optionalDocComment = this.scanner.doc();

        short modifiers = this.parseModifiersOpt();

        Java.PackageMemberTypeDeclaration res;
        if (this.peekKeyword("class")) {
            if (optionalDocComment == null) this.warning("CDCM", "Class doc comment missing", this.location());
            this.eatToken();
            res = (Java.PackageMemberClassDeclaration) this.parseClassDeclarationRest(
                optionalDocComment,                      // optionalDocComment
                modifiers,                               // modifiers
                ClassDeclarationContext.COMPILATION_UNIT // context
            );
        } else
        if (this.peekKeyword("interface")) {
            if (optionalDocComment == null) this.warning("IDCM", "Interface doc comment missing", this.location());
            this.eatToken();
            res = (Java.PackageMemberInterfaceDeclaration) this.parseInterfaceDeclarationRest(
                optionalDocComment,                          // optionalDocComment
                modifiers,                                   // modifiers
                InterfaceDeclarationContext.COMPILATION_UNIT // context
            );
        } else
        {
            this.throwParseException("Unexpected token \"" + this.scanner.peek() + "\" in class or interface declaration");
            /* NEVER REACHED */ return null;
        }
        return res;
    }

    /**
     * <pre>
     *   ModifiersOpt := { 'public' | 'protected' | 'private' | 'static' |
     *           'abstract' | 'final' | 'native' | 'synchronized' |
     *           'transient' | 'volatile' | 'strictfp'
     * </pre>
     */
    public short parseModifiersOpt() throws ParseException, Scanner.ScanException, IOException {
        short mod = 0;
        while (this.peekKeyword()) {
            String kw = this.scanner.peek().getKeyword();
            short x = (short) (
                kw == "public"       ? Mod.PUBLIC       :
                kw == "protected"    ? Mod.PROTECTED    :
                kw == "private"      ? Mod.PRIVATE      :
                kw == "static"       ? Mod.STATIC       :
                kw == "abstract"     ? Mod.ABSTRACT     :
                kw == "final"        ? Mod.FINAL        :
                kw == "native"       ? Mod.NATIVE       :
                kw == "synchronized" ? Mod.SYNCHRONIZED :
                kw == "transient"    ? Mod.TRANSIENT    :
                kw == "volatile"     ? Mod.VOLATILE     :
                kw == "strictfp"     ? Mod.STRICTFP     :
                -1
            );
            if (x == -1) break;

            this.eatToken();
            if ((mod & x) != 0) this.throwParseException("Duplicate modifier \"" + kw + "\"");
            for (int i = 0; i < Parser.MUTUALS.length; ++i) {
                short m = Parser.MUTUALS[i];
                if ((x & m) != 0 && (mod & m) != 0) {
                    this.throwParseException("Only one of \"" + Mod.shortToString(m) + "\" allowed");
                }
            }
            mod |= x;
        }
        return mod;
    }
    private static final short[] MUTUALS = {
        Mod.PUBLIC | Mod.PROTECTED | Mod.PRIVATE,
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
    public Java.NamedClassDeclaration parseClassDeclarationRest(
        String                  optionalDocComment,
        short                   modifiers,
        ClassDeclarationContext context
    ) throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        String className = this.readIdentifier();
        this.verifyIdentifierIsConventionalClassOrInterfaceName(className, location);

        Java.ReferenceType optionalExtendedType = null;
        if (this.peekKeyword("extends")) {
            this.eatToken();
            optionalExtendedType = this.parseReferenceType();
        }

        Java.ReferenceType[] implementedTypes = new Java.ReferenceType[0];
        if (this.peekKeyword("implements")) {
            this.eatToken();
            implementedTypes = this.parseReferenceTypeList();
        }

        Java.NamedClassDeclaration namedClassDeclaration;
        if (context == ClassDeclarationContext.COMPILATION_UNIT) {
            namedClassDeclaration = new Java.PackageMemberClassDeclaration(
                location,                              // location
                optionalDocComment,                    // optionalDocComment
                modifiers,                             // modifiers
                className,                             // name
                optionalExtendedType,                  // optinalExtendedType
                implementedTypes                       // implementedTypes
            );
        } else
        if (context == ClassDeclarationContext.TYPE_DECLARATION) {
            namedClassDeclaration = new Java.MemberClassDeclaration(
                location,             // location
                optionalDocComment,   // optionalDocComment
                modifiers,            // modifiers
                className,            // name
                optionalExtendedType, // optionalExtendedType
                implementedTypes      // implementedTypes
            );
        } else
        if (context == ClassDeclarationContext.BLOCK) {
            namedClassDeclaration = new Java.LocalClassDeclaration(
                location,             // location
                optionalDocComment,   // optionalDocComment
                modifiers,            // modifiers
                className,            // name
                optionalExtendedType, // optionalExtendedType
                implementedTypes      // implementedTypes
            );
        } else
        {
            throw new RuntimeException("SNO: Class declaration in unexpected context " + context);
        }

        this.parseClassBody(namedClassDeclaration);
        return namedClassDeclaration;
    }
    private static class ClassDeclarationContext extends Enumerator {
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
    public void parseClassBody(
        Java.ClassDeclaration classDeclaration
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.peekOperator("{")) this.throwParseException("\"{\" expected at start of class body");
        this.eatToken();

        for (;;) {
            if (this.peekOperator("}")) {
                this.eatToken();
                return;
            }

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
    public void parseClassBodyDeclaration(
        Java.ClassDeclaration classDeclaration
    ) throws ParseException, Scanner.ScanException, IOException {
        if (this.peekOperator(";")) {
            this.eatToken();
            return;
        }

        String optionalDocComment = this.scanner.doc();
        short modifiers = this.parseModifiersOpt();

        // Initializer?
        if (this.peekOperator("{")) {
            if ((modifiers & ~Mod.STATIC) != 0) this.throwParseException("Only modifier \"static\" allowed on initializer");

            Java.Initializer initializer = new Java.Initializer(
                this.location(),               // location
                (modifiers & Mod.STATIC) != 0, // statiC
                this.parseBlock()              // block
            );

            classDeclaration.addVariableDeclaratorOrInitializer(initializer);
            return;
        }

        // "void" method declaration.
        if (this.peekKeyword("void")) {
            Location location = this.location();
            this.eatToken();
            if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", location);
            String name = this.readIdentifier();
            classDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                classDeclaration,                                  // declaringType
                optionalDocComment,                                // optionalDocComment
                modifiers,                                         // modifiers
                new Java.BasicType(location, Java.BasicType.VOID), // type
                name                                               // name
            ));
            return;
        }

        // Member class.
        if (this.peekKeyword("class")) {
            if (optionalDocComment == null) this.warning("MCDCM", "Member class doc comment missing", this.location());
            this.eatToken();
            classDeclaration.addMemberTypeDeclaration((Java.MemberTypeDeclaration) this.parseClassDeclarationRest(
                optionalDocComment,                      // optionalDocComment
                modifiers,                               // modifiers
                ClassDeclarationContext.TYPE_DECLARATION // context
            ));
            return;
        }

        // Member interface.
        if (this.peekKeyword("interface")) {
            if (optionalDocComment == null) this.warning("MIDCM", "Member interface doc comment missing", this.location());
            this.eatToken();
            classDeclaration.addMemberTypeDeclaration((Java.MemberTypeDeclaration) this.parseInterfaceDeclarationRest(
                optionalDocComment,                                // optionalDocComment
                (short) (modifiers | Mod.STATIC),                  // modifiers
                InterfaceDeclarationContext.NAMED_TYPE_DECLARATION // context
            ));
            return;
        }

        // Constructor.
        if (
            classDeclaration instanceof Java.NamedClassDeclaration &&
            this.scanner.peek().isIdentifier(((Java.NamedClassDeclaration) classDeclaration).getName()) &&
            this.scanner.peekNextButOne().isOperator("(")
        ) {
            if (optionalDocComment == null) this.warning("CDCM", "Constructor doc comment missing", this.location());
            classDeclaration.addConstructor(this.parseConstructorDeclarator(
                classDeclaration,   // declaringClass
                optionalDocComment, // optionalDocComment
                modifiers           // modifiers
            ));
            return;
        }

        // Member method or field.
        Java.Type memberType = this.parseType();
        Location location = this.location();
        String memberName = this.readIdentifier();

        // Method declarator.
        if (this.peekOperator("(")) {
            if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.location());
            classDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                classDeclaration,   // declaringType
                optionalDocComment, // optionalDocComment
                modifiers,          // modifiers
                memberType,         // type
                memberName          // name
            ));
            return;
        }

        // Field declarator.
        if (optionalDocComment == null) this.warning("FDCM", "Field doc comment missing", this.location());
        Java.FieldDeclaration fd = new Java.FieldDeclaration(
            location,                                  // location
            optionalDocComment,                        // optionalDocComment
            modifiers,                                 // modifiers
            memberType,                                // type
            this.parseFieldDeclarationRest(memberName) // variableDeclarators
        );
        this.readOperator(";");
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
    public Java.InterfaceDeclaration parseInterfaceDeclarationRest(
        String                      optionalDocComment,
        short                       modifiers,
        InterfaceDeclarationContext context
    ) throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        String interfaceName = this.readIdentifier();
        this.verifyIdentifierIsConventionalClassOrInterfaceName(interfaceName, location);

        Java.ReferenceType[] extendedTypes = new Java.ReferenceType[0];
        if (this.peekKeyword("extends")) {
            this.eatToken();
            extendedTypes = this.parseReferenceTypeList();
        }

        Java.InterfaceDeclaration interfaceDeclaration;
        if (context == InterfaceDeclarationContext.COMPILATION_UNIT) {
            interfaceDeclaration = new Java.PackageMemberInterfaceDeclaration(
                location,                              // location
                optionalDocComment,                    // optionalDocComment
                modifiers,                             // modifiers
                interfaceName,                         // name
                extendedTypes                          // extendedTypes
            );
        } else
        if (context == InterfaceDeclarationContext.NAMED_TYPE_DECLARATION) {
            interfaceDeclaration = new Java.MemberInterfaceDeclaration(
                location,                                   // location
                optionalDocComment,                         // optionalDocComment
                modifiers,                                  // modifiers
                interfaceName,                              // name
                extendedTypes                               // extendedTypes
            );
        } else
        {
            throw new RuntimeException("SNO: Interface declaration in unexpected context " + context);
        }

        this.parseInterfaceBody(interfaceDeclaration);
        return interfaceDeclaration;
    }
    private static class InterfaceDeclarationContext extends Enumerator {
        public static final InterfaceDeclarationContext NAMED_TYPE_DECLARATION = new InterfaceDeclarationContext("named_type_declaration");
        public static final InterfaceDeclarationContext COMPILATION_UNIT       = new InterfaceDeclarationContext("compilation_unit");
        private InterfaceDeclarationContext(String name) { super(name); }
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
    public void parseInterfaceBody(
        Java.InterfaceDeclaration interfaceDeclaration
    ) throws ParseException, Scanner.ScanException, IOException {
        this.readOperator("{");

        for (;;) {
            if (this.peekOperator("}")) {
                this.eatToken();
                break;
            }

            if (this.peekOperator(";")) {
                this.eatToken();
                continue;
            }

            String optionalDocComment = this.scanner.doc();
            short modifiers = this.parseModifiersOpt();

            // "void" method declaration.
            if (this.peekKeyword("void")) {
                if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.location());
                Location location = this.location();
                this.eatToken();
                String name = this.readIdentifier();
                interfaceDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                    interfaceDeclaration,                              // declaringType
                    optionalDocComment,                                // optionalDocComment
                    (short) (modifiers | Mod.ABSTRACT | Mod.PUBLIC),   // modifiers
                    new Java.BasicType(location, Java.BasicType.VOID), // type
                    name                                               // name
                ));
            } else

            // Member class.
            if (this.peekKeyword("class")) {
                if (optionalDocComment == null) this.warning("MCDCM", "Member class doc comment missing", this.location());
                this.eatToken();
                interfaceDeclaration.addMemberTypeDeclaration((Java.MemberTypeDeclaration) this.parseClassDeclarationRest(
                    optionalDocComment,                            // optionalDocComment
                    (short) (modifiers | Mod.STATIC | Mod.PUBLIC), // modifiers
                    ClassDeclarationContext.TYPE_DECLARATION       // context
                ));
            } else

            // Member interface.
            if (this.peekKeyword("interface")) {
                if (optionalDocComment == null) this.warning("MIDCM", "Member interface doc comment missing", this.location());
                this.eatToken();
                interfaceDeclaration.addMemberTypeDeclaration((Java.MemberTypeDeclaration) this.parseInterfaceDeclarationRest(
                    optionalDocComment,                                // optionalDocComment
                    (short) (modifiers | Mod.STATIC | Mod.PUBLIC),     // modifiers
                    InterfaceDeclarationContext.NAMED_TYPE_DECLARATION // context
                ));
            } else

            // Member method or field.
            {
                Java.Type memberType = this.parseType();
                if (!this.scanner.peek().isIdentifier()) this.throwParseException("Identifier expected in member declaration");
                Location location = this.location();
                String memberName = this.readIdentifier();

                // Method declarator.
                if (this.peekOperator("(")) {
                    if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.location());
                    interfaceDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                        interfaceDeclaration,                            // declaringType
                        optionalDocComment,                              // optionalDocComment
                        (short) (modifiers | Mod.ABSTRACT | Mod.PUBLIC), // modifiers
                        memberType,                                      // type
                        memberName                                       // name
                    ));
                } else

                // Field declarator.
                {
                    if (optionalDocComment == null) this.warning("FDCM", "Field doc comment missing", this.location());
                    Java.FieldDeclaration fd = new Java.FieldDeclaration(
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
    public Java.ConstructorDeclarator parseConstructorDeclarator(
        Java.ClassDeclaration declaringClass,
        String                optionalDocComment,
        short                 modifiers
    ) throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readIdentifier();  // Class name

        // Parse formal parameters.
        Java.FunctionDeclarator.FormalParameter[] formalParameters = this.parseFormalParameters(
            declaringClass // enclosingScope
        );

        // Parse "throws" clause.
        Java.ReferenceType[] thrownExceptions;
        if (this.peekKeyword("throws")) {
            this.eatToken();
            thrownExceptions = this.parseReferenceTypeList();
        } else {
            thrownExceptions = new Java.ReferenceType[0];
        }

        // Parse constructor body.
        location = this.location();
        this.readOperator("{");

        // Special treatment for the first statement of the constructor body: If this is surely an
        // expression statement, and if it could be a "ConstructorInvocation", then parse the
        // expression and check if it IS a ConstructorInvocation.
        Java.ConstructorInvocation optionalConstructorInvocation = null;
        Java.Block body = new Java.Block(location);
        if (
            this.peekKeyword(new String[] {
                "this", "super", "new", "void",
                "byte", "char", "short", "int", "long", "float", "double", "boolean",
            } ) ||
            this.scanner.peek().isLiteral() ||
            this.scanner.peek().isIdentifier()
        ) {
            Java.Atom a = this.parseExpression();
            if (a instanceof Java.ConstructorInvocation) {
                this.readOperator(";");
                optionalConstructorInvocation = (Java.ConstructorInvocation) a;
            } else {
                Java.Statement s;
                if (this.scanner.peek().isIdentifier()) {
                    Java.Type variableType = a.toTypeOrPE();
                    s = new Java.LocalVariableDeclarationStatement(
                        a.getLocation(),                     // location
                        (short) 0,                           // modifiers
                        variableType,                        // type
                        this.parseLocalVariableDeclarators() // variableDeclarators
                    );
                    this.readOperator(";");
                } else {
                    s = new Java.ExpressionStatement(a.toRvalueOrPE());
                    this.readOperator(";");
                }
                body.addStatement(s);
            }
        }
        body.addStatements(this.parseBlockStatements());

        this.readOperator("}");

        return new Java.ConstructorDeclarator(
            location,                      // location
            optionalDocComment,            // optionalDocComment
            modifiers,                     // modifiers
            formalParameters,              // formalParameters
            thrownExceptions,              // thrownExceptions
            optionalConstructorInvocation, // optionalConstructorInvocationStatement
            body                           // body
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
    public Java.MethodDeclarator parseMethodDeclarationRest(
        Java.AbstractTypeDeclaration declaringType,
        String                       optionalDocComment,
        short                        modifiers,
        Java.Type                    type,
        String                       name
    ) throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();

        this.verifyIdentifierIsConventionalMethodName(name, location);

        Java.FunctionDeclarator.FormalParameter[] formalParameters = this.parseFormalParameters(
            declaringType // enclosingScope
        );

        for (int i = this.parseBracketsOpt(); i > 0; --i) type = new Java.ArrayType(type);

        Java.ReferenceType[] thrownExceptions;
        if (this.peekKeyword("throws")) {
            this.eatToken();
            thrownExceptions = this.parseReferenceTypeList();
        } else {
            thrownExceptions = new Java.ReferenceType[0];
        }

        Java.Block optionalBody;
        if (this.peekOperator(";")) {
            if ((modifiers & (Mod.ABSTRACT | Mod.NATIVE)) == 0) this.throwParseException("Non-abstract, non-native method must have a body");
            this.eatToken();
            optionalBody = null;
        } else {
            if ((modifiers & (Mod.ABSTRACT | Mod.NATIVE)) != 0) this.throwParseException("Abstract or native method must not have a body");
            optionalBody = this.parseMethodBody();
        }
        return new Java.MethodDeclarator(
            location,           // location
            optionalDocComment, // optionalDocComment
            modifiers,          // modifiers
            type,               // type
            name,               // name
            formalParameters,   // formalParameters
            thrownExceptions,   // thrownExceptions
            optionalBody        // optionalBody
        );
    }

    /**
     * <pre>
     *   VariableInitializer :=
     *     ArrayInitializer |
     *     Expression
     * </pre>
     */
    public Java.ArrayInitializerOrRvalue parseVariableInitializer() throws ParseException, Scanner.ScanException, IOException {
        if (this.peekOperator("{")) {
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
    public Java.ArrayInitializer parseArrayInitializer() throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readOperator("{");
        List l = new ArrayList(); // ArrayInitializerOrRvalue
        while (!this.peekOperator("}")) {
            l.add(this.parseVariableInitializer());
            if (this.peekOperator("}")) break;
            if (!this.peekOperator(",")) this.throwParseException("\",\" or \"}\" expected");
            this.eatToken();
        }
        this.eatToken();
        return new Java.ArrayInitializer(
            location,
            (Java.ArrayInitializerOrRvalue[]) l.toArray(new Java.ArrayInitializerOrRvalue[l.size()])
        );
    }

    /**
     * <pre>
     *   FormalParameters := '(' [ FormalParameter { ',' FormalParameter } ] ')'
     * </pre>
     */
    public Java.FunctionDeclarator.FormalParameter[] parseFormalParameters(
        Java.Scope enclosingScope
    ) throws ParseException, Scanner.ScanException, IOException {
        this.readOperator("(");
        if (this.peekOperator(")")) {
            this.eatToken();
            return new Java.FunctionDeclarator.FormalParameter[0];
        }

        List l = new ArrayList(); // Java.FormalParameter
        for (;;) {
            l.add(this.parseFormalParameter());
            if (!this.peekOperator(",")) break;
            this.eatToken();
        }
        this.readOperator(")");
        return (Java.FunctionDeclarator.FormalParameter[]) l.toArray(new Java.FunctionDeclarator.FormalParameter[l.size()]);
    }

    /**
     * <pre>
     *   FormalParameter := [ 'final' ] Type Identifier BracketsOpt
     * </pre>
     */
    public Java.FunctionDeclarator.FormalParameter parseFormalParameter() throws ParseException, Scanner.ScanException, IOException {
        boolean finaL = this.peekKeyword("final");
        if (finaL) this.eatToken();

        Java.Type type = this.parseType();

        Location location = this.location();
        String name = this.readIdentifier();
        this.verifyIdentifierIsConventionalLocalVariableOrParameterName(name, location);

        for (int i = this.parseBracketsOpt(); i > 0; --i) type = new Java.ArrayType(type);
        return new Java.FunctionDeclarator.FormalParameter(location, finaL, type, name);
    }

    /**
     * <pre>
     *   BracketsOpt := { '[' ']' }
     * </pre>
     */
    int parseBracketsOpt() throws Scanner.ScanException, IOException {
        int res = 0;
        while (
            this.scanner.peek().          isOperator("[") &&
            this.scanner.peekNextButOne().isOperator("]")
        ) {
            this.eatToken();
            this.eatToken();
            ++res;
        }
        return res;
    }

    /**
     * <pre>
     *   MethodBody := Block
     * </pre>
     */
    public Java.Block parseMethodBody() throws ParseException, Scanner.ScanException, IOException {
        return this.parseBlock();
    }

    /**
     * <pre>
     *   '{' BlockStatements '}'
     * </pre>
     */
    public Java.Block parseBlock() throws ParseException, Scanner.ScanException, IOException {
        Java.Block block = new Java.Block(this.location());
        this.readOperator("{");
        block.addStatements(this.parseBlockStatements());
        this.readOperator("}");
        return block;
    }

    /**
     * <pre>
     *   BlockStatements := { BlockStatement }
     * </pre>
     */
    public List parseBlockStatements() throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();
        while (
            !this.peekOperator("}") &&
            !this.peekKeyword("case") &&
            !this.peekKeyword("default")
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
    public Java.BlockStatement parseBlockStatement() throws ParseException, Scanner.ScanException, IOException {

        // Statement?
        if (
            (
                this.scanner.peek().isIdentifier() &&
                this.scanner.peekNextButOne().isOperator(":")
            ) ||
            this.peekKeyword(new String[] {
                "if", "for", "while", "do", "try", "switch", "synchronized",
                "return", "throw", "break", "continue"
            }) ||
            this.peekOperator(new String[] { "{", ";" })
        ) return this.parseStatement();

        // Local class declaration?
        if (this.peekKeyword("class")) {
            // JAVADOC[TM] ignores doc comments for local classes, but we
            // don't...
            String optionalDocComment = this.scanner.doc();
            if (optionalDocComment == null) this.warning("LCDCM", "Local class doc comment missing", this.location());

            this.eatToken();
            final Java.LocalClassDeclaration lcd = (Java.LocalClassDeclaration) this.parseClassDeclarationRest(
                optionalDocComment,                // optionalDocComment
                (short) (Mod.FINAL | Mod.PRIVATE), // modifiers
                ClassDeclarationContext.BLOCK      // context
            );
            return new Java.LocalClassDeclarationStatement(lcd);
        }

        // 'final' Type LocalVariableDeclarators ';'
        if (this.peekKeyword("final")) {
            Location location = this.location();
            this.eatToken();
            Java.Type variableType = this.parseType();
            Java.LocalVariableDeclarationStatement lvds = new Java.LocalVariableDeclarationStatement(
                location,                            // location
                Mod.FINAL,                           // modifiers
                variableType,                        // type
                this.parseLocalVariableDeclarators() // variableDeclarators
            );
            this.readOperator(";");
            return lvds;
        }

        // It's either a non-final local variable declaration or an expression statement. We can
        // only tell after parsing an expression.
        Java.Atom a = this.parseExpression();

        // Expression ';'
        if (this.peekOperator(";")) {
            this.eatToken();
            return new Java.ExpressionStatement(a.toRvalueOrPE());
        }

        // Expression LocalVariableDeclarators ';'
        Java.Type variableType = a.toTypeOrPE();
        Java.LocalVariableDeclarationStatement lvds = new Java.LocalVariableDeclarationStatement(
            a.getLocation(),                     // location
            Mod.NONE,                            // modifiers
            variableType,                        // type
            this.parseLocalVariableDeclarators() // variableDeclarators
        );
        this.readOperator(";");
        return lvds;
    }

    /**
     * <pre>
     *   LocalVariableDeclarators := VariableDeclarator { ',' VariableDeclarator }
     * </pre>
     */
    public Java.VariableDeclarator[] parseLocalVariableDeclarators() throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();
        for (;;) {
            Java.VariableDeclarator vd = this.parseVariableDeclarator();
            this.verifyIdentifierIsConventionalLocalVariableOrParameterName(vd.name, vd.getLocation());
            l.add(vd);
            if (!this.peekOperator(",")) break;
            this.eatToken();
        }
        return (Java.VariableDeclarator[]) l.toArray(new Java.VariableDeclarator[l.size()]);
    }

    /**
     * <pre>
     *   FieldDeclarationRest :=
     *     VariableDeclaratorRest
     *     { ',' VariableDeclarator }
     * </pre>
     */
    public Java.VariableDeclarator[] parseFieldDeclarationRest(String name) throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();

        Java.VariableDeclarator vd = this.parseVariableDeclaratorRest(name);
        this.verifyIdentifierIsConventionalFieldName(vd.name, vd.getLocation());
        l.add(vd);

        while (this.peekOperator(",")) {
            this.eatToken();

            vd = this.parseVariableDeclarator();
            this.verifyIdentifierIsConventionalFieldName(vd.name, vd.getLocation());
            l.add(vd);
        }
        return (Java.VariableDeclarator[]) l.toArray(new Java.VariableDeclarator[l.size()]);
    }

    /**
     * <pre>
     *   VariableDeclarator := Identifier VariableDeclaratorRest
     * </pre>
     */
    public Java.VariableDeclarator parseVariableDeclarator() throws ParseException, Scanner.ScanException, IOException {
        return this.parseVariableDeclaratorRest(this.readIdentifier());
    }

    /**
     * <pre>
     *   VariableDeclaratorRest := { '[' ']' } [ '=' VariableInitializer ]
     * </pre>
     * Used by field declarations and local variable declarations.
     */
    public Java.VariableDeclarator parseVariableDeclaratorRest(String name) throws ParseException, Scanner.ScanException, IOException  {
        Location loc = this.location();
        int brackets = this.parseBracketsOpt();
        Java.ArrayInitializerOrRvalue initializer = null;
        if (this.peekOperator("=")) {
            this.eatToken();
            initializer = this.parseVariableInitializer();
        }
        return new Java.VariableDeclarator(loc, name, brackets, initializer);
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
    public Java.Statement parseStatement() throws ParseException, Scanner.ScanException, IOException {
        if (
            this.scanner.peek().isIdentifier() &&
            this.scanner.peekNextButOne().isOperator(":")
        ) {
            return this.parseLabeledStatement();
        }

        Scanner.Token t = this.scanner.peek();
        Java.Statement stmt = (
            t.isOperator("{")           ? this.parseBlock() :
            t.isKeyword("if")           ? this.parseIfStatement() :
            t.isKeyword("for")          ? this.parseForStatement() :
            t.isKeyword("while")        ? this.parseWhileStatement() :
            t.isKeyword("do")           ? this.parseDoStatement() :
            t.isKeyword("try")          ? this.parseTryStatement() :
            t.isKeyword("switch")       ? this.parseSwitchStatement() :
            t.isKeyword("synchronized") ? this.parseSynchronizedStatement() :
            t.isKeyword("return")       ? this.parseReturnStatement() :
            t.isKeyword("throw")        ? this.parseThrowStatement() :
            t.isKeyword("break")        ? this.parseBreakStatement() :
            t.isKeyword("continue")     ? this.parseContinueStatement() :
            t.isOperator(";")           ? this.parseEmptyStatement() :
            this.parseExpressionStatement()
        );
        if (stmt == null) this.throwParseException("\"" + t.getKeyword() + "\" NYI");

        return stmt;
    }

    /**
     * <pre>
     *   LabeledStatement := Identifier ':' Statement
     * </pre>
     */
    public Java.Statement parseLabeledStatement() throws ParseException, Scanner.ScanException, IOException {
        String label = this.readIdentifier();
        this.readOperator(":");
        return new Java.LabeledStatement(
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
    public Java.Statement parseIfStatement() throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readKeyword("if");
        this.readOperator("(");
        final Java.Rvalue condition = this.parseExpression().toRvalueOrPE();
        this.readOperator(")");

        Java.Statement thenStatement = this.parseStatement();

        Java.Statement optionalElseStatement = null;
        if (this.peekKeyword("else")) {
            this.eatToken();
            optionalElseStatement = this.parseStatement();
        }

        return new Java.IfStatement(
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
    public Java.Statement parseForStatement() throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readKeyword("for");

        this.readOperator("(");

        Java.BlockStatement optionalInit = null;
        if (!this.peekOperator(";")) optionalInit = this.parseForInit();

        this.readOperator(";");

        Java.Rvalue optionalCondition = null;
        if (!this.peekOperator(";")) optionalCondition = this.parseExpression().toRvalueOrPE();

        this.readOperator(";");

        Java.Rvalue[] optionalUpdate = null;
        if (!this.peekOperator(")")) optionalUpdate = this.parseExpressionList();

        this.readOperator(")");

        // KLUDGE: Insert an implicit Block here.
//        Java.Block implicitBlock = new Java.Block(location);
        return /*Java.ForStatement forStatement =*/ new Java.ForStatement(
            location,             // location
            optionalInit,         // optionalInit
            optionalCondition,    // optionalCondition
            optionalUpdate,       // optionalUpdate
            this.parseStatement() // body
        );
//        implicitBlock.addStatement(forStatement);

//        return implicitBlock;
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
    private Java.BlockStatement parseForInit() throws ParseException, Scanner.ScanException, IOException {

        // Modifiers Type LocalVariableDeclarators
        // ModifiersOpt BasicType LocalVariableDeclarators
        if (this.peekKeyword(new String[] { "final", "byte", "short", "char", "int", "long", "float", "double", "boolean" })) {
            short modifiers = this.parseModifiersOpt();
            Java.Type variableType = this.parseType();
            return new Java.LocalVariableDeclarationStatement(
                this.location(),                     // location
                modifiers,                           // modifiers
                variableType,                        // type
                this.parseLocalVariableDeclarators() // variableDeclarators
            );
        }

        Java.Atom a = this.parseExpression();

        // Expression LocalVariableDeclarators
        if (this.scanner.peek().isIdentifier()) {
            Java.Type variableType = a.toTypeOrPE();
            return new Java.LocalVariableDeclarationStatement(
                a.getLocation(),                     // location
                Mod.NONE,                            // modifiers
                variableType,                        // type
                this.parseLocalVariableDeclarators() // variableDeclarators
            );
        }

        // Expression { ',' Expression }
        if (!this.peekOperator(",")) {
            return new Java.ExpressionStatement(a.toRvalueOrPE());
        }
        this.eatToken();
        List l = new ArrayList();
        l.add(new Java.ExpressionStatement(a.toRvalueOrPE()));
        for (;;) {
            l.add(new Java.ExpressionStatement(this.parseExpression().toRvalueOrPE()));
            if (!this.peekOperator(",")) break;
            this.eatToken();
        }

        Java.Block b = new Java.Block(a.getLocation());
        b.addStatements(l);
        return b;
    }

    /**
     * <pre>
     *   WhileStatement := 'while' '(' Expression ')' Statement
     * </pre>
     */
    public Java.Statement parseWhileStatement() throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readKeyword("while");

        this.readOperator("(");
        Java.Rvalue condition = this.parseExpression().toRvalueOrPE();
        this.readOperator(")");

        return new Java.WhileStatement(
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
    public Java.Statement parseDoStatement() throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readKeyword("do");

        Java.Statement body = this.parseStatement();

        this.readKeyword("while");
        this.readOperator("(");
        Java.Rvalue condition = this.parseExpression().toRvalueOrPE();
        this.readOperator(")");
        this.readOperator(";");

        return new Java.DoStatement(
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
    public Java.Statement parseTryStatement() throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readKeyword("try");

        Java.Block body = this.parseBlock();

        // { CatchClause }
        List ccs = new ArrayList();
        while (this.peekKeyword("catch")) {
            Location loc = this.location();
            this.eatToken();
            this.readOperator("(");
            Java.FunctionDeclarator.FormalParameter caughtException = this.parseFormalParameter();
            this.readOperator(")");
            ccs.add(new Java.CatchClause(
                loc,              // location
                caughtException,  // caughtException
                this.parseBlock() // body
            ));
        }
        Java.Block optionalFinally = null;
        if (this.peekKeyword("finally")) {
            this.eatToken();
            optionalFinally = this.parseBlock();
        }
        if (ccs.size() == 0 && optionalFinally == null) this.throwParseException("\"try\" statement must have at least one \"catch\" clause or a \"finally\" clause");

        return new Java.TryStatement(
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
    public Java.Statement parseSwitchStatement() throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readKeyword("switch");

        this.readOperator("(");
        Java.Rvalue condition = this.parseExpression().toRvalueOrPE();
        this.readOperator(")");

        this.readOperator("{");
        List sbsgs = new ArrayList();
        while (!this.peekOperator("}")) {
            Location location2 = this.location();
            boolean hasDefaultLabel = false;
            List caseLabels = new ArrayList();
            do {
                if (this.peekKeyword("case")) {
                    this.eatToken();
                    caseLabels.add(this.parseExpression().toRvalueOrPE());
                } else
                if (this.peekKeyword("default")) {
                    this.eatToken();
                    if (hasDefaultLabel) this.throwParseException("Duplicate \"default\" label");
                    hasDefaultLabel = true;
                } else {
                    this.throwParseException("\"case\" or \"default\" expected");
                }
                this.readOperator(":");
            } while (this.peekKeyword(new String[] { "case", "default" }));

            Java.SwitchStatement.SwitchBlockStatementGroup sbsg = new Java.SwitchStatement.SwitchBlockStatementGroup(
                location2, // location
                caseLabels, // caseLabels
                hasDefaultLabel, // hasDefaultLabel
                this.parseBlockStatements() // blockStatements
            );
            sbsgs.add(sbsg);
        }
        this.eatToken();
        return new Java.SwitchStatement(
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
    public Java.Statement parseSynchronizedStatement() throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readKeyword("synchronized");
        this.readOperator("(");
        Java.Rvalue expression = this.parseExpression().toRvalueOrPE();
        this.readOperator(")");
        return new Java.SynchronizedStatement(
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
    public Java.Statement parseReturnStatement() throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readKeyword("return");
        Java.Rvalue returnValue = this.peekOperator(";") ? null : this.parseExpression().toRvalueOrPE();
        this.readOperator(";");
        return new Java.ReturnStatement(location, returnValue);
    }

    /**
     * <pre>
     *   ThrowStatement := 'throw' Expression ';'
     * </pre>
     */
    public Java.Statement parseThrowStatement() throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readKeyword("throw");
        final Java.Rvalue expression = this.parseExpression().toRvalueOrPE();
        this.readOperator(";");

        return new Java.ThrowStatement(location, expression);
    }

    /**
     * <pre>
     *   BreakStatement := 'break' [ Identifier ] ';'
     * </pre>
     */
    public Java.Statement parseBreakStatement() throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readKeyword("break");
        String optionalLabel = null;
        if (this.scanner.peek().isIdentifier()) optionalLabel = this.readIdentifier();
        this.readOperator(";");
        return new Java.BreakStatement(location, optionalLabel);
    }

    /**
     * <pre>
     *   ContinueStatement := 'continue' [ Identifier ] ';'
     * </pre>
     */
    public Java.Statement parseContinueStatement() throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readKeyword("continue");
        String optionalLabel = null;
        if (this.scanner.peek().isIdentifier()) optionalLabel = this.readIdentifier();
        this.readOperator(";");
        return new Java.ContinueStatement(location, optionalLabel);
    }

    /**
     * <pre>
     *   EmptyStatement := ';'
     * </pre>
     */
    public Java.Statement parseEmptyStatement() throws ParseException, Scanner.ScanException, IOException {
        Location location = this.location();
        this.readOperator(";");
        return new Java.EmptyStatement(location);
    }

    /**
     * <pre>
     *   ExpressionList := Expression { ',' Expression }
     * </pre>
     */
    public Java.Rvalue[] parseExpressionList() throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();
        for (;;) {
            l.add(this.parseExpression().toRvalueOrPE());
            if (!this.peekOperator(",")) break;
            this.eatToken();
        }
        return (Java.Rvalue[]) l.toArray(new Java.Rvalue[l.size()]);
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
    public Java.Type parseType() throws ParseException, Scanner.ScanException, IOException {
        Scanner.Token t = this.scanner.peek();
        int bt = -1;
        if (t.isKeyword("byte"   )) { bt = Java.BasicType.BYTE   ; } else
        if (t.isKeyword("short"  )) { bt = Java.BasicType.SHORT  ; } else
        if (t.isKeyword("char"   )) { bt = Java.BasicType.CHAR   ; } else
        if (t.isKeyword("int"    )) { bt = Java.BasicType.INT    ; } else
        if (t.isKeyword("long"   )) { bt = Java.BasicType.LONG   ; } else
        if (t.isKeyword("float"  )) { bt = Java.BasicType.FLOAT  ; } else
        if (t.isKeyword("double" )) { bt = Java.BasicType.DOUBLE ; } else
        if (t.isKeyword("boolean")) { bt = Java.BasicType.BOOLEAN; }
        Java.Type res;
        if (bt != -1) {
            res = new Java.BasicType(t.getLocation(), bt);
            this.eatToken();
        } else {
            res = this.parseReferenceType();
        }
        for (int i = this.parseBracketsOpt(); i > 0; --i) res = new Java.ArrayType(res);
        return res;
    }

    /**
     * <pre>
     *   ReferenceType := QualifiedIdentifier
     * </pre>
     */
    public Java.ReferenceType parseReferenceType() throws ParseException, Scanner.ScanException, IOException {
        return new Java.ReferenceType(
            this.location(),                // location
            this.parseQualifiedIdentifier() // identifiers
        );
    }

    /**
     * <pre>
     *   ReferenceTypeList := ReferenceType { ',' ReferenceType }
     * </pre>
     */
    public Java.ReferenceType[] parseReferenceTypeList() throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();
        l.add(this.parseReferenceType());
        while (this.peekOperator(",")) {
            this.eatToken();
            l.add(this.parseReferenceType());
        }
        return (Java.ReferenceType[]) l.toArray(new Java.ReferenceType[l.size()]);
    }

    /**
     * <pre>
     *   Expression := AssignmentExpression
     * </pre>
     */
    public Java.Atom parseExpression() throws ParseException, Scanner.ScanException, IOException  {
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
    public Java.Atom parseAssignmentExpression() throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseConditionalExpression();
        if (this.peekOperator(new String[] { "=", "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", "<<=", ">>=", ">>>=" })) {
            Location location = this.location();
            String operator = this.readOperator();
            final Java.Lvalue lhs = a.toLvalueOrPE();
            final Java.Rvalue rhs = this.parseAssignmentExpression().toRvalueOrPE();
            return new Java.Assignment(location, lhs, operator, rhs);
        }
        return a;
    }

    /**
     * <pre>
     *   ConditionalExpression :=
     *     ConditionalOrExpression [ '?' Expression ':' ConditionalExpression ]
     * </pre>
     */
    public Java.Atom parseConditionalExpression() throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseConditionalOrExpression();
        if (!this.peekOperator("?")) return a;
        Location location = this.location();
        this.eatToken();

        Java.Rvalue lhs = a.toRvalueOrPE();
        Java.Rvalue mhs = this.parseExpression().toRvalueOrPE();
        this.readOperator(":");
        Java.Rvalue rhs = this.parseConditionalExpression().toRvalueOrPE();
        return new Java.ConditionalExpression(location, lhs, mhs, rhs);
    }

    /**
     * <pre>
     *   ConditionalOrExpression :=
     *     ConditionalAndExpression { '||' ConditionalAndExpression ]
     * </pre>
     */
    public Java.Atom parseConditionalOrExpression() throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseConditionalAndExpression();
        while (this.peekOperator("||")) {
            Location location = this.location();
            this.eatToken();
            a = new Java.BinaryOperation(
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
    public Java.Atom parseConditionalAndExpression() throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseInclusiveOrExpression();
        while (this.peekOperator("&&")) {
            Location location = this.location();
            this.eatToken();
            a = new Java.BinaryOperation(
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
    public Java.Atom parseInclusiveOrExpression() throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseExclusiveOrExpression();
        while (this.peekOperator("|")) {
            Location location = this.location();
            this.eatToken();
            a = new Java.BinaryOperation(
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
    public Java.Atom parseExclusiveOrExpression() throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseAndExpression();
        while (this.peekOperator("^")) {
            Location location = this.location();
            this.eatToken();
            a = new Java.BinaryOperation(
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
    public Java.Atom parseAndExpression() throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseEqualityExpression();
        while (this.peekOperator("&")) {
            Location location = this.location();
            this.eatToken();
            a = new Java.BinaryOperation(
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
    public Java.Atom parseEqualityExpression() throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseRelationalExpression();

        while (this.peekOperator(new String[] { "==", "!=" })) {
            a = new Java.BinaryOperation(
                this.location(),                                // location
                a.toRvalueOrPE(),                               // lhs
                this.readOperator(),                            // op
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
    public Java.Atom parseRelationalExpression() throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseShiftExpression();

        for (;;) {
            if (this.peekKeyword("instanceof")) {
                Location location = this.location();
                this.eatToken();
                a = new Java.Instanceof(
                    location,
                    a.toRvalueOrPE(),
                    this.parseType()
                );
            } else
            if (this.peekOperator(new String[] { "<", ">", "<=", ">=" })) {
                a = new Java.BinaryOperation(
                    this.location(),                    // location
                    a.toRvalueOrPE(),                          // lhs
                    this.readOperator(),                       // op
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
    public Java.Atom parseShiftExpression() throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseAdditiveExpression();

        while (this.peekOperator(new String[] { "<<", ">>", ">>>" })) {
            a = new Java.BinaryOperation(
                this.location(),                       // location
                a.toRvalueOrPE(),                             // lhs
                this.readOperator(),                          // op
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
    public Java.Atom parseAdditiveExpression() throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseMultiplicativeExpression();

        while (this.peekOperator(new String[] { "+", "-" })) {
            a = new Java.BinaryOperation(
                this.location(),                                    // location
                a.toRvalueOrPE(),                                   // lhs
                this.readOperator(),                                // op
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
    public Java.Atom parseMultiplicativeExpression() throws ParseException, Scanner.ScanException, IOException {
        Java.Atom a = this.parseUnaryExpression();

        while (this.peekOperator(new String[] { "*", "/", "%" })) {
            a = new Java.BinaryOperation(
                this.location(),                    // location
                a.toRvalueOrPE(),                          // lhs
                this.readOperator(),                       // op
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
    public Java.Atom parseUnaryExpression() throws ParseException, Scanner.ScanException, IOException {
        if (this.peekOperator(new String[] { "++", "--" })) {
            return new Java.Crement(
                this.location(),                           // location
                this.readOperator(),                       // operator
                this.parseUnaryExpression().toLvalueOrPE() // operand
            );
        }

        if (this.peekOperator(new String[] { "+", "-", "~", "!" })) {
            return new Java.UnaryOperation(
                this.location(),                           // location
                this.readOperator(),                       // operator
                this.parseUnaryExpression().toRvalueOrPE() // operand
            );
        }

        Java.Atom a = this.parsePrimary();

        while (this.peekOperator(new String[] { ".", "[" })) {
            a = this.parseSelector(a);
        }

        while (this.peekOperator(new String[] { "++", "--" })) {
            a = new Java.Crement(
                this.location(),    // location
                a.toLvalueOrPE(),   // operand
                this.readOperator() // operator
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
    public Java.Atom parsePrimary() throws ParseException, Scanner.ScanException, IOException {
        if (this.peekOperator("(")) {
            this.eatToken();
            if (this.peekKeyword(new String[] { "boolean", "char", "byte", "short", "int", "long", "float", "double", })) {
                // '(' PrimitiveType { '[]' } ')' UnaryExpression
                Java.Type type = this.parseType();
                int brackets = this.parseBracketsOpt();
                this.readOperator(")");
                for (int i = 0; i < brackets; ++i) type = new Java.ArrayType(type);
                return new Java.Cast(
                    this.location(),                           // location
                    type,                                      // targetType
                    this.parseUnaryExpression().toRvalueOrPE() // value
                );
            }
            Java.Atom a = this.parseExpression();
            this.readOperator(")");

            if (
                this.scanner.peek().isLiteral() ||
                this.scanner.peek().isIdentifier() ||
                this.peekOperator(new String[] { "(", "~", "!", }) ||
                this.peekKeyword(new String[] { "this", "super", "new", } )
            ) {
                // '(' Expression ')' UnaryExpression
                return new Java.Cast(
                    this.location(),                           // location
                    a.toTypeOrPE(),                            // targetType
                    this.parseUnaryExpression().toRvalueOrPE() // value
                );
            }

            // '(' Expression ')'
            return new Java.ParenthesizedExpression(a.getLocation(), a.toRvalueOrPE());
        }

        if (this.scanner.peek().isLiteral()) {
            // Literal
            return this.parseLiteral();
        }

        if (this.scanner.peek().isIdentifier()) {
            Location location = this.location();
            String[] qi = this.parseQualifiedIdentifier();
            if (this.peekOperator("(")) {
                // Name Arguments
                return new Java.MethodInvocation(
                    this.location(),                                // location
                    qi.length == 1 ? null : new Java.AmbiguousName( // optionalTarget
                        location,       // location
                        qi,             // identifiers
                        qi.length - 1   // n
                    ),
                    qi[qi.length - 1],                              // methodName
                    this.parseArguments()                           // arguments
                );
            }
            if (
                this.peekOperator("[")
                && this.scanner.peekNextButOne().isOperator("]")
            ) {
                // Name '[]' { '[]' }
                // Name '[]' { '[]' } '.' 'class'
                Java.Type res = new Java.ReferenceType(
                    location, // location
                    qi        // identifiers
                );
                int brackets = this.parseBracketsOpt();
                for (int i = 0; i < brackets; ++i) res = new Java.ArrayType(res);
                if (
                    this.peekOperator(".")
                    && this.scanner.peekNextButOne().isKeyword("class")
                ) {
                    this.eatToken();
                    Location location2 = this.location();
                    this.eatToken();
                    return new Java.ClassLiteral(location2, res);
                } else {
                    return res;
                }
            }
            // Name
            return new Java.AmbiguousName(
                this.location(), // location
                qi               // identifiers
            );
        }

        if (this.peekKeyword("this")) {
            Location location = this.location();
            this.eatToken();
            if (this.peekOperator("(")) {

                // 'this' Arguments
                // Alternate constructor invocation (JLS 8.8.5.1)
                return new Java.AlternateConstructorInvocation(
                    location,             // location
                    this.parseArguments() // arguments
                );
            } else
            {

                // 'this'
                return new Java.ThisReference(location);
            }
        }

        if (this.peekKeyword("super")) {
            this.eatToken();
            if (this.peekOperator("(")) {

                // 'super' Arguments
                // Unqualified superclass constructor invocation (JLS 8.8.5.1)
                return new Java.SuperConstructorInvocation(
                    this.location(),      // location
                    (Java.Rvalue) null,   // optionalQualification
                    this.parseArguments() // arguments
                );
            }
            this.readOperator(".");
            String name = this.readIdentifier();
            if (this.peekOperator("(")) {

                // 'super' '.' Identifier Arguments
                return new Java.SuperclassMethodInvocation(
                    this.location(),      // location
                    name,                 // methodName
                    this.parseArguments() // arguments
                );
            } else {

                // 'super' '.' Identifier
                this.throwParseException("Superclass field access NYI");
            }
        }

        // 'new'
        if (this.peekKeyword("new")) {
            Location location = this.location();
            this.eatToken();
            Java.Type type = this.parseType();
            if (type instanceof Java.ArrayType) {
                // 'new' ArrayType ArrayInitializer
                return new Java.NewInitializedArray(location, (Java.ArrayType) type, this.parseArrayInitializer());
            }
            if (
                type instanceof Java.ReferenceType
                && this.peekOperator("(")
            ) {
                // 'new' ReferenceType Arguments [ ClassBody ]
                Java.Rvalue[] arguments = this.parseArguments();
                if (this.peekOperator("{")) {
                    // 'new' ReferenceType Arguments ClassBody
                    final Java.AnonymousClassDeclaration anonymousClassDeclaration = new Java.AnonymousClassDeclaration(
                        this.location(), // location
                        type             // baseType
                    );
                    this.parseClassBody(anonymousClassDeclaration);
                    return new Java.NewAnonymousClassInstance(
                        location,                  // location
                        (Java.Rvalue) null,        // optionalQualification
                        anonymousClassDeclaration, // anonymousClassDeclaration
                        arguments                  // arguments
                    );
                } else {
                    // 'new' ReferenceType Arguments
                    return new Java.NewClassInstance(
                        location,           // location
                        (Java.Rvalue) null, // optionalQualification
                        type,               // type
                        arguments           // arguments
                    );
                }
            }
            // 'new' Type DimExprs { '[]' }
            return new Java.NewArray(
                location,               // location
                type,                   // type
                this.parseDimExprs(),   // dimExprs
                this.parseBracketsOpt() // dims
            );
        }

        // BasicType
        if (this.peekKeyword(new String[] { "boolean", "char", "byte", "short", "int", "long", "float", "double", })) {
            Java.Type res = this.parseType();
            int brackets = this.parseBracketsOpt();
            for (int i = 0; i < brackets; ++i) res = new Java.ArrayType(res);
            if (
                this.peekOperator(".")
                && this.scanner.peekNextButOne().isKeyword("class")
            ) {
                // BasicType { '[]' } '.' 'class'
                this.eatToken();
                Location location = this.location();
                this.eatToken();
                return new Java.ClassLiteral(location, res);
            }
            // BasicType { '[]' }
            return res;
        }

        // 'void'
        if (this.peekKeyword("void")) {
            this.eatToken();
            if (
                this.peekOperator(".")
                && this.scanner.peekNextButOne().isKeyword("class")
            ) {
                // 'void' '.' 'class'
                this.eatToken();
                Location location = this.location();
                this.eatToken();
                return new Java.ClassLiteral(location, new Java.BasicType(location, Java.BasicType.VOID));
            }
            this.throwParseException("\"void\" encountered in wrong context");
        }

        this.throwParseException("Unexpected token \"" + this.scanner.peek() + "\" in primary");
        /* NEVER REACHED */ return null;
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
    public Java.Atom parseSelector(Java.Atom atom) throws ParseException, Scanner.ScanException, IOException {
        if (this.peekOperator(".")) {
            this.eatToken();
            if (this.scanner.peek().isIdentifier()) {
                String identifier = this.readIdentifier();
                if (this.peekOperator("(")) {
                    // '.' Identifier Arguments
                    return new Java.MethodInvocation(
                        this.location(),      // location
                        atom.toRvalueOrPE(),  // optionalTarget
                        identifier,           // methodName
                        this.parseArguments() // arguments
                    );
                }
                // '.' Identifier
                return new Java.FieldAccessExpression(
                    this.location(),     // location
                    atom.toRvalueOrPE(), // lhs
                    identifier           // fieldName
                );
            }
            if (this.peekKeyword("this")) {
                // '.' 'this'
                Location location = this.location();
                this.eatToken();
                return new Java.QualifiedThisReference(
                    location,                // location
                    atom.toTypeOrPE()        // qualification
                );
            }
            if (this.peekKeyword("super")) {
                Location location = this.location();
                this.eatToken();
                if (this.peekOperator("(")) {

                    // '.' 'super' Arguments
                    // Qualified superclass constructor invocation (JLS 8.8.5.1) (LHS is an Rvalue)
                    return new Java.SuperConstructorInvocation(
                        location,             // location
                        atom.toRvalueOrPE(),  // optionalQualification
                        this.parseArguments() // arguments
                    );
                }
                this.readOperator(".");
                /*String identifier =*/ this.readIdentifier();

                if (this.peekOperator("(")) {

                    // '.' 'super' '.' Identifier Arguments
                    // Qualified superclass method invocation (JLS 15.12) (LHS is a ClassName)
                    // TODO: Qualified superclass method invocation
                    this.throwParseException("Qualified superclass method invocation NYI");
                } else {

                    // '.' 'super' '.' Identifier
                    // Qualified superclass field access (JLS 15.11.2) (LHS is an Rvalue)
                    // TODO: Qualified superclass field access
                    this.throwParseException("Qualified superclass field access NYI");
                }
            }
            if (this.peekKeyword("new")) {
                // '.' 'new' Identifier Arguments [ ClassBody ]
                Java.Rvalue lhs = atom.toRvalue();
                Location location = this.location();
                this.eatToken();
                String identifier = this.readIdentifier();
                Java.Type type = new Java.RvalueMemberType(
                    location,  // location
                    lhs,       // rValue
                    identifier // identifier
                );
                Java.Rvalue[] arguments = this.parseArguments();
                if (this.peekOperator("{")) {
                    // '.' 'new' Identifier Arguments ClassBody (LHS is an Rvalue)
                    final Java.AnonymousClassDeclaration anonymousClassDeclaration = new Java.AnonymousClassDeclaration(
                        this.location(), // location
                        type             // baseType
                    );
                    this.parseClassBody(anonymousClassDeclaration);
                    return new Java.NewAnonymousClassInstance(
                        location,                  // location
                        lhs,                       // optionalQualification
                        anonymousClassDeclaration, // anonymousClassDeclaration
                        arguments                  // arguments
                    );
                } else {
                    // '.' 'new' Identifier Arguments (LHS is an Rvalue)
                    return new Java.NewClassInstance(
                        location, // location
                        lhs,      // optionalQualification
                        type,     // referenceType
                        arguments // arguments
                    );
                }
            }
            if (this.peekKeyword("class")) {
                // '.' 'class'
                Location location = this.location();
                this.eatToken();
                return new Java.ClassLiteral(location, atom.toTypeOrPE());
            }
            this.throwParseException("Unexpected selector \"" + this.scanner.peek() + "\" after \".\"");
        }
        if (this.peekOperator("[")) {
            // '[' Expression ']'
            Location location = this.location();
            this.eatToken();
            Java.Rvalue index = this.parseExpression().toRvalueOrPE();
            this.readOperator("]");
            return new Java.ArrayAccessExpression(
                location,        // location
                atom.toRvalueOrPE(), // lhs
                index            // index
            );
        }
        this.throwParseException("Unexpected token \"" + this.scanner.peek() + "\" in selector");
        /* NEVER REACHED */ return null;
    }

    /**
     * <pre>
     *   DimExprs := DimExpr { DimExpr }
     * </pre>
     */
    public Java.Rvalue[] parseDimExprs() throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();
        l.add(this.parseDimExpr());
        while (
            this.peekOperator("[")
            && !this.scanner.peekNextButOne().isOperator("]")
        ) l.add(this.parseDimExpr());
        return (Java.Rvalue[]) l.toArray(new Java.Rvalue[l.size()]);
    }

    /**
     * <pre>
     *   DimExpr := '[' Expression ']'
     * </pre>
     */
    public Java.Rvalue parseDimExpr() throws Scanner.ScanException, ParseException, IOException {
        this.readOperator("[");
        Java.Rvalue res = this.parseExpression().toRvalueOrPE();
        this.readOperator("]");
        return res;
    }

    /**
     * <pre>
     *   Arguments := '(' [ ArgumentList ] ')'
     * </pre>
     */
    public Java.Rvalue[] parseArguments() throws ParseException, Scanner.ScanException, IOException {
        this.readOperator("(");
        if (this.peekOperator(")")) {
            this.eatToken();
            return new Java.Rvalue[0];
        }
        Java.Rvalue[] arguments = this.parseArgumentList();
        this.readOperator(")");
        return arguments;
    }

    /**
     * <pre>
     *   ArgumentList := Expression { ',' Expression }
     * </pre>
     */
    public Java.Rvalue[] parseArgumentList() throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();
        for (;;) {
            l.add(this.parseExpression().toRvalueOrPE());
            if (!this.peekOperator(",")) break;
            this.eatToken();
        }
        return (Java.Rvalue[]) l.toArray(new Java.Rvalue[l.size()]);
    }

    public Java.Atom parseLiteral() throws ParseException, Scanner.ScanException, IOException {
        Scanner.Token t = this.scanner.read();
        if (!t.isLiteral()) this.throwParseException("Literal expected");
        return new Java.Literal(t.getLocation(), t.getLiteralValue());
    }

    // Simplified access to the scanner.

    public Location location()                                           { return this.scanner.location(); }
    public void     eatToken() throws Scanner.ScanException, IOException { this.scanner.read(); }
    // Keyword-related.
    public boolean peekKeyword()                  { return this.scanner.peek().isKeyword(); }
    public boolean peekKeyword(String keyword)    { return this.scanner.peek().isKeyword(keyword); }
    public boolean peekKeyword(String[] keywords) { return this.scanner.peek().isKeyword(keywords); }
    public void    readKeyword(String keyword) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.read().isKeyword(keyword)) this.throwParseException("\"" + keyword + "\" expected"); // We want a ParseException, not a ScanException
    }
    // Operator-related.
    public boolean peekOperator(String operator)    { return this.scanner.peek().isOperator(operator); }
    public boolean peekOperator(String[] operators) { return this.scanner.peek().isOperator(operators); }
    public String  readOperator() throws ParseException, Scanner.ScanException, IOException {
        Scanner.Token t = this.scanner.read();
        if (!t.isOperator()) this.throwParseException("Operator expected"); // We want a ParseException, not a ScanException
        return t.getOperator();
    }
    public void    readOperator(String operator) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.read().isOperator(operator)) this.throwParseException("Operator \"" + operator + "\" expected"); // We want a ParseException, not a ScanException
    }
    // Identifier-related.
    public boolean peekIdentifier() { return this.scanner.peek().isIdentifier(); }
    public String readIdentifier() throws ParseException, Scanner.ScanException, IOException {
        Scanner.Token t = this.scanner.read();
        if (!t.isIdentifier()) this.throwParseException("Identifier expected"); // We want a ParseException, not a ScanException
        return t.getIdentifier();
    }

    /**
     * <pre>
     *   ExpressionStatement := Expression ';'
     * </pre>
     */
    public Java.Statement parseExpressionStatement() throws ParseException, Scanner.ScanException, IOException {
        Java.Rvalue rv = this.parseExpression().toRvalueOrPE();
        this.readOperator(";");

        return new Java.ExpressionStatement(rv);
    }

    /**
     * Issue a warning if the given string does not comply with the package naming conventions
     * (JLS2 6.8.1).
     */
    private void verifyStringIsConventionalPackageName(String s, Location loc) {
        if (!Character.isLowerCase(s.charAt(0))) {
            this.warning("UPN", "Package name \"" + s + "\" does not begin with a lower-case letter (see JLS2 6.8.1)", loc);
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
            this.warning("UCOIN1", "Class or interface name \"" + id + "\" does not begin with an upper-case letter (see JLS2 6.8.2)", loc);
            return;
        } 
        for (int i = 0; i < id.length(); ++i) {
            char c = id.charAt(i);
            if (!Character.isLetter(c) && !Character.isDigit(c)) {
                this.warning("UCOIN", "Class or interface name \"" + id + "\" contains unconventional character \"" + c + "\" (see JLS2 6.8.2)", loc);
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
            this.warning("UMN1", "Method name \"" + id + "\" does not begin with a lower-case letter (see JLS2 6.8.3)", loc);
            return;
        } 
        for (int i = 0; i < id.length(); ++i) {
            char c = id.charAt(i);
            if (!Character.isLetter(c) && !Character.isDigit(c)) {
                this.warning("UMN", "Method name \"" + id + "\" contains unconventional character \"" + c + "\" (see JLS2 6.8.3)", loc);
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
                    this.warning("UCN", "Constant name \"" + id + "\" contains unconventional character \"" + c + "\" (see JLS2 6.8.5)", loc);
                    return;
                } 
            }
        } else
        if (Character.isLowerCase(id.charAt(0))) {
            for (int i = 0; i < id.length(); ++i) {
                char c = id.charAt(i);
                if (!Character.isLetter(c) && !Character.isDigit(c)) {
                    this.warning("UFN", "Field name \"" + id + "\" contains unconventional character \"" + c + "\" (see JLS2 6.8.4)", loc);
                    return;
                } 
            }
        } else {
            this.warning("UFN1", "\"" + id + "\" is neither a conventional field name (JLS2 6.8.4) nor a conventional constant name (JLS2 6.8.5)", loc);
        }
    }

    /**
     * Issue a warning if the given identifier does not comply with the local variable and
     * parameter naming conventions (JLS2 6.8.6).
     */
    private void verifyIdentifierIsConventionalLocalVariableOrParameterName(String id, Location loc) {
        if (!Character.isLowerCase(id.charAt(0))) {
            this.warning("ULVN1", "Local variable name \"" + id + "\" does not begin with a lower-case letter (see JLS2 6.8.6)", loc);
            return;
        } 
        for (int i = 0; i < id.length(); ++i) {
            char c = id.charAt(i);
            if (!Character.isLetter(c) && !Character.isDigit(c)) {
                this.warning("ULVN", "Local variable name \"" + id + "\" contains unconventional character \"" + c + "\" (see JLS2 6.8.6)", loc);
                return;
            } 
        }
    }

    /**
     * By default, warnings are discarded, but an application my install a
     * {@link WarningHandler}.
     * <p>
     * Notice that there is no <code>Parser.setErrorHandler()</code> method, but parse errors
     * always throw a {@link ParseException}. The reason being is that there is no reasonable
     * way to recover from parse errors and continue parsing, so there is no need to install
     * a custom parse error handler.
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
        if (this.optionalWarningHandler != null) this.optionalWarningHandler.handleWarning(handle, message, optionalLocation);
    }

    /**
     * An exception that reflects an error during parsing.
     *
     * This exception is associated with a particular {@link Location
     * Location} in the source code.
     */
    public static class ParseException extends Scanner.LocatedException {
        ParseException(String message, Location location) {
            super(message, location);
        }
    }

    /**
     * Convenience method for throwing a ParseException.
     */
    protected final void throwParseException(String message) throws ParseException {
        throw new ParseException(message, this.location());
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
