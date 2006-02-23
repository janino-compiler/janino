
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

/**
 * Implementation of a simplified compiler for the Java<sup>TM</sup>
 * programming language.
 *
 * The following elements of the Java programming language are implemented:
 * <ul>
 *   <li><tt>package</tt> declaration
 *   <li><tt>import</tt> declaration
 *   <li><tt>class</tt> declaration
 *   <li><tt>interface</tt> declaration
 *   <li>Inheritance (<tt>extends</tt> and <tt>implements</tt>)
 *   <li>Static member type declaration
 *   <li>Inner classes (member classes, local classes, anonymous classes)
 *   <li>Class initializer
 *   <li>Instance initializer
 *   <li>Field declaration
 *   <li>Method declaration
 *   <li>Local variable declaration
 *   <li>Class variable initializer
 *   <li>Instance variable initializer
 *   <li>Block statement (<tt>{ ... }</tt>)
 *   <li><tt>if ... else</tt> statement
 *   <li><tt>for</tt> statement
 *   <li><tt>while</tt> statement
 *   <li><tt>do ... while</tt> statement
 *   <li><tt>try ... catch ... finally</tt> statement
 *   <li><tt>throw</tt> statement
 *   <li><tt>return</tt> statement
 *   <li><tt>break</tt> statement
 *   <li><tt>continue</tt> statement
 *   <li><tt>switch</tt> statement
 *   <li><tt>synchronized</tt> statement
 *   <li>All primitive types (<tt>boolean, char, byte, short, int, long, float, double</tt>)
 *   <li>Assignment operator <tt>=</tt>
 *   <li>Assignment operators <tt>+=, -=, *=, /=, &amp;=, |=, ^=, %=, &lt;&lt;=, &gt;&gt;=, &gt;&gt;&gt;=</tt>
 *   <li>Conditional operators <tt>?...:, &amp;&amp;, ||</tt>
 *   <li>Boolean logical operators <tt>&amp;, ^, |</tt>
 *   <li>Integer bitwise operators <tt>&amp;, ^, |</tt>
 *   <li>Numeric operators <tt>*, /, %, +, -, &lt;&lt;, &gt;&gt;, &gt;&gt;&gt;</tt>
 *   <li>String concatenation operator <tt>+</tt>
 *   <li>Operators <tt>++</tt> and <tt>--</tt>
 *   <li>Type comparison operator <tt>instanceof</tt>
 *   <li>Unary operators <tt>+, -, ~, !</tt>
 *   <li>Parenthesized expression
 *   <li>Field access (like <tt>System.out</tt>)
 *   <li>Superclass member access (<tt>super.meth();</tt>, <tt>super.field = x;</tt>
 *   <li><tt>this</tt> (reference to current instance)
 *   <li>Alternate constructor invocation (like <tt>this(a, b, c)</tt>)
 *   <li>Superclass constructor invocation (like <tt>super(a, b, c)</tt>)
 *   <li>Method invocation (like <tt>System.out.println("Hello")</tt>) (partially)
 *   <li>Class instance creation (like <tt>new Foo()</tt>)
 *   <li>Primitive array creation(like <tt>new int[10][5][]</tt>)
 *   <li>Class or interface array creation(like <tt>new Foo[10][5][]</tt>)
 *   <li>Array access (like <tt>args[0]</tt>) (currently read-only)
 *   <li>Local variable access
 *   <li>Integer, floating-point, boolean, character, string literal
 *   <li><tt>null</tt> literal
 *   <li>Unary numeric conversion
 *   <li>Binary numeric conversion
 *   <li>Widening numeric conversion
 *   <li>Narrowing numeric conversion
 *   <li>Widening reference conversion
 *   <li>Narrowing reference conversion
 *   <li>Cast
 *   <li>Assignment conversion
 *   <li>String conversion (for string concatenation)
 *   <li>Constant expression
 *   <li>Block scope, method scope, class scope, global scope
 *   <li><tt>throws</tt> clause
 *   <li>Array initializer (like <tt>String[] a = { "x", "y", "z" }</tt>)
 *   <li>Primitive class literals, e.g. "int.class"
 *   <li>Non-primitive class literals, e.g. "String.class"
 *   <li>References between uncompiled compilation units
 *   <li>Line number tables a la "-g:lines"
 *   <li>Source file information a la "-g:source"
 *   <li>Handling of <code>&#64;deprecated</code> doc comment tag
 *   <li>Accessibility checking
 * </ul>
 *
 * <a name="limitations"></a>
 * Limitations:
 *
 * <ul>
 *   <li>Local variable information information for debugging is not yet implemented (i.e. "-g:vars" is ignored)
 *   <li>Wrapping of private fields and methods of inner classes into "acces$...()" methods is not yet implemented; you must change private non-static methods to, e.g., "/*private&#42;/", otherwise you get a "Compiler limitation: ..." error
 *   <li><code>assert</code> (a JDK 1.4 language feature) is not yet implemented
 *   <li>The upcoming JDK 1.5 language features are not yet implemented
 *   <li>Checking of "definite assignment" (JLS2 16) is not executed
 * </ul>
 */

public class Parser {
    public Parser(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * <pre>
     *   CompilationUnit := [ 'package' QualifiedIdentifier ';' ]
     *                      { ImportDeclaration }
     *                      { TypeDeclaration }
     * </pre>
     */
    public Java.CompilationUnit parseCompilationUnit() throws ParseException, Scanner.ScanException, IOException {
        Java.CompilationUnit compilationUnit = new Java.CompilationUnit(this.scanner.peek().getLocation().getFileName());

        if (this.scanner.peek().isKeyword("package")) {
            this.scanner.read();
            Location loc = this.scanner.peek().getLocation();
            String packageName = Parser.join(this.parseQualifiedIdentifier(), ".");
            this.verifyStringIsConventionalPackageName(packageName, loc);
            compilationUnit.setPackageDeclaration(new Java.PackageDeclaration(loc, packageName));
            if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon expected after \"package\" directive");
        }

        while (this.scanner.peek().isKeyword("import")) {
            compilationUnit.addImportDeclaration(this.parseImportDeclaration());
        }

        while (!this.scanner.peek().isEOF()) {
            Java.PackageMemberTypeDeclaration tltd = (Java.PackageMemberTypeDeclaration) this.parseTypeDeclaration(compilationUnit);
            if (tltd != null) compilationUnit.addPackageMemberTypeDeclaration(tltd);
        }

        return compilationUnit;
    }

