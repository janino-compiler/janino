
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

package org.codehaus.janino;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.AbstractClassDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Java.AbstractCompilationUnit.ImportDeclaration;
import org.codehaus.janino.Java.AccessModifier;
import org.codehaus.janino.Java.AlternateConstructorInvocation;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.Annotation;
import org.codehaus.janino.Java.AnnotationTypeDeclaration;
import org.codehaus.janino.Java.AnonymousClassDeclaration;
import org.codehaus.janino.Java.ArrayAccessExpression;
import org.codehaus.janino.Java.ArrayCreationReference;
import org.codehaus.janino.Java.ArrayInitializer;
import org.codehaus.janino.Java.ArrayInitializerOrRvalue;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.AssertStatement;
import org.codehaus.janino.Java.Assignment;
import org.codehaus.janino.Java.Atom;
import org.codehaus.janino.Java.BinaryOperation;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BlockLambdaBody;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.BooleanLiteral;
import org.codehaus.janino.Java.BreakStatement;
import org.codehaus.janino.Java.Cast;
import org.codehaus.janino.Java.CatchClause;
import org.codehaus.janino.Java.CatchParameter;
import org.codehaus.janino.Java.CharacterLiteral;
import org.codehaus.janino.Java.ClassInstanceCreationReference;
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.ConditionalExpression;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.ContinueStatement;
import org.codehaus.janino.Java.Crement;
import org.codehaus.janino.Java.DoStatement;
import org.codehaus.janino.Java.ElementValue;
import org.codehaus.janino.Java.ElementValueArrayInitializer;
import org.codehaus.janino.Java.ElementValuePair;
import org.codehaus.janino.Java.EmptyStatement;
import org.codehaus.janino.Java.EnumConstant;
import org.codehaus.janino.Java.EnumDeclaration;
import org.codehaus.janino.Java.ExportsModuleDirective;
import org.codehaus.janino.Java.ExpressionLambdaBody;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FieldAccess;
import org.codehaus.janino.Java.FieldAccessExpression;
import org.codehaus.janino.Java.FieldDeclaration;
import org.codehaus.janino.Java.FloatingPointLiteral;
import org.codehaus.janino.Java.ForEachStatement;
import org.codehaus.janino.Java.ForStatement;
import org.codehaus.janino.Java.FormalLambdaParameters;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameter;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameters;
import org.codehaus.janino.Java.IdentifierLambdaParameters;
import org.codehaus.janino.Java.IfStatement;
import org.codehaus.janino.Java.InferredLambdaParameters;
import org.codehaus.janino.Java.Initializer;
import org.codehaus.janino.Java.Instanceof;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.InterfaceDeclaration;
import org.codehaus.janino.Java.LabeledStatement;
import org.codehaus.janino.Java.LambdaBody;
import org.codehaus.janino.Java.LambdaExpression;
import org.codehaus.janino.Java.LambdaParameters;
import org.codehaus.janino.Java.LocalClassDeclaration;
import org.codehaus.janino.Java.LocalClassDeclarationStatement;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.Lvalue;
import org.codehaus.janino.Java.MarkerAnnotation;
import org.codehaus.janino.Java.MemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.MemberClassDeclaration;
import org.codehaus.janino.Java.MemberEnumDeclaration;
import org.codehaus.janino.Java.MemberInterfaceDeclaration;
import org.codehaus.janino.Java.MemberTypeDeclaration;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.MethodInvocation;
import org.codehaus.janino.Java.MethodReference;
import org.codehaus.janino.Java.Modifier;
import org.codehaus.janino.Java.ModularCompilationUnit;
import org.codehaus.janino.Java.ModuleDeclaration;
import org.codehaus.janino.Java.ModuleDirective;
import org.codehaus.janino.Java.NamedClassDeclaration;
import org.codehaus.janino.Java.NewAnonymousClassInstance;
import org.codehaus.janino.Java.NewArray;
import org.codehaus.janino.Java.NewClassInstance;
import org.codehaus.janino.Java.NewInitializedArray;
import org.codehaus.janino.Java.NormalAnnotation;
import org.codehaus.janino.Java.NullLiteral;
import org.codehaus.janino.Java.OpensModuleDirective;
import org.codehaus.janino.Java.PackageDeclaration;
import org.codehaus.janino.Java.PackageMemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.PackageMemberClassDeclaration;
import org.codehaus.janino.Java.PackageMemberEnumDeclaration;
import org.codehaus.janino.Java.PackageMemberInterfaceDeclaration;
import org.codehaus.janino.Java.PackageMemberTypeDeclaration;
import org.codehaus.janino.Java.ParenthesizedExpression;
import org.codehaus.janino.Java.Primitive;
import org.codehaus.janino.Java.PrimitiveType;
import org.codehaus.janino.Java.ProvidesModuleDirective;
import org.codehaus.janino.Java.QualifiedThisReference;
import org.codehaus.janino.Java.ReferenceType;
import org.codehaus.janino.Java.RequiresModuleDirective;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.RvalueMemberType;
import org.codehaus.janino.Java.SingleElementAnnotation;
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
import org.codehaus.janino.Java.TypeArgument;
import org.codehaus.janino.Java.TypeParameter;
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.Java.UsesModuleDirective;
import org.codehaus.janino.Java.VariableDeclarator;
import org.codehaus.janino.Java.WhileStatement;
import org.codehaus.janino.Java.Wildcard;

/**
 * A parser for the Java programming language.
 * <p>
 *   'JLS7' refers to the <a href="http://docs.oracle.com/javase/specs/">Java Language Specification, Java SE 7
 *   Edition</a>.
 * </p>
 */
public
class Parser {

    private final Scanner     scanner;
    private final TokenStream tokenStream;

    public
    Parser(Scanner scanner) { this(scanner, new TokenStreamImpl(scanner)); }

    public
    Parser(Scanner scanner, TokenStream tokenStream) {
        (this.scanner = scanner).setIgnoreWhiteSpace(true);
        this.tokenStream = tokenStream;
    }

    /**
     * The optional JAVADOC comment preceding the {@link #nextToken}.
     */
    @Nullable private String optionalDocComment;

    /**
     * Gets the text of the doc comment (a.k.a. "JAVADOC comment") preceding the next token.
     *
     * @return {@code null} if the next token is not preceded by a doc comment
     */
    @Nullable public String
    doc() {
        String s = this.optionalDocComment;
        this.optionalDocComment = null;
        return s;
    }

    /**
     * @return The scanner that produces the tokens for this parser.
     */
    public Scanner
    getScanner() { return this.scanner; }

    /**
     * <pre>
     *   CompilationUnit := [ PackageDeclaration ]
     *                      { ImportDeclaration }
     *                      { TypeDeclaration }
     * </pre>
     */
    public AbstractCompilationUnit
    parseAbstractCompilationUnit() throws CompileException, IOException {


        String     docComment = this.doc();
        Modifier[] modifiers  = this.parseModifiers();

        PackageDeclaration optionalPackageDeclaration = null;
        if (this.peek("package")) {

            optionalPackageDeclaration = this.parsePackageDeclarationRest(docComment, modifiers);

            docComment = this.doc();
            modifiers  = this.parseModifiers();
        }

        ImportDeclaration[] importDeclarations;
        {
            List<ImportDeclaration> l = new ArrayList<ImportDeclaration>();
            while (this.peek("import")) {
                if (modifiers.length > 0) {
                    this.warning("import.modifiers", "No modifiers allowed on import declarations");
                }
                if (docComment != null) {
                    this.warning("import.doc_comment", "Doc comment on import declaration");
                }
                l.add(this.parseImportDeclaration());

                docComment = this.doc();
                modifiers  = this.parseModifiers();
            }
            importDeclarations = (ImportDeclaration[]) l.toArray(new ImportDeclaration[l.size()]);
        }

        if (this.peek("open", "module") != -1) {
            return new ModularCompilationUnit(
                this.location().getFileName(),
                importDeclarations,
                this.parseModuleDeclarationRest(modifiers)
            );
        }

        CompilationUnit compilationUnit = new CompilationUnit(this.location().getFileName(), importDeclarations);

        compilationUnit.setPackageDeclaration(optionalPackageDeclaration);

        if (this.peek(TokenType.END_OF_INPUT)) return compilationUnit;

        // Parse the first package-member type declaration.
        compilationUnit.addPackageMemberTypeDeclaration(
            this.parsePackageMemberTypeDeclarationRest(docComment, modifiers)
        );

        // Parse the second, third, ... package-member type declaration.
        while (!this.peek(TokenType.END_OF_INPUT)) {

            if (this.peekRead(";")) continue;

            compilationUnit.addPackageMemberTypeDeclaration(this.parsePackageMemberTypeDeclaration());
        }

        return compilationUnit;
    }

    public ModuleDeclaration
    parseModuleDeclarationRest(Modifier[] modifiers) throws CompileException, IOException {

        final boolean isOpen = this.peekRead("open");

        this.read("module");

        final String[] moduleName = this.parseQualifiedIdentifier();

        this.read("(");

        List<ModuleDirective> moduleDirectives = new ArrayList<ModuleDirective>();
        while (!this.peekRead(")")) {
            ModuleDirective md;
            switch (this.read("requires", "exports", "opens", "uses", "provides")) {
            case 0: // requires
                md = new RequiresModuleDirective(
                    this.location(),
                    this.parseModifiers(),
                    this.parseQualifiedIdentifier()
                );
                break;
            case 1: // exports
                {
                    String[] packageName = this.parseQualifiedIdentifier();

                    String[][] toModuleNames;
                    if (this.peekRead("to")) {
                        List<String[]> l = new ArrayList<String[]>();
                        l.add(this.parseQualifiedIdentifier());
                        while (this.peekRead(",")) l.add(this.parseQualifiedIdentifier());
                        toModuleNames = (String[][]) l.toArray(new String[l.size()][]);
                    } else {
                        toModuleNames = new String[0][];
                    }
                    md = new ExportsModuleDirective(this.location(), packageName, toModuleNames);
                }
                break;
            case 2: // opens
                {
                    String[] packageName = this.parseQualifiedIdentifier();

                    String[][] toModuleNames;
                    if (this.peekRead("to")) {
                        List<String[]> l = new ArrayList<String[]>();
                        l.add(this.parseQualifiedIdentifier());
                        while (this.peekRead(",")) l.add(this.parseQualifiedIdentifier());
                        toModuleNames = (String[][]) l.toArray(new String[l.size()][]);
                    } else {
                        toModuleNames = new String[0][];
                    }
                    md = new OpensModuleDirective(this.location(), packageName, toModuleNames);
                }
                break;
            case 3: // uses
                md = new UsesModuleDirective(this.location(), this.parseQualifiedIdentifier());
                break;
            case 4: // provides
                final String[] typeName = this.parseQualifiedIdentifier();
                this.read("with");
                List<String[]> withTypeNames = new ArrayList<String[]>();
                withTypeNames.add(this.parseQualifiedIdentifier());
                while (this.peekRead(",")) withTypeNames.add(this.parseQualifiedIdentifier());
                md = new ProvidesModuleDirective(
                    this.location(),
                    typeName,
                    (String[][]) withTypeNames.toArray(new String[withTypeNames.size()][])
                );
                break;
            default:
                throw new AssertionError();
            }
            this.read(";");
            moduleDirectives.add(md);
        }

        return new ModuleDeclaration(
            this.location(),
            modifiers,
            isOpen,
            moduleName,
            (ModuleDirective[]) moduleDirectives.toArray(new ModuleDirective[moduleDirectives.size()])
        );
    }

    /**
     * <pre>
     *   PackageDeclaration := 'package' QualifiedIdentifier ';'
     * </pre>
     */
    public PackageDeclaration
    parsePackageDeclarationRest(@Nullable String docComment, Modifier[] modifiers)
    throws CompileException, IOException {

        this.packageModifiers(modifiers);

        this.read("package");
        Location loc         = this.location();
        String   packageName = Parser.join(this.parseQualifiedIdentifier(), ".");
        this.read(";");
        this.verifyStringIsConventionalPackageName(packageName, loc);
        return new PackageDeclaration(loc, packageName);
    }

    /**
     * <pre>
     *   ImportDeclaration := 'import' ImportDeclarationBody ';'
     * </pre>
     */
    public AbstractCompilationUnit.ImportDeclaration
    parseImportDeclaration() throws CompileException, IOException {
        this.read("import");
        AbstractCompilationUnit.ImportDeclaration importDeclaration = this.parseImportDeclarationBody();
        this.read(";");
        return importDeclaration;
    }

    /**
     * <pre>
     *   ImportDeclarationBody := [ 'static' ] Identifier { '.' Identifier } [ '.' '*' ]
     * </pre>
     */
    public AbstractCompilationUnit.ImportDeclaration
    parseImportDeclarationBody() throws CompileException, IOException {
        final Location loc = this.location();

        boolean isStatic = this.peekRead("static");

        List<String> l = new ArrayList<String>();
        l.add(this.read(TokenType.IDENTIFIER));
        for (;;) {
            if (!this.peek(".")) {
                String[] identifiers = (String[]) l.toArray(new String[l.size()]);
                return (
                    isStatic
                    ? new AbstractCompilationUnit.SingleStaticImportDeclaration(loc, identifiers)
                    : new AbstractCompilationUnit.SingleTypeImportDeclaration(loc, identifiers)
                );
            }
            this.read(".");
            if (this.peekRead("*")) {
                String[] identifiers = (String[]) l.toArray(new String[l.size()]);
                return (
                    isStatic
                    ? new AbstractCompilationUnit.StaticImportOnDemandDeclaration(loc, identifiers)
                    : new AbstractCompilationUnit.TypeImportOnDemandDeclaration(loc, identifiers)
                );
            }
            l.add(this.read(TokenType.IDENTIFIER));
        }
    }

    /**
     * <pre>
     *   QualifiedIdentifier := Identifier { '.' Identifier }
     * </pre>
     */
    public String[]
    parseQualifiedIdentifier() throws CompileException, IOException {
        List<String> l = new ArrayList<String>();
        l.add(this.read(TokenType.IDENTIFIER));
        while (this.peek(".") && this.peekNextButOne().type == TokenType.IDENTIFIER) {
            this.read();
            l.add(this.read(TokenType.IDENTIFIER));
        }
        return (String[]) l.toArray(new String[l.size()]);
    }

    /**
     * <pre>
     *   PackageMemberTypeDeclaration := ModifiersOpt PackageMemberTypeDeclarationRest
     * </pre>
     */
    public PackageMemberTypeDeclaration
    parsePackageMemberTypeDeclaration() throws CompileException, IOException {

        return this.parsePackageMemberTypeDeclarationRest(this.doc(), this.parseModifiers());
    }

