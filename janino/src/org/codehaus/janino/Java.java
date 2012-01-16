
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.janino.CodeContext.Offset;
import org.codehaus.janino.util.Traverser;
import org.codehaus.janino.util.iterator.ReverseListIterator;

/**
 * This wrapper class defines classes that represent the elements of the
 * Java&trade; programming language.
 * <p>
 * Notices:
 * <ul>
 *   <li>
 *     "JLS1" refers to <a href="http://java.sun.com/docs/books/jls/first_edition/html/index.html">"The Java&trade;
 *     Language Specification, First Edition"</a>.
 *   </li>
 *   <li>
 *     "JLS" or "JLS2" refers to <a href="http://java.sun.com/docs/books/jls/second_edition/html/j.title.doc.html">"The
 *     Java&trade; Language Specification, Second Edition"</a>.
 *   </li>
 * </ul>
 */

public final class Java {
    private Java() {} // Don't instantiate me.

    public interface Scope {
        Scope getEnclosingScope();
    }

    /**
     * This interface is implemented by objects which are associated with a
     * location in the source code.
     */
    public interface Locatable {
        Location getLocation();

        /**
         * Throw a {@link CompileException} with the given message and this
         * object's location.
         *
         * @param message The message to report
         */
        void throwCompileException(String message) throws CompileException;
    }
    public abstract static class Located implements Locatable {
        public static final Located NOWHERE = new Located(Location.NOWHERE) { };
        private final Location location;

        protected Located(Location location) {
            //assert location != null;
            this.location = location;
        }

        // Implement "Locatable".

        public Location getLocation() { return this.location; }
        public void throwCompileException(String message) throws CompileException {
            throw new CompileException(message, this.location);
        }
    }

    /**
     * Holds the result of {@link Parser#parseCompilationUnit}.
     */
    public static final class CompilationUnit implements Scope {
        public final              String optionalFileName;
        public PackageDeclaration optionalPackageDeclaration    = null;
        public final List         importDeclarations            = new ArrayList(); // ImportDeclaration
        public final List         packageMemberTypeDeclarations = new ArrayList(); // PackageMemberTypeDeclaration

        public CompilationUnit(String optionalFileName) {
            this.optionalFileName = optionalFileName;
        }

        // Implement "Scope".
        public Scope getEnclosingScope() {
            throw new JaninoRuntimeException("A compilation unit has no enclosing scope");
        }

        public void setPackageDeclaration(PackageDeclaration packageDeclaration) {
            if (this.optionalPackageDeclaration != null) {
                throw new JaninoRuntimeException("Re-setting package declaration");
            }
            this.optionalPackageDeclaration = packageDeclaration;
        }

        public void addImportDeclaration(CompilationUnit.ImportDeclaration id) {

            // Conflicting imports are checked in UnitCompiler, not here.
            this.importDeclarations.add(id);
        }

        public void addPackageMemberTypeDeclaration(PackageMemberTypeDeclaration pmtd) {
            this.packageMemberTypeDeclarations.add(pmtd);
            pmtd.setDeclaringCompilationUnit(this);
        }

        /**
         * Get all classes and interfaces declared in this compilation unit.
         */
        public PackageMemberTypeDeclaration[] getPackageMemberTypeDeclarations() {
            return (PackageMemberTypeDeclaration[]) this.packageMemberTypeDeclarations.toArray(
                new PackageMemberTypeDeclaration[this.packageMemberTypeDeclarations.size()]
            );
        }

        /**
         * Return the package member class or interface declared with the given name.
         * @param name Declared (i.e. not the fully qualified) name
         * @return <code>null</code> if a package member type with that name is not declared in this compilation unit
         */
        public PackageMemberTypeDeclaration getPackageMemberTypeDeclaration(String name) {
            for (Iterator it = this.packageMemberTypeDeclarations.iterator(); it.hasNext();) {
                PackageMemberTypeDeclaration pmtd = (PackageMemberTypeDeclaration) it.next();
                if (pmtd.getName().equals(name)) return pmtd;
            }
            return null;
        }

        /**
         * Represents a single type import declaration like<pre>
         *     import java.util.Map;</pre>
         */
        public static class SingleTypeImportDeclaration extends ImportDeclaration {
            public final String[] identifiers;

            public SingleTypeImportDeclaration(Location location, String[] identifiers) {
                super(location);
                this.identifiers = identifiers;
            }
            public final void accept(Visitor.ImportVisitor visitor) { visitor.visitSingleTypeImportDeclaration(this); }
            public String toString() {
                return "import " + Java.join(this.identifiers, ".") + ';';
            }
        }

        /**
         * Represents a type-import-on-demand declaration like<pre>
         *     import java.util.*;</pre>
         */
        public static class TypeImportOnDemandDeclaration extends ImportDeclaration {
            public final String[] identifiers;

            public TypeImportOnDemandDeclaration(Location location, String[] identifiers) {
                super(location);
                this.identifiers = identifiers;
            }
            public final void accept(Visitor.ImportVisitor visitor) {
                visitor.visitTypeImportOnDemandDeclaration(this);
            }
            public String toString() {
                return "import " + Java.join(this.identifiers, ".") + ".*;";
            }
        }

        /**
         * Represents a single static import declaration like<pre>
         *     import java.util.Collections.EMPTY_MAP;</pre>
         */
        public static class SingleStaticImportDeclaration extends ImportDeclaration {
            public final String[] identifiers;

            public SingleStaticImportDeclaration(Location location, String[] identifiers) {
                super(location);
                this.identifiers = identifiers;
            }
            public final void accept(Visitor.ImportVisitor visitor) {
                visitor.visitSingleStaticImportDeclaration(this);
            }
        }

        /**
         * Represents a static-import-on-demand declaration like<pre>
         *     import java.util.Collections.*;</pre>
         */
        public static class StaticImportOnDemandDeclaration extends ImportDeclaration {
            public final String[] identifiers;

            public StaticImportOnDemandDeclaration(Location location, String[] identifiers) {
                super(location);
                this.identifiers = identifiers;
            }
            public final void accept(Visitor.ImportVisitor visitor) {
                visitor.visitStaticImportOnDemandDeclaration(this);
            }
        }

        public abstract static class ImportDeclaration extends Java.Located {
            public ImportDeclaration(Location location) {
                super(location);
            }
            public abstract void accept(Visitor.ImportVisitor visitor);
        }
    }

    /**
     * Represents a package declaration like<pre>
     *     package com.acme.tools;</pre>
     */
    public static class PackageDeclaration extends Located {
        public final String packageName;

        public PackageDeclaration(Location location, String packageName) {
            super(location);
            this.packageName = packageName;
        }
    }

    public interface TypeDeclaration extends Locatable, Scope {

        short getModifiers();

        /**
         * Return the member type with the given name.
         * @return <code>null</code> if a member type with that name is not declared
         */
        MemberTypeDeclaration getMemberTypeDeclaration(String name);

        Collection/*<MemberTypeDeclaration>*/ getMemberTypeDeclarations();

        /**
         * Return the first method declared with the given name. (Does not honor inherited
         * methods.)
         * @return <code>null</code> if a method with this name is not declared
         */
        MethodDeclarator getMethodDeclaration(String name);

        List/*<MethodDeclaration>*/ getMethodDeclarations();

        /**
         * Determine the effective class name, e.g. "pkg.Outer$Inner".
         */
        String getClassName();

        /**
         * Creates a unique name for a local class or interface.
         */
        String createLocalTypeName(String localTypeName);

        /**
         * Creates a unique name for an anonymous class.
         */
        String createAnonymousClassName();

        void accept(Visitor.TypeDeclarationVisitor visitor);
    }

    public interface DocCommentable {

        /**
         * Returns the doc comment of the object or <code>null</code>.
         */
        String getDocComment();

        /**
         * Returns <code>true</code> if the object has a doc comment and
         * the <code>&#64;deprecated</code> tag appears in the doc
         * comment.
         */
        boolean hasDeprecatedDocTag();
    }

    /**
     * Represents a class or interface declaration on compilation unit level. These are called
     * "package member types" because they are immediate members of a package, e.g.
     * "java.lang.String".
     */
    public interface PackageMemberTypeDeclaration extends NamedTypeDeclaration {
        void setDeclaringCompilationUnit(CompilationUnit declaringCompilationUnit);
        CompilationUnit getDeclaringCompilationUnit();
    }

    /**
     * Represents a class or interface declaration where the immediately enclosing scope is
     * another class or interface declaration.
     */
    public interface MemberTypeDeclaration extends NamedTypeDeclaration, TypeBodyDeclaration {
    }

    /**
     * Represents the declaration of a class or an interface that has a name. (All type
     * declarations are named, except for anonymous classes.)
     */
    public interface NamedTypeDeclaration extends TypeDeclaration {

        /**
         * Returns the declared (not the fully qualified) name of the class or interface.
         */
        String getName();
    }

    /**
     * Represents the declaration of an inner class, i.e. a class that exists in the context of
     * zero or more "enclosing instances". These are anonymous classes, local classes and member
     * classes.
     */
    interface InnerClassDeclaration extends TypeDeclaration {

        /**
         * Inner classes have zero or more synthetic fields that hold references to their enclosing
         * context:
         * <dl>
         *   <dt><code>this$<i>n</i></code></dt>
         *   <dd>
         *     (Mandatory for non-private non-static member classes; optional for private non-static
         *     member classes, local classes in non-static context, and anonymous classes in
         *     non-static context; forbidden for static member classes, local classes in static
         *     context, and anonymous classes in static context)
         *     Holds a reference to the immediately enclosing instance. <code><i>n</i></code> is
         *     N-1 for the Nth nesting level; e.g. the public non-static member class of a
         *     package member class has a synthetic field <code>this$0</code>.
         *   </dd>
         *   <dt><code>val$<i>local-variable-name</i></code></dt>
         *   <dd>
         *     (Allowed for local classes and anonymous classes; forbidden for member classes)
         *     Hold copies of <code>final</code> local variables of the defining context.
         *   </dd>
         * </dl>
         * Notice that these fields are not included in the {@link IClass.IField} array returned
         * by {@link IClass#getDeclaredIFields2()}.
         * <p>
         * If a synthetic field with the same name exists already, then it must have the same
         * type and the redefinition is ignored.
         * @param iField
         */
        void defineSyntheticField(IClass.IField iField) throws CompileException;
    }

    public abstract static class AbstractTypeDeclaration implements TypeDeclaration {
        private final Location location;
        private final short    modifiers;
        private final List     declaredMethods              = new ArrayList(); // MethodDeclarator
        private final List     declaredClassesAndInterfaces = new ArrayList(); // MemberTypeDeclaration
        private Scope          enclosingScope = null;

        IClass resolvedType = null;

        public AbstractTypeDeclaration(
            Location location,
            short    modifiers
        ) {
            this.location  = location;
            this.modifiers = modifiers;
        }

        public short getModifiers() {
            return this.modifiers;
        }


        public void setEnclosingScope(Scope enclosingScope) {
            if (this.enclosingScope != null && enclosingScope != this.enclosingScope) {
                throw new JaninoRuntimeException(
                    "Enclosing scope is already set for type declaration \""
                    + this.toString()
                    + "\" at "
                    + this.getLocation()
                );
            }
            this.enclosingScope = enclosingScope;
        }
        public Scope getEnclosingScope() {
            return this.enclosingScope;
        }