    /**
     * <pre>
     *   ImportDeclaration := 'import' Identifier
     *                        { '.' Identifier }
     *                        [ '.' '*' ]
     *                        ';'
     * </pre>
     */
    public Java.ImportDeclaration parseImportDeclaration() throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isKeyword("import")) this.throwParseException("\"import\" expected");
        Location loc = this.scanner.read().getLocation();
        if (!this.scanner.peek().isIdentifier()) this.throwParseException("Identifier expected after \"import\"");
        List l = new ArrayList();
        l.add(this.scanner.read().getIdentifier());
        for (;;) {
            if (this.scanner.peek().isOperator(";")) {
                this.scanner.read();
                return new Java.SingleTypeImportDeclaration(loc, (String[]) l.toArray(new String[l.size()]));
            }
            if (!this.scanner.read().isOperator(".")) this.throwParseException("\";\" or \".\" expected after identifier in \"import\" directive");
            if (this.scanner.peek().isOperator("*")) {
                this.scanner.read();
                if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon expected at end of type-import-on-demoand declaration");
                return new Java.TypeImportOnDemandDeclaration(loc, (String[]) l.toArray(new String[l.size()]));
            }
            if (!this.scanner.peek().isIdentifier()) this.throwParseException("Identifier or \"*\" expected after \".\" in \"import\" directive");
            l.add(this.scanner.read().getIdentifier());
        }
    }

    /**
     * QualifiedIdentifier := Identifier { '.' Identifier }
     */
    public String[] parseQualifiedIdentifier() throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isIdentifier()) this.throwParseException("Identifier expected");
        String s = this.scanner.read().getIdentifier();
        List l = new ArrayList();
        l.add(s);
        while (this.scanner.peek().isOperator(".") && this.scanner.peekNextButOne().isIdentifier()) {
            this.scanner.read();
            l.add(this.scanner.read().getIdentifier());
        }
        return (String[]) l.toArray(new String[l.size()]);
    }

    /**
     * <pre>
     *   TypeDeclaration := ClassOrInterfaceDeclaration
     * </pre>
     */
    public Java.NamedTypeDeclaration parseTypeDeclaration(
        Java.CompilationUnit compilationUnit
    ) throws ParseException, Scanner.ScanException, IOException {
        if (this.scanner.peek().isOperator(";")) {
            this.scanner.read();
            return null;
        }
        return this.parseClassOrInterfaceDeclaration(compilationUnit);
    }

    /**
     * <pre>
     *   ClassOrInterfaceDeclaration :=
     *             ModifiersOpt 'class' ClassDeclarationRest |
     *             ModifiersOpt 'interface' InterfaceDeclarationRest
     * </pre>
     */
    public Java.NamedTypeDeclaration parseClassOrInterfaceDeclaration(
        Java.Scope enclosingScope
    ) throws ParseException, Scanner.ScanException, IOException {
        String optionalDocComment = this.scanner.doc();

        short modifiers = this.parseModifiersOpt();

        Java.NamedTypeDeclaration res;
        if (this.scanner.peek().isKeyword("class")) {
            if (optionalDocComment == null) this.warning("CDCM", "Class doc comment missing", this.scanner.peek().getLocation());
            this.scanner.read();
            res = this.parseClassDeclarationRest(
                enclosingScope,     // enclosingScope
                optionalDocComment, // optionalDocComment
                modifiers           // modifiers
            );
        } else
        if (this.scanner.peek().isKeyword("interface")) {
            if (optionalDocComment == null) this.warning("IDCM", "Interface doc comment missing", this.scanner.peek().getLocation());
            this.scanner.read();
            res = this.parseInterfaceDeclarationRest(
                enclosingScope,     // enclosingScope
                optionalDocComment, // optionalDocComment
                modifiers           // modifiers
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
        while (this.scanner.peek().isKeyword()) {
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

            this.scanner.read();
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
        Java.Scope enclosingScope,
        String     optionalDocComment,
        short      modifiers
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isIdentifier()) this.throwParseException("Class name expected after \"class\"");
        Location location = this.scanner.peek().getLocation();
        String className = this.scanner.read().getIdentifier();
        this.verifyIdentifierIsConventionalClassOrInterfaceName(className, location);

        Java.ReferenceType optionalExtendedType = null;
        if (this.scanner.peek().isKeyword("extends")) {
            this.scanner.read();
            optionalExtendedType = this.parseReferenceType(enclosingScope);
        }

        Java.ReferenceType[] implementedTypes = new Java.ReferenceType[0];
        if (this.scanner.peek().isKeyword("implements")) {
            this.scanner.read();
            implementedTypes = this.parseReferenceTypeList(enclosingScope);
        }

        Java.NamedClassDeclaration namedClassDeclaration;
        if (enclosingScope instanceof Java.CompilationUnit) {
            namedClassDeclaration = new Java.PackageMemberClassDeclaration(
                location,                              // location
                (Java.CompilationUnit) enclosingScope, // declaringCompilationUnit
                optionalDocComment,                    // optionalDocComment
                modifiers,                             // modifiers
                className,                             // name
                optionalExtendedType,                  // optinalExtendedType
                implementedTypes                       // implementedTypes
            );
        } else
        if (enclosingScope instanceof Java.NamedTypeDeclaration) {
            namedClassDeclaration = new Java.MemberClassDeclaration(
                location,                                   // location
                (Java.NamedTypeDeclaration) enclosingScope, // declaringType
                optionalDocComment,                         // optionalDocComment
                modifiers,                                  // modifiers
                className,                                  // name
                optionalExtendedType,                       // optionalExtendedType
                implementedTypes                            // implementedTypes
            );
        } else
        if (enclosingScope instanceof Java.Block) {
            namedClassDeclaration = new Java.LocalClassDeclaration(
                location,                    // location
                (Java.Block) enclosingScope, // declaringBlock
                optionalDocComment,          // optionalDocComment
                modifiers,                   // modifiers
                className,                   // name
                optionalExtendedType,        // optionalExtendedType
                implementedTypes             // implementedTypes
            );
        } else
        {
            throw new RuntimeException("SNO: Class declaration in unexpected scope " + enclosingScope.getClass().getName());
        }

        this.parseClassBody(namedClassDeclaration);
        return namedClassDeclaration;
    }

    /**
     * <pre>
     *   ClassBody := '{' { ClassBodyDeclaration } '}'
     * </pre>
     */
    public void parseClassBody(
        Java.ClassDeclaration classDeclaration
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isOperator("{")) this.throwParseException("\"{\" expected at start of class body");
        this.scanner.read();

        for (;;) {
            if (this.scanner.peek().isOperator("}")) {
                this.scanner.read();
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
        if (this.scanner.peek().isOperator(";")) {
            this.scanner.read();
            return;
        }

        String optionalDocComment = this.scanner.doc();
        short modifiers = this.parseModifiersOpt();

        // Initializer?
        if (this.scanner.peek().isOperator("{")) {
            if ((modifiers & ~Mod.STATIC) != 0) this.throwParseException("Only modifier \"static\" allowed on initializer");

            Java.Initializer initializer = new Java.Initializer(
                this.scanner.peek().getLocation(),
                classDeclaration,                  // declaringType
                (modifiers & Mod.STATIC) != 0      // statiC
            );
            initializer.setBlock(this.parseBlock((Java.Scope) initializer));

            classDeclaration.addVariableDeclaratorOrInitializer(initializer);
            return;
        }

        // "void" method declaration.
        if (this.scanner.peek().isKeyword("void")) {
            Location location = this.scanner.read().getLocation();
            if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", location);
            if (!this.scanner.peek().isIdentifier()) this.throwParseException("Method name expected after \"void\"");
            String name = this.scanner.read().getIdentifier();
            classDeclaration.declaredMethods.add(this.parseMethodDeclarationRest(
                classDeclaration,                                  // declaringType
                optionalDocComment,                                // optionalDocComment
                modifiers,                                         // modifiers
                new Java.BasicType(location, Java.BasicType.VOID), // type
                name                                               // name
            ));
            return;
        }

        // Member class.
        if (this.scanner.peek().isKeyword("class")) {
            if (optionalDocComment == null) this.warning("MCDCM", "Member class doc comment missing", this.scanner.peek().getLocation());
            this.scanner.read();
            classDeclaration.addMemberTypeDeclaration((Java.MemberTypeDeclaration) this.parseClassDeclarationRest(
                (Java.Scope) classDeclaration, // enclosingScope
                optionalDocComment,            // optionalDocComment
                modifiers                      // modifiers
            ));
            return;
        }

        // Member interface.
        if (this.scanner.peek().isKeyword("interface")) {
            if (optionalDocComment == null) this.warning("MIDCM", "Member interface doc comment missing", this.scanner.peek().getLocation());
            this.scanner.read();
            classDeclaration.addMemberTypeDeclaration((Java.MemberTypeDeclaration) this.parseInterfaceDeclarationRest(
                classDeclaration,                // enclosingScope
                optionalDocComment,              // optionalDocComment
                (short) (modifiers | Mod.STATIC) // modifiers
            ));
            return;
        }

        // Constructor.
        if (
            classDeclaration instanceof Java.NamedClassDeclaration &&
            this.scanner.peek().isIdentifier(((Java.NamedClassDeclaration) classDeclaration).getName()) &&
            this.scanner.peekNextButOne().isOperator("(")
        ) {
            if (optionalDocComment == null) this.warning("CDCM", "Constructor doc comment missing", this.scanner.peek().getLocation());
            classDeclaration.addConstructor(this.parseConstructorDeclarator(
                classDeclaration,   // declaringClass
                optionalDocComment, // optionalDocComment
                modifiers           // modifiers
            ));
            return;
        }

        // Member method or field.
        Java.Type memberType = this.parseType((Java.Scope) classDeclaration);
        if (!this.scanner.peek().isIdentifier()) this.throwParseException("Identifier expected in member declaration");
        Location location = this.scanner.peek().getLocation();
        String memberName = this.scanner.read().getIdentifier();

        // Method declarator.
        if (this.scanner.peek().isOperator("(")) {
            if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.scanner.peek().getLocation());
            classDeclaration.declaredMethods.add(this.parseMethodDeclarationRest(
                classDeclaration,   // declaringType
                optionalDocComment, // optionalDocComment
                modifiers,          // modifiers
                memberType,         // type
                memberName          // name
            ));
            return;
        }

        // Field declarator.
        if (optionalDocComment == null) this.warning("FDCM", "Field doc comment missing", this.scanner.peek().getLocation());
        Java.FieldDeclaration fd = new Java.FieldDeclaration(
            location,           // location
            classDeclaration,   // declaringType
            optionalDocComment, // optionalDocComment
            modifiers,          // modifiers
            memberType          // type
        );

        Java.VariableDeclarator[] vds = this.parseFieldDeclarationRest(
            (Java.BlockStatement) fd, // enclosingBlockStatement
            memberType,               // type
            memberName                // name
        );
        if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon expected at end of field declaration");
        fd.setVariableDeclarators(vds);
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
        Java.Scope enclosingScope,
        String     optionalDocComment,
        short      modifiers
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isIdentifier()) this.throwParseException("Interface name expected after \"interface\"");
        Location location = this.scanner.peek().getLocation();
        String interfaceName = this.scanner.read().getIdentifier();
        this.verifyIdentifierIsConventionalClassOrInterfaceName(interfaceName, location);

        Java.ReferenceType[] extendedTypes = new Java.ReferenceType[0];
        if (this.scanner.peek().isKeyword("extends")) {
            this.scanner.read();
            extendedTypes = this.parseReferenceTypeList(enclosingScope);
        }

        Java.InterfaceDeclaration interfaceDeclaration;
        if (enclosingScope instanceof Java.CompilationUnit) {
            interfaceDeclaration = new Java.PackageMemberInterfaceDeclaration(
                location,                              // location
                (Java.CompilationUnit) enclosingScope, // declaringCompilationUnit
                optionalDocComment,                    // optionalDocComment
                modifiers,                             // modifiers
                interfaceName,                         // name
                extendedTypes                          // extendedTypes
            );
        } else
        if (enclosingScope instanceof Java.NamedTypeDeclaration) {
            interfaceDeclaration = new Java.MemberInterfaceDeclaration(
                location,                                   // location
                (Java.NamedTypeDeclaration) enclosingScope, // declaringType
                optionalDocComment,                         // optionalDocComment
                modifiers,                                  // modifiers
                interfaceName,                              // name
                extendedTypes                               // extendedTypes
            );
        } else
        {
            throw new RuntimeException("SNO: Interface declaration in unexpected scope " + enclosingScope.getClass().getName());
        }

        this.parseInterfaceBody(interfaceDeclaration);
        return interfaceDeclaration;
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
        if (!this.scanner.read().isOperator("{")) this.throwParseException("\"{\" expected at start of interface body");

        for (;;) {
            if (this.scanner.peek().isOperator("}")) {
                this.scanner.read();
                break;
            }

            if (this.scanner.peek().isOperator(";")) {
                this.scanner.read();
                continue;
            }

            String optionalDocComment = this.scanner.doc();
            short modifiers = this.parseModifiersOpt();

            // "void" method declaration.
            if (this.scanner.peek().isKeyword("void")) {
                if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.scanner.peek().getLocation());
                Location location = this.scanner.read().getLocation();
                if (!this.scanner.peek().isIdentifier()) this.throwParseException("Method name expected after \"void\"");
                String name = this.scanner.read().getIdentifier();
                interfaceDeclaration.declaredMethods.add(this.parseMethodDeclarationRest(
                    interfaceDeclaration,                              // declaringType
                    optionalDocComment,                                // optionalDocComment
                    (short) (modifiers | Mod.ABSTRACT | Mod.PUBLIC),   // modifiers
                    new Java.BasicType(location, Java.BasicType.VOID), // type
                    name                                               // name
                ));
            } else

            // Member class.
            if (this.scanner.peek().isKeyword("class")) {
                if (optionalDocComment == null) this.warning("MCDCM", "Member class doc comment missing", this.scanner.peek().getLocation());
                this.scanner.read();
                interfaceDeclaration.addMemberTypeDeclaration((Java.MemberTypeDeclaration) this.parseClassDeclarationRest(
                    interfaceDeclaration,                         // enclosingScope
                    optionalDocComment,                           // optionalDocComment
                    (short) (modifiers | Mod.STATIC | Mod.PUBLIC) // modifiers
                ));
            } else

            // Member interface.
            if (this.scanner.peek().isKeyword("interface")) {
                if (optionalDocComment == null) this.warning("MIDCM", "Member interface doc comment missing", this.scanner.peek().getLocation());
                this.scanner.read();
                interfaceDeclaration.addMemberTypeDeclaration((Java.MemberTypeDeclaration) this.parseInterfaceDeclarationRest(
                    interfaceDeclaration,                         // enclosingScope
                    optionalDocComment,                           // optionalDocComment
                    (short) (modifiers | Mod.STATIC | Mod.PUBLIC) // modifiers
                ));
            } else

            // Member method or field.
            {
                Java.Type memberType = this.parseType(interfaceDeclaration);
                if (!this.scanner.peek().isIdentifier()) this.throwParseException("Identifier expected in member declaration");
                String memberName = this.scanner.peek().getIdentifier();
                Location location = this.scanner.read().getLocation();

                // Method declarator.
                if (this.scanner.peek().isOperator("(")) {
                    if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.scanner.peek().getLocation());
                    interfaceDeclaration.declaredMethods.add(this.parseMethodDeclarationRest(
                        interfaceDeclaration,                            // declaringType
                        optionalDocComment,                              // optionalDocComment
                        (short) (modifiers | Mod.ABSTRACT | Mod.PUBLIC), // modifiers
                        memberType,                                      // type
                        memberName                                       // name
                    ));
                } else

                // Field declarator.
                {
                    if (optionalDocComment == null) this.warning("FDCM", "Field doc comment missing", this.scanner.peek().getLocation());
                    Java.FieldDeclaration fd = new Java.FieldDeclaration(
                        location,                          // location
                        interfaceDeclaration,              // declaringType
                        optionalDocComment,                // optionalDocComment
                        (short) (                          // modifiers
                            modifiers |
                            Mod.PUBLIC | Mod.STATIC | Mod.FINAL
                        ),
                        memberType                         // type
                    );
                    Java.VariableDeclarator[] vds = this.parseFieldDeclarationRest(
                        (Java.BlockStatement) fd, // enclosingBlockStatement
                        memberType,               // type
                        memberName                // name
                    );
                    fd.setVariableDeclarators(vds);
                    interfaceDeclaration.addConstantDeclaration(fd);
                }
            }
        }
    }

    /**
     * <pre>
     *   ConstructorDeclarator :=
     *     FormalParameters
     *     [ 'throws' ReferenceTypeList ]
     *     '{'
     *       [ 'this' Arguments ';' | 'super' Arguments ';' ]
     *       BlockStatements
     *     '}'
     * </pre>
     */
    public Java.ConstructorDeclarator parseConstructorDeclarator(
        Java.ClassDeclaration declaringClass,
        String                optionalDocComment,
        short                 modifiers
    ) throws ParseException, Scanner.ScanException, IOException {
        Location location = this.scanner.read().getLocation();

        // Parse formal parameters.
        Java.FormalParameter[] formalParameters = this.parseFormalParameters(
            declaringClass // enclosingScope
        );

        // Parse "throws" clause.
        Java.ReferenceType[] thrownExceptions;
        if (this.scanner.peek().isKeyword("throws")) {
            this.scanner.read();
            thrownExceptions = this.parseReferenceTypeList(declaringClass);
        } else {
            thrownExceptions = new Java.ReferenceType[0];
        }

        // Create ConstructorDeclarator object.
        Java.ConstructorDeclarator cd = new Java.ConstructorDeclarator(
            location,           // location
            declaringClass,     // declaringClass
            optionalDocComment, // optionalDocComment
            modifiers,          // modifiers
            formalParameters,   // formalParameters
            thrownExceptions    // thrownExceptions
        );

        // Parse constructor body.
        location = this.scanner.peek().getLocation();
        if (!this.scanner.read().isOperator("{")) this.throwParseException("\"{\" expected");

        Java.Block body = new Java.Block(
            location, // location
            cd        // enclosingScope
        );
        cd.setBody(body);

        // Special treatment for the first statement of the constructor body: If this is surely an
        // expression statement, and if it could be a "ConstructorInvocation", then parse the
        // expression and check if it IS a ConstructorInvocation.
        if (
            this.scanner.peek().isKeyword(new String[] {
                "this", "super", "new", "void",
                "byte", "char", "short", "int", "long", "float", "double", "boolean",
            } ) ||
            this.scanner.peek().isLiteral() ||
            this.scanner.peek().isIdentifier()
        ) {
            Java.Atom a = this.parseExpression(body);
            if (a instanceof Java.ConstructorInvocation) {
                if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon at end of constructor invocation expected");
                cd.setExplicitConstructorInvocation((Java.ConstructorInvocation) a);
            } else {
                Java.BlockStatement bs;
                if (this.scanner.peek().isIdentifier()) {
                    Java.Type variableType = a.toTypeOrPE();
                    bs = new Java.LocalVariableDeclarationStatement(
                        a.getLocation(),                    // location
                        body,                               // declaringBlock
                        (short) 0,                          // modifiers
                        variableType,                       // type
                        this.parseLocalVariableDeclarators( // variableDeclarators
                            (Java.BlockStatement) body, // enclosingBlockStatement
                            variableType                // type
                        )
                    );
                    if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon expected after local variable declarator");
                } else {
                    bs = new Java.ExpressionStatement(a.toRvalueOrPE(), body);
                    if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon at end of expression statement expected");
                }
                body.addStatement(bs);
            }
        }
        body.addStatements(this.parseBlockStatements(body));

        if (!this.scanner.read().isOperator("}")) this.throwParseException("\"}\" expected");

        return cd;
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
        Location location = this.scanner.peek().getLocation();

        this.verifyIdentifierIsConventionalMethodName(name, location);

        Java.FormalParameter[] formalParameters = this.parseFormalParameters(
            declaringType // enclosingScope
        );

        for (int i = this.parseBracketsOpt(); i > 0; --i) type = new Java.ArrayType(type);

        Java.ReferenceType[] thrownExceptions;
        if (this.scanner.peek().isKeyword("throws")) {
            this.scanner.read();
            thrownExceptions = this.parseReferenceTypeList(declaringType);
        } else {
            thrownExceptions = new Java.ReferenceType[0];
        }

        Java.MethodDeclarator md = new Java.MethodDeclarator(
            location,           // location
            declaringType,      // declaringType
            optionalDocComment, // optionalDocComment
            modifiers,          // modifiers
            type,               // type
            name,               // name
            formalParameters,   // formalParameters
            thrownExceptions    // thrownExceptions
        );
        if (this.scanner.peek().isOperator(";")) {
            if ((modifiers & (Mod.ABSTRACT | Mod.NATIVE)) == 0) this.throwParseException("Non-abstract, non-native method must have a body");
            this.scanner.read();
        } else {
            if ((modifiers & (Mod.ABSTRACT | Mod.NATIVE)) != 0) this.throwParseException("Abstract or native method must not have a body");
            md.setBody(this.parseMethodBody(
                md // declaringFunction
            ));
        }

        return md;
    }

    /**
     * <pre>
     *   VariableInitializer :=
     *     ArrayInitializer |
     *     Expression
     * </pre>
     */
    public Java.Rvalue parseVariableInitializer(
        Java.BlockStatement enclosingBlockStatement,
        Java.Type           type
    ) throws ParseException, Scanner.ScanException, IOException {
        if (this.scanner.peek().isOperator("{")) {
            if (!(type instanceof Java.ArrayType)) this.throwParseException("Cannot initialize non-array type \"" + type + "\" with an array");
            return this.parseArrayInitializer(
                enclosingBlockStatement,
                (Java.ArrayType) type
            );
        }
        return this.parseExpression(enclosingBlockStatement).toRvalueOrPE();
    }

    /**
     * <pre>
     *   ArrayInitializer :=
     *     '{' [ VariableInitializer { ',' VariableInitializer } [ ',' ] '}'
     * </pre>
     */
    public Java.Rvalue parseArrayInitializer(
        Java.BlockStatement enclosingBlockStatement,
        Java.ArrayType      arrayType
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isOperator("{")) this.throwParseException("\"{\" expected");
        Location location = this.scanner.read().getLocation();
        Java.Type componentType = arrayType.componentType;
        List l = new ArrayList(); // Rvalue
        while (!this.scanner.peek().isOperator("}")) {
            l.add(this.parseVariableInitializer(
                enclosingBlockStatement,
                componentType
            ));
            if (this.scanner.peek().isOperator("}")) break;
            if (!this.scanner.peek().isOperator(",")) this.throwParseException("\",\" or \"}\" expected");
            this.scanner.read();
        }
        this.scanner.read();
        return new Java.ArrayInitializer(
            location,
            arrayType,
            (Java.Rvalue[]) l.toArray(new Java.Rvalue[l.size()])
        );
    }

    /**
     * <pre>
     *   FormalParameters := '(' [ FormalParameter { ',' FormalParameter } ] ')'
     * </pre>
     */
    public Java.FormalParameter[] parseFormalParameters(
        Java.Scope enclosingScope
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.read().isOperator("(")) this.throwParseException("\"(\" expected in formal parameter list");
        if (this.scanner.peek().isOperator(")")) {
            this.scanner.read();
            return new Java.FormalParameter[0];
        }

        List l = new ArrayList(); // Java.FormalParameter
        for (;;) {
            l.add(this.parseFormalParameter(enclosingScope));
            if (!this.scanner.peek().isOperator(",")) break;
            this.scanner.read();
        }
        if (!this.scanner.read().isOperator(")")) this.throwParseException("\")\" expected at end of formal parameter list");
        return (Java.FormalParameter[]) l.toArray(new Java.FormalParameter[l.size()]);
    }

    /**
     * <pre>
     *   FormalParameter := [ 'final' ] Type Identifier BracketsOpt
     * </pre>
     */
    public Java.FormalParameter parseFormalParameter(
        Java.Scope enclosingScope
    ) throws ParseException, Scanner.ScanException, IOException {
        boolean finaL = this.scanner.peek().isKeyword("final");
        if (finaL) this.scanner.read();

        Java.Type type = this.parseType(enclosingScope);

        if (!this.scanner.peek().isIdentifier()) this.throwParseException("Formal parameter name expected");
        Location location = this.scanner.peek().getLocation();
        String name = this.scanner.read().getIdentifier();
        this.verifyIdentifierIsConventionalLocalVariableOrParameterName(name, location);

        for (int i = this.parseBracketsOpt(); i > 0; --i) type = new Java.ArrayType(type);
        return new Java.FormalParameter(finaL, type, name);
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
            this.scanner.read();
            this.scanner.read();
            ++res;
        }
        return res;
    }

    /**
     * <pre>
     *   MethodBody := Block
     * </pre>
     */
    public Java.Block parseMethodBody(
        Java.FunctionDeclarator declaringFunction
    ) throws ParseException, Scanner.ScanException, IOException {
        return this.parseBlock(
            declaringFunction // enclosingScope
        );
    }

    /**
     * <pre>
     *   '{' BlockStatements '}'
     * </pre>
     */
    public Java.Block parseBlock(
        Java.Scope enclosingScope
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isOperator("{")) this.throwParseException("\"{\" expected");
        Java.Block block = new Java.Block(
            this.scanner.read().getLocation(),
            enclosingScope
        );
        block.addStatements(this.parseBlockStatements(block));
        if (!this.scanner.read().isOperator("}")) this.throwParseException("\"}\" expected");
        return block;
    }

    /**
     * <pre>
     *   BlockStatements := { BlockStatement }
     * </pre>
     */
    public List parseBlockStatements(
        Java.Block enclosingBlock
    ) throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();
        while (
            !this.scanner.peek().isOperator("}") &&
            !this.scanner.peek().isKeyword("case") &&
            !this.scanner.peek().isKeyword("default")
        ) l.add(this.parseBlockStatement(enclosingBlock));
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
    public Java.BlockStatement parseBlockStatement(
        Java.Block enclosingBlock
    ) throws ParseException, Scanner.ScanException, IOException {

        // Statement?
        if (
            (
                this.scanner.peek().isIdentifier() &&
                this.scanner.peekNextButOne().isOperator(":")
            ) ||
            this.scanner.peek().isKeyword(new String[] {
                "if", "for", "while", "do", "try", "switch", "synchronized",
                "return", "throw", "break", "continue"
            }) ||
            this.scanner.peek().isOperator(new String[] { "{", ";" })
        ) return this.parseStatement((Java.BlockStatement) enclosingBlock);

        // Local class declaration?
        if (this.scanner.peek().isKeyword("class")) {
            // JAVADOC[TM] ignores doc comments for local classes, but we
            // don't...
            String optionalDocComment = this.scanner.doc();
            if (optionalDocComment == null) this.warning("LCDCM", "Local class doc comment missing", this.scanner.peek().getLocation());

            this.scanner.read();
            final Java.LocalClassDeclaration lcd = (Java.LocalClassDeclaration) this.parseClassDeclarationRest(
                (Java.Scope) enclosingBlock,      // enclosingScope
                optionalDocComment,               // optionalDocComment
                (short) (Mod.FINAL | Mod.PRIVATE) // modifiers
            );
            return new Java.LocalClassDeclarationStatement((Java.Scope) enclosingBlock, lcd);
        }

        // 'final' Type LocalVariableDeclarators ';'
        if (this.scanner.peek().isKeyword("final")) {
            Location location = this.scanner.read().getLocation();
            Java.Type variableType = this.parseType((Java.Scope) enclosingBlock);
            Java.LocalVariableDeclarationStatement lvds = new Java.LocalVariableDeclarationStatement(
                location,                           // location
                enclosingBlock,                     // declaringBlock
                Mod.FINAL,                          // modifiers
                variableType,                       // type
                this.parseLocalVariableDeclarators( // variableDeclarators
                    (Java.BlockStatement) enclosingBlock, // enclosingBlockStatement
                    variableType                          // type
                )
            );
            if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon expected after local variable declarator");
            return lvds;
        }

        // It's either a non-final local variable declaration or an expression statement. We can
        // only tell after parsing an expression.
        Java.Atom a = this.parseExpression(enclosingBlock);

        // Expression ';'
        if (this.scanner.peek().isOperator(";")) {
            this.scanner.read();
            return new Java.ExpressionStatement(a.toRvalueOrPE(), (Java.Scope) enclosingBlock);
        }

        // Expression LocalVariableDeclarators ';'
        Java.Type variableType = a.toTypeOrPE();
        Java.LocalVariableDeclarationStatement lvds = new Java.LocalVariableDeclarationStatement(
            a.getLocation(),                    // location
            enclosingBlock,                     // declaringBlock
            (short) 0,                          // modifiers
            variableType,                       // type
            this.parseLocalVariableDeclarators( // variableDeclarators
                (Java.BlockStatement) enclosingBlock, // enclosingBlockStatement
                variableType                          // type
            )
        );
        if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon expected after local variable declarator");
        return lvds;
    }

    /**
     * <pre>
     *   LocalVariableDeclarators := VariableDeclarator { ',' VariableDeclarator }
     * </pre>
     */
    public Java.VariableDeclarator[] parseLocalVariableDeclarators(
        Java.BlockStatement enclosingBlockStatement,
        Java.Type           type
    ) throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();
        for (;;) {
            Java.VariableDeclarator vd = this.parseVariableDeclarator(enclosingBlockStatement, type);
            this.verifyIdentifierIsConventionalLocalVariableOrParameterName(vd.name, vd.getLocation());
            l.add(vd);
            if (!this.scanner.peek().isOperator(",")) break;
            this.scanner.read();
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
    public Java.VariableDeclarator[] parseFieldDeclarationRest(
        Java.BlockStatement enclosingBlockStatement,
        Java.Type           type,
        String              name
    ) throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();

        Java.VariableDeclarator vd = this.parseVariableDeclaratorRest(enclosingBlockStatement, type, name);
        this.verifyIdentifierIsConventionalFieldName(vd.name, vd.getLocation());
        l.add(vd);

        while (this.scanner.peek().isOperator(",")) {
            this.scanner.read();

            vd = this.parseVariableDeclarator(enclosingBlockStatement, type);
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
    public Java.VariableDeclarator parseVariableDeclarator(
        Java.BlockStatement enclosingBlockStatement,
        Java.Type           type
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isIdentifier()) this.throwParseException("Variable name expected");
        String variableName = this.scanner.read().getIdentifier();
        return this.parseVariableDeclaratorRest(enclosingBlockStatement, type, variableName);
    }

    /**
     * <pre>
     *   VariableDeclaratorRest := { '[' ']' } [ '=' VariableInitializer ]
     * </pre>
     * Used by field declarations and local variable declarations.
     */
    public Java.VariableDeclarator parseVariableDeclaratorRest(
        Java.BlockStatement enclosingBlockStatement,
        Java.Type           type,
        String              name
    ) throws ParseException, Scanner.ScanException, IOException  {
        Location loc = this.scanner.peek().getLocation();
        int brackets = this.parseBracketsOpt();
        for (int i = 0; i < brackets; ++i) type = new Java.ArrayType(type);
        Java.Rvalue initializer = null;
        if (this.scanner.peek().isOperator("=")) {
            this.scanner.read();
            initializer = this.parseVariableInitializer(enclosingBlockStatement, type);
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
    public Java.Statement parseStatement(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        if (
            this.scanner.peek().isIdentifier() &&
            this.scanner.peekNextButOne().isOperator(":")
        ) {
            return this.parseLabeledStatement(enclosingBlockStatement);
        }

        Scanner.Token t = this.scanner.peek();
        Java.Statement stmt = (
            t.isOperator("{")           ? this.parseBlock(enclosingBlockStatement) :
            t.isKeyword("if")           ? this.parseIfStatement(enclosingBlockStatement) :
            t.isKeyword("for")          ? this.parseForStatement(enclosingBlockStatement) :
            t.isKeyword("while")        ? this.parseWhileStatement(enclosingBlockStatement) :
            t.isKeyword("do")           ? this.parseDoStatement(enclosingBlockStatement) :
            t.isKeyword("try")          ? this.parseTryStatement(enclosingBlockStatement) :
            t.isKeyword("switch")       ? this.parseSwitchStatement(enclosingBlockStatement) :
            t.isKeyword("synchronized") ? this.parseSynchronizedStatement(enclosingBlockStatement) :
            t.isKeyword("return")       ? this.parseReturnStatement(enclosingBlockStatement) :
            t.isKeyword("throw")        ? this.parseThrowStatement(enclosingBlockStatement) :
            t.isKeyword("break")        ? this.parseBreakStatement(enclosingBlockStatement) :
            t.isKeyword("continue")     ? this.parseContinueStatement(enclosingBlockStatement) :
            t.isOperator(";")           ? this.parseEmptyStatement(enclosingBlockStatement) :
            this.parseExpressionStatement(enclosingBlockStatement)
        );
        if (stmt == null) this.throwParseException("\"" + t.getKeyword() + "\" NYI");

        return stmt;
    }

    /**
     * <pre>
     *   LabeledStatement := Identifier ':' Statement
     * </pre>
     */
    public Java.Statement parseLabeledStatement(
        Java.Scope enclosingScope
    ) throws ParseException, Scanner.ScanException, IOException {
        Java.LabeledStatement labeledStatement = new Java.LabeledStatement(
            this.scanner.peek().getLocation(),  // location
            enclosingScope,                     // enclosingScope
            this.scanner.read().getIdentifier() // label
        );
        if (!this.scanner.read().isOperator(":")) this.throwParseException("Colon expected");
        labeledStatement.setBody(this.parseStatement(labeledStatement));
        return labeledStatement;
    }

    /**
     * <pre>
     *   IfStatement := 'if' '(' Expression ')' Statement [ 'else' Statement ]
     * </pre>
     */
    public Java.Statement parseIfStatement(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isKeyword("if")) this.throwParseException("\"if\" expected");
        Location location = this.scanner.read().getLocation();
        if (!this.scanner.read().isOperator("(")) this.throwParseException("Opening parenthesis expected after \"if\"");
        final Java.Rvalue condition = this.parseExpression(enclosingBlockStatement).toRvalueOrPE();
        if (!this.scanner.read().isOperator(")")) this.throwParseException("Closing parenthesis expected after \"if\" condition");

        Java.Statement thenStatement = this.parseStatement(enclosingBlockStatement);

        Java.Statement elseStatement = null;
        if (this.scanner.peek().isKeyword("else")) {
            this.scanner.read();
            elseStatement = this.parseStatement(enclosingBlockStatement);
        }

        return new Java.IfStatement(
            location,
            enclosingBlockStatement,
            condition,
            thenStatement,
            elseStatement
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
    public Java.Statement parseForStatement(
        Java.Scope enclosingScope
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isKeyword("for")) this.throwParseException("\"for\" expected");
        Location location = this.scanner.read().getLocation();

        Java.ForStatement forStatement = new Java.ForStatement(location, enclosingScope);

        if (!this.scanner.read().isOperator("(")) this.throwParseException("Opening parenthesis expected after \"for\"");

        Java.BlockStatement forInit = null;
        if (!this.scanner.peek().isOperator(";")) {
            forInit = this.parseForInit(
                forStatement.implicitBlock // enclosingBlock
            );
        }
        if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon expected after \"for\" initializer");

        Java.Rvalue forCondition = null;
        if (!this.scanner.peek().isOperator(";")) {
            forCondition = this.parseExpression((Java.BlockStatement) forStatement).toRvalueOrPE();
        }
        if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon expected after \"for\" condition");

        Java.Rvalue[] forUpdate = null;
        if (!this.scanner.peek().isOperator(")")) {
            forUpdate = this.parseExpressionList((Java.BlockStatement) forStatement);
        }
        if (!this.scanner.read().isOperator(")")) this.throwParseException("Closing parenthesis expected after \"for\" update");

        forStatement.set(
            forInit,
            forCondition,
            forUpdate,
            this.parseStatement((Java.BlockStatement) forStatement)
        );

        return forStatement;
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
    private Java.BlockStatement parseForInit(
        Java.Block enclosingBlock
    ) throws ParseException, Scanner.ScanException, IOException {

        // Modifiers Type LocalVariableDeclarators
        // ModifiersOpt BasicType LocalVariableDeclarators
        if (this.scanner.peek().isKeyword(new String[] { "final", "byte", "short", "char", "int", "long", "float", "double", "boolean" })) {
            short modifiers = this.parseModifiersOpt();
            Java.Type variableType = this.parseType((Java.Scope) enclosingBlock);
            return new Java.LocalVariableDeclarationStatement(
                this.scanner.peek().getLocation(),  // location
                enclosingBlock,                     // declaringBlock
                modifiers,                          // modifiers
                variableType,                       // type
                this.parseLocalVariableDeclarators( // variableDeclarators
                    (Java.BlockStatement) enclosingBlock, // enclosingBlockStatement
                    variableType                          // type
                )
            );
        }

        Java.Atom a = this.parseExpression((Java.BlockStatement) enclosingBlock);

        // Expression LocalVariableDeclarators
        if (this.scanner.peek().isIdentifier()) {
            Java.Type variableType = a.toTypeOrPE();
            return new Java.LocalVariableDeclarationStatement(
                a.getLocation(),                    // location
                enclosingBlock,                     // declaringBlock
                Mod.NONE,                           // modifiers
                variableType,                       // type
                this.parseLocalVariableDeclarators( // variableDeclarators
                    (Java.BlockStatement) enclosingBlock, // enclosingBlockStatement
                    variableType                          // type
                )
            );
        }

        // Expression { ',' Expression }
        if (!this.scanner.peek().isOperator(",")) {
            return new Java.ExpressionStatement(
                a.toRvalueOrPE(),               // rvalue
                (Java.Scope) enclosingBlock // enclosingScope
            );
        }
        this.scanner.read();
        List l = new ArrayList();
        l.add(new Java.ExpressionStatement(
            a.toRvalueOrPE(),               // rvalue
            (Java.Scope) enclosingBlock // enclosingScope
        ));
        for (;;) {
            l.add(new Java.ExpressionStatement(
                this.parseExpression(       // rvalue
                    (Java.BlockStatement) enclosingBlock
                ).toRvalueOrPE(),
                (Java.Scope) enclosingBlock // enclosingBlockStatement
            ));
            if (!this.scanner.peek().isOperator(",")) break;
            this.scanner.read();
        }

        Java.Block b = new Java.Block(
            a.getLocation(),            // location
            (Java.Scope) enclosingBlock // enclosingScope
        );
        b.addStatements(l);
        return b;
    }

    /**
     * <pre>
     *   WhileStatement := 'while' '(' Expression ')' Statement
     * </pre>
     */
    public Java.Statement parseWhileStatement(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isKeyword("while")) this.throwParseException("\"while\" expected");
        Location location = this.scanner.read().getLocation();

        if (!this.scanner.read().isOperator("(")) this.throwParseException("Opening parenthesis expected after \"while\"");
        Java.WhileStatement whileStatement = new Java.WhileStatement(
            location,
            enclosingBlockStatement,
            this.parseExpression(enclosingBlockStatement).toRvalueOrPE()
        );
        if (!this.scanner.read().isOperator(")")) this.throwParseException("Closing parenthesis expected after \"while\" condition");

        whileStatement.setBody(this.parseStatement(whileStatement));
        return whileStatement;
    }

    /**
     * <pre>
     *   DoStatement := 'do' Statement 'while' '(' Expression ')' ';'
     * </pre>
     */
    public Java.Statement parseDoStatement(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isKeyword("do")) this.throwParseException("\"do\" expected");
        Java.DoStatement doStatement = new Java.DoStatement(
            this.scanner.read().getLocation(),
            enclosingBlockStatement
        );

        doStatement.setBody(this.parseStatement(doStatement));

        if (!this.scanner.read().isKeyword("while")) this.throwParseException("\"while\" expected after body of \"do\" statement");
        if (!this.scanner.read().isOperator("(")) this.throwParseException("Opening parenthesis expected after \"while\"");
        doStatement.setCondition(this.parseExpression(doStatement).toRvalueOrPE());
        if (!this.scanner.read().isOperator(")")) this.throwParseException("Closing parenthesis expected after \"while\" condition");
        if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon expected at end of \"do\" statement");

        return doStatement;
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
    public Java.Statement parseTryStatement(
        Java.Scope enclosingScope
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isKeyword("try")) this.throwParseException("\"try\" expected");
        Java.TryStatement tryStatement = new Java.TryStatement(
            this.scanner.read().getLocation(),
            enclosingScope
        );

        tryStatement.setBody(this.parseBlock(tryStatement));

        while (this.scanner.peek().isKeyword("catch")) {
            this.scanner.read();
            if (!this.scanner.read().isOperator("(")) this.throwParseException("Opening parenthesis expected");
            Java.FormalParameter caughtException = this.parseFormalParameter(
                tryStatement
            );
            if (!this.scanner.read().isOperator(")")) this.throwParseException("Closing parenthesis expected");
            tryStatement.addCatchClause(new Java.CatchClause(
                caughtException,              // caughtException
                this.parseBlock(tryStatement) // body
            ));
        }
        if (this.scanner.peek().isKeyword("finally")) {
            this.scanner.read();
            // Important! The enclosing scope is the block enclosing the "try"
            // statement, not the "try" statement itself! Otherwise, a "return"
            // in the "finally" clause would lead to infinite recursion!
            tryStatement.setFinally(this.parseBlock(tryStatement));
        }
        if (
            tryStatement.catchClauses.size() == 0 &&
            tryStatement.optionalFinally == null
        ) this.throwParseException("\"try\" statement must have at least one \"catch\" clause or a \"finally\" clause");
        return tryStatement;
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
    public Java.Statement parseSwitchStatement(
        Java.Scope enclosingScope
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isKeyword("switch")) this.throwParseException("\"switch\" expected");
        Java.SwitchStatement switchStatement = new Java.SwitchStatement(
            this.scanner.read().getLocation(),
            enclosingScope
        );

        if (!this.scanner.read().isOperator("(")) this.throwParseException("Opening parenthesis expected");
        switchStatement.setCondition(this.parseExpression(switchStatement).toRvalueOrPE());
        if (!this.scanner.read().isOperator(")")) this.throwParseException("Closing parenthesis expected");

        if (!this.scanner.read().isOperator("{")) this.throwParseException("\"{\" expected");
        Java.Block block = new Java.Block(this.scanner.peek().getLocation(), switchStatement);
        while (!this.scanner.peek().isOperator("}")) {
            Java.SwitchBlockStatementGroup sbsg = new Java.SwitchBlockStatementGroup(this.scanner.peek().getLocation());
            do {
                if (this.scanner.peek().isKeyword("case")) {
                    this.scanner.read();
                    sbsg.addSwitchLabel(this.parseExpression((Java.BlockStatement) block).toRvalueOrPE());
                } else
                if (this.scanner.peek().isKeyword("default")) {
                    this.scanner.read();
                    sbsg.addDefaultSwitchLabel();
                } else {
                    this.throwParseException("\"case\" or \"default\" expected");
                }
                if (!this.scanner.read().isOperator(":")) this.throwParseException("Colon expected");
            } while (this.scanner.peek().isKeyword(new String[] { "case", "default" }));

            sbsg.setBlockStatements(this.parseBlockStatements(block));
            switchStatement.addSwitchBlockStatementGroup(sbsg);
        }
        this.scanner.read();
        return switchStatement;
    }

    /**
     * <pre>
     *   SynchronizedStatement :=
     *     'synchronized' '(' expression ')' Block
     * </pre>
     */
    public Java.Statement parseSynchronizedStatement(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isKeyword("synchronized")) this.throwParseException("\"synchronized\" expected");
        Location location = this.scanner.read().getLocation();
        if (!this.scanner.read().isOperator("(")) this.throwParseException("Opening parenthesis expected");
        Java.SynchronizedStatement synchronizedStatement = new Java.SynchronizedStatement(
            location,
            enclosingBlockStatement,
            this.parseExpression(enclosingBlockStatement).toRvalueOrPE()
        );
        if (!this.scanner.read().isOperator(")")) this.throwParseException("Closing parenthesis expected");

        synchronizedStatement.setBody(this.parseBlock(synchronizedStatement));

        return synchronizedStatement;
    }

    /**
     * <pre>
     *   ReturnStatement := 'return' [ Expression ] ';'
     * </pre>
     */
    public Java.Statement parseReturnStatement(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isKeyword("return")) this.throwParseException("\"return\" expected");
        Location location = this.scanner.read().getLocation();
        Java.Rvalue returnValue = this.scanner.peek().isOperator(";") ? null : this.parseExpression(enclosingBlockStatement).toRvalueOrPE();
        if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon expected at end of \"return\" statement");
        return new Java.ReturnStatement(location, enclosingBlockStatement, returnValue);
    }

    /**
     * <pre>
     *   ThrowStatement := 'throw' Expression ';'
     * </pre>
     */
    public Java.Statement parseThrowStatement(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isKeyword("throw")) this.throwParseException("\"throw\" expected");
        Location location = this.scanner.read().getLocation();
        final Java.Rvalue expression = this.parseExpression(enclosingBlockStatement).toRvalueOrPE();
        if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon expected");

        return new Java.ThrowStatement(
            location,
            enclosingBlockStatement,
            expression
        );
    }

    /**
     * <pre>
     *   BreakStatement := 'break' [ Identifier ] ';'
     * </pre>
     */
    public Java.Statement parseBreakStatement(
        Java.Scope enclosingScope
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isKeyword("break")) this.throwParseException("\"break\" expected");
        Location location = this.scanner.read().getLocation();
        String optionalLabel = null;
        if (this.scanner.peek().isIdentifier()) optionalLabel = this.scanner.read().getIdentifier();
        if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon expected at end of \"break\" statement");
        return new Java.BreakStatement(location, enclosingScope, optionalLabel);
    }

    /**
     * <pre>
     *   ContinueStatement := 'continue' [ Identifier ] ';'
     * </pre>
     */
    public Java.Statement parseContinueStatement(
        Java.Scope enclosingScope
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.peek().isKeyword("continue")) this.throwParseException("\"continue\" expected");
        Location location = this.scanner.read().getLocation();
        String optionalLabel = null;
        if (this.scanner.peek().isIdentifier()) optionalLabel = this.scanner.read().getIdentifier();
        if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon expected at end of \"continue\" statement");
        return new Java.ContinueStatement(location, enclosingScope, optionalLabel);
    }

    /**
     * <pre>
     *   EmptyStatement := ';'
     * </pre>
     */
    public Java.Statement parseEmptyStatement(
        Java.Scope enclosingScope
    ) throws ParseException, Scanner.ScanException, IOException {
        Scanner.Token t = this.scanner.read();
        if (!t.isOperator(";")) this.throwParseException("Semicolon expected");
        return new Java.EmptyStatement(t.getLocation(), enclosingScope);
    }

    /**
     * <pre>
     *   ExpressionList := Expression { ',' Expression }
     * </pre>
     */
    public Java.Rvalue[] parseExpressionList(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();
        for (;;) {
            l.add(this.parseExpression(enclosingBlockStatement).toRvalueOrPE());
            if (!this.scanner.peek().isOperator(",")) break;
            this.scanner.read();
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
    public Java.Type parseType(
        Java.Scope scope
    ) throws ParseException, Scanner.ScanException, IOException {
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
            this.scanner.read();
        } else {
            res = this.parseReferenceType(scope);
        }
        for (int i = this.parseBracketsOpt(); i > 0; --i) res = new Java.ArrayType(res);
        return res;
    }

    /**
     * <pre>
     *   ReferenceType := QualifiedIdentifier
     * </pre>
     */
    public Java.ReferenceType parseReferenceType(
        Java.Scope scope
    ) throws ParseException, Scanner.ScanException, IOException {
        return new Java.ReferenceType(
            this.scanner.peek().getLocation(), // location
            scope,                             // scope
            this.parseQualifiedIdentifier()    // identifiers
        );
    }

    /**
     * <pre>
     *   ReferenceTypeList := ReferenceType { ',' ReferenceType }
     * </pre>
     */
    public Java.ReferenceType[] parseReferenceTypeList(
        Java.Scope scope
    ) throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();
        l.add(this.parseReferenceType(scope));
        while (this.scanner.peek().isOperator(",")) {
            this.scanner.read();
            l.add(this.parseReferenceType(scope));
        }
        return (Java.ReferenceType[]) l.toArray(new Java.ReferenceType[l.size()]);
    }

    /**
     * <pre>
     *   Expression := AssignmentExpression
     * </pre>
     */
    public Java.Atom parseExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException  {
        return this.parseAssignmentExpression(enclosingBlockStatement);
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
    public Java.Atom parseAssignmentExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseConditionalExpression(enclosingBlockStatement);
        if (this.scanner.peek().isOperator(new String[] { "=", "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", "<<=", ">>=", ">>>=" })) {
            String operator = this.scanner.peek().getOperator();
            Location location = this.scanner.read().getLocation();
            final Java.Lvalue lhs = a.toLvalueOrPE();
            final Java.Rvalue rhs = this.parseAssignmentExpression(enclosingBlockStatement).toRvalueOrPE();
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
    public Java.Atom parseConditionalExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseConditionalOrExpression(enclosingBlockStatement);
        if (!this.scanner.peek().isOperator("?")) return a;
        Location location = this.scanner.read().getLocation();

        Java.Rvalue lhs = a.toRvalueOrPE();
        Java.Rvalue mhs = this.parseExpression(enclosingBlockStatement).toRvalueOrPE();
        if (!this.scanner.read().isOperator(":")) this.throwParseException("\":\" expected");
        Java.Rvalue rhs = this.parseConditionalExpression(enclosingBlockStatement).toRvalueOrPE();
        return new Java.ConditionalExpression(location, lhs, mhs, rhs);
    }

    /**
     * <pre>
     *   ConditionalOrExpression :=
     *     ConditionalAndExpression { '||' ConditionalAndExpression ]
     * </pre>
     */
    public Java.Atom parseConditionalOrExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseConditionalAndExpression(enclosingBlockStatement);
        while (this.scanner.peek().isOperator("||")) {
            a = new Java.BinaryOperation(
                this.scanner.read().getLocation(),
                a.toRvalueOrPE(),
                "||",
                this.parseConditionalAndExpression(enclosingBlockStatement).toRvalueOrPE()
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
    public Java.Atom parseConditionalAndExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseInclusiveOrExpression(enclosingBlockStatement);
        while (this.scanner.peek().isOperator("&&")) {
            a = new Java.BinaryOperation(
                this.scanner.read().getLocation(),
                a.toRvalueOrPE(),
                "&&",
                this.parseInclusiveOrExpression(enclosingBlockStatement).toRvalueOrPE()
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
    public Java.Atom parseInclusiveOrExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseExclusiveOrExpression(enclosingBlockStatement);
        while (this.scanner.peek().isOperator("|")) {
            a = new Java.BinaryOperation(
                this.scanner.read().getLocation(),
                a.toRvalueOrPE(),
                "|",
                this.parseExclusiveOrExpression(enclosingBlockStatement).toRvalueOrPE()
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
    public Java.Atom parseExclusiveOrExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseAndExpression(enclosingBlockStatement);
        while (this.scanner.peek().isOperator("^")) {
            a = new Java.BinaryOperation(
                this.scanner.read().getLocation(),
                a.toRvalueOrPE(),
                "^",
                this.parseAndExpression(enclosingBlockStatement).toRvalueOrPE()
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
    public Java.Atom parseAndExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseEqualityExpression(enclosingBlockStatement);
        while (this.scanner.peek().isOperator("&")) {
            a = new Java.BinaryOperation(
                this.scanner.read().getLocation(),
                a.toRvalueOrPE(),
                "&",
                this.parseEqualityExpression(enclosingBlockStatement).toRvalueOrPE()
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
    public Java.Atom parseEqualityExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseRelationalExpression(enclosingBlockStatement);

        while (this.scanner.peek().isOperator(new String[] { "==", "!=" })) {
            a = new Java.BinaryOperation(
                this.scanner.peek().getLocation(),
                a.toRvalueOrPE(),
                this.scanner.read().getOperator(),
                this.parseRelationalExpression(enclosingBlockStatement).toRvalueOrPE()
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
    public Java.Atom parseRelationalExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseShiftExpression(enclosingBlockStatement);

        for (;;) {
            if (this.scanner.peek().isKeyword("instanceof")) {
                a = new Java.Instanceof(
                    this.scanner.read().getLocation(),
                    a.toRvalueOrPE(),
                    this.parseType(enclosingBlockStatement)
                );
            } else
            if (this.scanner.peek().isOperator(new String[] { "<", ">", "<=", ">=" })) {
                a = new Java.BinaryOperation(
                    this.scanner.peek().getLocation(),
                    a.toRvalueOrPE(),
                    this.scanner.read().getOperator(),
                    this.parseShiftExpression(enclosingBlockStatement).toRvalueOrPE()
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
    public Java.Atom parseShiftExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseAdditiveExpression(enclosingBlockStatement);

        while (this.scanner.peek().isOperator(new String[] { "<<", ">>", ">>>" })) {
            a = new Java.BinaryOperation(
                this.scanner.peek().getLocation(),
                a.toRvalueOrPE(),
                this.scanner.read().getOperator(),
                this.parseAdditiveExpression(enclosingBlockStatement).toRvalueOrPE()
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
    public Java.Atom parseAdditiveExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException  {
        Java.Atom a = this.parseMultiplicativeExpression(enclosingBlockStatement);

        while (this.scanner.peek().isOperator(new String[] { "+", "-" })) {
            a = new Java.BinaryOperation(
                this.scanner.peek().getLocation(),
                a.toRvalueOrPE(),
                this.scanner.read().getOperator(),
                this.parseMultiplicativeExpression(enclosingBlockStatement).toRvalueOrPE()
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
    public Java.Atom parseMultiplicativeExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        Java.Atom a = this.parseUnaryExpression(enclosingBlockStatement);

        while (this.scanner.peek().isOperator(new String[] { "*", "/", "%" })) {
            a = new Java.BinaryOperation(
                this.scanner.peek().getLocation(),
                a.toRvalueOrPE(),
                this.scanner.read().getOperator(),
                this.parseUnaryExpression(enclosingBlockStatement).toRvalueOrPE()
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
    public Java.Atom parseUnaryExpression(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        if (this.scanner.peek().isOperator(new String[] { "++", "--" })) {
            return new Java.Crement(
                this.scanner.peek().getLocation(),                            // location
                this.scanner.read().getOperator(),                            // operator
                this.parseUnaryExpression(enclosingBlockStatement).toLvalueOrPE() // operand
            );
        }

        if (this.scanner.peek().isOperator(new String[] { "+", "-", "~", "!" })) {
            return new Java.UnaryOperation(
                this.scanner.peek().getLocation(),
                this.scanner.read().getOperator(),
                this.parseUnaryExpression(enclosingBlockStatement).toRvalueOrPE()
            );
        }

        Java.Atom a = this.parsePrimary(enclosingBlockStatement);

        while (this.scanner.peek().isOperator(new String[] { ".", "[" })) {
            a = this.parseSelector(enclosingBlockStatement, a);
        }

        while (this.scanner.peek().isOperator(new String[] { "++", "--" })) {
            a = new Java.Crement(
                this.scanner.peek().getLocation(), // location
                a.toLvalueOrPE(),                 // operand
                this.scanner.read().getOperator()  // operator
            );
        }

        return a;
    }

    /**
     * <pre>
     *   Primary :=
     *     '(' PrimitiveType { '[]' } ')' UnaryExpression | // CastExpression 15.16
     *     '(' Expression ')' UnaryExpression |    // CastExpression 15.16
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
     *     'new' ReferenceType Arguments [ ClassBody ] | // ClassInstanceCreationExpression 15.9
     *     'new' Type DimExprs { '[]' } |          // ArrayCreationExpression 15.10
     *     'new' ArrayType ArrayInitializer |      // ArrayInitializer 10.6
     *     BasicType { '[]' } |                    // Type
     *     BasicType { '[]' } '.' 'class' |        // ClassLiteral 15.8.2
     *     'void' '.' 'class'                      // ClassLiteral 15.8.2
     * </pre>
     */
    public Java.Atom parsePrimary(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        if (this.scanner.peek().isOperator("(")) {
            this.scanner.read();
            if (this.scanner.peek().isKeyword(new String[] { "boolean", "char", "byte", "short", "int", "long", "float", "double", })) {
                // '(' PrimitiveType { '[]' } ')' UnaryExpression
                Java.Type type = this.parseType(enclosingBlockStatement);
                int brackets = this.parseBracketsOpt();
                if (!this.scanner.read().isOperator(")")) this.throwParseException("Closing parenthesis expected");
                for (int i = 0; i < brackets; ++i) type = new Java.ArrayType(type);
                return new Java.Cast(
                    this.scanner.peek().getLocation(),
                    type,
                    this.parseUnaryExpression(enclosingBlockStatement).toRvalueOrPE()
                );
            }
            Java.Atom a = this.parseExpression(enclosingBlockStatement);
            if (!this.scanner.read().isOperator(")")) this.throwParseException("Closing parenthesis expected");

            if (
                this.scanner.peek().isLiteral() ||
                this.scanner.peek().isIdentifier() ||
                this.scanner.peek().isOperator(new String[] { "(", "~", "!", }) ||
                this.scanner.peek().isKeyword(new String[] { "this", "super", "new", } )
            ) {
                // '(' Expression ')' UnaryExpression
                return new Java.Cast(
                    this.scanner.peek().getLocation(),
                    a.toTypeOrPE(),
                    this.parseUnaryExpression(enclosingBlockStatement).toRvalueOrPE()
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
            Location location = this.scanner.peek().getLocation();
            String[] qi = this.parseQualifiedIdentifier();
            if (this.scanner.peek().isOperator("(")) {
                // Name Arguments
                return new Java.MethodInvocation(
                    this.scanner.peek().getLocation(),              // location
                    enclosingBlockStatement,                                 // enclosingScope
                    qi.length == 1 ? null : new Java.AmbiguousName( // optionalTarget
                        location,       // location
                        enclosingBlockStatement, // scope
                        qi,             // identifiers
                        qi.length - 1   // n
                    ),
                    qi[qi.length - 1],                              // methodName
                    this.parseArguments(enclosingBlockStatement)             // arguments
                );
            }
            if (
                this.scanner.peek().isOperator("[") &&
                this.scanner.peekNextButOne().isOperator("]")
            ) {
                // Name '[]' { '[]' }
                // Name '[]' { '[]' } '.' 'class'
                Java.Type res = new Java.ReferenceType(
                    location,       // location
                    enclosingBlockStatement, // scope
                    qi              // identifiers
                );
                int brackets = this.parseBracketsOpt();
                for (int i = 0; i < brackets; ++i) res = new Java.ArrayType(res);
                if (
                    this.scanner.peek().isOperator(".") &&
                    this.scanner.peekNextButOne().isKeyword("class")
                ) {
                    this.scanner.read();
                    return new Java.ClassLiteral(this.scanner.read().getLocation(), enclosingBlockStatement, res);
                } else {
                    return res;
                }
            }
            // Name
            return new Java.AmbiguousName(
                this.scanner.peek().getLocation(), // location
                enclosingBlockStatement,                    // scope
                qi                                 // identifiers
            );
        }

        if (this.scanner.peek().isKeyword("this")) {
            Location location = this.scanner.read().getLocation();
            if (this.scanner.peek().isOperator("(")) {

                // 'this' Arguments
                // Alternate constructor invocation (JLS 8.8.5.1)
                Java.Scope s = enclosingBlockStatement;
                while (!(s instanceof Java.FunctionDeclarator)) s = s.getEnclosingScope();
                if (!(s instanceof Java.ConstructorDeclarator)) this.throwParseException("Alternate constructor invocation only allowed in constructor context");
                Java.ConstructorDeclarator declaringConstructor = (Java.ConstructorDeclarator) s;
                Java.ClassDeclaration      declaringClass = (Java.ClassDeclaration) declaringConstructor.getDeclaringType();
                return new Java.AlternateConstructorInvocation(
                    location,                                    // location
                    declaringClass,                              // declaringClass
                    declaringConstructor,                        // declaringConstructor
                    this.parseArguments(enclosingBlockStatement) // arguments
                );
            } else
            {

                // 'this'
                return new Java.ThisReference(
                    location,       // location
                    enclosingBlockStatement  // scope
                );
            }
        }

        if (this.scanner.peek().isKeyword("super")) {
            this.scanner.read();
            if (this.scanner.peek().isOperator("(")) {

                // 'super' Arguments
                // Unqualified superclass constructor invocation (JLS 8.8.5.1)
                Java.Scope s = enclosingBlockStatement;
                while (!(s instanceof Java.FunctionDeclarator)) s = s.getEnclosingScope();
                if (!(s instanceof Java.ConstructorDeclarator)) this.throwParseException("Unqualified superclass constructor invocation only allowed in constructor context");                Java.ConstructorDeclarator declaringConstructor = (Java.ConstructorDeclarator) s;
                Java.ClassDeclaration      declaringClass = (Java.ClassDeclaration) declaringConstructor.getDeclaringType();
                return new Java.SuperConstructorInvocation(
                    this.scanner.peek().getLocation(),            // location
                    declaringClass,                               // declaringClass
                    declaringConstructor,                         // declaringConstructor
                    (Java.Rvalue) null,                           // optionalQualification
                    this.parseArguments(enclosingBlockStatement)  // arguments
                );
            }
            if (!this.scanner.read().isOperator(".")) this.throwParseException("\".\" expected after \"super\"");
            if (!this.scanner.peek().isIdentifier()) this.throwParseException("Identifier expected after \"super\"");
            String name = this.scanner.read().getIdentifier();
            if (this.scanner.peek().isOperator("(")) {

                // 'super' '.' Identifier Arguments
                return new Java.SuperclassMethodInvocation(
                    this.scanner.peek().getLocation(),  // location
                    enclosingBlockStatement,                     // enclosingScope
                    name,                               // methodName
                    this.parseArguments(enclosingBlockStatement) // arguments
                );
            } else {

                // 'super' '.' Identifier
                this.throwParseException("Superclass field access NYI");
            }
        }

        // 'new'
        if (this.scanner.peek().isKeyword("new")) {
            Location location = this.scanner.read().getLocation();
            Java.Type type = this.parseType(enclosingBlockStatement);
            if (type instanceof Java.ArrayType) {
                // 'new' ArrayType ArrayInitializer
                return this.parseArrayInitializer(
                    enclosingBlockStatement,
                    (Java.ArrayType) type
                );
            }
            if (
                type instanceof Java.ReferenceType &&
                this.scanner.peek().isOperator("(")
            ) {
                // 'new' ReferenceType Arguments [ ClassBody ]
                Java.Rvalue[] arguments = this.parseArguments(enclosingBlockStatement);
                if (this.scanner.peek().isOperator("{")) {
                    // 'new' ReferenceType Arguments ClassBody
                    final Java.AnonymousClassDeclaration anonymousClassDeclaration = new Java.AnonymousClassDeclaration(
                        this.scanner.peek().getLocation(), // location
                        enclosingBlockStatement,                    // enclosingScope
                        type                               // baseType
                    );
                    this.parseClassBody(anonymousClassDeclaration);
                    return new Java.NewAnonymousClassInstance(
                        location,                  // location
                        enclosingBlockStatement,            // scope
                        (Java.Rvalue) null,        // optionalQualification
                        anonymousClassDeclaration, // anonymousClassDeclaration
                        arguments                  // arguments
                    );
                } else {
                    // 'new' ReferenceType Arguments
                    return new Java.NewClassInstance(
                        location,           // location
                        enclosingBlockStatement,     // scope
                        (Java.Rvalue) null, // optionalQualification
                        type,               // type
                        arguments           // arguments
                    );
                }
            }
            // 'new' Type DimExprs { '[]' }
            return new Java.NewArray(
                location,                           // location
                type,                               // type
                this.parseDimExprs(enclosingBlockStatement), // dimExprs
                this.parseBracketsOpt()             // dims
            );
        }

        // BasicType
        if (this.scanner.peek().isKeyword(new String[] { "boolean", "char", "byte", "short", "int", "long", "float", "double", })) {
            Java.Type res = this.parseType(enclosingBlockStatement);
            int brackets = this.parseBracketsOpt();
            for (int i = 0; i < brackets; ++i) res = new Java.ArrayType(res);
            if (
                this.scanner.peek().isOperator(".") &&
                this.scanner.peekNextButOne().isKeyword("class")
            ) {
                // BasicType { '[]' } '.' 'class'
                this.scanner.read();
                return new Java.ClassLiteral(this.scanner.read().getLocation(), enclosingBlockStatement, res);
            }
            // BasicType { '[]' }
            return res;
        }

        // 'void'
        if (this.scanner.peek().isKeyword("void")) {
            this.scanner.read();
            if (
                this.scanner.peek().isOperator(".") &&
                this.scanner.peekNextButOne().isKeyword("class")
            ) {
                // 'void' '.' 'class'
                this.scanner.read();
                Location location = this.scanner.read().getLocation();
                return new Java.ClassLiteral(location, enclosingBlockStatement, new Java.BasicType(location, Java.BasicType.VOID));
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
    public Java.Atom parseSelector(
        Java.BlockStatement enclosingBlockStatement,
        Java.Atom           atom
    ) throws ParseException, Scanner.ScanException, IOException {
        if (this.scanner.peek().isOperator(".")) {
            this.scanner.read();
            if (this.scanner.peek().isIdentifier()) {
                String identifier = this.scanner.read().getIdentifier();
                if (this.scanner.peek().isOperator("(")) {
                    // '.' Identifier Arguments
                    return new Java.MethodInvocation(
                        this.scanner.peek().getLocation(),           // location
                        enclosingBlockStatement,                     // enclosingScope
                        atom.toRvalueOrPE(),                         // optionalTarget
                        identifier,                                  // methodName
                        this.parseArguments(enclosingBlockStatement) // arguments
                    );
                }
                // '.' Identifier
                return new Java.FieldAccessExpression(
                    this.scanner.peek().getLocation(), // location
                    enclosingBlockStatement,           // enclosingBlockStatement
                    atom.toRvalueOrPE(),               // lhs
                    identifier                         // fieldName
                );
            }
            if (this.scanner.peek().isKeyword("this")) {
                // '.' 'this'
                return new Java.QualifiedThisReference(
                    this.scanner.read().getLocation(), // location
                    enclosingBlockStatement,                    // scope
                    atom.toTypeOrPE()                  // qualification
                );
            }
            if (this.scanner.peek().isKeyword("super")) {
                Location location = this.scanner.read().getLocation();
                if (this.scanner.peek().isOperator("(")) {

                    // '.' 'super' Arguments
                    // Qualified superclass constructor invocation (JLS 8.8.5.1) (LHS is an Rvalue)
                    Java.Scope s = enclosingBlockStatement.getEnclosingScope();
                    if (!(s instanceof Java.ConstructorDeclarator)) this.throwParseException("Qualified superclass constructor does not appear in constructor scope");
                    Java.ConstructorDeclarator declaringConstructor = (Java.ConstructorDeclarator) s;
                    Java.ClassDeclaration declaringClass = (Java.ClassDeclaration) s.getEnclosingScope();
                    return new Java.SuperConstructorInvocation(
                        location,                           // location
                        declaringClass,                     // declaringClass
                        declaringConstructor,               // declaringConstructor
                        atom.toRvalueOrPE(),                // optionalQualification
                        this.parseArguments(enclosingBlockStatement) // arguments
                    );
                }
                if (!this.scanner.read().isOperator(".")) this.throwParseException("\"(\" or \".\" expected after \"super\"");
                /*String identifier =*/ this.scanner.read().getIdentifier();

                if (this.scanner.peek().isOperator("(")) {

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
            if (this.scanner.peek().isKeyword("new")) {
                // '.' 'new' Identifier Arguments [ ClassBody ]
                Java.Rvalue lhs = atom.toRvalue();
                Location location = this.scanner.read().getLocation();
                String identifier = this.scanner.read().getIdentifier();
                Java.Type type = new Java.RvalueMemberType(
                    location,  // location
                    lhs,       // rValue
                    identifier // identifier
                );
                Java.Rvalue[] arguments = this.parseArguments(enclosingBlockStatement);
                if (this.scanner.peek().isOperator("{")) {
                    // '.' 'new' Identifier Arguments ClassBody (LHS is an Rvalue)
                    final Java.AnonymousClassDeclaration anonymousClassDeclaration = new Java.AnonymousClassDeclaration(
                        this.scanner.peek().getLocation(), // location
                        enclosingBlockStatement,                    // enclosingScope
                        type                               // baseType
                    );
                    this.parseClassBody(anonymousClassDeclaration);
                    return new Java.NewAnonymousClassInstance(
                        location,                  // location
                        enclosingBlockStatement,            // scope
                        lhs,                       // optionalQualification
                        anonymousClassDeclaration, // anonymousClassDeclaration
                        arguments                  // arguments
                    );
                } else {
                    // '.' 'new' Identifier Arguments (LHS is an Rvalue)
                    return new Java.NewClassInstance(
                        location,       // location
                        enclosingBlockStatement, // scope
                        lhs,            // optionalQualification
                        type,           // referenceType
                        arguments       // arguments
                    );
                }
            }
            if (this.scanner.peek().isKeyword("class")) {
                // '.' 'class'
                return new Java.ClassLiteral(this.scanner.read().getLocation(), enclosingBlockStatement, atom.toTypeOrPE());
            }
            this.throwParseException("Unexpected selector \"" + this.scanner.peek() + "\" after \".\"");
        }
        if (this.scanner.peek().isOperator("[")) {
            // '[' Expression ']'
            Location location = this.scanner.read().getLocation();
            Java.Rvalue index = this.parseExpression(enclosingBlockStatement).toRvalueOrPE();
            if (!this.scanner.read().isOperator("]")) this.throwParseException("\"]\" expected");
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
    public Java.Rvalue[] parseDimExprs(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();
        l.add(this.parseDimExpr(enclosingBlockStatement));
        while (
            this.scanner.peek().isOperator("[") &&
            !this.scanner.peekNextButOne().isOperator("]")
        ) l.add(this.parseDimExpr(enclosingBlockStatement));
        return (Java.Rvalue[]) l.toArray(new Java.Rvalue[l.size()]);
    }

    /**
     * <pre>
     *   DimExpr := '[' Expression ']'
     * </pre>
     */
    public Java.Rvalue parseDimExpr(
        Java.BlockStatement enclosingBlockStatement
    ) throws Scanner.ScanException, ParseException, IOException {
        if (!this.scanner.read().isOperator("[")) this.throwParseException("Dimension expression (\"[...]\") expected");
        Java.Rvalue res = this.parseExpression(enclosingBlockStatement).toRvalueOrPE();
        if (!this.scanner.read().isOperator("]")) this.throwParseException("\"]\" expected");
        return res;
    }

    /**
     * <pre>
     *   Arguments := '(' [ ArgumentList ] ')'
     * </pre>
     */
    public Java.Rvalue[] parseArguments(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        if (!this.scanner.read().isOperator("(")) this.throwParseException("Opening parenthesis expected");
        if (this.scanner.peek().isOperator(")")) {
            this.scanner.read();
            return new Java.Rvalue[0];
        }
        Java.Rvalue[] arguments = this.parseArgumentList(enclosingBlockStatement);
        if (!this.scanner.read().isOperator(")")) this.throwParseException("Closing parenthesis after argument list expected");
        return arguments;
    }

    /**
     * <pre>
     *   ArgumentList := Expression { ',' Expression }
     * </pre>
     */
    public Java.Rvalue[] parseArgumentList(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        List l = new ArrayList();
        for (;;) {
            l.add(this.parseExpression(enclosingBlockStatement).toRvalueOrPE());
            if (!this.scanner.peek().isOperator(",")) break;
            this.scanner.read();
        }
        return (Java.Rvalue[]) l.toArray(new Java.Rvalue[l.size()]);
    }

    public Java.Atom parseLiteral() throws ParseException, Scanner.ScanException, IOException {
        Scanner.Token t = this.scanner.read();
        if (!t.isLiteral()) this.throwParseException("Literal expected");
        return new Java.Literal(t.getLocation(), t.getLiteralValue());
    }

    /**
     * <pre>
     *   ExpressionStatement := Expression ';'
     * </pre>
     */
    public Java.Statement parseExpressionStatement(
        Java.BlockStatement enclosingBlockStatement
    ) throws ParseException, Scanner.ScanException, IOException {
        Java.Rvalue rv = this.parseExpression(enclosingBlockStatement).toRvalueOrPE();
        if (!this.scanner.read().isOperator(";")) this.throwParseException("Semicolon at and of expression statement expected");

        return new Java.ExpressionStatement(rv, enclosingBlockStatement);
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
     * By default, warnings are discarded, but an application my install a (thread-local)
     * {@link WarningHandler}.
     */
    public void setWarningHandler(WarningHandler warningHandler) {
        this.warningHandler = warningHandler;
    }

    // Used for elaborate warning handling.
    private WarningHandler warningHandler = null;

    /**
     * Issues a warning with the given message an location an returns. This is done through
     * a {@link WarningHandler} that was installed through
     * {@link #setWarningHandler(WarningHandler)}.
     * <p>
     * The <code>handle</code> argument qulifies the warning and is typically used by
     * the {@link WarningHandler} to suppress individual warnings.
     *
     * @param handle
     * @param message
     * @param optionalLocation
     */
    private void warning(String handle, String message, Location optionalLocation) {
        WarningHandler wh = this.warningHandler;
        if (wh != null) wh.handleWarning(handle, message, optionalLocation);
    }

    private final Scanner scanner;

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
        throw new ParseException(message, this.scanner.peek().getLocation());
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