    /**
     * <pre>
     *   PackageMemberTypeDeclarationRest :=
     *             'class' ClassDeclarationRest |
     *             'enum' EnumDeclarationRest |
     *             'interface' InterfaceDeclarationRest
     *             '@' 'interface' AnnotationTypeDeclarationRest
     * </pre>
     */
    private PackageMemberTypeDeclaration
    parsePackageMemberTypeDeclarationRest(@Nullable String optionalDocComment, Modifier[] modifiers)
    throws CompileException, IOException {

        switch (this.read("class", "enum", "interface", "@")) {

        case 0: // "class"
            if (optionalDocComment == null) this.warning("CDCM", "Class doc comment missing");
            return (PackageMemberClassDeclaration) this.parseClassDeclarationRest(
                optionalDocComment,                      // optionalDocComment
                modifiers,                               // modifiers
                ClassDeclarationContext.COMPILATION_UNIT // context
            );

        case 1: // "enum"
            if (optionalDocComment == null) this.warning("EDCM", "Enum doc comment missing");
            return (PackageMemberEnumDeclaration) this.parseEnumDeclarationRest(
                optionalDocComment,                      // optionalDocComment
                modifiers,                               // modifiers
                ClassDeclarationContext.COMPILATION_UNIT // context
            );

        case 2: // "interface"
            if (optionalDocComment == null) this.warning("IDCM", "Interface doc comment missing");
            return (PackageMemberInterfaceDeclaration) this.parseInterfaceDeclarationRest(
                optionalDocComment,                          // optionalDocComment
                modifiers,                                   // modifiers
                InterfaceDeclarationContext.COMPILATION_UNIT // context
            );

        case 3: // "@"
            this.read("interface");
            if (optionalDocComment == null) {
                this.warning("ATDCM", "Annotation type doc comment missing");
            }
            return (PackageMemberAnnotationTypeDeclaration) this.parseAnnotationTypeDeclarationRest(
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
     *   Modifiers := { Modifier }
     * </pre>
     * <p>
     *   Includes the case "no modifiers".
     * </p>
     */
    public Modifier[]
    parseModifiers() throws CompileException, IOException {
        List<Modifier> result = new ArrayList<Modifier>();
        for (;;) {
            Modifier m = this.parseOptionalModifier();
            if (m == null) break;
            result.add(m);
        }
        return (Modifier[]) result.toArray(new Modifier[result.size()]);
    }

    /**
     * <pre>
     *   Modifier :=
     *       Annotation
     *       | 'public' | 'protected' | 'private'
     *       | 'static' | 'abstract' | 'final' | 'native' | 'synchronized' | 'transient' | 'volatile' | 'strictfp'
     *       | 'default'
     * </pre>
     */
    @Nullable public Modifier
    parseOptionalModifier() throws CompileException, IOException {

        if (this.peek("@")) {

            // Annotation type declaration ahead?
            if (this.peekNextButOne().value.equals("interface")) return null;

            return this.parseAnnotation();
        }

        int idx = this.peekRead(Parser.ACCESS_MODIFIER_KEYWORDS);
        if (idx == -1) return null;

        return new AccessModifier(Parser.ACCESS_MODIFIER_KEYWORDS[idx], this.location());
    }
    private static final String[] ACCESS_MODIFIER_KEYWORDS = {
        "public", "protected", "private",
        "static", "abstract", "final", "native", "synchronized", "transient", "volatile", "strictfp",
        "default",
        "transitive",
    };

    /**
     * <pre>
     *   Annotation :=
     *           MarkerAnnotation             // JLS7 9.7.2
     *           | SingleElementAnnotation    // JLS7 9.7.3
     *           | NormalAnnotation           // JLS7 9.7.1
     *
     *   MarkerAnnotation        := '@' Identifier
     *
     *   SingleElementAnnotation := '@' Identifier '(' ElementValue ')'
     *
     *   NormalAnnotation        := '@' TypeName '(' ElementValuePairsOpt ')'
     *
     *   ElementValuePairsOpt    := [ ElementValuePair { ',' ElementValuePair } ]
     * </pre>
     */
    private Annotation
    parseAnnotation() throws CompileException, IOException {
        this.read("@");
        ReferenceType type = new ReferenceType(
            this.location(),
            new Annotation[0],
            this.parseQualifiedIdentifier(),
            null                             // optionalTypeArguments
        );

        // Marker annotation?
        if (!this.peekRead("(")) return new MarkerAnnotation(type);

        // Single-element annotation?
        if (!this.peek(TokenType.IDENTIFIER) || !this.peekNextButOne("=")) {
            ElementValue elementValue = this.parseElementValue();
            this.read(")");
            return new SingleElementAnnotation(type, elementValue);
        }

        // Normal annotation.
        ElementValuePair[] elementValuePairs;
        if (this.peekRead(")")) {
            elementValuePairs = new ElementValuePair[0];
        } else {
            List<ElementValuePair> evps = new ArrayList<ElementValuePair>();
            do {
                evps.add(this.parseElementValuePair());
            } while (this.read(",", ")") == 0);
            elementValuePairs = (ElementValuePair[]) evps.toArray(new ElementValuePair[evps.size()]);
        }

        return new NormalAnnotation(type, elementValuePairs);
    }

    /**
     * <pre>
     *   ElementValuePair := Identifier '=' ElementValue
     * </pre>
     */
    private ElementValuePair
    parseElementValuePair() throws CompileException, IOException {
        String identifier = this.read(TokenType.IDENTIFIER);
        this.read("=");
        return new ElementValuePair(identifier, this.parseElementValue());
    }

    /**
     * <pre>
     *   ElementValue :=
     *           ConditionalExpression
     *           | Annotation
     *           | ElementValueArrayInitializer
     * </pre>
     */
    private ElementValue
    parseElementValue() throws CompileException, IOException {
        if (this.peek("@")) return this.parseAnnotation();
        if (this.peek("{")) return this.parseElementValueArrayInitializer();
        return this.parseConditionalAndExpression().toRvalueOrCompileException();
    }

    /**
     * <pre>
     *   ElementValueArrayInitializer := '{' { ElementValue | ',' } '}'
     * </pre>
     */
    private ElementValue
    parseElementValueArrayInitializer() throws CompileException, IOException {
        this.read("{");
        Location           loc = this.location();
        List<ElementValue> evs = new ArrayList<ElementValue>();
        while (!this.peekRead("}")) {
            if (this.peekRead(",")) continue;
            evs.add(this.parseElementValue());
        }
        return new ElementValueArrayInitializer((ElementValue[]) evs.toArray(new ElementValue[evs.size()]), loc);
    }

    /**
     * <pre>
     *   ClassDeclarationRest :=
     *        Identifier [ typeParameters ]
     *        [ 'extends' ReferenceType ]
     *        [ 'implements' ReferenceTypeList ]
     *        ClassBody
     * </pre>
     */
    public NamedClassDeclaration
    parseClassDeclarationRest(
        @Nullable String        optionalDocComment,
        Modifier[]              modifiers,
        ClassDeclarationContext context
    ) throws CompileException, IOException {
        Location location  = this.location();
        String   className = this.read(TokenType.IDENTIFIER);
        this.verifyIdentifierIsConventionalClassOrInterfaceName(className, location);

        TypeParameter[] optionalTypeParameters = this.parseTypeParametersOpt();

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
                location,                                    // location
                optionalDocComment,                          // optionalDocComment
                this.packageMemberClassModifiers(modifiers), // modifiers
                className,                                   // name
                optionalTypeParameters,                      // optionalTypeParameters
                optionalExtendedType,                        // optinalExtendedType
                implementedTypes                             // implementedTypes
            );
        } else
        if (context == ClassDeclarationContext.TYPE_DECLARATION) {
            namedClassDeclaration = new MemberClassDeclaration(
                location,                       // location
                optionalDocComment,             // optionalDocComment
                this.classModifiers(modifiers), // modifiers
                className,                      // name
                optionalTypeParameters,         // optionalTypeParameters
                optionalExtendedType,           // optionalExtendedType
                implementedTypes                // implementedTypes
            );
        } else
        if (context == ClassDeclarationContext.BLOCK) {
            namedClassDeclaration = new LocalClassDeclaration(
                location,                       // location
                optionalDocComment,             // optionalDocComment
                this.classModifiers(modifiers), // modifiers
                className,                      // name
                optionalTypeParameters,         // optionalTypeParameters
                optionalExtendedType,           // optionalExtendedType
                implementedTypes                // implementedTypes
            );
        } else
        {
            throw new InternalCompilerException("SNO: Class declaration in unexpected context " + context);
        }

        this.parseClassBody(namedClassDeclaration);
        return namedClassDeclaration;
    }

    /**
     * <pre>
     *   EnumDeclarationRest := Identifier [ 'implements' ReferenceTypeList ] EnumBody
     * </pre>
     */
    public EnumDeclaration
    parseEnumDeclarationRest(
        @Nullable String        optionalDocComment,
        Modifier[]              modifiers,
        ClassDeclarationContext context
    ) throws CompileException, IOException {
        Location location = this.location();
        String   enumName = this.read(TokenType.IDENTIFIER);
        this.verifyIdentifierIsConventionalClassOrInterfaceName(enumName, location);

        if (this.peekRead("<")) {
            throw this.compileException("Enum declaration must not have type parameters");
        }

        if (this.peekRead("extends")) {
            throw this.compileException("Enum declaration must not have an EXTENDS clause");
        }

        ReferenceType[] implementedTypes = new ReferenceType[0];
        if (this.peekRead("implements")) {
            implementedTypes = this.parseReferenceTypeList();
        }

        EnumDeclaration enumDeclaration;
        if (context == ClassDeclarationContext.COMPILATION_UNIT) {
            enumDeclaration = new PackageMemberEnumDeclaration(
                location,                       // location
                optionalDocComment,             // optionalDocComment
                this.classModifiers(modifiers), // modifiers
                enumName,                       // name
                implementedTypes                // implementedTypes
            );
        } else
        if (context == ClassDeclarationContext.TYPE_DECLARATION) {
            enumDeclaration = new MemberEnumDeclaration(
                location,                       // location
                optionalDocComment,             // optionalDocComment
                this.classModifiers(modifiers), // modifiers
                enumName,                       // name
                implementedTypes                // implementedTypes
            );
        } else
        {
            throw new InternalCompilerException("SNO: Enum declaration in unexpected context " + context);
        }

        this.parseEnumBody(enumDeclaration);

        return enumDeclaration;
    }

    /**
     * The kinds of context where a class declaration can occur.
     */
    public
    enum ClassDeclarationContext {

        /**
         * The class declaration appears inside a 'block'.
         */
        BLOCK,

        /**
         * The class declaration appears (directly) inside a type declaration.
         */
        TYPE_DECLARATION,

        /**
         * The class declaration appears on the top level.
         */
        COMPILATION_UNIT,
    }

    /**
     * The kinds of context where a method declaration can occur.
     */
    public
    enum MethodDeclarationContext {

        CLASS_DECLARATION,
        INTERFACE_DECLARATION,
        ANNOTATION_TYPE_DECLARATION,
    }

    /**
     * <pre>
     *   ClassBody := '{' { ClassBodyDeclaration } '}'
     * </pre>
     */
    public void
    parseClassBody(AbstractClassDeclaration classDeclaration) throws CompileException, IOException {
        this.read("{");

        for (;;) {
            if (this.peekRead("}")) return;

            this.parseClassBodyDeclaration(classDeclaration);
        }
    }

    /**
     * <pre>
     *   EnumBody := '{' [ EnumConstant { ',' EnumConstant } [ ',' ] [ ';' ] { ClassBodyDeclaration } '}'
     * </pre>
     */
    public void
    parseEnumBody(EnumDeclaration enumDeclaration) throws CompileException, IOException {
        this.read("{");

        while (this.peek(";", "}") == -1) {
            enumDeclaration.addConstant(this.parseEnumConstant());
            if (!this.peekRead(",")) break;
        }

        while (!this.peekRead("}")) {
            this.parseClassBodyDeclaration((AbstractClassDeclaration) enumDeclaration);
        }
    }

    /**
     * <pre>
     *   EnumConstant := [ Annotations ] Identifier [ Arguments ] [ ClassBody ]
     * </pre>
     */
    public EnumConstant
    parseEnumConstant() throws CompileException, IOException {

        EnumConstant result = new EnumConstant(
            this.location(),                                   // location
            this.doc(),                                        // optionalDocComment
            this.enumConstantModifiers(this.parseModifiers()), // modifiers
            this.read(TokenType.IDENTIFIER),                   // name
            this.peek("(") ? this.parseArguments() : null      // optionalArguments
        );

        if (this.peek("{")) {
            this.parseClassBody(result);
        }

        return result;
    }

    /**
     * <pre>
     *   ClassBodyDeclaration :=
     *     ';' |
     *     ModifiersOpt (
     *       Block |                                    // Instance (JLS7 8.6) or static initializer (JLS7 8.7)
     *       'void' Identifier MethodDeclarationRest |
     *       'class' ClassDeclarationRest |
     *       'interface' InterfaceDeclarationRest |
     *       ConstructorDeclarator |
     *       [ TypeArguments ] Type Identifier MethodDeclarationRest |
     *       Type Identifier FieldDeclarationRest ';'
     *     )
     * </pre>
     */
    public void
    parseClassBodyDeclaration(AbstractClassDeclaration classDeclaration) throws CompileException, IOException {
        if (this.peekRead(";")) return;

        final String     optionalDocComment = this.doc();
        final Modifier[] modifiers          = this.parseModifiers();

        // Initializer?
        if (this.peek("{")) {
            if (Parser.hasAccessModifierOtherThan(modifiers, "static")) {
                throw this.compileException("Only access flag \"static\" allowed on initializer");
            }

            Initializer initializer = new Initializer(
                this.location(),                               // location
                Parser.hasAccessModifier(modifiers, "static"), // statiC
                this.parseBlock()                              // block
            );

            classDeclaration.addInitializer(initializer);
            return;
        }

        // "void" method declaration (without type arguments).
        if (this.peekRead("void")) {
            Location location = this.location();
            if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", location);
            String name = this.read(TokenType.IDENTIFIER);
            classDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                optionalDocComment,                          // optionalDocComment
                this.methodModifiers(modifiers),             // modifiers
                null,                                        // optionalTypeParameters
                new PrimitiveType(location, Primitive.VOID), // type
                name,                                        // name
                false,                                       // allowDefaultClause
                MethodDeclarationContext.CLASS_DECLARATION   // context
            ));
            return;
        }