        public void invalidateMethodCaches() {
            if (this.resolvedType != null) {
                this.resolvedType.declaredIMethods = null;
                this.resolvedType.declaredIMethodCache = null;
            }
        }

        // Implement TypeDeclaration.
        public void addMemberTypeDeclaration(MemberTypeDeclaration mcoid) {
            this.declaredClassesAndInterfaces.add(mcoid);
            mcoid.setDeclaringType(this);
        }
        public Collection/*<MemberTypeDeclaration>*/ getMemberTypeDeclarations() {
            return this.declaredClassesAndInterfaces;
        }
        public MemberTypeDeclaration getMemberTypeDeclaration(String name) {
            for (Iterator it = this.declaredClassesAndInterfaces.iterator(); it.hasNext();) {
                MemberTypeDeclaration mtd = (MemberTypeDeclaration) it.next();
                if (mtd.getName().equals(name)) return mtd;
            }
            return null;
        }

        public void addDeclaredMethod(MethodDeclarator method) {
            this.declaredMethods.add(method);
            method.setDeclaringType(this);
        }

        public MethodDeclarator getMethodDeclaration(String name) {
            for (Iterator it = this.declaredMethods.iterator(); it.hasNext();) {
                MethodDeclarator md = (MethodDeclarator) it.next();
                if (md.name.equals(name)) return md;
            }
            return null;
        }

        public List getMethodDeclarations() {
            return this.declaredMethods;
        }

        public String createLocalTypeName(String localTypeName) {
            return (
                this.getClassName()
                + '$'
                + ++this.localClassCount
                + '$'
                + localTypeName
            );
        }
        public String createAnonymousClassName() {
            return (
                this.getClassName()
                + '$'
                + ++this.anonymousClassCount
            );
        }

        // Implement "Locatable".
        public Location getLocation() { return this.location; }
        public void throwCompileException(String message) throws CompileException {
            throw new CompileException(message, this.location);
        }

        public abstract String toString();

        public int anonymousClassCount = 0; // For naming anonymous classes.
        public int localClassCount = 0;     // For naming local classes.
    }

    public abstract static class ClassDeclaration extends AbstractTypeDeclaration {
        public final List constructors = new ArrayList(); // ConstructorDeclarator
        public final List variableDeclaratorsAndInitializers = new ArrayList(); // TypeBodyDeclaration

        public ClassDeclaration(
            Location location,
            short    modifiers
        ) {
            super(location, modifiers);
        }

        public void addConstructor(ConstructorDeclarator cd) {
            this.constructors.add(cd);
            cd.setDeclaringType(this);
        }
        public void addVariableDeclaratorOrInitializer(TypeBodyDeclaration tbd) {
            this.variableDeclaratorsAndInitializers.add(tbd);
            tbd.setDeclaringType(this);

            // Clear resolved type cache.
            if (this.resolvedType != null) this.resolvedType.clearIFieldCaches();
        }

        // Compile time members.

        // Implement InnerClassDeclaration.
        public void defineSyntheticField(IClass.IField iField) throws CompileException {
            if (!(this instanceof InnerClassDeclaration)) throw new JaninoRuntimeException();

            IClass.IField if2 = (IClass.IField) this.syntheticFields.get(iField.getName());
            if (if2 != null) {
                if (iField.getType() != if2.getType()) throw new JaninoRuntimeException();
                return;
            }
            this.syntheticFields.put(iField.getName(), iField);
        }

        /**
         * Return the declared constructors, or the default constructor.
         */
        ConstructorDeclarator[] getConstructors() {
            if (this.constructors.isEmpty()) {
                ConstructorDeclarator defaultConstructor = new ConstructorDeclarator(
                    this.getLocation(),                        // location
                    null,                                      // optionalDocComment
                    Mod.PUBLIC,                                // modifiers
                    new FunctionDeclarator.FormalParameter[0], // formalParameters
                    new Type[0],                               // thrownExceptions
                    null,                                      // optionalExplicitConstructorInvocation
                    Collections.EMPTY_LIST                     // optionalStatements
                );
                defaultConstructor.setDeclaringType(this);
                return new ConstructorDeclarator[] { defaultConstructor };
            }

            return (ConstructorDeclarator[]) this.constructors.toArray(
                new ConstructorDeclarator[this.constructors.size()]
            );
        }

        // All field names start with "this$" or "val$".
        final SortedMap syntheticFields = new TreeMap(); // String name => IClass.IField
    }

    public static final class AnonymousClassDeclaration extends ClassDeclaration implements InnerClassDeclaration {
        public final Type baseType;  // Base class or interface

        public AnonymousClassDeclaration(
            Location location,
            Type     baseType
        ) {
            super(
                location,                         // location
                (short) (Mod.PRIVATE | Mod.FINAL) // modifiers
            );
            (this.baseType = baseType).setEnclosingScope(new EnclosingScopeOfTypeDeclaration(this));
        }

        public void accept(Visitor.TypeDeclarationVisitor visitor) {
            visitor.visitAnonymousClassDeclaration(this);
        }

        // Implement TypeDeclaration.
        public String getClassName() {
            if (this.myName == null) {
                Scope s = this.getEnclosingScope();
                for (; !(s instanceof TypeDeclaration); s = s.getEnclosingScope());
                this.myName = ((TypeDeclaration) s).createAnonymousClassName();
            }
            return this.myName;
        }
        private String myName = null;
        public String toString() { return this.getClassName(); }
    }

    public abstract static class NamedClassDeclaration
    extends ClassDeclaration
    implements NamedTypeDeclaration, DocCommentable {
        private final String optionalDocComment;
        public final String  name;
        public final Type    optionalExtendedType;
        public final Type[]  implementedTypes;

        public NamedClassDeclaration(
            Location location,
            String   optionalDocComment,
            short    modifiers,
            String   name,
            Type     optionalExtendedType,
            Type[]   implementedTypes
        ) {
            super(location, modifiers);
            this.optionalDocComment   = optionalDocComment;
            this.name                 = name;
            this.optionalExtendedType = optionalExtendedType;
            if (optionalExtendedType != null) {
                optionalExtendedType.setEnclosingScope(new EnclosingScopeOfTypeDeclaration(this));
            }
            this.implementedTypes     = implementedTypes;
            for (int i = 0; i < implementedTypes.length; ++i) {
                implementedTypes[i].setEnclosingScope(new EnclosingScopeOfTypeDeclaration(this));
            }
        }

        public String toString() { return this.name; }

        // Implement NamedTypeDeclaration.
        public String getName() { return this.name; }

        // Implement DocCommentable.
        public String getDocComment() { return this.optionalDocComment; }
        public boolean hasDeprecatedDocTag() {
            return this.optionalDocComment != null && this.optionalDocComment.indexOf("@deprecated") != -1;
        }
    }

    /**
     * Lazily determines and returns the enclosing
     * {@link org.codehaus.janino.Java.Scope} of the given
     * {@link org.codehaus.janino.Java.TypeDeclaration}.
     */
    public static final class EnclosingScopeOfTypeDeclaration implements Scope {
        public final TypeDeclaration typeDeclaration;
        public EnclosingScopeOfTypeDeclaration(TypeDeclaration typeDeclaration) {
            this.typeDeclaration = typeDeclaration;
        }
        public Scope getEnclosingScope() { return this.typeDeclaration.getEnclosingScope(); }
    }

    public static final class MemberClassDeclaration
    extends NamedClassDeclaration
    implements MemberTypeDeclaration, InnerClassDeclaration {
        public MemberClassDeclaration(
            Location             location,
            String               optionalDocComment,
            short                modifiers,
            String               name,
            Type                 optionalExtendedType,
            Type[]               implementedTypes
        ) {
            super(
                location,              // location
                optionalDocComment,    // optionalDocComment
                modifiers,             // modifiers
                name,                  // name
                optionalExtendedType,  // optionalExtendedType
                implementedTypes       // implementedTypes
            );
        }

        // Implement TypeBodyDeclaration.
        public void setDeclaringType(TypeDeclaration declaringType) {
            this.setEnclosingScope(declaringType);
        }
        public TypeDeclaration getDeclaringType() {
            return (TypeDeclaration) this.getEnclosingScope();
        }
        public boolean isStatic() {
            return (this.getModifiers() & Mod.STATIC) != 0;
        }

        // Implement TypeDeclaration.
        public String getClassName() {
            return (
                this.getDeclaringType().getClassName()
                + '$'
                + this.getName()
            );
        }

        public void accept(Visitor.TypeDeclarationVisitor visitor) { visitor.visitMemberClassDeclaration(this); }
        public void accept(Visitor.TypeBodyDeclarationVisitor visitor) { visitor.visitMemberClassDeclaration(this); }
    }

    public static final class LocalClassDeclaration extends NamedClassDeclaration implements InnerClassDeclaration {
        public LocalClassDeclaration(
            Location location,
            String   optionalDocComment,
            short    modifiers,
            String   name,
            Type     optionalExtendedType,
            Type[]   implementedTypes
        ) {
            super(
                location,               // location
                optionalDocComment,     // optionalDocComment
                modifiers,              // modifiers
                name,                   // name
                optionalExtendedType,   // optionalExtendedType
                implementedTypes        // implementedTypes
            );
        }

        // Implement ClassDeclaration.
        protected IClass getOuterIClass2() {
            Scope s = this.getEnclosingScope();
            for (; !(s instanceof FunctionDeclarator); s = s.getEnclosingScope());
            if (
                s instanceof MethodDeclarator
                && (((MethodDeclarator) s).modifiers & Mod.STATIC) != 0
            ) return null;
            for (; !(s instanceof TypeDeclaration); s = s.getEnclosingScope());
            return ((AbstractTypeDeclaration) s).resolvedType;
        }

        // Implement TypeDeclaration.
        public String getClassName() {
            for (Scope s = this.getEnclosingScope();; s = s.getEnclosingScope()) {
                if (s instanceof Java.TypeDeclaration) {
                    return ((Java.TypeDeclaration) s).getClassName() + '$' + this.name;
                }
            }
        }

        public void accept(Visitor.TypeDeclarationVisitor visitor) { visitor.visitLocalClassDeclaration(this); }
    }

    public static final class PackageMemberClassDeclaration
    extends NamedClassDeclaration
    implements PackageMemberTypeDeclaration {
        public PackageMemberClassDeclaration(
            Location location,
            String   optionalDocComment,
            short    modifiers,
            String   name,
            Type     optionalExtendedType,
            Type[]   implementedTypes
        ) throws CompileException {
            super(
                location,                         // location
                optionalDocComment,               // optionalDocComment
                modifiers,                        // modifiers
                name,                             // name
                optionalExtendedType,             // optionalExtendedType
                implementedTypes                  // implementedTypes
            );

            // Check for forbidden modifiers (JLS 7.6).
            if ((modifiers & (Mod.PROTECTED | Mod.PRIVATE | Mod.STATIC)) != 0) {
                this.throwCompileException(
                    "Modifiers \"protected\", \"private\" and \"static\" not allowed in package member class "
                    + "declaration"
                );
            }
        }

        // Implement PackageMemberTypeDeclaration.
        public void setDeclaringCompilationUnit(CompilationUnit declaringCompilationUnit) {
            this.setEnclosingScope(declaringCompilationUnit);
        }
        public CompilationUnit getDeclaringCompilationUnit() {
            return (CompilationUnit) this.getEnclosingScope();
        }

        // Implement ClassDeclaration.
        protected IClass getOuterIClass2() {
            return null;
        }

        // Implement TypeDeclaration.
        public String getClassName() {
            String className = this.getName();

            CompilationUnit compilationUnit = (CompilationUnit) this.getEnclosingScope();
            if (compilationUnit.optionalPackageDeclaration != null) {
                className = compilationUnit.optionalPackageDeclaration.packageName + '.' + className;
            }

            return className;
        }

        public void accept(Visitor.TypeDeclarationVisitor visitor) { visitor.visitPackageMemberClassDeclaration(this); }
    }