        // Member class.
        if (this.peekRead("class")) {
            if (optionalDocComment == null) this.warning("MCDCM", "Member class doc comment missing");
            classDeclaration.addMemberTypeDeclaration((MemberTypeDeclaration) this.parseClassDeclarationRest(
                optionalDocComment,                      // optionalDocComment
                this.classModifiers(modifiers),          // modifiers
                ClassDeclarationContext.TYPE_DECLARATION // context
            ));
            return;
        }

        // Member enum.
        if (this.peekRead("enum")) {
            if (optionalDocComment == null) this.warning("MEDCM", "Member enum doc comment missing");
            classDeclaration.addMemberTypeDeclaration((MemberTypeDeclaration) this.parseEnumDeclarationRest(
                optionalDocComment,                      // optionalDocComment
                this.classModifiers(modifiers),          // modifiers
                ClassDeclarationContext.TYPE_DECLARATION // context
            ));
            return;
        }

        // Member interface.
        if (this.peekRead("interface")) {
            if (optionalDocComment == null) {
                this.warning("MIDCM", "Member interface doc comment missing");
            }
            classDeclaration.addMemberTypeDeclaration((MemberTypeDeclaration) this.parseInterfaceDeclarationRest(
                optionalDocComment,                                // optionalDocComment
                this.interfaceModifiers(modifiers),                // modifiers
                InterfaceDeclarationContext.NAMED_TYPE_DECLARATION // context
            ));
            return;
        }

        // Member annotation type.
        if (this.peek("@") && this.peekNextButOne("interface")) {
            this.read();
            this.read();
            if (optionalDocComment == null) {
                this.warning("MATDCM", "Member annotation type doc comment missing", this.location());
            }
            classDeclaration.addMemberTypeDeclaration((MemberTypeDeclaration) this.parseInterfaceDeclarationRest(
                optionalDocComment,                                // optionalDocComment
                this.interfaceModifiers(modifiers),                // modifiers
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
                optionalDocComment,                  // declaringClass
                this.constructorModifiers(modifiers) // modifiers
            ));
            return;
        }

        // Member method or field.
        TypeParameter[] optionalTypeParameters = this.parseTypeParametersOpt();

        // VOID method declaration?
        if (this.peekRead("void")) {
            if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.location());
            String name = this.read(TokenType.IDENTIFIER);
            classDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                optionalDocComment,                                 // optionalDocComment
                this.methodModifiers(modifiers),                    // modifiers
                optionalTypeParameters,                             // optionalTypeParameters
                new PrimitiveType(this.location(), Primitive.VOID), // type
                name,                                               // name
                false,                                              // allowDefaultClause
                MethodDeclarationContext.CLASS_DECLARATION          // context
            ));
            return;
        }

        Type           memberType    = this.parseType();
        Location       location      = this.location();
        String         memberName    = this.read(TokenType.IDENTIFIER);

        // Method declarator.
        if (this.peek("(")) {
            if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.location());
            classDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                optionalDocComment,                        // optionalDocComment
                this.methodModifiers(modifiers),           // modifiers
                optionalTypeParameters,                    // optionalTypeParameters
                memberType,                                // type
                memberName,                                // name
                false,                                     // allowDefaultClause
                MethodDeclarationContext.CLASS_DECLARATION // context
            ));
            return;
        }

        // Field declarator.
        if (optionalTypeParameters != null) {
            throw this.compileException("Type parameters not allowed on field declaration");
        }
        if (optionalDocComment == null) this.warning("FDCM", "Field doc comment missing", this.location());
        FieldDeclaration fd = new FieldDeclaration(
            location,                                  // location
            optionalDocComment,                        // optionalDocComment
            this.fieldModifiers(modifiers),            // modifiers
            memberType,                                // type
            this.parseFieldDeclarationRest(memberName) // variableDeclarators
        );
        this.read(";");
        classDeclaration.addFieldDeclaration(fd);
    }

    /**
     * <pre>
     *   InterfaceDeclarationRest :=
     *     Identifier [ typeParameters ]
     *     [ 'extends' ReferenceTypeList ]
     *     InterfaceBody
     * </pre>
     */
    public InterfaceDeclaration
    parseInterfaceDeclarationRest(
        @Nullable String            optionalDocComment,
        Modifier[]                  modifiers,
        InterfaceDeclarationContext context
    ) throws CompileException, IOException {
        Location location      = this.location();
        String   interfaceName = this.read(TokenType.IDENTIFIER);
        this.verifyIdentifierIsConventionalClassOrInterfaceName(interfaceName, location);

        TypeParameter[] optionalTypeParameters = this.parseTypeParametersOpt();

        ReferenceType[] extendedTypes = new ReferenceType[0];
        if (this.peekRead("extends")) {
            extendedTypes = this.parseReferenceTypeList();
        }

        InterfaceDeclaration id;
        if (context == InterfaceDeclarationContext.COMPILATION_UNIT) {
            id = new PackageMemberInterfaceDeclaration(
                location,                                        // location
                optionalDocComment,                              // optionalDocComment
                this.packageMemberInterfaceModifiers(modifiers), // modifiers
                interfaceName,                                   // name
                optionalTypeParameters,                          // optionalTypeParameters
                extendedTypes                                    // extendedTypes
            );
        } else
        if (context == InterfaceDeclarationContext.NAMED_TYPE_DECLARATION) {
            id = new MemberInterfaceDeclaration(
                location,                           // location
                optionalDocComment,                 // optionalDocComment
                this.interfaceModifiers(modifiers), // modifiers
                interfaceName,                      // name
                optionalTypeParameters,             // optionalTypeParameters
                extendedTypes                       // extendedTypes
            );
        } else
        {
            throw new InternalCompilerException("SNO: Interface declaration in unexpected context " + context);
        }

        this.parseInterfaceBody(id);
        return id;
    }

    /**
     * <pre>
     *   AnnotationTypeDeclarationRest := Identifier AnnotationTypeBody
     * </pre>
     */
    public AnnotationTypeDeclaration
    parseAnnotationTypeDeclarationRest(
        @Nullable String            optionalDocComment,
        Modifier[]                  modifiers,
        InterfaceDeclarationContext context
    ) throws CompileException, IOException {

        Location location           = this.location();
        String   annotationTypeName = this.read(TokenType.IDENTIFIER);

        this.verifyIdentifierIsConventionalClassOrInterfaceName(annotationTypeName, location);

        AnnotationTypeDeclaration atd;
        if (context == InterfaceDeclarationContext.COMPILATION_UNIT) {
            atd = new PackageMemberAnnotationTypeDeclaration(
                location,                                        // location
                optionalDocComment,                              // optionalDocComment
                this.packageMemberInterfaceModifiers(modifiers), // modifiers
                annotationTypeName                               // name
            );
        } else
        if (context == InterfaceDeclarationContext.NAMED_TYPE_DECLARATION) {
            atd = new MemberAnnotationTypeDeclaration(
                location,                           // location
                optionalDocComment,                 // optionalDocComment
                this.interfaceModifiers(modifiers), // modifiers
                annotationTypeName                  // name
            );
        } else
        {
            throw new InternalCompilerException("SNO: Annotation type declaration in unexpected context " + context);
        }

        this.parseInterfaceBody((InterfaceDeclaration) atd);

        return atd;
    }

    /**
     * The kinds of context where an interface declaration can occur.
     */
    public
    enum InterfaceDeclarationContext {

        /**
         * The interface declaration appears (directly) inside a 'named type declaration'.
         */
        NAMED_TYPE_DECLARATION,

        /**
         * The interface declaration appears at the top level.
         */
        COMPILATION_UNIT,
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
    public void
    parseInterfaceBody(InterfaceDeclaration interfaceDeclaration) throws CompileException, IOException {
        this.read("{");

        while (!this.peekRead("}")) {

            if (this.peekRead(";")) continue;

            String     optionalDocComment = this.doc();
            Modifier[] modifiers          = this.parseModifiers();

            // "void" method declaration (without type parameters).
            if (this.peekRead("void")) {
                if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.location());
                if (Parser.hasAccessModifier(modifiers, "default")) {
                    throw this.compileException("Default interface methods not implemented");
                }
                interfaceDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                    optionalDocComment,                                 // optionalDocComment
                    modifiers,                                          // modifiers
                    null,                                               // optionalTypeParameters
                    new PrimitiveType(this.location(), Primitive.VOID), // type
                    this.read(TokenType.IDENTIFIER),                    // name
                    true,                                               // allowDefaultClause
                    (                                                   // context
                        interfaceDeclaration instanceof AnnotationTypeDeclaration
                        ? MethodDeclarationContext.ANNOTATION_TYPE_DECLARATION
                        : MethodDeclarationContext.INTERFACE_DECLARATION
                    )
                ));
                continue;
            }

            // Member class.
            if (this.peekRead("class")) {
                if (optionalDocComment == null) {
                    this.warning("MCDCM", "Member class doc comment missing", this.location());
                }
                if (Parser.hasAccessModifier(modifiers, "default")) {
                    throw this.compileException("Modifier \"default\" not allowed on member class declaration");
                }
                interfaceDeclaration.addMemberTypeDeclaration(
                    (MemberTypeDeclaration) this.parseClassDeclarationRest(
                        optionalDocComment,                      // optionalDocComment
                        this.classModifiers(modifiers),          // modifiers
                        ClassDeclarationContext.TYPE_DECLARATION // context
                    )
                );
                continue;
            }

            // Member enum.
            if (this.peekRead("enum")) {
                if (optionalDocComment == null) {
                    this.warning("MEDCM", "Member enum doc comment missing", this.location());
                }
                if (Parser.hasAccessModifier(modifiers, "default")) {
                    throw this.compileException("Modifier \"default\" not allowed on member enum declaration");
                }
                interfaceDeclaration.addMemberTypeDeclaration(
                    (MemberTypeDeclaration) this.parseClassDeclarationRest(
                        optionalDocComment,                      // optionalDocComment
                        this.classModifiers(modifiers),          // modifiers
                        ClassDeclarationContext.TYPE_DECLARATION // context
                    )
                );
                continue;
            }

            // Member interface.
            if (this.peekRead("interface")) {
                if (optionalDocComment == null) {
                    this.warning("MIDCM", "Member interface doc comment missing", this.location());
                }
                if (Parser.hasAccessModifier(modifiers, "default")) {
                    throw this.compileException("Modifier \"default\" not allowed on member interface declaration");
                }
                interfaceDeclaration.addMemberTypeDeclaration(
                    (MemberTypeDeclaration) this.parseInterfaceDeclarationRest(
                        optionalDocComment,                                // optionalDocComment
                        this.interfaceModifiers(modifiers),                // modifiers
                        InterfaceDeclarationContext.NAMED_TYPE_DECLARATION // context
                    )
                );
                continue;
            }

            // Member annotation type.
            if (this.peek("@") && this.peekNextButOne("interface")) {
                this.read();
                this.read();
                if (optionalDocComment == null) {
                    this.warning("MATDCM", "Member annotation type doc comment missing", this.location());
                }
                if (Parser.hasAccessModifier(modifiers, "default")) {
                    throw this.compileException(
                        "Modifier \"default\" not allowed on member annotation type declaration"
                    );
                }
                interfaceDeclaration.addMemberTypeDeclaration(
                    (MemberTypeDeclaration) this.parseInterfaceDeclarationRest(
                        optionalDocComment,                                // optionalDocComment
                        this.interfaceModifiers(modifiers),                // modifiers
                        InterfaceDeclarationContext.NAMED_TYPE_DECLARATION // context
                    )
                );
                continue;
            }

            // Member method or field.

            TypeParameter[] optionalTypeParameters = this.parseTypeParametersOpt();

            // "void" method declaration?
            if (this.peekRead("void")) {
                if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.location());
                if (Parser.hasAccessModifier(modifiers, "default")) {
                    throw this.compileException("Default interface methods not implemented");
                }
                Location location = this.location();
                String   name     = this.read(TokenType.IDENTIFIER);
                interfaceDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                    optionalDocComment,                            // optionalDocComment
                    modifiers,                                     // modifiers
                    optionalTypeParameters,                        // optionalTypeParameters
                    new PrimitiveType(location, Primitive.VOID),   // type
                    name,                                          // name
                    true,                                          // allowDefaultClause
                    (                                              // context
                        interfaceDeclaration instanceof AnnotationTypeDeclaration
                        ? MethodDeclarationContext.ANNOTATION_TYPE_DECLARATION
                        : MethodDeclarationContext.INTERFACE_DECLARATION
                    )
                ));
                continue;
            }

            Type     memberType = this.parseType();
            String   memberName = this.read(TokenType.IDENTIFIER);
            Location location   = this.location();

            // Method declarator?
            if (this.peek("(")) {
                if (optionalDocComment == null) this.warning("MDCM", "Method doc comment missing", this.location());
                interfaceDeclaration.addDeclaredMethod(this.parseMethodDeclarationRest(
                    optionalDocComment,                            // optionalDocComment
                    modifiers,                                     // modifiers
                    optionalTypeParameters,                        // optionalTypeParameters
                    memberType,                                    // type
                    memberName,                                    // name
                    true,                                          // allowDefaultClause
                    (                                              // context
                        interfaceDeclaration instanceof AnnotationTypeDeclaration
                        ? MethodDeclarationContext.ANNOTATION_TYPE_DECLARATION
                        : MethodDeclarationContext.INTERFACE_DECLARATION
                    )
                ));
                continue;
            }

            // Must be a constant declarator.
            if (optionalTypeParameters != null) {
                throw this.compileException("Type parameters not allowed with constant declaration");
            }
            if (optionalDocComment == null) this.warning("CDCM", "Constant doc comment missing", this.location());
            if (Parser.hasAccessModifier(modifiers, "default")) {
                throw this.compileException("Modifier \"default\" not allowed for constants");
            }
            FieldDeclaration cd = new FieldDeclaration(
                location,                                  // location
                optionalDocComment,                        // optionalDocComment
                this.constantModifiers(modifiers),         // modifiers
                memberType,                                // type
                this.parseFieldDeclarationRest(memberName) // variableDeclarators
            );
            interfaceDeclaration.addConstantDeclaration(cd);
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
    public ConstructorDeclarator
    parseConstructorDeclarator(@Nullable String optionalDocComment, Modifier[] modifiers)
    throws CompileException, IOException {
        this.read(TokenType.IDENTIFIER);  // Class name

        // Parse formal parameters.
        final FormalParameters formalParameters = this.parseFormalParameters();

        // Parse "throws" clause.
        ReferenceType[] thrownExceptions;
        if (this.peekRead("throws")) {
            thrownExceptions = this.parseReferenceTypeList();
        } else {
            thrownExceptions = new ReferenceType[0];
        }

        // Parse constructor body.
        final Location location = this.location();
        this.read("{");

        // Special treatment for the first statement of the constructor body: If this is surely an
        // expression statement, and if it could be a "ConstructorInvocation", then parse the
        // expression and check if it IS a ConstructorInvocation.
        ConstructorInvocation optionalConstructorInvocation = null;
        List<BlockStatement>  statements                    = new ArrayList<BlockStatement>();
        if (
            this.peek(
                "this", "super", "new", "void", // SUPPRESS CHECKSTYLE Wrap:1
                "byte", "char", "short", "int", "long", "float", "double", "boolean"
            ) != -1
            || this.peekLiteral()
            || this.peek(TokenType.IDENTIFIER)
        ) {
            Atom a = this.parseExpression();
            if (a instanceof ConstructorInvocation) {
                this.read(";");
                optionalConstructorInvocation = (ConstructorInvocation) a;
            } else {
                Statement s;
                if (this.peek(TokenType.IDENTIFIER)) {
                    Type variableType = a.toTypeOrCompileException();
                    s = new LocalVariableDeclarationStatement(
                        a.getLocation(),                // location
                        new Modifier[0],                // modifiers
                        variableType,                   // type
                        this.parseVariableDeclarators() // variableDeclarators
                    );
                    this.read(";");
                } else {
                    s = new ExpressionStatement(a.toRvalueOrCompileException());
                    this.read(";");
                }
                statements.add(s);
            }
        }
        statements.addAll(this.parseBlockStatements());

        this.read("}");

        return new ConstructorDeclarator(
            location,                             // location
            optionalDocComment,                   // optionalDocComment
            this.constructorModifiers(modifiers), // modifiers
            formalParameters,                     // formalParameters
            thrownExceptions,                     // thrownExceptions
            optionalConstructorInvocation,        // optionalConstructorInvocationStatement
            statements                            // statements
        );
    }

    /**
     * Equivalent with {@code parseMethodDeclaration(false, MethodDeclarationContext.CLASS_DECLARATION)}.
     *
     * @see #parseMethodDeclaration(boolean, MethodDeclarationContext)
     */
    public MethodDeclarator
    parseMethodDeclaration() throws CompileException, IOException {
        return this.parseMethodDeclaration(false, MethodDeclarationContext.CLASS_DECLARATION);
    }

    /**
     * <pre>
     *   MethodDeclaration :=
     *     [ DocComment ] Modifiers [ TypeParameters ] VoidOrType Identifier MethodDeclarationRest
     * </pre>
     *
     * @param allowDefaultClause Whether a "default clause" for an "annotation type element" (JLS8 9.6.2) should be
     *                           parsed
     */
    public MethodDeclarator
    parseMethodDeclaration(boolean allowDefaultClause, MethodDeclarationContext context)
    throws CompileException, IOException {

        return this.parseMethodDeclarationRest(
            this.doc(),                      // optionalDocComment
            this.parseModifiers(),           // modifiers
            this.parseTypeParametersOpt(),   // optionalTypeParameters
            this.parseVoidOrType(),          // type
            this.read(TokenType.IDENTIFIER), // name
            allowDefaultClause,              // allowDefaultClause
            context                          // context
        );
    }

    /**
     * <pre>
     *   VoidOrType := 'void' | Type
     * </pre>
     */
    public Type
    parseVoidOrType() throws CompileException, IOException {
        return this.peekRead("void") ? new PrimitiveType(this.location(), Primitive.VOID) : this.parseType();
    }

    /**
     * <pre>
     *   MethodDeclarationRest :=
     *     FormalParameters
     *     { '[' ']' }
     *     [ 'throws' ReferenceTypeList ]
     *     [ 'default' expression ]
     *     ( ';' | MethodBody )
     * </pre>
     *
     * @param allowDefaultClause Whether a "default clause" for an "annotation type element" (JLS8 9.6.2) should be
     *                           parsed
     */
    public MethodDeclarator
    parseMethodDeclarationRest(
        @Nullable String          optionalDocComment,
        Modifier[]                modifiers,
        @Nullable TypeParameter[] optionalTypeParameters,
        Type                      type,
        String                    name,
        boolean                   allowDefaultClause,
        MethodDeclarationContext  context
    ) throws CompileException, IOException {
        Location location = this.location();

        this.verifyIdentifierIsConventionalMethodName(name, location);

        final FormalParameters formalParameters = this.parseFormalParameters();

        for (int i = this.parseBracketsOpt(); i > 0; --i) type = new ArrayType(type);

        ReferenceType[] thrownExceptions;
        if (this.peekRead("throws")) {
            thrownExceptions = this.parseReferenceTypeList();
        } else {
            thrownExceptions = new ReferenceType[0];
        }

        ElementValue defaultValue = (
            allowDefaultClause && this.peekRead("default")
            ? this.parseElementValue()
            : null
        );

        List<BlockStatement> optionalStatements;
        if (this.peekRead(";")) {
            optionalStatements = null;
        } else {
            if (Parser.hasAccessModifier(modifiers, "abstract", "native")) {
                throw this.compileException("Abstract or native method must not have a body");
            }
            this.read("{");
            optionalStatements = this.parseBlockStatements();
            this.read("}");
        }

        return new MethodDeclarator(
            location,                          // location
            optionalDocComment,                // optionalDocComment
            (                                  // modifiers
                context == MethodDeclarationContext.ANNOTATION_TYPE_DECLARATION
                ? this.annotationTypeElementModifiers(modifiers)
                : context == MethodDeclarationContext.CLASS_DECLARATION
                ? this.methodModifiers(modifiers)
                : context == MethodDeclarationContext.INTERFACE_DECLARATION
                ? this.interfaceMethodModifiers(modifiers)
                : new Modifier[1]
            ),
            optionalTypeParameters,            // optionalTypeParameters
            type,                              // type
            name,                              // name
            formalParameters,                  // formalParameters
            thrownExceptions,                  // thrownExceptions
            defaultValue,                      // defaultValue
            optionalStatements                 // optionalStatements
        );
    }

    /**
     * <pre>
     *   VariableInitializer :=
     *     ArrayInitializer |
     *     Expression
     * </pre>
     */
    public ArrayInitializerOrRvalue
    parseVariableInitializer() throws CompileException, IOException {
        if (this.peek("{")) {
            return this.parseArrayInitializer();
        } else
        {
            return this.parseExpression().toRvalueOrCompileException();
        }
    }

    /**
     * <pre>
     *   ArrayInitializer :=
     *     '{' [ VariableInitializer { ',' VariableInitializer } [ ',' ] '}'
     * </pre>
     */
    public ArrayInitializer
    parseArrayInitializer() throws CompileException, IOException {
        final Location location = this.location();
        this.read("{");
        List<ArrayInitializerOrRvalue> l = new ArrayList<ArrayInitializerOrRvalue>();
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
     *   FormalParameters := '(' [ FormalParameterList ] ')'
     * </pre>
     */
    public FormalParameters
    parseFormalParameters() throws CompileException, IOException {

        this.read("(");

        if (this.peekRead(")")) return new FormalParameters(this.location());

        FormalParameters result = this.parseFormalParameterList();

        this.read(")");

        return result;
    }

    /**
     * <pre>
     *   FormalParameterList := FormalParameter { ',' FormalParameter }
     * </pre>
     */
    public FormalParameters
    parseFormalParameterList() throws CompileException, IOException {

        List<FormalParameter> l           = new ArrayList<FormalParameter>();
        final boolean[]       hasEllipsis = new boolean[1];
        do {
            if (hasEllipsis[0]) throw this.compileException("Only the last parameter may have an ellipsis");
            l.add(this.parseFormalParameter(hasEllipsis));
        } while (this.peekRead(","));
        return new FormalParameters(
            this.location(),                                              // location
            (FormalParameter[]) l.toArray(new FormalParameter[l.size()]), // parameters
            hasEllipsis[0]                                                // variableArity
        );
    }

    /**
     * <pre>
     *   FormalParameterListRest := Identifier { ',' FormalParameter }
     * </pre>
     */
    public FormalParameters
    parseFormalParameterListRest(Type firstParameterType) throws CompileException, IOException {

        List<FormalParameter> l           = new ArrayList<FormalParameter>();
        final boolean[]       hasEllipsis = new boolean[1];

        l.add(this.parseFormalParameterRest(false, firstParameterType, hasEllipsis));

        while (this.peekRead(",")) {
            if (hasEllipsis[0]) throw this.compileException("Only the last parameter may have an ellipsis");
            l.add(this.parseFormalParameter(hasEllipsis));
        }

        return new FormalParameters(
            this.location(),                                              // location
            (FormalParameter[]) l.toArray(new FormalParameter[l.size()]), // parameters
            hasEllipsis[0]                                                // variableArity
        );
    }

    /**
     * <pre>
     *   FormalParameter := [ 'final' ] Type FormalParameterRest
     * </pre>
     */
    public FormalParameter
    parseFormalParameter(boolean[] hasEllipsis) throws CompileException, IOException {

        Modifier[] modifiers = this.parseModifiers();
        if (Parser.hasAccessModifier(modifiers, "default")) {
            throw this.compileException("Modifier \"default\" not allowed on formal parameters");
        }

        // Ignore annotations.

        return this.parseFormalParameterRest(
            Parser.hasAccessModifier(modifiers, "final"),
            this.parseType(),
            hasEllipsis
        );
    }

    /**
     * <pre>
     *   FormalParameterRest := [ '.' '.' '.' ] Identifier BracketsOpt
     * </pre>
     */
    public FormalParameter
    parseFormalParameterRest(boolean finaL, Type type, boolean[] hasEllipsis) throws CompileException, IOException {

        if (this.peekRead(".")) {
            this.read(".");
            this.read(".");
            hasEllipsis[0] = true;
        }

        Location location = this.location();
        String   name     = this.read(TokenType.IDENTIFIER);
        this.verifyIdentifierIsConventionalLocalVariableOrParameterName(name, location);

        for (int i = this.parseBracketsOpt(); i > 0; --i) type = new ArrayType(type);
        return new FormalParameter(location, finaL, type, name);
    }

    /**
     * <pre>
     *   CatchFormalParameter      := { VariableModifier } CatchType VariableDeclaratorId
     *   CatchType                 := UnannClassType { '|' ClassType }
     *   VariableModifier          := Annotation | 'final'
     *   VariableDeclaratorId      := Identifier [ Dims ]
     *   Dims                      := { Annotation } '[' ']' { { Annotation } '[' ']' }
     *   UnannClassType            :=
     *       Identifier [ TypeArguments ]
     *       | UnannClassOrInterfaceType '.' { Annotation } Identifier [ TypeArguments ]
     *   UnannInterfaceType        := UnannClassType
     *   UnannClassOrInterfaceType := UnannClassType | UnannInterfaceType
     *   ClassType                 :=
     *       { Annotation } Identifier [ TypeArguments ]
     *       | ClassOrInterfaceType '.' { Annotation } Identifier [ TypeArguments ]
     * </pre>
     */
    public CatchParameter
    parseCatchParameter() throws CompileException, IOException {

        Modifier[] modifiers = this.parseModifiers();
        this.variableModifiers(modifiers);

        List<ReferenceType> catchTypes = new ArrayList<ReferenceType>();
        catchTypes.add(this.parseReferenceType());
        while (this.peekRead("|")) catchTypes.add(this.parseReferenceType());

        Location location = this.location();
        String   name     = this.read(TokenType.IDENTIFIER);
        this.verifyIdentifierIsConventionalLocalVariableOrParameterName(name, location);

        return new CatchParameter(
            location,
            Parser.hasAccessModifier(modifiers, "final"),
            (ReferenceType[]) catchTypes.toArray(new ReferenceType[catchTypes.size()]),
            name
        );
    }

    /**
     * <pre>
     *   BracketsOpt := { '[' ']' }
     * </pre>
     */
    int
    parseBracketsOpt() throws CompileException, IOException {
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
    public Block
    parseMethodBody() throws CompileException, IOException { return this.parseBlock(); }

    /**
     * <pre>
     *   Block := '{' BlockStatements '}'
     * </pre>
     */
    public Block
    parseBlock() throws CompileException, IOException {
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
    public List<BlockStatement>
    parseBlockStatements() throws CompileException, IOException {

        List<BlockStatement> l = new ArrayList<BlockStatement>();
        while (
            !this.peek("}")
            && !this.peek("case")
            && !this.peek("default")
            && !this.peek(TokenType.END_OF_INPUT)
        ) l.add(this.parseBlockStatement());

        return l;
    }

    /**
     * <pre>
     *   BlockStatement :=
     *     Statement |                                     (1)
     *     'class' ... |                                   (2)
     *     Modifiers Type VariableDeclarators ';' |
     *     Expression ';' |
     *     Expression BracketsOpt VariableDeclarators ';'  (3)
     * </pre>
     *
     * <p>
     *   (1) Includes the "labeled statement".
     * </p>
     * <p>
     *   (2) Local class declaration.
     * </p>
     * <p>
     *   (3) Local variable declaration statement; "Expression" must pose a type, and has optional trailing brackets.
     * </p>
     */
    public BlockStatement
    parseBlockStatement() throws CompileException, IOException {

        // Statement?
        if (
            (this.peek(TokenType.IDENTIFIER) && this.peekNextButOne(":"))
            || this.peek(
                "if", "for", "while", "do", "try", "switch", "synchronized", // SUPPRESS CHECKSTYLE Wrap|LineLength:1
                "return", "throw", "break", "continue", "assert"
            ) != -1
            || this.peek("{", ";") != -1
        ) return this.parseStatement();

        // Local class declaration?
        if (this.peekRead("class")) {
            // JAVADOC[TM] ignores doc comments for local classes, but we
            // don't...
            String optionalDocComment = this.doc();
            if (optionalDocComment == null) this.warning("LCDCM", "Local class doc comment missing", this.location());

            final LocalClassDeclaration lcd = (LocalClassDeclaration) this.parseClassDeclarationRest(
                optionalDocComment,           // optionalDocComment
                new Modifier[0],              // modifiers
                ClassDeclarationContext.BLOCK // context
            );
            return new LocalClassDeclarationStatement(lcd);
        }

        // Modifiers Type VariableDeclarators ';'
        if (this.peek("final", "@") != -1) {
            LocalVariableDeclarationStatement lvds = new LocalVariableDeclarationStatement(
                this.location(),                               // location
                this.variableModifiers(this.parseModifiers()), // modifiers
                this.parseType(),                              // type
                this.parseVariableDeclarators()                // variableDeclarators
            );
            this.read(";");
            return lvds;
        }

        // It's either a non-final local variable declaration or an expression statement. We can
        // only tell after parsing an expression.

        Atom a = this.parseExpression();

        // Expression ';'
        if (this.peekRead(";")) {
            return new ExpressionStatement(a.toRvalueOrCompileException());
        }

        // Expression BracketsOpt VariableDeclarators ';'
        Type variableType = a.toTypeOrCompileException();
        for (int i = this.parseBracketsOpt(); i > 0; --i) variableType = new ArrayType(variableType);
        LocalVariableDeclarationStatement lvds = new LocalVariableDeclarationStatement(
            a.getLocation(),                // location
            new Modifier[0],                // modifiers
            variableType,                   // type
            this.parseVariableDeclarators() // variableDeclarators
        );
        this.read(";");
        return lvds;
    }

    /**
     * <pre>
     *   VariableDeclarators := VariableDeclarator { ',' VariableDeclarator }
     * </pre>
     */
    public VariableDeclarator[]
    parseVariableDeclarators() throws CompileException, IOException {
        List<VariableDeclarator> l = new ArrayList<VariableDeclarator>();
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
    public VariableDeclarator[]
    parseFieldDeclarationRest(String name) throws CompileException, IOException {
        List<VariableDeclarator> l = new ArrayList<VariableDeclarator>();

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
    public VariableDeclarator
    parseVariableDeclarator() throws CompileException, IOException {
        return this.parseVariableDeclaratorRest(this.read(TokenType.IDENTIFIER));
    }

    /**
     * <pre>
     *   VariableDeclaratorRest := { '[' ']' } [ '=' VariableInitializer ]
     * </pre>
     * <p>
     *   Used by field declarations and local variable declarations.
     * </p>
     */
    public VariableDeclarator
    parseVariableDeclaratorRest(String name) throws CompileException, IOException  {
        Location                 loc         = this.location();
        int                      brackets    = this.parseBracketsOpt();
        ArrayInitializerOrRvalue initializer = null;

        if (this.peekRead("=")) initializer = this.parseVariableInitializer();

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
    public Statement
    parseStatement() throws CompileException, IOException {
        if (this.peek(TokenType.IDENTIFIER) && this.peekNextButOne(":")) {
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
            this.peek("assert")       ? this.parseAssertStatement() :
            this.peek(";")            ? this.parseEmptyStatement() :
            this.parseExpressionStatement()
        );

        return stmt;
    }

    /**
     * <pre>
     *   LabeledStatement := Identifier ':' Statement
     * </pre>
     */
    public Statement
    parseLabeledStatement() throws CompileException, IOException {
        String label = this.read(TokenType.IDENTIFIER);
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
    public Statement
    parseIfStatement() throws CompileException, IOException {
        final Location location = this.location();
        this.read("if");
        this.read("(");
        final Rvalue condition = this.parseExpression().toRvalueOrCompileException();
        this.read(")");

        Statement thenStatement = this.parseStatement();

        Statement elseStatement = this.peekRead("else") ? this.parseStatement() : null;

        return new IfStatement(
            location,      // location
            condition,     // condition
            thenStatement, // thenStatement
            elseStatement  // optionalElseStatement
        );
    }

    /**
     * <pre>
     *   ForStatement :=
     *     'for' '(' [ ForInit ] ';' [ Expression ] ';' [ ExpressionList ] ')' Statement
     *     | 'for' '(' FormalParameter ':' Expression ')' Statement
     *
     *   ForInit :=
     *     Modifiers Type VariableDeclarators
     *     | ModifiersOpt PrimitiveType VariableDeclarators
     *     | Expression VariableDeclarators              (1)
     *     | Expression { ',' Expression }
     * </pre>
     * <p>
     *   (1) "Expression" must pose a type.
     * </p>
     */
    public Statement
    parseForStatement() throws CompileException, IOException {
        this.read("for");
        Location forLocation = this.location();

        this.read("(");

        BlockStatement optionalInit = null;
        INIT:
        if (!this.peek(";")) {

            // 'for' '(' Modifiers Type VariableDeclarators
            // 'for' '(' [ Modifiers ] PrimitiveType VariableDeclarators
            if (this.peek(
                "final", "@", "byte", "short", "char", "int", "long", "float", "double", "boolean" // SUPPRESS CHECKSTYLE LineLength|Wrap
            ) != -1) {
                Modifier[] modifiers = this.parseModifiers();
                Type       type      = this.parseType();
                if (this.peek(TokenType.IDENTIFIER) && this.peekNextButOne(":")) {

                    // 'for' '(' [ Modifiers ] Type identifier ':' Expression ')' Statement
                    final String   name         = this.read(TokenType.IDENTIFIER);
                    final Location nameLocation = this.location();
                    this.read(":");
                    Rvalue expression = this.parseExpression().toRvalueOrCompileException();
                    this.read(")");
                    return new ForEachStatement(
                        forLocation,          // location
                        new FormalParameter(  // currentElement
                            nameLocation,
                            Parser.hasAccessModifier(modifiers, "final"),
                            type,
                            name
                        ),
                        expression,           // expression
                        this.parseStatement() // body
                    );
                }

                // 'for' '(' [ Modifiers ] Type VariableDeclarators
                optionalInit = new LocalVariableDeclarationStatement(
                    this.location(),                   // location
                    this.variableModifiers(modifiers), // modifiers
                    type,                              // type
                    this.parseVariableDeclarators()    // variableDeclarators
                );
                break INIT;
            }

            Atom a = this.parseExpression();

            if (this.peek(TokenType.IDENTIFIER)) {
                if (this.peekNextButOne(":")) {

                    // 'for' '(' Expression identifier ':' Expression ')' Statement
                    final String   name         = this.read(TokenType.IDENTIFIER);
                    final Location nameLocation = this.location();
                    this.read(":");
                    Rvalue expression = this.parseExpression().toRvalueOrCompileException();
                    this.read(")");
                    return new ForEachStatement(
                        forLocation,          // location
                        new FormalParameter(  // currentElement
                            nameLocation,
                            false,
                            a.toTypeOrCompileException(),
                            name
                        ),
                        expression,           // expression
                        this.parseStatement() // body
                    );
                }

                // 'for' '(' Expression VariableDeclarators
                optionalInit = new LocalVariableDeclarationStatement(
                    this.location(),                // location
                    new Modifier[0],                // modifiers
                    a.toTypeOrCompileException(),   // type
                    this.parseVariableDeclarators() // variableDeclarators
                );
                break INIT;
            }

            if (!this.peekRead(",")) {

                // 'for' '(' Expression
                optionalInit = new ExpressionStatement(a.toRvalueOrCompileException());
                break INIT;
            }

            // 'for' '(' Expression { ',' Expression }
            {
                List<BlockStatement> l = new ArrayList<BlockStatement>();
                l.add(new ExpressionStatement(a.toRvalueOrCompileException()));
                do {
                    l.add(new ExpressionStatement(this.parseExpression().toRvalueOrCompileException()));
                } while (this.peekRead(","));

                Block b = new Block(a.getLocation());
                b.addStatements(l);
                optionalInit = b;
            }
        }

        this.read(";");

        Rvalue optionalCondition = null;
        if (!this.peek(";")) optionalCondition = this.parseExpression().toRvalueOrCompileException();

        this.read(";");

        Rvalue[] optionalUpdate = null;
        if (!this.peek(")")) optionalUpdate = this.parseExpressionList();

        this.read(")");

        return new ForStatement(
            forLocation,          // location
            optionalInit,         // optionalInit
            optionalCondition,    // optionalCondition
            optionalUpdate,       // optionalUpdate
            this.parseStatement() // body
        );
    }

    /**
     * <pre>
     *   WhileStatement := 'while' '(' Expression ')' Statement
     * </pre>
     */
    public Statement
    parseWhileStatement() throws CompileException, IOException {
        final Location location = this.location();
        this.read("while");

        this.read("(");
        Rvalue condition = this.parseExpression().toRvalueOrCompileException();
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
    public Statement
    parseDoStatement() throws CompileException, IOException {
        final Location location = this.location();
        this.read("do");

        final Statement body = this.parseStatement();

        this.read("while");
        this.read("(");
        final Rvalue condition = this.parseExpression().toRvalueOrCompileException();
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
    public Statement
    parseTryStatement() throws CompileException, IOException {
        final Location location = this.location();

        this.read("try");

        // '(' Resource { ';' Resource } [ ';' ] ')'
        List<TryStatement.Resource> resources = new ArrayList<TryStatement.Resource>();
        if (this.peekRead("(")) {
            resources.add(this.parseResource());
            RESOURCES: for (;;) {
                switch (this.read(";", ")")) {
                case 0:
                    if (this.peekRead(")")) break RESOURCES;
                    resources.add(this.parseResource());
                    break;
                case 1:
                    break RESOURCES;
                default:
                    throw new AssertionError();
                }
            }
        }

        final Block body = this.parseBlock();

        // { CatchClause }
        List<CatchClause> ccs = new ArrayList<CatchClause>();
        while (this.peekRead("catch")) {
            final Location loc = this.location();
            this.read("(");
            final CatchParameter catchParameter = this.parseCatchParameter();
            this.read(")");
            ccs.add(new CatchClause(
                loc,              // location
                catchParameter,   // catchParameter
                this.parseBlock() // body
            ));
        }

        // [ 'finally' block ]
        Block finallY = this.peekRead("finally") ? this.parseBlock() : null;

        if (resources.isEmpty() && ccs.isEmpty() && finallY == null) {
            throw this.compileException(
                "\"try\" statement must have at least one resource, \"catch\" clause or \"finally\" clause"
            );
        }

        return new TryStatement(
            location,  // location
            resources, // resources
            body,      // body
            ccs,       // catchClauses
            finallY    // finallY
        );
    }

    /**
     * <pre>
     * Resource :=
     *     Modifiers Type VariableDeclarator
     *     | VariableAccess
     * </pre>
     */
    private TryStatement.Resource
    parseResource() throws CompileException, IOException {

        Location   loc       = this.location();
        Modifier[] modifiers = this.parseModifiers();
        Atom       a         = this.parseExpression();

        if (modifiers.length > 0 || this.peek(TokenType.IDENTIFIER)) {

            if (Parser.hasAccessModifier(modifiers, "default")) {
                throw this.compileException("Modifier \"default\" not allowed on resource");
            }

            // Modifiers Type VariableDeclarator
            return new TryStatement.LocalVariableDeclaratorResource(
                loc,                               // location
                this.variableModifiers(modifiers), // modifiers
                a.toTypeOrCompileException(),      // type
                this.parseVariableDeclarator()     // variableDeclarator
            );
        }

        // VariableAccess
        Rvalue rv = a.toRvalueOrCompileException();

        if (!(rv instanceof FieldAccess)) {
            this.compileException("Rvalue " + rv.getClass().getSimpleName() + " disallowed as a resource");
        }

        return new TryStatement.VariableAccessResource(loc, rv);
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
    public Statement
    parseSwitchStatement() throws CompileException, IOException {
        final Location location = this.location();
        this.read("switch");

        this.read("(");
        final Rvalue condition = this.parseExpression().toRvalueOrCompileException();
        this.read(")");

        this.read("{");

        List<SwitchStatement.SwitchBlockStatementGroup>
        sbsgs = new ArrayList<SwitchStatement.SwitchBlockStatementGroup>();
        while (!this.peekRead("}")) {
            Location     location2       = this.location();
            boolean      hasDefaultLabel = false;
            List<Rvalue> caseLabels      = new ArrayList<Rvalue>();
            do {
                if (this.peekRead("case")) {
                    caseLabels.add(this.parseExpression().toRvalueOrCompileException());
                } else
                if (this.peekRead("default")) {
                    if (hasDefaultLabel) throw this.compileException("Duplicate \"default\" label");
                    hasDefaultLabel = true;
                } else {
                    throw this.compileException("\"case\" or \"default\" expected");
                }
                this.read(":");
            } while (this.peek("case", "default") != -1);

            SwitchStatement.SwitchBlockStatementGroup sbsg = new SwitchStatement.SwitchBlockStatementGroup(
                location2,                  // location
                caseLabels,                 // caseLabels
                hasDefaultLabel,            // hasDefaultLabel
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
    public Statement
    parseSynchronizedStatement() throws CompileException, IOException {
        final Location location = this.location();
        this.read("synchronized");
        this.read("(");
        Rvalue expression = this.parseExpression().toRvalueOrCompileException();
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
    public Statement
    parseReturnStatement() throws CompileException, IOException {
        final Location location = this.location();
        this.read("return");
        Rvalue returnValue = this.peek(";") ? null : this.parseExpression().toRvalueOrCompileException();
        this.read(";");
        return new ReturnStatement(location, returnValue);
    }

    /**
     * <pre>
     *   ThrowStatement := 'throw' Expression ';'
     * </pre>
     */
    public Statement
    parseThrowStatement() throws CompileException, IOException {
        final Location location = this.location();
        this.read("throw");
        final Rvalue expression = this.parseExpression().toRvalueOrCompileException();
        this.read(";");

        return new ThrowStatement(location, expression);
    }

    /**
     * <pre>
     *   BreakStatement := 'break' [ Identifier ] ';'
     * </pre>
     */
    public Statement
    parseBreakStatement() throws CompileException, IOException {
        final Location location = this.location();
        this.read("break");
        String optionalLabel = null;
        if (this.peek(TokenType.IDENTIFIER)) optionalLabel = this.read(TokenType.IDENTIFIER);
        this.read(";");
        return new BreakStatement(location, optionalLabel);
    }

    /**
     * <pre>
     *   ContinueStatement := 'continue' [ Identifier ] ';'
     * </pre>
     */
    public Statement
    parseContinueStatement() throws CompileException, IOException {
        final Location location = this.location();
        this.read("continue");
        String optionalLabel = null;
        if (this.peek(TokenType.IDENTIFIER)) optionalLabel = this.read(TokenType.IDENTIFIER);
        this.read(";");
        return new ContinueStatement(location, optionalLabel);
    }

    /**
     * <pre>
     *   AssertStatement := 'assert' Expression [ ':' Expression ] ';'
     * </pre>
     */
    public Statement
    parseAssertStatement() throws CompileException, IOException {
        this.read("assert");
        Location loc = this.location();

        Rvalue expression1         = this.parseExpression().toRvalueOrCompileException();
        Rvalue optionalExpression2 = this.peekRead(":") ? this.parseExpression().toRvalueOrCompileException() : null;
        this.read(";");

        return new AssertStatement(loc, expression1, optionalExpression2);
    }

    /**
     * <pre>
     *   EmptyStatement := ';'
     * </pre>
     */
    public Statement
    parseEmptyStatement() throws CompileException, IOException {
        Location location = this.location();
        this.read(";");
        return new EmptyStatement(location);
    }

    /**
     * <pre>
     *   ExpressionList := Expression { ',' Expression }
     * </pre>
     */
    public Rvalue[]
    parseExpressionList() throws CompileException, IOException {
        List<Rvalue> l = new ArrayList<Rvalue>();
        do {
            l.add(this.parseExpression().toRvalueOrCompileException());
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
    public Type
    parseType() throws CompileException, IOException {

        Type res;
        switch (this.peekRead(
            "byte", "short", "char", "int", "long", "float", "double", "boolean" // SUPPRESS CHECKSTYLE Wrap|LineLength
        )) {
        case 0:  res = new PrimitiveType(this.location(), Primitive.BYTE);    break;
        case 1:  res = new PrimitiveType(this.location(), Primitive.SHORT);   break;
        case 2:  res = new PrimitiveType(this.location(), Primitive.CHAR);    break;
        case 3:  res = new PrimitiveType(this.location(), Primitive.INT);     break;
        case 4:  res = new PrimitiveType(this.location(), Primitive.LONG);    break;
        case 5:  res = new PrimitiveType(this.location(), Primitive.FLOAT);   break;
        case 6:  res = new PrimitiveType(this.location(), Primitive.DOUBLE);  break;
        case 7:  res = new PrimitiveType(this.location(), Primitive.BOOLEAN); break;
        case -1: res = this.parseReferenceType(); break;
        default: throw new AssertionError();
        }

        for (int i = this.parseBracketsOpt(); i > 0; --i) res = new ArrayType(res);

        return res;
    }

    /**
     * <pre>
     *   ReferenceType := { Annotation } QualifiedIdentifier [ TypeArguments ]
     * </pre>
     */
    public ReferenceType
    parseReferenceType() throws CompileException, IOException {

        List<Annotation> annotations = new ArrayList<Annotation>();
        while (this.peek("@")) annotations.add(this.parseAnnotation());

        return new ReferenceType(
            this.location(),
            (Annotation[]) annotations.toArray(new Annotation[annotations.size()]),
            this.parseQualifiedIdentifier(),
            this.parseTypeArgumentsOpt()
        );
    }

    /**
     * <pre>
     *   TypeParameters := '<' TypeParameter { ',' TypeParameter } '>'
     * </pre>
     */
    @Nullable private TypeParameter[]
    parseTypeParametersOpt() throws CompileException, IOException {

        if (!this.peekRead("<")) return null;

        List<TypeParameter> l = new ArrayList<TypeParameter>();
        l.add(this.parseTypeParameter());
        while (this.read(",", ">") == 0) {
            l.add(this.parseTypeParameter());
        }
        return (TypeParameter[]) l.toArray(new TypeParameter[l.size()]);
    }

    /**
     * <pre>
     *   TypeParameter := identifier [ 'extends' ( identifier | ReferenceType { '&' ReferenceType }
     * </pre>
     */
    private TypeParameter
    parseTypeParameter() throws CompileException, IOException {
        String name = this.read(TokenType.IDENTIFIER);
        if (this.peekRead("extends")) {
            List<ReferenceType> bound = new ArrayList<ReferenceType>();
            bound.add(this.parseReferenceType());
            while (this.peekRead("&")) this.parseReferenceType();
            return new TypeParameter(name, (ReferenceType[]) bound.toArray(new ReferenceType[bound.size()]));
        }
        return new TypeParameter(name, null);
    }

    /**
     * <pre>
     *   TypeArguments := '<' [ TypeArgument { ',' TypeArgument } ] '>'
     * </pre>
     *
     * @return {@code null} iff there are no type arguments
     */
    @Nullable private TypeArgument[]
    parseTypeArgumentsOpt() throws CompileException, IOException {

        if (!this.peekRead("<")) return null;

        if (this.peekRead(">")) return new TypeArgument[0];

        List<TypeArgument> typeArguments = new ArrayList<TypeArgument>();
        typeArguments.add(this.parseTypeArgument());
        while (this.read(">", ",") == 1) {
            typeArguments.add(this.parseTypeArgument());
        }
        return (TypeArgument[]) typeArguments.toArray(new TypeArgument[typeArguments.size()]);
    }

    /**
     * <pre>
     *   TypeArgument :=
     *     ReferenceType { '[' ']' }    &lt;= The optional brackets are missing in JLS7, section 18!?
     *     | PrimitiveType '[' ']' { '[' ']' }
     *     | '?' extends ReferenceType
     *     | '?' super ReferenceType
     * </pre>
     */
    private TypeArgument
    parseTypeArgument() throws CompileException, IOException {
        if (this.peekRead("?")) {
            return (
                this.peekRead("extends") ? new Wildcard(Wildcard.BOUNDS_EXTENDS, this.parseReferenceType()) :
                this.peekRead("super")   ? new Wildcard(Wildcard.BOUNDS_SUPER,   this.parseReferenceType()) :
                new Wildcard()
            );
        }

        Type t = this.parseType();

        int i = this.parseBracketsOpt();
        for (; i > 0; i--) t = new ArrayType(t);
        if (!(t instanceof TypeArgument)) throw this.compileException("'" + t + "' is not a valid type argument");
        return (TypeArgument) t;
    }

    /**
     * <pre>
     *   ReferenceTypeList := ReferenceType { ',' ReferenceType }
     * </pre>
     */
    public ReferenceType[]
    parseReferenceTypeList() throws CompileException, IOException {
        List<ReferenceType> l = new ArrayList<ReferenceType>();
        l.add(this.parseReferenceType());
        while (this.peekRead(",")) {
            l.add(this.parseReferenceType());
        }
        return (ReferenceType[]) l.toArray(new ReferenceType[l.size()]);
    }

    /**
     * <pre>
     *   Expression := AssignmentExpression | LambdaExpression
     * </pre>
     * <p>
     *   Notice that all other kinds of lambda expressions are parsed in {@link #parsePrimary()}.
     * </p>
     */
    public Atom
    parseExpression() throws CompileException, IOException  {

        if (this.peek(TokenType.IDENTIFIER) && this.peekNextButOne("->")) return this.parseLambdaExpression();

        return this.parseAssignmentExpression();
    }

    /**
     * <pre>
     *   AssignmentExpression :=
     *     ConditionalExpression [ AssignmentOperator AssignmentExpression ]
     *
     *   AssignmentOperator :=
     *     '=' | '*=' | '/=' | '%=' | '+=' | '-=' | '&lt;&lt;=' |
     *     '&gt;&gt;=' | '&gt;&gt;&gt;=' | '&amp;=' | '^=' | '|='
     * </pre>
     */
    public Atom
    parseAssignmentExpression() throws CompileException, IOException {
        Atom a = this.parseConditionalExpression();
        if (this.peek(
            "=", "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", "<<=", ">>=", ">>>=" // SUPPRESS CHECKSTYLE Wrap|LineLength
        ) != -1) {
            final Lvalue lhs      = a.toLvalueOrCompileException();
            Location     location = this.location();
            String       operator = this.read(TokenType.OPERATOR); // An interned string!
            final Rvalue rhs      = this.parseAssignmentExpression().toRvalueOrCompileException();
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
    public Atom
    parseConditionalExpression() throws CompileException, IOException  {
        Atom a = this.parseConditionalOrExpression();
        if (!this.peekRead("?")) return a;
        Location location = this.location();

        Rvalue lhs = a.toRvalueOrCompileException();
        Rvalue mhs = this.parseExpression().toRvalueOrCompileException();
        this.read(":");
        Rvalue rhs = this.parseConditionalExpression().toRvalueOrCompileException();
        return new ConditionalExpression(location, lhs, mhs, rhs);
    }

    /**
     * <pre>
     *   ConditionalOrExpression :=
     *     ConditionalAndExpression { '||' ConditionalAndExpression ]
     * </pre>
     */
    public Atom
    parseConditionalOrExpression() throws CompileException, IOException  {
        Atom a = this.parseConditionalAndExpression();
        while (this.peekRead("||")) {
            Location location = this.location();
            a = new BinaryOperation(
                location,
                a.toRvalueOrCompileException(),
                "||",
                this.parseConditionalAndExpression().toRvalueOrCompileException()
            );
        }
        return a;
    }

    /**
     * <pre>
     *   ConditionalAndExpression :=
     *     InclusiveOrExpression { '&amp;&amp;' InclusiveOrExpression }
     * </pre>
     */
    public Atom
    parseConditionalAndExpression() throws CompileException, IOException  {
        Atom a = this.parseInclusiveOrExpression();
        while (this.peekRead("&&")) {
            Location location = this.location();
            a = new BinaryOperation(
                location,
                a.toRvalueOrCompileException(),
                "&&",
                this.parseInclusiveOrExpression().toRvalueOrCompileException()
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
    public Atom
    parseInclusiveOrExpression() throws CompileException, IOException  {
        Atom a = this.parseExclusiveOrExpression();
        while (this.peekRead("|")) {
            Location location = this.location();
            a = new BinaryOperation(
                location,
                a.toRvalueOrCompileException(),
                "|",
                this.parseExclusiveOrExpression().toRvalueOrCompileException()
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
    public Atom
    parseExclusiveOrExpression() throws CompileException, IOException  {
        Atom a = this.parseAndExpression();
        while (this.peekRead("^")) {
            Location location = this.location();
            a = new BinaryOperation(
                location,
                a.toRvalueOrCompileException(),
                "^",
                this.parseAndExpression().toRvalueOrCompileException()
            );
        }
        return a;
    }

    /**
     * <pre>
     *   AndExpression :=
     *     EqualityExpression { '&amp;' EqualityExpression }
     * </pre>
     */
    public Atom
    parseAndExpression() throws CompileException, IOException  {
        Atom a = this.parseEqualityExpression();
        while (this.peekRead("&")) {
            Location location = this.location();
            a = new BinaryOperation(
                location,
                a.toRvalueOrCompileException(),
                "&",
                this.parseEqualityExpression().toRvalueOrCompileException()
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
    public Atom
    parseEqualityExpression() throws CompileException, IOException  {
        Atom a = this.parseRelationalExpression();

        while (this.peek("==", "!=") != -1) {
            a = new BinaryOperation(
                this.location(),                                              // location
                a.toRvalueOrCompileException(),                               // lhs
                this.read().value,                                            // operator
                this.parseRelationalExpression().toRvalueOrCompileException() // rhs
            );
        }
        return a;
    }

    /**
     * <pre>
     *   RelationalExpression :=
     *     ShiftExpression {
     *       'instanceof' ReferenceType
     *       | '&lt;' ShiftExpression [ { ',' TypeArgument } '&gt;' ]
     *       | '&lt;' TypeArgument [ { ',' TypeArgument } '&gt;' ]
     *       | ( '&gt;' | '&lt;=' | '&gt;=' ) ShiftExpression
     *     }
     * </pre>
     */
    public Atom
    parseRelationalExpression() throws CompileException, IOException  {
        Atom a = this.parseShiftExpression();

        for (;;) {
            if (this.peekRead("instanceof")) {
                Location location = this.location();
                a = new Instanceof(
                    location,
                    a.toRvalueOrCompileException(),
                    this.parseType()
                );
            } else
            if (this.peek("<", ">", "<=", ">=") != -1) {

                // ambiguous-name '<' '?' ...
                if (a instanceof AmbiguousName && this.peek("<") && this.peekNextButOne("?")) {
                    return new ReferenceType(
                        this.location(),
                        new Annotation[0],
                        ((AmbiguousName) a).identifiers,
                        this.parseTypeArgumentsOpt()
                    );
                }

                String operator = this.read().value;

                Atom rhs = this.parseShiftExpression();

                if ("<".equals(operator) && a instanceof AmbiguousName) {

                    if (this.peek("<", ">", ",") != -1) {
                        final String[] identifiers = ((AmbiguousName) a).identifiers;

                        // '<' ShiftExpression [ TypeArguments ] ( '<' | '>' | ',' )
                        this.parseTypeArgumentsOpt();
                        Type t = rhs.toTypeOrCompileException();

                        TypeArgument ta;
                        if (t instanceof ArrayType)     { ta = (ArrayType)     t; } else
                        if (t instanceof ReferenceType) { ta = (ReferenceType) t; } else
                        {
                            throw this.compileException("'" + t + "' is not a valid type argument");
                        }

                        List<TypeArgument> typeArguments = new ArrayList<TypeArgument>();
                        typeArguments.add(ta);
                        while (this.read(">", ",") == 1) {
                            typeArguments.add(this.parseTypeArgument());
                        }

                        return new ReferenceType(
                            this.location(),
                            new Annotation[0],
                            identifiers,
                            (TypeArgument[]) typeArguments.toArray(new TypeArgument[typeArguments.size()])
                        );
                    }
                }

                a = new BinaryOperation(
                    this.location(),                 // location
                    a.toRvalueOrCompileException(),  // lhs
                    operator,                        // operator
                    rhs.toRvalueOrCompileException() // rhs
                );
            } else {
                return a;
            }
        }
    }

    /**
     * <pre>
     *   ShiftExpression :=
     *     AdditiveExpression { ( '&lt;&lt;' | '&gt;&gt;' | '&gt;&gt;&gt;' ) AdditiveExpression }
     * </pre>
     */
    public Atom
    parseShiftExpression() throws CompileException, IOException  {
        Atom a = this.parseAdditiveExpression();

        while (this.peek("<<", ">>", ">>>") != -1) {
            a = new BinaryOperation(
                this.location(),                                            // location
                a.toRvalueOrCompileException(),                             // lhs
                this.read().value,                                          // operator
                this.parseAdditiveExpression().toRvalueOrCompileException() // rhs
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
    public Atom
    parseAdditiveExpression() throws CompileException, IOException  {
        Atom a = this.parseMultiplicativeExpression();

        while (this.peek("+", "-") != -1) {
            a = new BinaryOperation(
                this.location(),                                                  // location
                a.toRvalueOrCompileException(),                                   // lhs
                this.read().value,                                                // operator
                this.parseMultiplicativeExpression().toRvalueOrCompileException() // rhs
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
    public Atom
    parseMultiplicativeExpression() throws CompileException, IOException {
        Atom a = this.parseUnaryExpression();

        while (this.peek("*", "/", "%") != -1) {
            a = new BinaryOperation(
                this.location(),                                         // location
                a.toRvalueOrCompileException(),                          // lhs
                this.read().value,                                       // operator
                this.parseUnaryExpression().toRvalueOrCompileException() // rhs
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
    public Atom
    parseUnaryExpression() throws CompileException, IOException {
        if (this.peek("++", "--") != -1) {
            return new Crement(
                this.location(),                                         // location
                this.read().value,                                       // operator
                this.parseUnaryExpression().toLvalueOrCompileException() // operand
            );
        }

        if (this.peek("+", "-", "~", "!") != -1) {
            return new UnaryOperation(
                this.location(),                                         // location
                this.read().value,                                       // operator
                this.parseUnaryExpression().toRvalueOrCompileException() // operand
            );
        }

        Atom a = this.parsePrimary();

        while (this.peek(".", "[") != -1) {
            a = this.parseSelector(a);
        }

        if (this.peekRead("::")) {

            if (a instanceof ArrayType) {
                this.read("new");

                // ArrayType '::' 'new'
                return new ArrayCreationReference(this.location(), (ArrayType) a);
            }

            TypeArgument[] typeArguments = this.parseTypeArgumentsOpt();

            switch (this.peek(TokenType.KEYWORD /* 'new' */, TokenType.IDENTIFIER)) {

            case 0: // keyword
                // ClassType '::' [ TypeArguments ] 'new'
                this.read("new");
                return new ClassInstanceCreationReference(
                    this.location(),
                    a.toTypeOrCompileException(),
                    typeArguments
                );

            case 1: // identifier
                // ( Rvalue | ReferenceType ) '::' [ TypeArguments ] identifier
                return new MethodReference(this.location(), a, this.read(TokenType.IDENTIFIER));

            default:
                throw new AssertionError(this.peek());
            }
        }

        while (this.peek("++", "--") != -1) {
            a = new Crement(
                this.location(),                // location
                a.toLvalueOrCompileException(), // operand
                this.read().value               // operator
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
     *     PrimitiveType { '[]' } |                // Type
     *     PrimitiveType { '[]' } '.' 'class' |    // ClassLiteral 15.8.2
     *     'void' '.' 'class' |                    // ClassLiteral 15.8.2
     *     MethodReference                         // MethodReference JLS9 15.13
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
    public Atom
    parsePrimary() throws CompileException, IOException {

        if (this.peekRead("(")) {

            if (
                this.peek("boolean", "char", "byte", "short", "int", "long", "float", "double") != -1
                && !this.peekNextButOne(TokenType.IDENTIFIER)
            ) {

                // '(' PrimitiveType { '[]' } ')' UnaryExpression
                Type type     = this.parseType();
                int  brackets = this.parseBracketsOpt();
                this.read(")");
                for (int i = 0; i < brackets; ++i) type = new ArrayType(type);
                return new Cast(
                    this.location(),                                         // location
                    type,                                                    // targetType
                    this.parseUnaryExpression().toRvalueOrCompileException() // value
                );
            }

            // '(' ')'
            if (this.peekRead(")")) {
                LambdaParameters parameters = new FormalLambdaParameters(new FormalParameters(this.location()));
                Location         loc        = this.location();
                this.read("->");
                return new LambdaExpression(loc, parameters, this.parseLambdaBody());
            }

            Atom a;
            if (this.peek(TokenType.IDENTIFIER) && (this.peekNextButOne(",") || this.peekNextButOne(")"))) {

                // '(' Identifier { ',' Identifier } ')' ->
                String[] names;
                {
                    List<String> l = new ArrayList<String>();
                    l.add(this.read(TokenType.IDENTIFIER));
                    while (this.peekRead(",")) l.add(this.read(TokenType.IDENTIFIER));
                    names = (String[]) l.toArray(new String[l.size()]);
                }

                Location loc = this.location();
                if (this.peek(")") && this.peekNextButOne("->")) {
                    this.read();
                    this.read();
                    return new LambdaExpression(
                        loc,
                        new InferredLambdaParameters(names),
                        this.parseLambdaBody()
                    );
                }

                if (names.length != 1) throw this.compileException("Lambda expected");
                a = new AmbiguousName(loc, new String[] { names[0] });
            } else {
                a = this.parseExpression();
            }

            // '(' FormalParameterList ')'
            if (this.peek(TokenType.IDENTIFIER)) {
                FormalParameters fpl = this.parseFormalParameterListRest(a.toTypeOrCompileException());
                this.read(")");
                LambdaParameters parameters = new FormalLambdaParameters(fpl);
                Location         loc        = this.location();
                this.read("->");
                return new LambdaExpression(loc, parameters, this.parseLambdaBody());
            }

            // '(' atom ')'
            this.read(")");

            if (
                this.peekLiteral()
                || this.peek(TokenType.IDENTIFIER)
                || this.peek("(", "~", "!") != -1
                || this.peek("this", "super", "new") != -1
            ) {
                // '(' Expression ')' UnaryExpression
                return new Cast(
                    this.location(),                                         // location
                    a.toTypeOrCompileException(),                            // targetType
                    this.parseUnaryExpression().toRvalueOrCompileException() // value
                );
            }

            // '(' Expression ')'
            return new ParenthesizedExpression(a.getLocation(), a.toRvalueOrCompileException());
        }

        if (this.peekLiteral()) {

            // Literal
            return this.parseLiteral();
        }

        if (this.peek(TokenType.IDENTIFIER)) {
            String[] qi       = this.parseQualifiedIdentifier();
            if (this.peek("(")) {
                // Name Arguments
                return new MethodInvocation(
                    this.location(),                           // location
                    qi.length == 1 ? null : new AmbiguousName( // optionalTarget
                        this.location(), // location
                        qi,              // identifiers
                        qi.length - 1    // n
                    ),
                    qi[qi.length - 1],                         // methodName
                    this.parseArguments()                      // arguments
                );
            }
            if (this.peek("[") && this.peekNextButOne("]")) {
                // Name '[]' { '[]' }
                // Name '[]' { '[]' } '.' 'class'
                Type res = new ReferenceType(
                    this.location(),   // location
                    new Annotation[0], // annotations
                    qi,                // identifiers
                    null               // optionalTypeArguments
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
                // Alternate constructor invocation (JLS7 8.8.7.1).
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
                // Unqualified superclass constructor invocation (JLS7 8.8.7.1).
                return new SuperConstructorInvocation(
                    this.location(),      // location
                    (Rvalue) null,        // optionalQualification
                    this.parseArguments() // arguments
                );
            }
            this.read(".");
            String name = this.read(TokenType.IDENTIFIER);
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
            Type     type     = this.parseType();
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

        // PrimitiveType
        if (this.peek("boolean", "char", "byte", "short", "int", "long", "float", "double") != -1) {
            Type res      = this.parseType();
            int  brackets = this.parseBracketsOpt();
            for (int i = 0; i < brackets; ++i) res = new ArrayType(res);
            if (this.peek(".") && this.peekNextButOne("class")) {
                // PrimitiveType { '[]' } '.' 'class'
                this.read();
                Location location = this.location();
                this.read();
                return new ClassLiteral(location, res);
            }
            // PrimitiveType { '[]' }
            return res;
        }

        // 'void'
        if (this.peekRead("void")) {
            if (this.peek(".") && this.peekNextButOne("class")) {
                // 'void' '.' 'class'
                this.read();
                Location location = this.location();
                this.read();
                return new ClassLiteral(location, new PrimitiveType(location, Primitive.VOID));
            }
            throw this.compileException("\"void\" encountered in wrong context");
        }

        throw this.compileException("Unexpected token \"" + this.read().value + "\" in primary");
    }

    /**
     * <pre>
     *   Selector :=
     *     '.' Identifier |                                // FieldAccess 15.11.1
     *     '.' Identifier Arguments |                      // MethodInvocation
     *     '.' '&lt;' TypeList '&gt;' 'super' Arguments                  // Superconstructor invocation (?)
     *     '.' '&lt;' TypeList '&gt;' 'super' '.' . Identifier           // ???
     *     '.' '&lt;' TypeList '&gt;' 'super' '.' . Identifier Arguments // Supermethod invocation
     *     '.' '&lt;' TypeList '&gt;' Identifier Arguments // ExplicitGenericInvocation
     *     '.' 'this'                                      // QualifiedThis 15.8.4
     *     '.' 'super' Arguments                           // Qualified superclass constructor invocation (JLS7 8.8.7.1)
     *     '.' 'super' '.' Identifier |                    // SuperclassFieldReference (JLS7 15.11.2)
     *     '.' 'super' '.' Identifier Arguments |          // SuperclassMethodInvocation (JLS7 15.12.3)
     *     '.' 'new' Identifier Arguments [ ClassBody ] |  // QualifiedClassInstanceCreationExpression  15.9
     *     '.' 'class'
     *     '[' Expression ']'                              // ArrayAccessExpression 15.13
     *
     *   ExplicitGenericInvocationSuffix :=
     *     'super' SuperSuffix
     *     | Identifier Arguments
     * </pre>
     */
    public Atom
    parseSelector(Atom atom) throws CompileException, IOException {
        if (this.peekRead(".")) {

            // Parse and ignore type arguments.
            this.parseTypeArgumentsOpt();

            if (this.peek().type == TokenType.IDENTIFIER) {
                String identifier = this.read(TokenType.IDENTIFIER);
                if (this.peek("(")) {
                    // '.' Identifier Arguments
                    return new MethodInvocation(
                        this.location(),                   // location
                        atom.toRvalueOrCompileException(), // optionalTarget
                        identifier,                        // methodName
                        this.parseArguments()              // arguments
                    );
                }
                // '.' Identifier
                return new FieldAccessExpression(
                    this.location(),                   // location
                    atom.toRvalueOrCompileException(), // lhs
                    identifier                         // fieldName
                );
            }
            if (this.peekRead("this")) {
                // '.' 'this'
                Location location = this.location();
                return new QualifiedThisReference(
                    location,                       // location
                    atom.toTypeOrCompileException() // qualification
                );
            }
            if (this.peekRead("super")) {
                Location location = this.location();
                if (this.peek("(")) {

                    // '.' 'super' Arguments
                    // Qualified superclass constructor invocation (JLS7 8.7.1.2.2) (LHS is an Rvalue)
                    return new SuperConstructorInvocation(
                        location,                          // location
                        atom.toRvalueOrCompileException(), // optionalQualification
                        this.parseArguments()              // arguments
                    );
                }
                this.read(".");
                String identifier = this.read(TokenType.IDENTIFIER);

                if (this.peek("(")) {

                    // '.' 'super' '.' Identifier Arguments
                    // Qualified superclass method invocation (JLS7 15.12.1.1.4) (LHS is a ClassName).
                    // TODO: Qualified superclass method invocation
                    throw this.compileException("Qualified superclass method invocation NYI");
                } else {

                    // '.' 'super' '.' Identifier
                    // Qualified superclass field access (JLS7 15.11.2) (LHS is an Rvalue).
                    return new SuperclassFieldAccessExpression(
                        location,                        // location
                        atom.toTypeOrCompileException(), // optionalQualification
                        identifier                       // fieldName
                    );
                }
            }
            if (this.peekRead("new")) {
                // '.' 'new' Identifier Arguments [ ClassBody ]
                Rvalue   lhs        = atom.toRvalueOrCompileException();
                Location location   = this.location();
                String   identifier = this.read(TokenType.IDENTIFIER);
                Type     type       = new RvalueMemberType(
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
                return new ClassLiteral(location, atom.toTypeOrCompileException());
            }
            throw this.compileException("Unexpected selector '" + this.peek().value + "' after \".\"");
        }
        if (this.peekRead("[")) {
            // '[' Expression ']'
            Location location = this.location();
            Rvalue   index    = this.parseExpression().toRvalueOrCompileException();
            this.read("]");
            return new ArrayAccessExpression(
                location,                          // location
                atom.toRvalueOrCompileException(), // lhs
                index                              // index
            );
        }
        throw this.compileException("Unexpected token '" + this.peek().value + "' in selector");
    }

    /**
     * <pre>
     *   DimExprs := DimExpr { DimExpr }
     * </pre>
     */
    public Rvalue[]
    parseDimExprs() throws CompileException, IOException {
        List<Rvalue> l = new ArrayList<Rvalue>();
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
    public Rvalue
    parseDimExpr() throws CompileException, IOException {
        this.read("[");
        Rvalue res = this.parseExpression().toRvalueOrCompileException();
        this.read("]");
        return res;
    }

    /**
     * <pre>
     *   Arguments := '(' [ ArgumentList ] ')'
     * </pre>
     */
    public Rvalue[]
    parseArguments() throws CompileException, IOException {
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
    public Rvalue[]
    parseArgumentList() throws CompileException, IOException {
        List<Rvalue> l = new ArrayList<Rvalue>();
        do {
            l.add(this.parseExpression().toRvalueOrCompileException());
        } while (this.peekRead(","));
        return (Rvalue[]) l.toArray(new Rvalue[l.size()]);
    }

    /**
     * <pre>
     *   Literal :=
     *     IntegerLiteral
     *     | FloatingPointLiteral
     *     | BooleanLiteral
     *     | CharacterLiteral
     *     | StringLiteral
     *     | NullLiteral
     * </pre>
     */
    public Rvalue
    parseLiteral() throws CompileException, IOException {
        Token t = this.read();
        switch (t.type) {
        case INTEGER_LITERAL:        return new IntegerLiteral(t.getLocation(), t.value);
        case FLOATING_POINT_LITERAL: return new FloatingPointLiteral(t.getLocation(), t.value);
        case BOOLEAN_LITERAL:        return new BooleanLiteral(t.getLocation(), t.value);
        case CHARACTER_LITERAL:      return new CharacterLiteral(t.getLocation(), t.value);
        case STRING_LITERAL:         return new StringLiteral(t.getLocation(), t.value);
        case NULL_LITERAL:           return new NullLiteral(t.getLocation());
        default:
            throw this.compileException("Literal expected");
        }
    }

    /**
     * <pre>
     *   LambdaExpression := LambdaParameters '->' LambdaBody
     * </pre>
     */
    private LambdaExpression
    parseLambdaExpression() throws CompileException, IOException {

        LambdaParameters parameters = this.parseLambdaParameters();
        Location         loc        = this.location();
        this.read("->");
        LambdaBody body = this.parseLambdaBody();

        return new LambdaExpression(loc, parameters, body);
    }

    /**
     * <pre>
     *   LambdaParameters :=
     *       Identifier
     *       | '(' [ FormalParameterList ] ')'
     *       | '(' InferredFormalParameterList ')'
     * </pre>
     */
    private LambdaParameters
    parseLambdaParameters() throws CompileException, IOException {

        // Identifier
        String identifier = this.peekRead(TokenType.IDENTIFIER);
        if (identifier != null) return new IdentifierLambdaParameters(identifier);

        this.read("(");

        // '(' ')'
        if (this.peekRead(")")) return new FormalLambdaParameters(new FormalParameters(this.location()));

        // '(' Identifier { ',' Identifier } ')'
        if (this.peek(TokenType.IDENTIFIER) && (this.peekNextButOne(",") || this.peekNextButOne(")"))) {
            List<String> names = new ArrayList<String>();
            names.add(this.read(TokenType.IDENTIFIER));
            while (this.peekRead(",")) names.add(this.read(TokenType.IDENTIFIER));
            this.read(")");
            return new InferredLambdaParameters((String[]) names.toArray(new String[names.size()]));
        }

        // '(' FormalParameterList ')'
        FormalParameters fpl = this.parseFormalParameterList();
        this.read(")");
        return new FormalLambdaParameters(fpl);
    }

    /**
     * <pre>
     *   LambdaBody :=
     * </pre>
     */
    private LambdaBody
    parseLambdaBody() throws CompileException, IOException {
        return (
            this.peek("{")
            ? new BlockLambdaBody(this.parseBlock())
            : new ExpressionLambdaBody(this.parseExpression().toRvalueOrCompileException())
        );
    }

    /**
     * <pre>
     *   ExpressionStatement := Expression ';'
     * </pre>
     */
    public Statement
    parseExpressionStatement() throws CompileException, IOException {
        Rvalue rv = this.parseExpression().toRvalueOrCompileException();
        this.read(";");

        return new ExpressionStatement(rv);
    }

    /**
     * @return The location of the first character of the previously <em>read</em> (not <em>peek</em>ed!) token
     */
    public Location
    location() { return this.tokenStream.location(); }

    // Token-level methods.

    // Shorthand for the various "TokenStream" methods.       SUPPRESS CHECKSTYLE LineLength|JavadocMethod:16
    public Token            peek()                              throws CompileException, IOException { return this.tokenStream.peek();                             }
    public Token            read()                              throws CompileException, IOException { return this.tokenStream.read();                             }
    public boolean          peek(String suspected)              throws CompileException, IOException { return this.tokenStream.peek(suspected);                    }
    public int              peek(String... suspected)           throws CompileException, IOException { return this.tokenStream.peek(suspected);                    }
    public boolean          peek(TokenType suspected)           throws CompileException, IOException { return this.tokenStream.peek(suspected);                    }
    public int              peek(TokenType... suspected)        throws CompileException, IOException { return this.tokenStream.peek(suspected);                    }
    public Token            peekNextButOne()                    throws CompileException, IOException { return this.tokenStream.peekNextButOne();                   }
    public boolean          peekNextButOne(String suspected)    throws CompileException, IOException { return this.tokenStream.peekNextButOne(suspected);          }
    public boolean          peekNextButOne(TokenType suspected) throws CompileException, IOException { return this.tokenStream.peekNextButOne().type == suspected; }
    public void             read(String expected)               throws CompileException, IOException { this.tokenStream.read(expected);                            }
    public int              read(String... expected)            throws CompileException, IOException { return this.tokenStream.read(expected);                     }
    public String           read(TokenType expected)            throws CompileException, IOException { return this.tokenStream.read(expected);                     }
    public boolean          peekRead(String suspected)          throws CompileException, IOException { return this.tokenStream.peekRead(suspected);                }
    public int              peekRead(String... suspected)       throws CompileException, IOException { return this.tokenStream.peekRead(suspected);                }
    @Nullable public String peekRead(TokenType suspected)       throws CompileException, IOException { return this.tokenStream.peekRead(suspected);                }

    public boolean
    peekLiteral() throws CompileException, IOException {
        return this.peek(
            TokenType.INTEGER_LITERAL, TokenType.FLOATING_POINT_LITERAL, TokenType.BOOLEAN_LITERAL,
            TokenType.CHARACTER_LITERAL, TokenType.STRING_LITERAL, TokenType.NULL_LITERAL
        ) != -1;
    }

    /**
     * Issues a warning if the given string does not comply with the package naming conventions.
     */
    private void
    verifyStringIsConventionalPackageName(String s, Location loc) throws CompileException {
        if (!Character.isLowerCase(s.charAt(0))) {
            this.warning(
                "UPN",
                "Package name \"" + s + "\" does not begin with a lower-case letter (see JLS7 6.8.1)",
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
     * Issues a warning if the given identifier does not comply with the class and interface type naming conventions
     * (JLS7 6.8.2).
     */
    private void
    verifyIdentifierIsConventionalClassOrInterfaceName(String id, Location loc) throws CompileException {
        if (!Character.isUpperCase(id.charAt(0))) {
            this.warning(
                "UCOIN1",
                "Class or interface name \"" + id + "\" does not begin with an upper-case letter (see JLS7 6.8.2)",
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
                    + "\" (see JLS7 6.8.2)"
                ), loc);
                return;
            }
        }
    }

    /**
     * Issues a warning if the given identifier does not comply with the method naming conventions (JLS7 6.8.3).
     */
    private void
    verifyIdentifierIsConventionalMethodName(String id, Location loc) throws CompileException {
        if (!Character.isLowerCase(id.charAt(0))) {
            this.warning(
                "UMN1",
                "Method name \"" + id + "\" does not begin with a lower-case letter (see JLS7 6.8.3)",
                loc
            );
            return;
        }
        for (int i = 0; i < id.length(); ++i) {
            char c = id.charAt(i);
            if (!Character.isLetter(c) && !Character.isDigit(c)) {
                this.warning(
                    "UMN",
                    "Method name \"" + id + "\" contains unconventional character \"" + c + "\" (see JLS7 6.8.3)",
                    loc
                );
                return;
            }
        }
    }

    /**
     * Issues a warning if the given identifier does not comply with the field naming conventions (JLS7 6.8.4) and
     * constant naming conventions (JLS7 6.8.5).
     */
    private void
    verifyIdentifierIsConventionalFieldName(String id, Location loc) throws CompileException {

        // In practice, a field is not always a constant iff it is static-final. So let's
        // always tolerate both field and constant names.

        if (Character.isUpperCase(id.charAt(0))) {
            for (int i = 0; i < id.length(); ++i) {
                char c = id.charAt(i);
                if (!Character.isUpperCase(c) && !Character.isDigit(c) && c != '_') {
                    this.warning(
                        "UCN",
                        "Constant name \"" + id + "\" contains unconventional character \"" + c + "\" (see JLS7 6.8.5)",
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
                        "Field name \"" + id + "\" contains unconventional character \"" + c + "\" (see JLS7 6.8.4)",
                        loc
                    );
                    return;
                }
            }
        } else {
            this.warning("UFN1", (
                "\""
                + id
                + "\" is neither a conventional field name (JLS7 6.8.4) nor a conventional constant name (JLS7 6.8.5)"
            ), loc);
        }
    }

    /**
     * Issues a warning if the given identifier does not comply with the local variable and parameter naming
     * conventions (JLS7 6.8.6).
     */
    private void
    verifyIdentifierIsConventionalLocalVariableOrParameterName(String id, Location loc) throws CompileException {
        if (!Character.isLowerCase(id.charAt(0))) {
            this.warning(
                "ULVN1",
                "Local variable name \"" + id + "\" does not begin with a lower-case letter (see JLS7 6.8.6)",
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
                    + "\" (see JLS7 6.8.6)"
                ), loc);
                return;
            }
        }
    }

    /**
     * By default, warnings are discarded, but an application my install a {@link WarningHandler}.
     * <p>
     *   Notice that there is no {@code Parser.setErrorHandler()} method, but parse errors always throw a {@link
     *   CompileException}. The reason being is that there is no reasonable way to recover from parse errors and
     *   continue parsing, so there is no need to install a custom parse error handler.
     * </p>
     *
     * @param optionalWarningHandler {@code null} to indicate that no warnings be issued
     */
    public void
    setWarningHandler(@Nullable WarningHandler optionalWarningHandler) {
        this.optionalWarningHandler = optionalWarningHandler;
        this.tokenStream.setWarningHandler(optionalWarningHandler);
    }

    // Used for elaborate warning handling.
    @Nullable private WarningHandler optionalWarningHandler;

    private void
    warning(String handle, String message) throws CompileException {
        this.warning(handle, message, this.location());
    }

    /**
     * Issues a warning with the given message and location and returns. This is done through
     * a {@link WarningHandler} that was installed through
     * {@link #setWarningHandler(WarningHandler)}.
     * <p>
     * The {@code handle} argument qualifies the warning and is typically used by
     * the {@link WarningHandler} to suppress individual warnings.
     *
     * @throws CompileException The optionally installed {@link WarningHandler} decided to throw a {@link
     *                          CompileException}
     */
    private void
    warning(String handle, String message, @Nullable Location optionalLocation) throws CompileException {
        if (this.optionalWarningHandler != null) {
            this.optionalWarningHandler.handleWarning(handle, message, optionalLocation);
        }
    }

    /**
     * Convenience method for throwing a {@link CompileException}.
     */
    protected final CompileException
    compileException(String message) { return Parser.compileException(message, this.location()); }

    /**
     * Convenience method for throwing a {@link CompileException}.
     */
    protected static CompileException
    compileException(String message, Location location) { return new CompileException(message, location); }

    private static String
    join(@Nullable String[] sa, String separator) {

        if (sa == null) return ("(null)");

        if (sa.length == 0) return ("(zero length array)");
        StringBuilder sb = new StringBuilder(sa[0]);
        for (int i = 1; i < sa.length; ++i) {
            sb.append(separator).append(sa[i]);
        }
        return sb.toString();
    }

    private static boolean
    hasAccessModifier(Modifier[] modifiers, String... keywords) {
        for (String kw : keywords) {
            for (Modifier m : modifiers) {
                if (m instanceof AccessModifier && kw.equals(((AccessModifier) m).keyword)) return true;
            }
        }
        return false;
    }

    private static boolean
    hasAccessModifierOtherThan(Modifier[] modifiers, String... keywords) {

        MODIFIER:
        for (Modifier m : modifiers) {
            if (m instanceof AccessModifier) {
                for (String kw : keywords) {
                    if (kw.equals(((AccessModifier) m).keyword)) continue MODIFIER;
                }
                return true;
            }
        }
        return false;
    }

    private Modifier[]
    packageModifiers(Modifier[] modifiers) throws CompileException {
        return this.checkModifiers(modifiers, "xxx");
    }

    private Modifier[]
    classModifiers(Modifier[] modifiers) throws CompileException {
        return this.checkModifiers(
            modifiers,
            "public", "protected", "private", "abstract", "static", "final", "strictfp"
        );
    }

    private Modifier[]
    packageMemberClassModifiers(Modifier[] modifiers) throws CompileException {

        // JLS 8 8.1.1 Class Modifiers
        return this.checkModifiers(modifiers, "public", "abstract", "final", "strictfp");
    }

    private Modifier[]
    fieldModifiers(Modifier[] modifiers) throws CompileException {
        return this.checkModifiers(
            modifiers,
            "public", "protected", "private", "static", "final", "transient", "volatile"
        );
    }

    private Modifier[]
    methodModifiers(Modifier[] modifiers) throws CompileException {
        return this.checkModifiers(
            modifiers,
            "public", "protected", "private", "abstract", "static", "final", "synchronized", "native", "strictfp"
        );
    }

    private Modifier[]
    variableModifiers(Modifier[] modifiers) throws CompileException {
        return this.checkModifiers(modifiers, "final");
    }

    private Modifier[]
    constructorModifiers(Modifier[] modifiers) throws CompileException {
        return this.checkModifiers(modifiers, "public", "protected", "private");
    }

    private Modifier[]
    enumConstantModifiers(Modifier[] modifiers) throws CompileException {
        return this.checkModifiers(modifiers, "xxx");
    }

    private Modifier[]
    interfaceModifiers(Modifier[] modifiers) throws CompileException {
        return this.checkModifiers(modifiers, "public", "protected", "private", "abstract", "static", "strictfp");
    }

    private Modifier[]
    packageMemberInterfaceModifiers(Modifier[] modifiers) throws CompileException {

        // JLS8 9.1.1 Interface Modifiers
        return this.checkModifiers(modifiers, "public", "abstract", "strictfp");
    }

    private Modifier[]
    constantModifiers(Modifier[] modifiers) throws CompileException {
        return this.checkModifiers(modifiers, "public", "static", "final");
    }

    private Modifier[]
    interfaceMethodModifiers(Modifier[] modifiers) throws CompileException {
        return this.checkModifiers(modifiers, "public", "private", "abstract", "default", "static", "strictfp");
    }

    private Modifier[]
    annotationTypeElementModifiers(Modifier[] modifiers) throws CompileException {
        return this.checkModifiers(modifiers, "public", "abstract");
    }

    /**
     * Verifies that the <var>modifiers</var> are consistent.
     * <ul>
     *   <li>No two annotations must have the same type.</li>
     *   <li>No access modifier must appear more than once</li>
     *   <li>Certain modifier combinations must not occur (e.g. {@code abstract final}).
     *   <li>
     * </ul>
     *
     * @return <var>modifiers</var>
     */
    private Modifier[]
    checkModifiers(Modifier[] modifiers, String... allowedKeywords) throws CompileException {

        // Check for duplicate annotations is not possible at parse-time because the annotations' types must be
        // *resolved* before we can check for duplicates. See "UnitCompiler.compileAnnotations()".

        Set<String> keywords = new HashSet<String>();
        for (Modifier m : modifiers) {
            if (!(m instanceof Java.AccessModifier)) continue;
            AccessModifier am = (Java.AccessModifier) m;

            // Duplicate access modifier?
            if (!keywords.add(am.keyword)) {
                throw Parser.compileException("Duplication access modifier \"" + am.keyword + "\"", am.getLocation());
            }

            // Mutually exclusive access modifier keywords?
            for (Set<String> meams : Parser.MUTUALLY_EXCLUSIVE_ACCESS_MODIFIERS) {
                Set<String> tmp = new HashSet<String>(keywords);
                tmp.retainAll(meams);
                if (tmp.size() > 1) {
                    String[] a = (String[]) tmp.toArray(new String[tmp.size()]);
                    Arrays.sort(a);
                    throw Parser.compileException(
                        "Only one of " + Parser.join(a, " ") + " is allowed",
                        am.getLocation()
                    );
                }
            }
        }

        // Disallowed access modifiers?
        for (String kw : allowedKeywords) keywords.remove(kw);
        if (!keywords.isEmpty()) {
            String[] a = (String[]) keywords.toArray(new String[keywords.size()]);
            Arrays.sort(a);
            throw this.compileException(
                "Access modifier(s) " + Parser.join(a, " ") + " not allowed in this context"
            );
        }

        return modifiers;
    }

    @SuppressWarnings("unchecked") private static final List<Set<String>>
    MUTUALLY_EXCLUSIVE_ACCESS_MODIFIERS = Arrays.<Set<String>>asList(
        new HashSet<String>(Arrays.asList("public", "protected", "private")),
        new HashSet<String>(Arrays.asList("abstract", "final"))
    );
}