    public abstract static class InterfaceDeclaration
    extends AbstractTypeDeclaration
    implements NamedTypeDeclaration, DocCommentable {
        private final String optionalDocComment;
        public final String  name;

        protected InterfaceDeclaration(
            Location location,
            String   optionalDocComment,
            short    modifiers,
            String   name,
            Type[]   extendedTypes
        ) {
            super(location, modifiers);
            this.optionalDocComment = optionalDocComment;
            this.name               = name;
            this.extendedTypes      = extendedTypes;
            for (int i = 0; i < extendedTypes.length; ++i) {
                extendedTypes[i].setEnclosingScope(new EnclosingScopeOfTypeDeclaration(this));
            }
        }

        public String toString() { return this.name; }

        public void addConstantDeclaration(FieldDeclaration fd) {
            this.constantDeclarations.add(fd);
            fd.setDeclaringType(this);

            // Clear resolved type cache.
            if (this.resolvedType != null) this.resolvedType.clearIFieldCaches();
        }

        public final Type[] extendedTypes;
        public final List   constantDeclarations = new ArrayList(); // FieldDeclaration

        // Set during "compile()".
        IClass[] interfaces = null;

        // Implement NamedTypeDeclaration.
        public String getName() { return this.name; }

        // Implement DocCommentable.
        public String getDocComment() { return this.optionalDocComment; }
        public boolean hasDeprecatedDocTag() {
            return this.optionalDocComment != null && this.optionalDocComment.indexOf("@deprecated") != -1;
        }
    }

    public static final class MemberInterfaceDeclaration extends InterfaceDeclaration implements MemberTypeDeclaration {
        public MemberInterfaceDeclaration(
            Location             location,
            String               optionalDocComment,
            short                modifiers,
            String               name,
            Type[]               extendedTypes
        ) {
            super(
                location,              // location
                optionalDocComment,    // optionalDocComment
                modifiers,             // modifiers
                name,                  // name
                extendedTypes          // extendedTypes
            );
        }

        // Implement TypeDeclaration.
        public String getClassName() {
            NamedTypeDeclaration declaringType = (NamedTypeDeclaration) this.getEnclosingScope();
            return (
                declaringType.getClassName()
                + '$'
                + this.getName()
            );
        }

        // Implement TypeBodyDeclaration.
        public void setDeclaringType(TypeDeclaration declaringType) { this.setEnclosingScope(declaringType); }
        public TypeDeclaration getDeclaringType() {
            return (TypeDeclaration) this.getEnclosingScope();
        }
        public boolean isStatic() {
            return (this.getModifiers() & Mod.STATIC) != 0;
        }

        public void accept(Visitor.TypeDeclarationVisitor visitor) {
            visitor.visitMemberInterfaceDeclaration(this);
        }
        public void accept(Visitor.TypeBodyDeclarationVisitor visitor) {
            visitor.visitMemberInterfaceDeclaration(this);
        }
    }

    public static final class PackageMemberInterfaceDeclaration
    extends InterfaceDeclaration
    implements PackageMemberTypeDeclaration {
        public PackageMemberInterfaceDeclaration(
            Location        location,
            String          optionalDocComment,
            short           modifiers,
            String          name,
            Type[]          extendedTypes
        ) throws CompileException {
            super(
                location,                         // location
                optionalDocComment,               // optionalDocComment
                modifiers,                        // modifiers
                name,                             // name
                extendedTypes                     // extendedTypes
            );

            // Check for forbidden modifiers (JLS 7.6).
            if ((modifiers & (Mod.PROTECTED | Mod.PRIVATE | Mod.STATIC)) != 0) {
                this.throwCompileException(
                    "Modifiers \"protected\", \"private\" and \"static\" not allowed in package member interface "
                    + "declaration"
                );
            }
        }

        // Implement PackageMemberTypeDeclaration.
        public void setDeclaringCompilationUnit(CompilationUnit declaringCompilationUnit) {
            this.setEnclosingScope(declaringCompilationUnit);
        }

        public CompilationUnit getDeclaringCompilationUnit() {
            return (CompilationUnit) this.getEnclosingScope();
        }

        // Implement TypeDeclaration.
        public String getClassName() {
            String className = this.getName();

            CompilationUnit compilationUnit = (CompilationUnit) this.getEnclosingScope();
            if (compilationUnit.optionalPackageDeclaration != null) {
                className = compilationUnit.optionalPackageDeclaration.packageName + '.' + className;
            }

            return className;
        }

        public void accept(Visitor.TypeDeclarationVisitor visitor) {
            visitor.visitPackageMemberInterfaceDeclaration(this);
        }
    }

    /**
     * Representation of a "ClassBodyDeclaration" or an "InterfaceMemberDeclaration". These are:
     * <ul>
     *   <li>Field declarators
     *   <li>Method declarators
     *   <li>Static and non-static initializers
     *   <li>Member type declarations
     * </ul>
     */
    public interface TypeBodyDeclaration extends Locatable, Scope {
        void            setDeclaringType(TypeDeclaration declaringType);
        TypeDeclaration getDeclaringType();
        boolean         isStatic();
        void            accept(Visitor.TypeBodyDeclarationVisitor visitor);
    }

    public abstract static class AbstractTypeBodyDeclaration extends Located implements TypeBodyDeclaration {
        private TypeDeclaration declaringType;
        public final boolean    statiC;

        protected AbstractTypeBodyDeclaration(
            Location location,
            boolean  statiC
        ) {
            super(location);
            this.statiC = statiC;
        }

        // Implement TypeBodyDeclaration.
        public void setDeclaringType(TypeDeclaration declaringType) {
            if (this.declaringType != null && declaringType != null) {
                throw new JaninoRuntimeException(
                    "Declaring type for type body declaration \""
                    + this.toString()
                    + "\"at "
                    + this.getLocation()
                    + " is already set"
                );
            }
            this.declaringType = declaringType;
        }
        public TypeDeclaration getDeclaringType() {
            return this.declaringType;
        }

        public boolean isStatic() {
            return this.statiC;
        }

        // Implement BlockStatement.
        public void setEnclosingScope(Scope enclosingScope) {
            this.declaringType = (TypeDeclaration) enclosingScope;
        }
        public Scope getEnclosingScope() {
            return this.declaringType;
        }
    }

    /**
     * Representation of an instance (JLS2 8.6) or static initializer (JLS2 8.7).
     */
    public static final class Initializer extends AbstractTypeBodyDeclaration implements BlockStatement {
        public final Block block;

        public Initializer(
            Location location,
            boolean  statiC,
            Block    block
        ) {
            super(location, statiC);
            (this.block = block).setEnclosingScope(this);
        }
        public String toString() {
            return this.statiC ? "static " + this.block : this.block.toString();
        }

        // Implement BlockStatement.

        public void accept(Visitor.TypeBodyDeclarationVisitor visitor) { visitor.visitInitializer(this); }
        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitInitializer(this); }

        public Java.LocalVariable findLocalVariable(String name) {
            return this.block.findLocalVariable(name);
        }
    }

    /**
     * Abstract base class for {@link Java.ConstructorDeclarator} and
     * {@link Java.MethodDeclarator}.
     */
    public abstract static class FunctionDeclarator extends AbstractTypeBodyDeclaration implements DocCommentable {
        private final String                  optionalDocComment;
        public final short                    modifiers;
        public final Type                     type;
        public final String                   name;
        public final FormalParameter[]        formalParameters;
        public final Type[]                   thrownExceptions;
        public final List/*<BlockStatement>*/ optionalStatements;

        public FunctionDeclarator(
            Location                 location,
            String                   optionalDocComment,
            short                    modifiers,
            Type                     type,
            String                   name,
            FormalParameter[]        formalParameters,
            Type[]                   thrownExceptions,
            List/*<BlockStatement>*/ optionalStatements
        ) {
            super(location, (modifiers & Mod.STATIC) != 0);
            this.optionalDocComment = optionalDocComment;
            this.modifiers          = modifiers;
            (this.type               = type).setEnclosingScope(this);
            this.name               = name;
            this.formalParameters   = formalParameters;
            for (int i = 0; i < formalParameters.length; ++i) formalParameters[i].type.setEnclosingScope(this);
            this.thrownExceptions   = thrownExceptions;
            for (int i = 0; i < thrownExceptions.length; ++i) thrownExceptions[i].setEnclosingScope(this);
            this.optionalStatements = optionalStatements;
            if (optionalStatements != null) {
                for (Iterator it = optionalStatements.iterator(); it.hasNext();) {
                    Java.BlockStatement bs = (Java.BlockStatement) it.next();

                    // Catch 22: In the initializers, some statement have their enclosing already
                    // set!
                    if (("<init>".equals(name) || "<clinit>".equals(name)) && bs.getEnclosingScope() != null) continue;

                    bs.setEnclosingScope(this);
                }
            }
        }

        // Implement "Scope".
        public Scope getEnclosingScope() {
            return this.getDeclaringType();
        }

        // Set by "compile()".
        IClass returnType = null;

        // Implement DocCommentable.
        public String getDocComment() { return this.optionalDocComment; }
        public boolean hasDeprecatedDocTag() {
            return this.optionalDocComment != null && this.optionalDocComment.indexOf("@deprecated") != -1;
        }

        public static final class FormalParameter extends Java.Located {
            public final boolean finaL;
            public final Type    type;
            public final String  name;

            public FormalParameter(
                Location location,
                boolean  finaL,
                Type     type,
                String   name
            ) {
                super(location);
                this.finaL = finaL;
                this.type  = type;
                this.name  = name;
            }

            public String toString() {
                return this.type.toString() + ' ' + this.name;
            }

            // Compile time members.

            public Java.LocalVariable localVariable = null;
        }

        // Compile time members
        public Map localVariables = null; // String name => Java.LocalVariable
    }

    public static final class ConstructorDeclarator extends FunctionDeclarator {
        IClass.IConstructor                iConstructor = null;
        public final ConstructorInvocation optionalConstructorInvocation;

        public ConstructorDeclarator(
            Location                             location,
            String                               optionalDocComment,
            short                                modifiers,
            FunctionDeclarator.FormalParameter[] formalParameters,
            Type[]                               thrownExceptions,
            ConstructorInvocation                optionalConstructorInvocation,
            List                                 statements // BlockStatement
        ) {
            super(
                location,                                // location
                optionalDocComment,                      // optionalDocComment
                modifiers,                               // modifiers
                new BasicType(location, BasicType.VOID), // type
                "<init>",                                // name
                formalParameters,                        // formalParameters
                thrownExceptions,                        // thrownExceptions
                statements                               // optionalStatements
            );
            this.optionalConstructorInvocation = optionalConstructorInvocation;
            if (optionalConstructorInvocation != null) optionalConstructorInvocation.setEnclosingScope(this);
        }

        public ClassDeclaration getDeclaringClass() {
            return (ClassDeclaration) this.getEnclosingScope();
        }

        // Compile time members.

        final Map syntheticParameters = new HashMap(); // String name => LocalVariable

        // Implement "FunctionDeclarator":

        public String toString() {
            StringBuffer sb = new StringBuffer(this.getDeclaringClass().getClassName());
            sb.append('(');
            FunctionDeclarator.FormalParameter[] fps = this.formalParameters;
            for (int i = 0; i < fps.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(fps[i].toString());
            }
            sb.append(')');
            return sb.toString();
        }

        public void accept(Visitor.TypeBodyDeclarationVisitor visitor) {
            visitor.visitConstructorDeclarator(this);
        }
    }

    public static final class MethodDeclarator extends FunctionDeclarator {
        public MethodDeclarator(
            Location                             location,
            String                               optionalDocComment,
            short                                modifiers,
            Type                                 type,
            String                               name,
            FunctionDeclarator.FormalParameter[] formalParameters,
            Type[]                               thrownExceptions,
            List                                 optionalStatements
        ) {
            super(
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

        public String toString() {
            StringBuffer sb = new StringBuffer(this.name);
            sb.append('(');
            FunctionDeclarator.FormalParameter[] fps = this.formalParameters;
            for (int i = 0; i < fps.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(fps[i].toString());
            }
            sb.append(')');
            return sb.toString();
        }

        public void accept(Visitor.TypeBodyDeclarationVisitor visitor) { visitor.visitMethodDeclarator(this); }

        IClass.IMethod iMethod = null;
    }

    /**
     * This class is derived from "Statement", because it provides for the
     * initialization of the field. In other words, "compile()" generates the
     * code that initializes the field.
     */
    public static final class FieldDeclaration extends Statement implements TypeBodyDeclaration, DocCommentable {
        private final String              optionalDocComment;
        public final short                modifiers;
        public final Type                 type;
        public final VariableDeclarator[] variableDeclarators;

        public FieldDeclaration(
            Location             location,
            String               optionalDocComment,
            short                modifiers,
            Type                 type,
            VariableDeclarator[] variableDeclarators
        ) {
            super(location);
            this.optionalDocComment  = optionalDocComment;
            this.modifiers           = modifiers;
            (this.type                = type).setEnclosingScope(this);
            this.variableDeclarators = variableDeclarators;
            for (int i = 0; i < variableDeclarators.length; ++i) {
                VariableDeclarator vd = variableDeclarators[i];
                if (vd.optionalInitializer != null) Java.setEnclosingBlockStatement(vd.optionalInitializer, this);
            }
        }

        // Implement TypeBodyDeclaration.
        public void setDeclaringType(TypeDeclaration declaringType) {
            this.setEnclosingScope(declaringType);
        }
        public TypeDeclaration getDeclaringType() {
            return (TypeDeclaration) this.getEnclosingScope();
        }
        public boolean isStatic() {
            return (this.modifiers & Mod.STATIC) != 0;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(Mod.shortToString(this.modifiers)).append(' ').append(this.type).append(' ');
            sb.append(this.variableDeclarators[0]);
            for (int i = 1; i < this.variableDeclarators.length; ++i) {
                sb.append(", ").append(this.variableDeclarators[i]);
            }
            return sb.toString();
        }

        public void accept(Visitor.TypeBodyDeclarationVisitor visitor) { visitor.visitFieldDeclaration(this); }
        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitFieldDeclaration(this); }

        // Implement DocCommentable.
        public String getDocComment() { return this.optionalDocComment; }
        public boolean hasDeprecatedDocTag() {
            return this.optionalDocComment != null && this.optionalDocComment.indexOf("@deprecated") != -1;
        }
    }
    private static void setEnclosingBlockStatement(
        ArrayInitializerOrRvalue aiorv,
        BlockStatement           enclosingBlockStatement
    ) {
        if (aiorv instanceof Rvalue) {
            ((Rvalue) aiorv).setEnclosingBlockStatement(enclosingBlockStatement);
        } else
        if (aiorv instanceof ArrayInitializer) {
            ArrayInitializerOrRvalue[] values = ((ArrayInitializer) aiorv).values;
            for (int i = 0; i < values.length; ++i) Java.setEnclosingBlockStatement(values[i], enclosingBlockStatement);
        } else
        {
            throw new JaninoRuntimeException("Unexpected array or initializer class " + aiorv.getClass().getName());
        }
    }

    /** Used by FieldDeclaration and LocalVariableDeclarationStatement. */
    public static final class VariableDeclarator extends Located {
        public final String                   name;
        public final int                      brackets;
        public final ArrayInitializerOrRvalue optionalInitializer;

        public VariableDeclarator(
            Location                 location,
            String                   name,
            int                      brackets,
            ArrayInitializerOrRvalue optionalInitializer
        ) {
            super(location);
            this.name                = name;
            this.brackets            = brackets;
            this.optionalInitializer = optionalInitializer;

            // Used both by field declarations an local variable declarations, so naming
            // conventions checking (JLS2 6.8) cannot be done here.
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.name);
            for (int i = 0; i < this.brackets; ++i) sb.append("[]");
            if (this.optionalInitializer != null) sb.append(" = ").append(this.optionalInitializer);
            return sb.toString();
        }

        // Compile time members.

        // Used only if the variable declarator declares a local variable.
        public LocalVariable localVariable = null;
    }

    /**
     * Base of all statements that can appear in a block.
     */
    public interface BlockStatement extends Locatable, Scope {
        void setEnclosingScope(Scope enclosingScope);
        Scope getEnclosingScope();

        void accept(Visitor.BlockStatementVisitor visitor);
        Java.LocalVariable findLocalVariable(String name);
    }

    public abstract static class Statement extends Located implements BlockStatement {
        private Scope enclosingScope = null;

        protected Statement(Location location) {
            super(location);
        }

        // Implement "BlockStatement".
        public void setEnclosingScope(Scope enclosingScope) {
            if (this.enclosingScope != null && enclosingScope != this.enclosingScope) {
                throw new JaninoRuntimeException(
                    "Enclosing scope is already set for statement \""
                    + this.toString()
                    + "\" at "
                    + this.getLocation()
                );
            }
            this.enclosingScope = enclosingScope;
        }
        public Scope getEnclosingScope() { return this.enclosingScope; }

        // Compile time members
        public Map localVariables = null; // String name => Java.LocalVariable
        public Java.LocalVariable findLocalVariable(String name) {
            if (this.localVariables == null) { return null; }
            return (LocalVariable) this.localVariables.get(name);
        }
    }

    public static final class LabeledStatement extends BreakableStatement {
        public final String    label;
        public final Statement body;

        public LabeledStatement(
            Location  location,
            String    label,
            Statement body
        ) {
            super(location);
            this.label = label;
            (this.body  = body).setEnclosingScope(this);
        }
        public String toString() {
            return this.label + ": " + this.body;
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitLabeledStatement(this); }
    }

    /**
     * Representation of a Java&trade; "block" (JLS 14.2).
     * <p>
     * The statements that the block defines are executed in sequence.
     */
    public static final class Block extends Statement {
        public final List statements = new ArrayList(); // BlockStatement

        public Block(Location location) {
            super(location);
        }

        public void addStatement(BlockStatement statement) {
            this.statements.add(statement);
            statement.setEnclosingScope(this);
        }

        public void addStatements(
            List statements // BlockStatement
        ) {
            this.statements.addAll(statements);
            for (Iterator it = statements.iterator(); it.hasNext();) {
                ((BlockStatement) it.next()).setEnclosingScope(this);
            }
        }

        public BlockStatement[] getStatements() {
            return (BlockStatement[]) this.statements.toArray(new BlockStatement[this.statements.size()]);
        }

        // Compile time members.
        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitBlock(this); }

        public String toString() {
            return "{ ... }";
        }
    }

    /**
     * Base class for statements that can be terminated abnormally with a
     * "break" statement.
     * <p>
     * According to the JLS, statements that can be terminated abnormally with
     * a "break" statement are: "COntinuable" statements ("for", "do" and
     * "while"), labeled statements, and the "switch" statement.
     */
    public abstract static class BreakableStatement extends Statement {
        protected BreakableStatement(Location location) {
            super(location);
        }

        CodeContext.Offset whereToBreak = null;
    }

    public abstract static class ContinuableStatement extends BreakableStatement {
        protected ContinuableStatement(Location location) {
            super(location);
        }

        protected CodeContext.Offset whereToContinue = null;
    }

    public static final class ExpressionStatement extends Statement {
        public final Rvalue rvalue;

        public ExpressionStatement(Rvalue rvalue) throws CompileException {
            super(rvalue.getLocation());
            if (!(
                rvalue instanceof Java.Assignment
                || rvalue instanceof Java.Crement
                || rvalue instanceof Java.MethodInvocation
                || rvalue instanceof Java.SuperclassMethodInvocation
                || rvalue instanceof Java.NewClassInstance
                || rvalue instanceof Java.NewAnonymousClassInstance
            )) {
                String expressionType = rvalue.getClass().getName();
                expressionType = expressionType.substring(expressionType.lastIndexOf('.') + 1);
                this.throwCompileException(
                    expressionType
                    + " is not allowed as an expression statement. "
                    + "Expressions statements must be one of assignments, method invocations, or object allocations."
                );
            }
            (this.rvalue = rvalue).setEnclosingBlockStatement(this);
        }

        public String toString() {
            return this.rvalue.toString() + ';';
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitExpressionStatement(this); }
    }

    public static final class LocalClassDeclarationStatement extends Statement {
        public final LocalClassDeclaration lcd;

        public LocalClassDeclarationStatement(Java.LocalClassDeclaration lcd) {
            super(lcd.getLocation());
            (this.lcd = lcd).setEnclosingScope(this);
        }
        public String toString() {
            return this.lcd.toString();
        }

        public void accept(Visitor.BlockStatementVisitor visitor) {
            visitor.visitLocalClassDeclarationStatement(this);
        }
    }

    public static final class IfStatement extends Statement {
        public final Rvalue         condition;
        public final BlockStatement thenStatement;
        public final BlockStatement optionalElseStatement;

        /**
         * Notice that the <code>elseStatement</code> is mandatory; for an if statement without
         * an "else" clause, a dummy {@link Java.EmptyStatement} should be passed.
         */
        public IfStatement(
            Location       location,
            Rvalue         condition,
            BlockStatement thenStatement,
            BlockStatement optionalElseStatement
        ) {
            super(location);
            (this.condition             = condition).setEnclosingBlockStatement(this);
            (this.thenStatement         = thenStatement).setEnclosingScope(this);
            this.optionalElseStatement = optionalElseStatement;
            if (optionalElseStatement != null) optionalElseStatement.setEnclosingScope(this);
        }

        public String toString() {
            return this.optionalElseStatement == null ? "if" : "if ... else";
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitIfStatement(this); }
    }

    public static final class ForStatement extends ContinuableStatement {
        public final BlockStatement optionalInit;
        public final Rvalue         optionalCondition;
        public final Rvalue[]       optionalUpdate;
        public final BlockStatement body;

        public ForStatement(
            Location       location,
            BlockStatement optionalInit,
            Rvalue         optionalCondition,
            Rvalue[]       optionalUpdate,
            BlockStatement body
        ) {
            super(location);
            this.optionalInit = optionalInit;
            if (optionalInit != null) optionalInit.setEnclosingScope(this);
            this.optionalCondition = optionalCondition;
            if (optionalCondition != null) optionalCondition.setEnclosingBlockStatement(this);
            this.optionalUpdate = optionalUpdate;
            if (optionalUpdate != null) {
                for (int i = 0; i < optionalUpdate.length; ++i) optionalUpdate[i].setEnclosingBlockStatement(this);
            }
            (this.body = body).setEnclosingScope(this);
        }
        public String toString() {
            return "for (...; ...; ...) ...";
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitForStatement(this); }
    }

    public static final class WhileStatement extends ContinuableStatement {
        public final Rvalue         condition;
        public final BlockStatement body;

        public WhileStatement(
            Location       location,
            Rvalue         condition,
            BlockStatement body
        ) {
            super(location);
            (this.condition = condition).setEnclosingBlockStatement(this);
            (this.body = body).setEnclosingScope(this);
        }
        public String toString() {
            return "while (" + this.condition + ") " + this.body + ';';
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitWhileStatement(this); }
    }

    public static final class TryStatement extends Statement {
        public final BlockStatement body;
        public final List           catchClauses; // CatchClause
        public final Block          optionalFinally;

        public TryStatement(
            Location       location,
            BlockStatement body,
            List           catchClauses, // CatchClause
            Block          optionalFinally
        ) {
            super(location);
            (this.body            = body).setEnclosingScope(this);
            this.catchClauses    = catchClauses;
            for (Iterator it = catchClauses.iterator(); it.hasNext();) {
                ((CatchClause) it.next()).setEnclosingTryStatement(this);
            }
            this.optionalFinally = optionalFinally;
            if (optionalFinally != null) optionalFinally.setEnclosingScope(this);
        }
        public String toString() {
            return (
                "try ... "
                + this.catchClauses.size()
                + (this.optionalFinally == null ? " catches" : " catches ... finally")
            );
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitTryStatement(this); }

        CodeContext.Offset finallyOffset = null;
    }

    public static class CatchClause extends Located implements Scope {
        public final FunctionDeclarator.FormalParameter caughtException;
        public final Block                              body;
        private TryStatement                            enclosingTryStatement = null;

        // Compile time fields.

        public boolean reachable;

        public CatchClause(
            Location                           location,
            FunctionDeclarator.FormalParameter caughtException,
            Block                              body
        ) {
            super(location);
            (this.caughtException = caughtException).type.setEnclosingScope(this);
            (this.body            = body).setEnclosingScope(this);
        }

        public void setEnclosingTryStatement(TryStatement enclosingTryStatement) {
            if (this.enclosingTryStatement != null && enclosingTryStatement != this.enclosingTryStatement) {
                throw new JaninoRuntimeException(
                    "Enclosing TYR statement already set for catch clause "
                    + this.toString()
                    + " at "
                    + this.getLocation()
                );
            }
            this.enclosingTryStatement = enclosingTryStatement;
        }
        public Scope getEnclosingScope() { return this.enclosingTryStatement; }
    }

    /**
     * 14.10 The "switch" Statement
     */
    public static final class SwitchStatement extends BreakableStatement {
        public final Rvalue condition;
        public final List   sbsgs; // SwitchBlockStatementGroup

        public SwitchStatement(
            Location location,
            Rvalue   condition,
            List     sbsgs
        ) {
            super(location);
            (this.condition = condition).setEnclosingBlockStatement(this);
            this.sbsgs     = sbsgs;
            for (Iterator it = sbsgs.iterator(); it.hasNext();) {
                SwitchBlockStatementGroup sbsg = ((SwitchBlockStatementGroup) it.next());
                for (Iterator it2 = sbsg.caseLabels.iterator(); it2.hasNext();) {
                    ((Rvalue) (it2.next())).setEnclosingBlockStatement(this);
                }
                for (Iterator it2 = sbsg.blockStatements.iterator(); it2.hasNext();) {
                    ((BlockStatement) (it2.next())).setEnclosingScope(this);
                }
            }
        }
        public String toString() {
            return "switch (" + this.condition + ") { (" + this.sbsgs.size() + " statement groups) }";
        }

        public static class SwitchBlockStatementGroup extends Java.Located {
            public final List    caseLabels; // Rvalue
            public final boolean hasDefaultLabel;
            public final List    blockStatements; // BlockStatement

            public SwitchBlockStatementGroup(
                Location location,
                List     caseLabels,      // Rvalue
                boolean  hasDefaultLabel,
                List     blockStatements  // BlockStatement
            ) {
                super(location);
                this.caseLabels      = caseLabels;
                this.hasDefaultLabel = hasDefaultLabel;
                this.blockStatements = blockStatements;
            }
            public String toString() {
                return (
                    this.caseLabels.size()
                    + (this.hasDefaultLabel ? " case label(s) plus DEFAULT" : " case label(s)")
                );
            }
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitSwitchStatement(this); }
    }
    static class Padder extends CodeContext.Inserter implements CodeContext.FixUp {
        public Padder(CodeContext codeContext) {
            codeContext.super();
        }
        public void fixUp() {
            int x = this.offset % 4;
            if (x != 0) {
                CodeContext ca = this.getCodeContext();
                ca.pushInserter(this);
                {
                    ca.makeSpace((short) -1, 4 - x);
                }
                ca.popInserter();
            }
        }
    }

    public static final class SynchronizedStatement extends Statement {
        public final Rvalue         expression;
        public final BlockStatement body;

        public SynchronizedStatement(
            Location       location,
            Rvalue         expression,
            BlockStatement body
        ) {
            super(location);
            (this.expression = expression).setEnclosingBlockStatement(this);
            (this.body       = body).setEnclosingScope(this);
        }
        public String toString() {
            return "synchronized(" + this.expression + ") " + this.body;
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitSynchronizedStatement(this); }

        short monitorLvIndex = -1;
    }

    public static final class DoStatement extends ContinuableStatement {
        public final BlockStatement body;
        public final Rvalue         condition;

        public DoStatement(
            Location       location,
            BlockStatement body,
            Rvalue         condition
        ) {
            super(location);
            (this.body      = body).setEnclosingScope(this);
            (this.condition = condition).setEnclosingBlockStatement(this);
        }
        public String toString() {
            return "do " + this.body + " while(" + this.condition + ");";
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitDoStatement(this); }
    }

    public static final class LocalVariableDeclarationStatement extends Statement {
        public final short                modifiers;
        public final Type                 type;
        public final VariableDeclarator[] variableDeclarators;

        /**
         * @param modifiers Only "final" allowed.
         */
        public LocalVariableDeclarationStatement(
            Location             location,
            short                modifiers,
            Type                 type,
            VariableDeclarator[] variableDeclarators
        ) {
            super(location);
            this.modifiers           = modifiers;
            (this.type                = type).setEnclosingScope(this);
            this.variableDeclarators = variableDeclarators;
            for (int i = 0; i < variableDeclarators.length; ++i) {
                VariableDeclarator vd = variableDeclarators[i];
                if (vd.optionalInitializer != null) Java.setEnclosingBlockStatement(vd.optionalInitializer, this);
            }
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) {
            visitor.visitLocalVariableDeclarationStatement(this);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (this.modifiers != 0) sb.append(Mod.shortToString(this.modifiers)).append(' ');
            sb.append(this.type).append(' ').append(this.variableDeclarators[0].toString());
            for (int i = 1; i < this.variableDeclarators.length; ++i) {
                sb.append(", ").append(this.variableDeclarators[i].toString());
            }
            return sb.append(';').toString();
        }
    }

    public static final class ReturnStatement extends Statement {
        public final Rvalue optionalReturnValue;

        public ReturnStatement(
            Location location,
            Rvalue   optionalReturnValue
        ) {
            super(location);
            this.optionalReturnValue = optionalReturnValue;
            if (optionalReturnValue != null) optionalReturnValue.setEnclosingBlockStatement(this);
        }

        public String toString() {
            return this.optionalReturnValue == null ? "return;" : "return " + this.optionalReturnValue + ';';
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitReturnStatement(this); }
    }

    public static final class ThrowStatement extends Statement {
        public final Rvalue expression;

        public ThrowStatement(
            Location location,
            Rvalue   expression
        ) {
            super(location);
            (this.expression = expression).setEnclosingBlockStatement(this);
        }
        public String toString() {
            return "throw " + this.expression + ';';
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitThrowStatement(this); }
    }

    /**
     * Representation of the Java&trade; "break" statement (JLS 14.14).
     */
    public static final class BreakStatement extends Statement {
        public final String optionalLabel;

        public BreakStatement(
            Location location,
            String   optionalLabel
        ) {
            super(location);
            this.optionalLabel = optionalLabel;
        }
        public String toString() {
            return this.optionalLabel == null ? "break;" : "break " + this.optionalLabel + ';';
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitBreakStatement(this); }
    }

    /**
     * Representation of the Java&trade; "continue" statement (JLS
     * 14.15).
     */
    public static final class ContinueStatement extends Statement {
        public final String optionalLabel;

        public ContinueStatement(
            Location location,
            String   optionalLabel
        ) {
            super(location);
            this.optionalLabel = optionalLabel;
        }
        public String toString() {
            return this.optionalLabel == null ? "continue;" : "continue " + this.optionalLabel + ';';
        }

        // Compile time members:

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitContinueStatement(this); }
    }

    /**
     * Represents the "empty statement", i.e. the blank semicolon.
     */
    public static final class EmptyStatement extends Statement {
        public EmptyStatement(Location location) {
            super(location);
        }
        public String toString() {
            return ";";
        }

        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitEmptyStatement(this); }
    }

    /**
     * Abstract base class for {@link Java.Type}, {@link Java.Rvalue} and
     * {@link Java.Lvalue}.
     */
    public abstract static class Atom extends Located {
        public Atom(Location location) {
            super(location);
        }

        public Type   toType()   { return null; }
        public Rvalue toRvalue() { return null; }
        public Lvalue toLvalue() { return null; }

        public abstract String toString();

        // Parse time members:

        public final Type toTypeOrPE() throws CompileException {
            Type result = this.toType();
            if (result == null) this.throwCompileException("Expression \"" + this.toString() + "\" is not a type");
            return result;
        }
        public final Rvalue toRvalueOrPE() throws CompileException {
            Rvalue result = this.toRvalue();
            if (result == null) this.throwCompileException("Expression \"" + this.toString() + "\" is not an rvalue");
            return result;
        }
        public final Lvalue toLvalueOrPE() throws CompileException {
            Lvalue result = this.toLvalue();
            if (result == null) this.throwCompileException("Expression \"" + this.toString() + "\" is not an lvalue");
            return result;
        }

        public abstract void accept(Visitor.AtomVisitor visitor);
    }

    /**
     * Representation of a Java&trade; type.
     */
    public abstract static class Type extends Atom {
        private Scope enclosingScope = null;

        protected Type(Location location) {
            super(location);
        }

        /**
         * Sets the enclosing scope for this object and all subordinate
         * {@link org.codehaus.janino.Java.Type} objects.
         */
        public void setEnclosingScope(final Scope enclosingScope) {
            if (this.enclosingScope != null && enclosingScope != this.enclosingScope) {
                throw new JaninoRuntimeException(
                    "Enclosing scope already set for type \""
                    + this.toString()
                    + "\" at "
                    + this.getLocation()
                );
            }
            this.enclosingScope = enclosingScope;
        }
        public Scope getEnclosingScope() {
            return this.enclosingScope;
        }
        public Type toType() { return this; }

        public abstract void accept(Visitor.TypeVisitor visitor);
    }

    public static final class SimpleType extends Type {
        public final IClass iClass;

        public SimpleType(Location location, IClass iClass) {
            super(location);
            this.iClass = iClass;
        }
        public String toString() { return this.iClass.toString(); }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitSimpleType(this); }
        public void accept(Visitor.TypeVisitor visitor) { visitor.visitSimpleType(this); }
    };

    /**
     * Representation of a Java&trade; "basic type" (obviously
     * equaivalent to a "primitive type") (JLS 4.2).
     */
    public static final class BasicType extends Type {
        public final int index;

        public BasicType(Location location, int index) {
            super(location);
            this.index = index;
        }

        public String toString() {
            switch (this.index) {
            case BasicType.VOID:
                return "void";
            case BasicType.BYTE:
                return "byte";
            case BasicType.SHORT:
                return "short";
            case BasicType.CHAR:
                return "char";
            case BasicType.INT:
                return "int";
            case BasicType.LONG:
                return "long";
            case BasicType.FLOAT:
                return "float";
            case BasicType.DOUBLE:
                return "double";
            case BasicType.BOOLEAN:
                return "boolean";
            default:
                throw new JaninoRuntimeException("Invalid index " + this.index);
            }
        }

        public void accept(Visitor.TypeVisitor visitor) { visitor.visitBasicType(this); }
        public void accept(Visitor.AtomVisitor visitor) { visitor.visitBasicType(this); }

        public static final int VOID    = 0;
        public static final int BYTE    = 1;
        public static final int SHORT   = 2;
        public static final int CHAR    = 3;
        public static final int INT     = 4;
        public static final int LONG    = 5;
        public static final int FLOAT   = 6;
        public static final int DOUBLE  = 7;
        public static final int BOOLEAN = 8;
    }

    public static final class ReferenceType extends Type {
        public final String[] identifiers;

        public ReferenceType(
            Location location,
            String[] identifiers
        ) {
            super(location);
            this.identifiers = identifiers;
        }

        public String toString() { return Java.join(this.identifiers, "."); }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitReferenceType(this); }
        public void accept(Visitor.TypeVisitor visitor) { visitor.visitReferenceType(this); }
    }

    // Helper class for JLS 15.9.1
    public static final class RvalueMemberType extends Type {
        public final Rvalue rvalue;
        public final String identifier;

        /**
         * Notice: The <code>rvalue</code> is not a subordinate object!
         */
        public RvalueMemberType(
            Location location,
            Rvalue   rvalue,
            String   identifier
        ) {
            super(location);
            this.rvalue     = rvalue;
            this.identifier = identifier;
        }

        public String toString() { return this.identifier; }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitRvalueMemberType(this); }
        public void accept(Visitor.TypeVisitor visitor) { visitor.visitRvalueMemberType(this); }
    }


    /**
     * Representation of a Java&trade; array type (JLS 10.1).
     */
    public static final class ArrayType extends Type {
        public final Type componentType;

        public ArrayType(Type componentType) {
            super(componentType.getLocation());
            this.componentType = componentType;
        }

        public void setEnclosingScope(final Scope enclosingScope) {
            super.setEnclosingScope(enclosingScope);
            this.componentType.setEnclosingScope(enclosingScope);
        }
        public String toString() {
            return this.componentType.toString() + "[]";
        }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitArrayType(this); }
        public void accept(Visitor.TypeVisitor visitor) { visitor.visitArrayType(this); }
    }

    /**
     * Representation of an "rvalue", i.e. an expression that has a type and
     * a value, but cannot be assigned to: An expression that can be the
     * right-hand-side of an assignment.
     */
    public abstract static class Rvalue extends Atom implements ArrayInitializerOrRvalue {
        private Java.BlockStatement enclosingBlockStatement = null;

        protected Rvalue(Location location) {
            super(location);
        }

        /**
         * Sets enclosing block statement for this object and all subordinate
         * {@link org.codehaus.janino.Java.Rvalue} objects.
         */
        public final void setEnclosingBlockStatement(final Java.BlockStatement enclosingBlockStatement) {
            this.accept((Visitor.RvalueVisitor) new Traverser() {
                public void traverseRvalue(Java.Rvalue rv) {
                    if (rv.enclosingBlockStatement != null && enclosingBlockStatement != rv.enclosingBlockStatement) {
                        throw new JaninoRuntimeException(
                            "Enclosing block statement for rvalue \""
                            + rv
                            + "\" at "
                            + rv.getLocation()
                            + " is already set"
                        );
                    }
                    rv.enclosingBlockStatement = enclosingBlockStatement;
                    super.traverseRvalue(rv);
                }
                public void traverseAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) {
                    acd.setEnclosingScope(enclosingBlockStatement);
                    ;
                }
                public void traverseType(Java.Type t) {
                    if (t.enclosingScope != null && enclosingBlockStatement != t.enclosingScope) {
                        throw new JaninoRuntimeException(
                            "Enclosing scope already set for type \""
                            + this.toString()
                            + "\" at "
                            + t.getLocation()
                        );
                    }
                    t.enclosingScope = enclosingBlockStatement;
//                    t.setEnclosingScope(enclosingBlockStatement);
                    super.traverseType(t);
                }
            }.comprehensiveVisitor());
        }
        public Java.BlockStatement getEnclosingBlockStatement() {
            return this.enclosingBlockStatement;
        }
        public Rvalue toRvalue() { return this; }

        static final Object CONSTANT_VALUE_UNKNOWN = new Object() {
            public String toString() { return "CONSTANT_VALUE_UNKNOWN"; }
        };
        Object constantValue = Java.Rvalue.CONSTANT_VALUE_UNKNOWN;

        public abstract void accept(Visitor.RvalueVisitor rvv);

        public static final boolean JUMP_IF_TRUE  = true;
        public static final boolean JUMP_IF_FALSE = false;
    }

    /**
     * Base class for {@link Java.Rvalue}s that compile better as conditional
     * branches.
     */
    public abstract static class BooleanRvalue extends Rvalue {
        protected BooleanRvalue(Location location) {
            super(location);
        }
    }

    /**
     * Representation of an "lvalue", i.e. an expression that has a type and
     * a value, and can be assigned to: An expression that can be the
     * left-hand-side of an assignment.
     */
    public abstract static class Lvalue extends Rvalue {
        protected Lvalue(Location location) {
            super(location);
        }

        public Lvalue toLvalue() { return this; }

        public abstract void accept(Visitor.LvalueVisitor lvv);
    }

    /**
     * This class is special: It does not extend/implement the Atom subclasses,
     * but overrides Atom's "to...()" methods.
     */
    public static final class AmbiguousName extends Lvalue {
        public final String[] identifiers;
        public final int      n;

        public AmbiguousName(
            Location location,
            String[] identifiers
        ) {
            this(location, identifiers, identifiers.length);
        }
        public AmbiguousName(
            Location location,
            String[] identifiers,
            int      n
        ) {
            super(location);
            this.identifiers = identifiers;
            this.n           = n;
        }

        // Override "Atom.toType()".
        private Type type = null;
        public Type toType() {
            if (this.type == null) {
                String[] is = new String[this.n];
                System.arraycopy(this.identifiers, 0, is, 0, this.n);
                this.type = new ReferenceType(this.getLocation(), is);
                this.type.setEnclosingScope(this.getEnclosingBlockStatement());
            }
            return this.type;
        }

        // Compile time members.

        public String toString() {
            return Java.join(this.identifiers, ".", 0, this.n);
        }

        public Lvalue toLvalue() {
            if (this.reclassified != null) { return this.reclassified.toLvalue(); }
            return this;
        }

        public Rvalue toRvalue() {
            if (this.reclassified != null) { return this.reclassified.toRvalue(); }
            return this;
        }

        Atom reclassified = null;

        public void accept(Visitor.AtomVisitor visitor)   { visitor.visitAmbiguousName(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitAmbiguousName(this); }
        public void accept(Visitor.LvalueVisitor visitor) { visitor.visitAmbiguousName(this); }
    }

    // Helper class for 6.5.2.1.7, 6.5.2.2.1
    public static final class Package extends Atom {
        public final String name;

        public Package(Location location, String name) {
            super(location);
            this.name = name;
        }
        public String toString() { return this.name; }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitPackage(this); }
    }

    /**
     * Representation of a local variable access -- used during compilation.
     */
    public static final class LocalVariableAccess extends Lvalue {
        public final LocalVariable localVariable;

        public LocalVariableAccess(
            Location      location,
            LocalVariable localVariable
        ) {
            super(location);
            this.localVariable = localVariable;
        }

        // Compile time members.

        public String toString() { return this.localVariable.toString(); }

        public void accept(Visitor.LvalueVisitor visitor) { visitor.visitLocalVariableAccess(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitLocalVariableAccess(this); }
        public void accept(Visitor.AtomVisitor visitor) { visitor.visitLocalVariableAccess(this); }
    }

    /**
     * Representation of an access to a field of a class or an interface. (Does not implement the
     * "array length" expression, e.g. "ia.length".)
     */
    public static final class FieldAccess extends Lvalue {
        public final Atom           lhs;
        public final IClass.IField  field;

        public FieldAccess(
            Location       location,
            Atom           lhs,
            IClass.IField  field
        ) {
            super(location);
            this.lhs   = lhs;
            this.field = field;
        }

        // Compile time members.

        // Implement "Atom".
        public String toString() { return this.lhs.toString() + '.' + this.field.getName(); }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitFieldAccess(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitFieldAccess(this); }
        public void accept(Visitor.LvalueVisitor visitor) { visitor.visitFieldAccess(this); }
    }

    public static final class ArrayLength extends Rvalue {
        public final Rvalue lhs;

        public ArrayLength(
            Location location,
            Rvalue   lhs
        ) {
            super(location);
            this.lhs = lhs;
        }

        // Compile time members.

        // Implement "Atom".
        public String toString() { return this.lhs.toString() + ".length"; }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitArrayLength(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitArrayLength(this); }
    }

    /**
     * Representation of an access to the innermost enclosing instance.
     */
    public static final class ThisReference extends Rvalue {

        /**
         * Access the declaring class.
         */
        public ThisReference(Location location) {
            super(location);
        }

        // Compile time members.

        IClass iClass = null;

        // Implement "Atom".
        public String toString() { return "this"; }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitThisReference(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitThisReference(this); }
    }

    /**
     * Representation of an access to the current object or an enclosing instance.
     */
    public static final class QualifiedThisReference extends Rvalue {
        public final Type qualification;

        /**
         * Access the given enclosing instance of the declaring class.
         */
        public QualifiedThisReference(
            Location location,
            Type     qualification
        ) {
            super(location);

            if (qualification == null) throw new NullPointerException();
            this.qualification = qualification;
        }

        // Compile time members.

        ClassDeclaration    declaringClass               = null;
        TypeBodyDeclaration declaringTypeBodyDeclaration = null;
        IClass              targetIClass                 = null;

        // Used at compile time.
//        public QualifiedThisReference(
//            Location            location,
//            ClassDeclaration    declaringClass,
//            TypeBodyDeclaration declaringTypeBodyDeclaration,
//            IClass              targetIClass
//        ) {
//            super(location);
//            if (declaringClass               == null) throw new NullPointerException();
//            if (declaringTypeBodyDeclaration == null) throw new NullPointerException();
//            if (targetIClass                 == null) throw new NullPointerException();
//
//            this.qualification                = null;
//            this.declaringClass               = declaringClass;
//            this.declaringTypeBodyDeclaration = declaringTypeBodyDeclaration;
//            this.targetIClass                 = targetIClass;
//        }

        // Implement "Atom".
        public String toString() {
            return this.qualification.toString() + ".this";
        }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitQualifiedThisReference(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitQualifiedThisReference(this); }
    }

    public static final class ClassLiteral extends Rvalue {
        public final Type type;

        public ClassLiteral(
            Location location,
            Type     type
        ) {
            super(location);
            this.type = type;
        }

        // Compile time members.

        //Implement "Atom".
        public String toString() { return this.type.toString() + ".class"; }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitClassLiteral(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitClassLiteral(this); }
    }

    public static final class Assignment extends Rvalue {
        public final Lvalue lhs;
        public final String operator;
        public final Rvalue rhs;

        public Assignment(
            Location location,
            Lvalue   lhs,
            String   operator,
            Rvalue   rhs
        ) {
            super(location);
            this.lhs      = lhs;
            this.operator = operator;
            this.rhs      = rhs;
        }

        // Compile time members.

        // Implement "Atom".
        public String toString() { return this.lhs.toString() + ' ' + this.operator + ' ' + this.rhs.toString(); }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitAssignment(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitAssignment(this); }
    }

    public static final class ConditionalExpression extends Rvalue {
        public final Rvalue lhs, mhs, rhs;

        public ConditionalExpression(
            Location location,
            Rvalue   lhs,
            Rvalue   mhs,
            Rvalue   rhs
        ) {
            super(location);
            this.lhs = lhs;
            this.mhs = mhs;
            this.rhs = rhs;
        }

        // Implement "Atom".
        public String toString() {
            return this.lhs.toString() + " ? " + this.mhs.toString() + " : " + this.rhs.toString();
        }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitConditionalExpression(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitConditionalExpression(this); }
    }

    /**
     * Objects of this class represent represent one pre- or post-increment
     * or decrement.
     */
    public static final class Crement extends Rvalue {
        public final boolean pre;
        public final String  operator; // "++" or "--"
        public final Lvalue  operand;

        public Crement(Location location, String operator, Lvalue operand) {
            super(location);
            this.pre      = true;
            this.operator = operator;
            this.operand  = operand;
        }
        public Crement(Location location, Lvalue operand, String operator) {
            super(location);
            this.pre      = false;
            this.operator = operator;
            this.operand  = operand;
        }

        // Compile time members.

        // Implement "Atom".
        public String toString() {
            return this.pre ? this.operator + this.operand : this.operand + this.operator;
        }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitCrement(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitCrement(this); }
    }

    /**
     * This class implements an array access.
     */
    public static final class ArrayAccessExpression extends Lvalue {
        public final Rvalue lhs;
        public final Rvalue index;

        public ArrayAccessExpression(
            Location location,
            Rvalue   lhs,
            Rvalue   index
        ) {
            super(location);
            this.lhs = lhs;
            this.index = index;
        }

        // Compile time members:

        // Implement "Atom".
        public String toString() { return this.lhs.toString() + '[' + this.index + ']'; }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitArrayAccessExpression(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitArrayAccessExpression(this); }
        public void accept(Visitor.LvalueVisitor visitor) { visitor.visitArrayAccessExpression(this); }
    }

    /**
     * This class implements class or interface field access, and also the "array length"
     * expression "xy.length".
     */
    public static final class FieldAccessExpression extends Lvalue {
        public final Atom   lhs;
        public final String fieldName;

        public FieldAccessExpression(
            Location location,
            Atom     lhs,
            String   fieldName
        ) {
            super(location);
            this.lhs       = lhs;
            this.fieldName = fieldName;
        }

        // Compile time members:

        // Implement "Atom".
        public String toString() { return this.lhs.toString() + '.' + this.fieldName; }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitFieldAccessExpression(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitFieldAccessExpression(this); }
        public void accept(Visitor.LvalueVisitor visitor) { visitor.visitFieldAccessExpression(this); }

        Rvalue value = null;
    }

    /**
     * Representation of "super.fld" and "Type.super.fld".
     */
    public static final class SuperclassFieldAccessExpression extends Lvalue {
        public final Type   optionalQualification;
        public final String fieldName;

        public SuperclassFieldAccessExpression(
            Location location,
            Type     optionalQualification,
            String   fieldName
        ) {
            super(location);
            this.optionalQualification = optionalQualification;
            this.fieldName             = fieldName;
        }

        // Compile time members.

        // Implement "Atom".
        public String toString() {
            return (
                this.optionalQualification == null
                ? "super."
                : this.optionalQualification.toString() + ".super."
            ) + this.fieldName;
        }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitSuperclassFieldAccessExpression(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitSuperclassFieldAccessExpression(this); }
        public void accept(Visitor.LvalueVisitor visitor) { visitor.visitSuperclassFieldAccessExpression(this); }

        Rvalue value = null;
    }

    /**
     * This class implements the unary operators "+", "-", "~" and "!".
     */
    public static final class UnaryOperation extends BooleanRvalue {
        public final String operator;
        public final Rvalue operand;

        public UnaryOperation(
            Location location,
            String   operator,
            Rvalue   operand
        ) {
            super(location);
            this.operator = operator;
            this.operand  = operand;
        }

        // Implement "Atom".
        public String toString() { return this.operator + this.operand.toString(); }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitUnaryOperation(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitUnaryOperation(this); }
    }

    public static final class Instanceof extends Rvalue {
        public final Rvalue lhs;
        public final Type   rhs;

        public Instanceof(
            Location location,
            Rvalue   lhs,
            Type     rhs // ReferenceType or ArrayType
        ) {
            super(location);
            this.lhs = lhs;
            this.rhs = rhs;
        }

        // Compile time members.

        // Implement "Atom".
        public String toString() { return this.lhs.toString() + " instanceof " + this.rhs.toString(); }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitInstanceof(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitInstanceof(this); }
    }

    /**
     * Representation of all non-operand-modifying Java&trade; binary
     * operations.
     * <p>
     * Operations with boolean result:<br>
     * <tt>|| && == != < > <= >=</tt>
     * <p>
     * Operations with non-boolean result:<br>
     * <tt>| ^ & * / % + - << >> >>></tt>
     */
    public static final class BinaryOperation extends BooleanRvalue {
        public final Rvalue lhs;
        public final String op;
        public final Rvalue rhs;

        public BinaryOperation(
            Location location,
            Rvalue   lhs,
            String   op,
            Rvalue   rhs
        ) {
            super(location);
            this.lhs = lhs;
            this.op = op;
            this.rhs = rhs;
        }

        // Compile time members.

        // Implement "Atom".
        public String toString() {
            return this.lhs.toString() + ' ' + this.op + ' ' + this.rhs.toString();
        }

        /**
         * Returns an {@link Iterator} over a left-to-right sequence of {@link Java.Rvalue}s.
         */
        public Iterator unrollLeftAssociation() {
            List operands = new ArrayList();
            BinaryOperation x = this;
            for (;;) {
                operands.add(x.rhs);
                Rvalue lhs = x.lhs;
                if (lhs instanceof BinaryOperation && ((BinaryOperation) lhs).op == this.op) {
                    x = (BinaryOperation) lhs;
                } else {
                    operands.add(lhs);
                    break;
                }
            }
            return new ReverseListIterator(operands.listIterator(operands.size()));
        }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitBinaryOperation(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitBinaryOperation(this); }
    }

    public static final class Cast extends Rvalue {
        public final Type   targetType;
        public final Rvalue value;

        public Cast(
            Location location,
            Type     targetType,
            Rvalue   value
        ) {
            super(location);
            this.targetType = targetType;
            this.value      = value;
        }

        // Compile time members.

        // Implement "Atom".
        public String toString() { return '(' + this.targetType.toString() + ") " + this.value.toString(); }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitCast(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitCast(this); }
    }

    public static final class ParenthesizedExpression extends Lvalue {
        public final Rvalue value;

        public ParenthesizedExpression(Location location, Rvalue value) {
            super(location);
            this.value = value;
        }

        public String toString() {
            return '(' + this.value.toString() + ')';
        }
        public void accept(Visitor.AtomVisitor visitor) { visitor.visitParenthesizedExpression(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitParenthesizedExpression(this); }
        public void accept(Visitor.LvalueVisitor visitor) { visitor.visitParenthesizedExpression(this); }
    }

    public abstract static class ConstructorInvocation extends Atom implements BlockStatement {
        public final Rvalue[] arguments;
        private Scope         enclosingScope = null;

        protected ConstructorInvocation(
            Location location,
            Rvalue[] arguments
        ) {
            super(location);
            this.arguments = arguments;
            for (int i = 0; i < arguments.length; ++i) arguments[i].setEnclosingBlockStatement(this);
        }

        // Implement BlockStatement
        public void setEnclosingScope(Scope enclosingScope) {
            if (this.enclosingScope != null && enclosingScope != null) {
                throw new JaninoRuntimeException(
                    "Enclosing scope is already set for statement \""
                    + this.toString()
                    + "\" at "
                    + this.getLocation()
                );
            }
            this.enclosingScope = enclosingScope;
        }
        public Scope getEnclosingScope() { return this.enclosingScope; }

        // Compile time members
        public Map localVariables = null; // String name => Java.LocalVariable
        public Java.LocalVariable findLocalVariable(String name) {
            if (this.localVariables == null) { return null; }
            return (LocalVariable) this.localVariables.get(name);
        }
    }

    public static final class AlternateConstructorInvocation extends ConstructorInvocation {
        public AlternateConstructorInvocation(
            Location location,
            Rvalue[] arguments
        ) {
            super(location, arguments);
        }

        // Implement Atom.
        public String toString() { return "this()"; }
        public void accept(Visitor.AtomVisitor visitor) {
            ((Visitor.BlockStatementVisitor) visitor).visitAlternateConstructorInvocation(this);
        }

        // Implement BlockStatement.
        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitAlternateConstructorInvocation(this); }
    }

    public static final class SuperConstructorInvocation extends ConstructorInvocation {
        public final Rvalue optionalQualification;

        public SuperConstructorInvocation(
            Location              location,
            Rvalue                optionalQualification,
            Rvalue[]              arguments
        ) {
            super(location, arguments);
            this.optionalQualification = optionalQualification;
            if (optionalQualification != null) optionalQualification.setEnclosingBlockStatement(this);
        }

        // Implement Atom.
        public String toString() { return "super()"; }
        public void accept(Visitor.AtomVisitor visitor) {
            ((Visitor.BlockStatementVisitor) visitor).visitSuperConstructorInvocation(this);
        }

        // Implement BlockStatement.
        public void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitSuperConstructorInvocation(this); }
    }

    public static final class MethodInvocation extends Invocation {

        /** null == method invocation by simple method name */
        public final Atom optionalTarget;

        public MethodInvocation(
            Location location,
            Atom     optionalTarget,
            String   methodName,
            Rvalue[] arguments
        ) {
            super(location, methodName, arguments);
            this.optionalTarget = optionalTarget;
        }

        // Implement "Atom".
        IClass.IMethod iMethod;
        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (this.optionalTarget != null) sb.append(this.optionalTarget.toString()).append('.');
            sb.append(this.methodName).append('(');
            for (int i = 0; i < this.arguments.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(this.arguments[i].toString());
            }
            sb.append(')');
            return sb.toString();
        }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitMethodInvocation(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitMethodInvocation(this); }
    }

    public static final class SuperclassMethodInvocation extends Invocation {
        public SuperclassMethodInvocation(
            Location       location,
            String         methodName,
            Rvalue[]       arguments
        ) {
            super(location, methodName, arguments);
        }

        // Implement "Atom".
        public String toString() { return "super." + this.methodName + "()"; }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitSuperclassMethodInvocation(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitSuperclassMethodInvocation(this); }
    }

    public abstract static class Invocation extends Rvalue {
        public final Rvalue[] arguments;
        public final String methodName;

        protected Invocation(Location location, String methodName, Rvalue[] arguments) {
            super(location);
            this.methodName = methodName;
            this.arguments = arguments;
        }
    }

    public static final class NewClassInstance extends Rvalue {
        public final Rvalue   optionalQualification;
        public final Type     type;
        public final Rvalue[] arguments;

        public NewClassInstance(
            Location location,
            Rvalue   optionalQualification,
            Type     type,
            Rvalue[] arguments
        ) {
            super(location);
            this.optionalQualification = optionalQualification;
            this.type                  = type;
            this.arguments             = arguments;
        }

        // Compile time members.

        protected IClass iClass = null;

        public NewClassInstance(
            Location location,
            Rvalue   optionalQualification,
            IClass   iClass,
            Rvalue[] arguments
        ) {
            super(location);
            this.optionalQualification = optionalQualification;
            this.type                  = null;
            this.arguments             = arguments;
            this.iClass                = iClass;
        }

        // Implement "Atom".
        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (this.optionalQualification != null) sb.append(this.optionalQualification.toString()).append('.');
            sb.append("new ");
            if (this.type != null) {
                sb.append(this.type.toString());
            } else
            if (this.iClass != null) {
                sb.append(this.iClass.toString());
            } else {
                sb.append("???");
            }
            sb.append('(');
            for (int i = 0; i < this.arguments.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(this.arguments[i].toString());
            }
            sb.append(')');
            return sb.toString();
        }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitNewClassInstance(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitNewClassInstance(this); }
    }

    public static final class NewAnonymousClassInstance extends Rvalue {
        public final Rvalue                    optionalQualification;
        public final AnonymousClassDeclaration anonymousClassDeclaration;
        public final Rvalue[]                  arguments;

        public NewAnonymousClassInstance(
            Location                  location,
            Rvalue                    optionalQualification,
            AnonymousClassDeclaration anonymousClassDeclaration,
            Rvalue[]                  arguments
        ) {
            super(location);
            this.optionalQualification     = optionalQualification;
            this.anonymousClassDeclaration = anonymousClassDeclaration;
            this.arguments                 = arguments;
        }

        // Implement "Atom".
        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (this.optionalQualification != null) sb.append(this.optionalQualification.toString()).append('.');
            sb.append("new ").append(this.anonymousClassDeclaration.baseType.toString()).append("() { ... }");
            return sb.toString();
        }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitNewAnonymousClassInstance(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitNewAnonymousClassInstance(this); }
    }

    // Used during compile-time.
    public static final class ParameterAccess extends Rvalue {
        public final FunctionDeclarator.FormalParameter formalParameter;

        public ParameterAccess(Location location, FunctionDeclarator.FormalParameter formalParameter) {
            super(location);
            this.formalParameter   = formalParameter;
        }

        // Implement Atom
        public String toString() { return this.formalParameter.name; }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitParameterAccess(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitParameterAccess(this); }
    }

    public static final class NewArray extends Rvalue {
        public final Type     type;
        public final Rvalue[] dimExprs;
        public final int      dims;

        /**
         * Create a new array with dimension dimExprs.length + dims
         * <p>
         * e.g. byte[12][][] is created with
         *     new NewArray(
         *         null,
         *         Java.BasicType(NULL, Java.BasicType.BYTE),
         *         new Rvalue[] {
         *             new Java.Literal(null, Integer.valueOf(12)
         *         },
         *         2
         *     )
         * @param location  the location of this element
         * @param type      the base type of the array
         * @param dimExprs  sizes for dimensions being allocated with specific sizes
         * @param dims      the number of dimensions that are not yet allocated
         */
        public NewArray(
            Location location,
            Type     type,
            Rvalue[] dimExprs,
            int      dims
        ) {
            super(location);
            this.type     = type;
            this.dimExprs = dimExprs;
            this.dims     = dims;
        }

        // Implement "Atom".
        public String toString() { return "new " + this.type.toString() + "[]..."; }
        public void accept(Visitor.AtomVisitor visitor) { visitor.visitNewArray(this); }

        // Implement "Rvalue".
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitNewArray(this); }
    }

    public static final class NewInitializedArray extends Rvalue {
        public final ArrayType        arrayType;
        public final ArrayInitializer arrayInitializer;

        public NewInitializedArray(
            Location         location,
            ArrayType        arrayType,
            ArrayInitializer arrayInitializer
        ) {
            super(location);
            this.arrayType        = arrayType;
            this.arrayInitializer = arrayInitializer;
        }

        // Implement "Atom".
        public String toString() { return "new " + this.arrayType.toString() + " { ... }"; }
        public void accept(Visitor.AtomVisitor visitor) { visitor.visitNewInitializedArray(this); }

        // Implement "Rvalue".
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitNewInitializedArray(this); }
    }

    /**
     * Represents a Java&trade; array initializer (JLS 10.6).
     * <p>
     * Allocates an array and initializes its members with (not necessarily
     * constant) values.
     */
    public static final class ArrayInitializer extends Located implements ArrayInitializerOrRvalue {
        public final ArrayInitializerOrRvalue[] values;

        public ArrayInitializer(
            Location                   location,
            ArrayInitializerOrRvalue[] values
        ) {
            super(location);
            this.values = values;
        }
        public String toString() {
            return " { (" + this.values.length + " values) }";
        }
    }

    public interface ArrayInitializerOrRvalue {
    }

    public static abstract class Literal extends Rvalue {
        public final String value;

        /**
         * @param value The text of the literal token, as in the source code.
         */
        public Literal(Location location, String value) {
            super(location);
            this.value = value;
        }

        // Implement "Atom".
        public String toString() {
            return this.value;
        }
    }

    public static final class IntegerLiteral extends Literal {
        public IntegerLiteral(Location location, String value) { super(location, value); }
        public void accept(Visitor.AtomVisitor visitor) { visitor.visitIntegerLiteral(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitIntegerLiteral(this); }
    }

    public static final class FloatingPointLiteral extends Literal {
        public FloatingPointLiteral(Location location, String value) { super(location, value); }
        public void accept(Visitor.AtomVisitor visitor) { visitor.visitFloatingPointLiteral(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitFloatingPointLiteral(this); }
    }

    public static final class BooleanLiteral extends Literal {
        public BooleanLiteral(Location location, String value) { super(location, value); }
        public void accept(Visitor.AtomVisitor visitor) { visitor.visitBooleanLiteral(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitBooleanLiteral(this); }
    }

    public static final class CharacterLiteral extends Literal {
        public CharacterLiteral(Location location, String value) { super(location, value); }
        public void accept(Visitor.AtomVisitor visitor) { visitor.visitCharacterLiteral(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitCharacterLiteral(this); }
    }

    public static final class StringLiteral extends Literal {
        public StringLiteral(Location location, String value) { super(location, value); }
        public void accept(Visitor.AtomVisitor visitor) { visitor.visitStringLiteral(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitStringLiteral(this); }
    }

    public static final class NullLiteral extends Literal {
        public NullLiteral(Location location, String value) { super(location, value); }
        public void accept(Visitor.AtomVisitor visitor) { visitor.visitNullLiteral(this); }
        public void accept(Visitor.RvalueVisitor visitor) { visitor.visitNullLiteral(this); }
    }

    /**
     * All local variables have a slot number, local variables that get written into the localvariabletable
     * also have a start and end offset that defines the variable's extent in the bytecode. If the name is null,
     * or variable debugging is not on, then the variable won't be written into the localvariabletable and the
     * offsets can be ignored.
     */
    public static class LocalVariableSlot {
        private short        slotIndex = -1;
        private String       name;
        private final IClass type;
        private Offset       start, end;

        public LocalVariableSlot(
            String name,
            short slotNumber,
            IClass type
        ) {
            this.name = name;
            this.slotIndex = slotNumber;
            this.type = type;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer("local var(");

            buf.append(this.name);
            buf.append(", ").append(this.slotIndex);
            if (this.name != null) {
                buf.append(", ").append(this.type);
                buf.append(", ").append(this.start.offset);
                buf.append(", ").append(this.end.offset);
            }
            buf.append(")");

            return buf.toString();
        }

        public short getSlotIndex() { return this.slotIndex; }
        public void setSlotIndex(short slotIndex) { this.slotIndex = slotIndex; }

        public String getName() { return this.name; }
        public void setName(String name) { this.name = name; }

        public Offset getStart() { return this.start; }
        public void setStart(Offset start) { this.start = start; }

        public Offset getEnd() { return this.end; }
        public void setEnd(Offset end) { this.end = end; }

        public IClass getType() { return this.type; }
    }

    /**
     * Used during resolution.
     */
    public static class LocalVariable {
        public final boolean finaL;
        public final IClass  type;
        public LocalVariableSlot slot;

        public LocalVariable(
            boolean finaL,
            IClass  type
        ) {
            this.finaL = finaL;
            this.type  = type;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();

            if (this.finaL) sb.append("final ");
            sb.append(this.type).append(" ");

            return sb.toString();
        }

        public void setSlot(LocalVariableSlot slot) { this.slot = slot; }

        public short getSlotIndex() {
            if (this.slot == null) return -1;
            return this.slot.getSlotIndex();
        }
    }

    public static String join(Object[] a, String separator) {
        return Java.join(a, separator, 0, a.length);
    }

    public static String join(Object[] a, String separator, int off, int len) {
        if (a == null) return ("(null)");
        if (off >= len) return "";
        StringBuffer sb = new StringBuffer(a[off].toString());
        for (++off; off < len; ++off) {
            sb.append(separator);
            sb.append(a[off]);
        }
        return sb.toString();
    }
}
