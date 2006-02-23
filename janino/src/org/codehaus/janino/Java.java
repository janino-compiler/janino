
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

import java.util.*;

import org.codehaus.janino.util.iterator.*;


/**
 * This wrapper class defines classes that represent the elements of the
 * Java<sup>TM</sup> programming language.
 * <p>
 * Notices:
 * <ul>
 *   <li>"JLS1" refers to <a href="http://java.sun.com/docs/books/jls/first_edition/html/index.html">"The Java<sup>TM</sup> Language Specification, First Edition"</a>.
 *   <li>"JLS" or "JLS2" refers to <a href="http://java.sun.com/docs/books/jls/second_edition/html/j.title.doc.html">"The Java<sup>TM</sup> Language Specification, Second Edition"</a>.
 * </ul>
 */

public class Java {
    private Java() {} // Don't instantiate me.

    public interface Scope {

        /**
         * @return Enclusing scope or "null".
         */
        public Scope getEnclosingScope();
    }

    /**
     * This interface is implemented by objects which are associated with a
     * location in the source code.
     */
    public interface Locatable {
        public Location getLocation();

        /**
         * Throw a {@link Parser.ParseException} with the given message and this
         * object's location.
         * 
         * @param message The message to report
         */
        public void throwParseException(String message) throws Parser.ParseException;
    }
    public static abstract class Located implements Locatable {
        private final Location location;

        protected Located(Location location) {
            this.location = location;
        }

        // Implement "Locatable".

        public Location getLocation() { return this.location; }
        public void throwParseException(String message) throws Parser.ParseException {
            throw new Parser.ParseException(message, this.location);
        }
    }

    /**
     * Holds the result of {@link Parser#parseCompilationUnit}.
     */
    public static final class CompilationUnit implements Scope {
        public /*final*/          String optionalFileName;
        public PackageDeclaration optionalPackageDeclaration    = null;
        public final List         importDeclarations            = new ArrayList(); // ImportDeclaration
        public final List         packageMemberTypeDeclarations = new ArrayList(); // PackageMemberTypeDeclaration

        public CompilationUnit(String optionalFileName) {
            this.optionalFileName = optionalFileName;
        }

        // Implement "Scope".
        public Scope getEnclosingScope() { return null; }

        public void setPackageDeclaration(PackageDeclaration packageDeclaration) {
            if (this.optionalPackageDeclaration != null) throw new RuntimeException("Re-setting package declaration");
            this.optionalPackageDeclaration = packageDeclaration;
        }

        public void addImportDeclaration(ImportDeclaration id) throws Parser.ParseException {

            // Check for conflicting single-type import.
            if (id instanceof SingleTypeImportDeclaration) {
                String[] ss = ((SingleTypeImportDeclaration) id).identifiers;
                String name = ss[ss.length - 1];
                for (Iterator it = this.importDeclarations.iterator(); it.hasNext();) {
                    ImportDeclaration id2 = (ImportDeclaration) it.next();
                    if (id2 instanceof SingleTypeImportDeclaration) {
                        String[] ss2 = ((SingleTypeImportDeclaration) id2).identifiers;
                        if (ss2[ss2.length - 1].equals(name)) {
                            if (!Java.join(ss, ".").equals(Java.join(ss2, "."))) id.throwParseException("Class \"" + name + "\" was first imported from \"" + Java.join(ss, ".") + "\", now again from \"" + Java.join(ss2, ".") + "\"");
                        }
                    }
                }
            }
            this.importDeclarations.add(id);
        }

        public void addPackageMemberTypeDeclaration(PackageMemberTypeDeclaration pmtd) {
            this.packageMemberTypeDeclarations.add(pmtd);
        }

        /**
         * Get all classes and interfaces declared in this compilation unit.
         */
        public PackageMemberTypeDeclaration[] getPackageMemberTypeDeclarations() {
            return (PackageMemberTypeDeclaration[]) this.packageMemberTypeDeclarations.toArray(new PackageMemberTypeDeclaration[this.packageMemberTypeDeclarations.size()]);
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
         * Check if the given name was imported through a "single type import", e.g.<pre>
         *     import java.util.Map</pre>
         * 
         * @return the fully qualified name or <code>null</code>
         */
        public String[] getSingleTypeImport(String name) {
            for (Iterator it = this.importDeclarations.iterator(); it.hasNext();) {
                ImportDeclaration id = (ImportDeclaration) it.next();
                if (id instanceof SingleTypeImportDeclaration) {
                    String[] ss = ((SingleTypeImportDeclaration) id).identifiers;
                    if (ss[ss.length - 1].equals(name)) return ss;
                }
            }
            return null;
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

    public abstract static class ImportDeclaration extends Located {
        public ImportDeclaration(Location location) {
            super(location);
        }
        public abstract void accept(Visitor.ComprehensiveVisitor visitor);
    }

    /**
     * Represents a single type import like<pre>
     *     import java.util.Map;</pre>
     */
    public static class SingleTypeImportDeclaration extends ImportDeclaration {
        public final String[] identifiers;

        public SingleTypeImportDeclaration(Location location, String[] identifiers) {
            super(location);
            this.identifiers = identifiers;
        }
        public final void accept(Visitor.ComprehensiveVisitor visitor) { visitor.visitSingleTypeImportDeclaration(this); }
    }

    /**
     * Represents a type-import-on-demand like<pre>
     *     import java.util.*;</pre>
     */
    public static class TypeImportOnDemandDeclaration extends ImportDeclaration {
        public final String[] identifiers;

        public TypeImportOnDemandDeclaration(Location location, String[] identifiers) {
            super(location);
            this.identifiers = identifiers;
        }
        public final void accept(Visitor.ComprehensiveVisitor visitor) { visitor.visitTypeImportOnDemandDeclaration(this); }
    }

    public interface TypeDeclaration extends Locatable, Scope {

        /**
         * Return the member type with the given name.
         * @return <code>null</code> if a member type with that name is not declared
         */
        MemberTypeDeclaration getMemberTypeDeclaration(String name);

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
        String  getDocComment();

        /**
         * Returns <code>true</code> if the object has a doc comment and
         * the <code>&#64#deprecated</code> tag appears in the doc
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
        public String getName();
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
         * by {@link IClass#getDeclaredIFields()}.
         * <p>
         * If a synthetic field with the same name exists already, then it must have the same
         * type and the redefinition is ignored.
         * @param iField
         */
        void defineSyntheticField(IClass.IField iField) throws CompileException;
    }

    public abstract static class AbstractTypeDeclaration implements TypeDeclaration {
        private   final Location location;
        protected final Scope    enclosingScope;
        protected final short    modifiers;
        public final List        declaredMethods              = new ArrayList(); // MethodDeclarator
        public final List        declaredClassesAndInterfaces = new ArrayList(); // MemberTypeDeclaration

        /*package*/ IClass resolvedType = null;

        public AbstractTypeDeclaration(
            Location location,
            Scope    enclosingScope,
            short    modifiers
        ) {
            this.location       = location;
            this.enclosingScope = enclosingScope;
            this.modifiers      = modifiers;
        }

        public void addDeclaredMethod(MethodDeclarator method) {
            this.declaredMethods.add(method);
        }

        // Implement "Scope".
        public Scope getEnclosingScope() {
            return this.enclosingScope;
        }

        // Implement TypeDeclaration.
        public void addMemberTypeDeclaration(MemberTypeDeclaration mcoid) {
            this.declaredClassesAndInterfaces.add(mcoid);
        }
        public Collection getMemberTypeDeclarations() {
            return this.declaredClassesAndInterfaces;
        }
        public MemberTypeDeclaration getMemberTypeDeclaration(String name) {
            for (Iterator it = this.declaredClassesAndInterfaces.iterator(); it.hasNext();) {
                MemberTypeDeclaration mtd = (MemberTypeDeclaration) it.next();
                if (mtd.getName().equals(name)) return mtd;
            }
            return null;
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
        public void throwParseException(String message) throws Parser.ParseException {
            throw new Parser.ParseException(message, this.location);
        }

        abstract public String toString();

        public int anonymousClassCount = 0; // For naming anonymous classes.
        public int localClassCount = 0;     // For naming local classes.
    }

    public abstract static class ClassDeclaration extends AbstractTypeDeclaration {
        public final List constructors = new ArrayList(); // ConstructorDeclarator
        public final List variableDeclaratorsAndInitializers = new ArrayList(); // TypeBodyDeclaration

        public ClassDeclaration(
            Location location,
            Scope    enclosingScope,
            short    modifiers
        ) {
            super(location, enclosingScope, modifiers);
        }

        public void addConstructor(ConstructorDeclarator cd) {
            this.constructors.add(cd);
        }
        public void addVariableDeclaratorOrInitializer(TypeBodyDeclaration tbd) {
            this.variableDeclaratorsAndInitializers.add(tbd);

            // Clear resolved type cache.
            if (this.resolvedType != null) this.resolvedType.declaredIFields = null;
        }

        // Compile time members.

        // Implement InnerClassDeclaration.
        public void defineSyntheticField(IClass.IField iField) throws CompileException {
            if (!(this instanceof InnerClassDeclaration)) throw new RuntimeException();

            IClass.IField if2 = (IClass.IField) this.syntheticFields.get(iField.getName());
            if (if2 != null) {
                if (iField.getType() != if2.getType()) throw new RuntimeException();
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
                    this.getLocation(),          // location
                    this,                        // declaringClass
                    null,                        // optionalDocComment
                    Mod.PUBLIC,                  // modifiers
                    new Java.FormalParameter[0], // formalParameters
                    new Java.Type[0]             // thrownExceptions
                );
                defaultConstructor.setBody(new Block(this.getLocation(), (Scope) this));
                return new ConstructorDeclarator[] { defaultConstructor };
            }

            return (ConstructorDeclarator[]) this.constructors.toArray(new ConstructorDeclarator[this.constructors.size()]);
        }

        // All field names start with "this$" or "val$".
        final SortedMap syntheticFields = new TreeMap(); // String name => IClass.IField
    }

    public static final class AnonymousClassDeclaration extends ClassDeclaration implements InnerClassDeclaration {
        public /*final*/ Type   baseType;  // Base class or interface

        private /*final*/ String className; // Effective class name.

        public AnonymousClassDeclaration(
            Location location,
            Scope    enclosingScope,
            Type     baseType
        ) {
            super(
                location,                         // location
                enclosingScope,                   // enclosingScope
                (short) (Mod.PRIVATE | Mod.FINAL) // modifiers
            );
            this.baseType = baseType;

            Scope s = this.getEnclosingScope();
            for (; !(s instanceof TypeDeclaration); s = s.getEnclosingScope());
            TypeDeclaration immediatelyEnclosingTypeDeclaration = (TypeDeclaration) s;
            this.className = immediatelyEnclosingTypeDeclaration.createAnonymousClassName();
        }

        public final void accept(Visitor.TypeDeclarationVisitor visitor) {
            visitor.visitAnonymousClassDeclaration(this);
        }

        // Implement TypeDeclaration.
        public String getClassName() { return this.className; }
        public String toString() { return "ANONYMOUS"; }
    }

    public abstract static class NamedClassDeclaration extends ClassDeclaration implements NamedTypeDeclaration, DocCommentable {
        private final String optionalDocComment;
        public final String  name;
        public final Type    optionalExtendedType;
        public final Type[]  implementedTypes;

        public NamedClassDeclaration(
            Location location,
            Scope    enclosingScope,
            String   optionalDocComment,
            short    modifiers,
            String   name,
            Type     optionalExtendedType,
            Type[]   implementedTypes
        ) {
            super(location, enclosingScope, modifiers);
            this.optionalDocComment   = optionalDocComment;
            this.name                 = name;
            this.optionalExtendedType = optionalExtendedType;
            this.implementedTypes     = implementedTypes;
        }

        public String toString() { return this.name; }

        // Implement NamedTypeDeclaration.
        public String getName() { return this.name; }

        // Implement DocCommentable.
        public String getDocComment() { return this.optionalDocComment; }
        public boolean hasDeprecatedDocTag() { return this.optionalDocComment != null && this.optionalDocComment.indexOf("@deprecated") != -1; }
    }

    public static final class MemberClassDeclaration extends NamedClassDeclaration implements MemberTypeDeclaration, InnerClassDeclaration {
        public MemberClassDeclaration(
            Location             location,
            NamedTypeDeclaration declaringType,
            String               optionalDocComment,
            short                modifiers,
            String               name,
            Type                 optionalExtendedType,
            Type[]               implementedTypes
        ) throws Parser.ParseException {
            super(
                location,              // location
                (Scope) declaringType, // enclosingScope
                optionalDocComment,    // optionalDocComment
                modifiers,             // modifiers
                name,                  // name
                optionalExtendedType,  // optionalExtendedType
                implementedTypes       // implementedTypes
            );

            // Check for redefinition of member type.
            MemberTypeDeclaration mcoid = declaringType.getMemberTypeDeclaration(name);
            if (mcoid != null) this.throwParseException("Redeclaration of class \"" + name + "\", previously declared in " + mcoid.getLocation());
        }

        // Implement TypeBodyDeclaration.
        public TypeDeclaration getDeclaringType() {
            return (TypeDeclaration) this.getEnclosingScope();
        }
        public boolean isStatic() {
            return (this.modifiers & Mod.STATIC) != 0;
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
        private final String className;

        public LocalClassDeclaration(
            Location location,
            Block    declaringBlock,
            String   optionalDocComment,
            short    modifiers,
            String   name,
            Type     optionalExtendedType,
            Type[]   implementedTypes
        ) throws Parser.ParseException {
            super(
                location,               // location
                (Scope) declaringBlock, // enclosingScope
                optionalDocComment,     // optionalDocComment
                modifiers,              // modifiers
                name,                   // name
                optionalExtendedType,   // optionalExtendedType
                implementedTypes        // implementedTypes
            );

            // Check for redefinition.
            for (Scope s = declaringBlock; s instanceof Block; s = s.getEnclosingScope()) {
                Block b = (Block) s;
                LocalClassDeclaration lcd = b.getLocalClassDeclaration(name);
                if (lcd != null) this.throwParseException("Redeclaration of local class \"" + name + "\"; previously declared in " + lcd.getLocation());
            }

            // Determine effective class name.
            Scope s = this.getEnclosingScope();
            while (!(s instanceof TypeDeclaration)) s = s.getEnclosingScope();
            TypeDeclaration immediatelyEnclosingTypeDeclaration = (TypeDeclaration) s;
            this.className = immediatelyEnclosingTypeDeclaration.createLocalTypeName(name);
        }

        // Implement ClassDeclaration.
        protected IClass getOuterIClass2() {
            Scope s = this.getEnclosingScope();
            for (; !(s instanceof FunctionDeclarator); s = s.getEnclosingScope());
            boolean isStaticMethod = (s instanceof MethodDeclarator) && (((FunctionDeclarator) s).modifiers & Mod.STATIC) != 0;
            for (; !(s instanceof TypeDeclaration); s = s.getEnclosingScope());
            TypeDeclaration immediatelyEnclosingTypeDeclaration = (TypeDeclaration) s;
            return (
                immediatelyEnclosingTypeDeclaration instanceof ClassDeclaration &&
                !isStaticMethod
            ) ? (IClass) immediatelyEnclosingTypeDeclaration : null;
        }

        // Implement TypeDeclaration.
        public String getClassName() {
            return this.className;
        }

        public final void accept(Visitor.TypeDeclarationVisitor visitor) { visitor.visitLocalClassDeclaration(this); }
    }

    public static final class PackageMemberClassDeclaration extends NamedClassDeclaration implements PackageMemberTypeDeclaration {
        public PackageMemberClassDeclaration(
            Location        location,
            CompilationUnit declaringCompilationUnit,
            String          optionalDocComment,
            short           modifiers,
            String          name,
            Type            optionalExtendedType,
            Type[]          implementedTypes
        ) throws Parser.ParseException {
            super(
                location,                         // location
                (Scope) declaringCompilationUnit, // enclosingScope
                optionalDocComment,               // optionalDocComment
                modifiers,                        // modifiers
                name,                             // name
                optionalExtendedType,             // optionalExtendedType
                implementedTypes                  // implementedTypes
            );

            // Check for forbidden modifiers (JLS 7.6).
            if ((modifiers & (
                Mod.PROTECTED |
                Mod.PRIVATE |
                Mod.STATIC
            )) != 0) this.throwParseException("Modifiers \"protected\", \"private\" and \"static\" not allowed in package member class declaration");

            // Check for conflict with single-type-import (7.6).
            {
                String[] ss = declaringCompilationUnit.getSingleTypeImport(name);
                if (ss != null) this.throwParseException("Package member class declaration \"" + name + "\" conflicts with single-type-import \"" + Java.join(ss, ".") + "\"");
            }

            // Check for redefinition within compilation unit (7.6).
            {
                PackageMemberTypeDeclaration pmtd = declaringCompilationUnit.getPackageMemberTypeDeclaration(name);
                if (pmtd != null) this.throwParseException("Redeclaration of class \"" + name + "\", previously declared in " + pmtd.getLocation());
            }
        }

        // Implement ClassDeclaration.
        protected IClass getOuterIClass2() {
            return null;
        }

        // Implement TypeDeclaration.
        public String getClassName() {
            String className = this.getName();

            CompilationUnit compilationUnit = (CompilationUnit) this.getEnclosingScope();
            if (compilationUnit.optionalPackageDeclaration != null) className = compilationUnit.optionalPackageDeclaration.packageName + '.' + className;

            return className;
        }

        public final void accept(Visitor.TypeDeclarationVisitor visitor) { visitor.visitPackageMemberClassDeclaration(this); }
    }

    public abstract static class InterfaceDeclaration extends AbstractTypeDeclaration implements NamedTypeDeclaration, DocCommentable {
        private final String    optionalDocComment;
        public /*final*/ String name;

        protected InterfaceDeclaration(
            Location location,
            Scope    enclosingScope,
            String   optionalDocComment,
            short    modifiers,
            String   name,
            Type[]   extendedTypes
        ) {
            super(
                location,
                enclosingScope,
                modifiers
            );
            this.optionalDocComment = optionalDocComment;
            this.name               = name;
            this.extendedTypes      = extendedTypes;
        }

        public String toString() { return this.name; }

        public void addConstantDeclaration(FieldDeclaration fd) {
            this.constantDeclarations.add(fd);

            // Clear resolved type cache.
            if (this.resolvedType != null) this.resolvedType.declaredIFields = null;
        }

        public /*final*/ Type[] extendedTypes;
        public final List       constantDeclarations = new ArrayList(); // FieldDeclaration

        // Set during "compile()".
        IClass[] interfaces = null;

        // Implement NamedTypeDeclaration.
        public String getName() { return this.name; }

        // Implement DocCommentable.
        public String getDocComment() { return this.optionalDocComment; }
        public boolean hasDeprecatedDocTag() { return this.optionalDocComment != null && this.optionalDocComment.indexOf("@deprecated") != -1; }
    }

    public static final class MemberInterfaceDeclaration extends InterfaceDeclaration implements MemberTypeDeclaration {
        public MemberInterfaceDeclaration(
            Location             location,
            NamedTypeDeclaration declaringType,
            String               optionalDocComment,
            short                modifiers,
            String               name,
            Type[]               extendedTypes
        ) throws Parser.ParseException {
            super(
                location,              // location
                (Scope) declaringType, // enclosingScope
                optionalDocComment,    // optionalDocComment
                modifiers,             // modifiers
                name,                  // name
                extendedTypes          // extendedTypes
            );

            // Check for redefinition of member type.
            MemberTypeDeclaration mcoid = declaringType.getMemberTypeDeclaration(name);
            if (mcoid != null) this.throwParseException("Redeclaration of interface \"" + name + "\", previously declared in " + mcoid.getLocation());
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
        public TypeDeclaration getDeclaringType() {
            return (TypeDeclaration) this.getEnclosingScope();
        }
        public boolean isStatic() {
            return (this.modifiers & Mod.STATIC) != 0;
        }

        public final void accept(Visitor.TypeDeclarationVisitor visitor) { visitor.visitMemberInterfaceDeclaration(this); }
        public final void accept(Visitor.TypeBodyDeclarationVisitor visitor) { visitor.visitMemberInterfaceDeclaration(this); }
    }

    public static final class PackageMemberInterfaceDeclaration extends InterfaceDeclaration implements PackageMemberTypeDeclaration {
        public PackageMemberInterfaceDeclaration(
            Location        location,
            CompilationUnit declaringCompilationUnit,
            String          optionalDocComment,
            short           modifiers,
            String          name,
            Type[]          extendedTypes
        ) throws Parser.ParseException {
            super(
                location,                         // location
                (Scope) declaringCompilationUnit, // enclosingScope
                optionalDocComment,               // optionalDocComment
                modifiers,                        // modifiers
                name,                             // name
                extendedTypes                     // extendedTypes
            );

            // Check for forbidden modifiers (JLS 7.6).
            if ((modifiers & (
                Mod.PROTECTED |
                Mod.PRIVATE |
                Mod.STATIC
            )) != 0) this.throwParseException("Modifiers \"protected\", \"private\" and \"static\" not allowed in package member interface declaration");

            // Check for conflict with single-type-import (JLS 7.6).
            {
                String[] ss = declaringCompilationUnit.getSingleTypeImport(name);
                if (ss != null) this.throwParseException("Package member interface declaration \"" + name + "\" conflicts with single-type-import \"" + Java.join(ss, ".") + "\"");
            }

            // Check for redefinition within compilation unit (JLS 7.6).
            {
                PackageMemberTypeDeclaration pmtd = declaringCompilationUnit.getPackageMemberTypeDeclaration(name);
                if (pmtd != null) this.throwParseException("Redeclaration of interface \"" + name + "\", previously declared in " + pmtd.getLocation());
            }
        }

        // Implement TypeDeclaration.
        public String getClassName() {
            String className = this.getName();

            CompilationUnit compilationUnit = (CompilationUnit) this.getEnclosingScope();
            if (compilationUnit.optionalPackageDeclaration != null) className = compilationUnit.optionalPackageDeclaration.packageName + '.' + className;

            return className;
        }

        public final void accept(Visitor.TypeDeclarationVisitor visitor) { visitor.visitPackageMemberInterfaceDeclaration(this); }
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
        TypeDeclaration getDeclaringType();
        boolean         isStatic();
        void            accept(Visitor.TypeBodyDeclarationVisitor visitor);
    }

    public abstract static class AbstractTypeBodyDeclaration extends Located implements TypeBodyDeclaration {
        final TypeDeclaration declaringType;
        final boolean         statiC;

        protected AbstractTypeBodyDeclaration(
            Location        location,
            TypeDeclaration declaringType,
            boolean         statiC
        ) {
            super(location);
            this.declaringType = declaringType;
            this.statiC        = statiC;
        }

        // Implement TypeBodyDeclaration.
        public TypeDeclaration getDeclaringType() {
            return this.declaringType;
        }

        public boolean isStatic() {
            return this.statiC;
        }

        // Implement Scope.
        public Scope getEnclosingScope() {
            return (Scope) this.declaringType;
        }
    }

    /**
     * Representation of an instance (JLS2 8.6) or static initializer (JLS2 8.7).
     */
    public final static class Initializer extends AbstractTypeBodyDeclaration implements BlockStatement {
        public Block block = null;

        public Initializer(
            Location        location,
            TypeDeclaration declaringType,
            boolean         statiC
        ) {
            super(location, declaringType, statiC);
        }

        public void setBlock(Block block) {
            if (this.block != null) throw new RuntimeException();
            this.block = block;
        }

        // Compile time members.

        // Implement BlockStatement.

        public final void accept(Visitor.TypeBodyDeclarationVisitor visitor) { visitor.visitInitializer(this); }
        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitInitializer(this); }
    }

    /**
     * Abstract base class for {@link Java.ConstructorDeclarator} and
     * {@link Java.MethodDeclarator}.
     */
    public abstract static class FunctionDeclarator extends AbstractTypeBodyDeclaration implements DocCommentable {
        private final String           optionalDocComment;
        public final short             modifiers;
        final Type                     type;
        public final String            name;
        public final FormalParameter[] formalParameters;
        protected final Type[]         thrownExceptions;
        public Block                   optionalBody = null;

        public FunctionDeclarator(
            Location          location,
            TypeDeclaration   declaringType,
            String            optionalDocComment,
            short             modifiers,
            Type              type,
            String            name,
            FormalParameter[] formalParameters,
            Type[]            thrownExceptions
        ) {
            super(location, declaringType, (modifiers & Mod.STATIC) != 0);
            this.optionalDocComment = optionalDocComment;
            this.modifiers          = modifiers;
            this.type               = type;
            this.name               = name;
            this.formalParameters   = formalParameters;
            this.thrownExceptions   = thrownExceptions;
        }

        public void setBody(Block body) {
            if (this.optionalBody != null) throw new RuntimeException("Body must be set exactly once");
            this.optionalBody = body;
        }

        // Implement "Scope".
        public Scope getEnclosingScope() {
            return this.getDeclaringType();
        }

        // Set by "compile()".
        IClass                  returnType = null;
        final HashMap           parameters = new HashMap();   // String name => LocalVariable

        // Implement DocCommentable.
        public String getDocComment() { return this.optionalDocComment; }
        public boolean hasDeprecatedDocTag() { return this.optionalDocComment != null && this.optionalDocComment.indexOf("@deprecated") != -1; }
    }

    public static final class ConstructorDeclarator extends FunctionDeclarator {
        final ClassDeclaration       declaringClass;
        IClass.IConstructor          iConstructor = null;
        public ConstructorInvocation optionalExplicitConstructorInvocation = null;

        public ConstructorDeclarator(
            Location          location,
            ClassDeclaration  declaringClass,
            String            optionalDocComment,
            short             modifiers,
            FormalParameter[] formalParameters,
            Type[]            thrownExceptions
        ) {
            super(
                location,                                // location
                declaringClass,                          // declaringType
                optionalDocComment,                      // optionalDocComment
                modifiers,                               // modifiers
                new BasicType(location, BasicType.VOID), // type
                "<init>",                                // name
                formalParameters,                        // formalParameters
                thrownExceptions                         // thrownExceptions
            );

            this.declaringClass = declaringClass;
        }
        public void setExplicitConstructorInvocation(
            ConstructorInvocation explicitConstructorInvocation
        ) {
            this.optionalExplicitConstructorInvocation = explicitConstructorInvocation;
        }

        // Compile time members.

        Map syntheticParameters = new HashMap(); // String name => LocalVariable

        // Implement "FunctionDeclarator":

        public String toString() {
            StringBuffer sb = new StringBuffer(this.declaringClass.getClassName());
            sb.append('(');
            FormalParameter[] fps = this.formalParameters;
            for (int i = 0; i < fps.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(fps[i].toString());
            }
            sb.append(')');
            return sb.toString();
        }

        public final void accept(Visitor.TypeBodyDeclarationVisitor visitor) { visitor.visitConstructorDeclarator(this); }
    }

    public final static class MethodDeclarator extends FunctionDeclarator {
        public MethodDeclarator(
            Location                      location,
            final AbstractTypeDeclaration declaringType,
            String                        optionalDocComment,
            short                         modifiers,
            Type                          type,
            String                        name,
            FormalParameter[]             formalParameters,
            Type[]                        thrownExceptions
        ) {
            super(
                location,           // location
                declaringType,      // declaringType
                optionalDocComment, // optionalDocComment
                modifiers,          // modifiers
                type,               // type
                name,               // name
                formalParameters,   // formalParameters
                thrownExceptions    // thrownExceptions
            );
        }

        public String toString() {
            StringBuffer sb = new StringBuffer(this.name);
            sb.append('(');
            FormalParameter[] fps = this.formalParameters;
            for (int i = 0; i < fps.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(fps[i].toString());
            }
            sb.append(')');
            return sb.toString();
        }

        public final void accept(Visitor.TypeBodyDeclarationVisitor visitor) { visitor.visitMethodDeclarator(this); }

        IClass.IMethod iMethod = null;
    }

    /**
     * This class is derived from "Statement", because it provides for the
     * initialization of the field. In other words, "compile()" generates the
     * code that initializes the field.
     */
    public static final class FieldDeclaration extends Statement implements TypeBodyDeclaration, DocCommentable {
        private final String          optionalDocComment;
        final AbstractTypeDeclaration declaringType;
        final short                   modifiers;
        public final Type             type;
        public VariableDeclarator[]   variableDeclarators = null;

        public FieldDeclaration(
            Location                location,
            AbstractTypeDeclaration declaringType,
            String                  optionalDocComment,
            short                   modifiers,
            Type                    type
        ) {
            super(
                location,
                (Scope) declaringType // enclosingScope
            );
            this.optionalDocComment = optionalDocComment;
            this.modifiers          = modifiers;
            this.declaringType      = declaringType;
            this.type               = type;
        }
        public void setVariableDeclarators(VariableDeclarator[] variableDeclarators) {
            this.variableDeclarators = variableDeclarators;
        }

        // Implement TypeBodyDeclaration.
        public TypeDeclaration getDeclaringType() {
            return this.declaringType;
        }
        public boolean isStatic() {
            return (this.modifiers & Mod.STATIC) != 0;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(Mod.shortToString(this.modifiers)).append(' ').append(this.type).append(' ').append(this.variableDeclarators[0]);
            for (int i = 1; i < this.variableDeclarators.length; ++i) {
                sb.append(", ").append(this.variableDeclarators[i]);
            }
            return sb.toString();
        }

        public final void accept(Visitor.TypeBodyDeclarationVisitor visitor) { visitor.visitFieldDeclaration(this); }
        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitFieldDeclaration(this); }

        // Implement DocCommentable.
        public String getDocComment() { return this.optionalDocComment; }
        public boolean hasDeprecatedDocTag() { return this.optionalDocComment != null && this.optionalDocComment.indexOf("@deprecated") != -1; }
    }

    public final static class VariableDeclarator extends Located {
        public final String name;
        public final int    brackets;
        public final Rvalue optionalInitializer;

        public VariableDeclarator(
            Location location,
            String   name,
            int      brackets,
            Rvalue   optionalInitializer
        ) {
            super(location);
            this.name                = name;
            this.brackets            = brackets;
            this.optionalInitializer = optionalInitializer;

            // Used both by field declarations an local variable declarations, so naming
            // conventions checking (JLS2 6.8) cannot be done here.
        }
    }

    public static final class FormalParameter {
        public FormalParameter(
            boolean finaL,
            Type    type,
            String  name
        ) {
            this.finaL = finaL;
            this.type  = type;
            this.name  = name;
        }

        public String toString() {
            return this.type.toString() + ' ' + this.name;
        }

        public final boolean finaL;
        public final Type    type;
        public final String  name;
    }

    /**
     * Base of all statements that can appear in a block.
     */
    public interface BlockStatement extends Locatable, Scope {
        void accept(Visitor.BlockStatementVisitor visitor);
    }

    public static abstract class Statement extends Located implements BlockStatement {
        protected final Scope enclosingScope;

        protected Statement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location);
            this.enclosingScope = enclosingScope;
        }

        // Implement "Scope".
        public Scope getEnclosingScope() { return this.enclosingScope; }
    }

    public final static class LabeledStatement extends BreakableStatement {
        public LabeledStatement(
            Location location,
            Scope    enclosingScope,
            String   label
        ) {
            super(location, enclosingScope);
            this.label = label;
        }

        public void setBody(Statement body) {
            this.body = body;
        }

        /*final*/ String label;
        public Statement body = null;

        // Compile time members:

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitLabeledStatement(this); }
    }

    /**
     * Representation of a Java<sup>TM</sup> "block" (JLS 14.2).
     * <p>
     * The statements that the block defines are executed in sequence.
     */
    public final static class Block extends Statement {
        public final List statements = new ArrayList(); // BlockStatement

        public Block(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        public void addStatement(BlockStatement statement) {
            this.statements.add(statement);
        }
        public void addStatements(
            List statements // BlockStatement
        ) {
            this.statements.addAll(statements);
        }
        public BlockStatement[] getStatements() {
            return (BlockStatement[]) this.statements.toArray(new BlockStatement[this.statements.size()]);
        }


        // Compile time members.

        // Fills while the block is being compiled.
        final Map declaredLocalClasses = new HashMap(); // String declaredName => LocalClassDeclaration

        public LocalClassDeclaration getLocalClassDeclaration(String name) {
            return (LocalClassDeclaration) this.declaredLocalClasses.get(name);
        }

        /**
         * JLS 14.10 specifies that under certain circumstances it is not an error that
         * statements are physically unreachable, precisely
         * <pre>
         *   {
         *     if (true) return;
         *     a = 1; // Physically unreachable, but not an error, i.e. "dead"
         *   }
         *   b = 2; // Same thing.
         * </pre>
         * The "if" statement notifies its enclosing block by calling this method.
         */
        /*private*/ void followingStatementsAreDead() {
            this.keepCompiling = false;

            Java.Scope s = this.getEnclosingScope();
            if (s instanceof Java.Block) ((Java.Block) s).followingStatementsAreDead();
        }

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitBlock(this); }

        HashMap localVariables = new HashMap(); // String name => LocalVariable
        boolean keepCompiling;
    }

    /**
     * Base class for statements that can be terminated abnormally with a
     * "break" statement.
     * <p>
     * According to the JLS, statements that can be terminated abnormally with
     * a "break" statement are: "COntinuable" statements ("for", "do" and
     * "while"), labeled statements, and the "switch" statement.
     */
    public static abstract class BreakableStatement extends Statement {
        protected BreakableStatement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        CodeContext.Offset whereToBreak = null;
    }

    public static abstract class ContinuableStatement extends BreakableStatement {
        protected ContinuableStatement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        protected CodeContext.Offset whereToContinue = null;
        protected boolean            bodyHasContinue = false;
    }

    public final static class ExpressionStatement extends Statement {
        public ExpressionStatement(
            Rvalue rvalue,
            Scope  enclosingScope
        ) throws Parser.ParseException {
            super(rvalue.getLocation(), enclosingScope);
            if (!(
                rvalue instanceof Java.Assignment
                || rvalue instanceof Java.Crement
                || rvalue instanceof Java.MethodInvocation
                || rvalue instanceof Java.SuperclassMethodInvocation
                || rvalue instanceof Java.NewClassInstance
            )) this.throwParseException("This kind of expression is not allowed in an expression statement");
            this.rvalue = rvalue;
        }
        public final Rvalue rvalue;

        // Compile time members:

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitExpressionStatement(this); }
    }

    public final static class LocalClassDeclarationStatement extends Statement {
        public LocalClassDeclarationStatement(
            Scope                      enclosingScope,
            Java.LocalClassDeclaration lcd
        ) {
            super(lcd.getLocation(), enclosingScope);
            this.lcd = lcd;
        }
        public final LocalClassDeclaration lcd;

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitLocalClassDeclarationStatement(this); }
    }

    public final static class IfStatement extends Statement {

        /**
         * Notice that the <code>elseStatement</code> is mandatory; for an if statement without
         * an "else" clause, a dummy {@link Java.EmptyStatement} should be passed.
         */
        public IfStatement(
            Location       location,
            Scope          enclosingScope,
            Rvalue         condition,
            BlockStatement thenStatement,
            BlockStatement optionalElseStatement
        ) {
            super(location, enclosingScope);
            this.condition             = condition;
            this.thenStatement         = thenStatement;
            this.optionalElseStatement = optionalElseStatement;
        }

        public final Rvalue         condition;
        public final BlockStatement thenStatement;
        public final BlockStatement optionalElseStatement;

        // Compile time members:

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitIfStatement(this); }
    }

    public final static class ForStatement extends ContinuableStatement {
        public ForStatement(
            Location location,
            Scope    enclosingScope
        ) {

            // Trick: Insert a "Block" between the enclosing scope and the
            // "for" statement.
            super(location, new Block(location, enclosingScope));
            this.implicitBlock = (Block) this.enclosingScope;
            this.body = new Block(
                location,
                this      // enclosingScope
            );
        }

        public void set(
            BlockStatement optionalInit,
            Rvalue         optionalCondition,
            Rvalue[]       optionalUpdate,
            BlockStatement body
        ) {
            this.optionalInit      = optionalInit;
            this.optionalCondition = optionalCondition;
            this.optionalUpdate    = optionalUpdate;
            this.body              = body;
        }

        public final Block    implicitBlock;
        public BlockStatement optionalInit      = null;
        public Rvalue         optionalCondition = null;
        public Rvalue[]       optionalUpdate    = null;
        public BlockStatement body;

        // Compile time members:

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitForStatement(this); }
    }

    public final static class WhileStatement extends ContinuableStatement {
        public WhileStatement(
            Location location,
            Scope    enclosingScope,
            Rvalue   condition
        ) {
            super(location, enclosingScope);
            this.condition = condition;
        }

        public void setBody(BlockStatement body) { this.body = body; }

        public final Rvalue   condition;
        public BlockStatement body = null;

        // Compile time members:


        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitWhileStatement(this); }
    }

    public final static class TryStatement extends Statement {
        public BlockStatement body            = null;
        public final List     catchClauses    = new ArrayList(); // CatchClause
        public Block          optionalFinally = null;

        public TryStatement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        public void setBody(BlockStatement body) {
            this.body = body;
        }
        public void addCatchClause(CatchClause catchClause) {
            this.catchClauses.add(catchClause);
        }
        public void setFinally(Block finallY) {
            this.optionalFinally = finallY;
        }

        // Compile time members:

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitTryStatement(this); }

        // Preserves "leave stack".
        short stackValueLvIndex;

        CodeContext.Offset finallyOffset = null;
    }

    public static class CatchClause {
        public CatchClause(
            FormalParameter caughtException,
            Block           body
        ) {
            this.caughtException = caughtException;
            this.body            = body;
        }

        public final FormalParameter caughtException;
        public final Block           body;
    }

    /**
     * 14.10 The "switch" Statement
     */
    public static final class SwitchStatement extends BreakableStatement {
        public SwitchStatement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        public void setCondition(Rvalue condition) {
            this.condition = condition;
        }
        public void addSwitchBlockStatementGroup(SwitchBlockStatementGroup sbsg) {
            this.sbsgs.add(sbsg);
        }

        public Rvalue     condition = null;
        public final List sbsgs = new ArrayList(); // SwitchBlockStatementGroup

        // Compile time members:

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitSwitchStatement(this); }
    }
    static class Padder extends CodeContext.Inserter implements CodeContext.FixUp {
        public Padder(CodeContext codeContext) {
            codeContext.super();
        }
        public void fixUp() {
            int x = this.offset % 4;
            if (x != 0) {
                CodeContext ca = this.getCodeContext();
                ca.pushInserter(this); {
                    ca.write((short) -1, new byte[4 - x]);
                } ca.popInserter();
            }
        }
    }

    public static class SwitchBlockStatementGroup extends Located {
        public SwitchBlockStatementGroup(Location location) {
            super(location);
        }

        public void addSwitchLabel(Rvalue value) {
            this.caseLabels.add(value);
        }
        public void addDefaultSwitchLabel() throws Parser.ParseException {
            if (this.hasDefaultLabel) this.throwParseException("Duplicate \"default\" switch label");
            this.hasDefaultLabel = true;
        }
        public void setBlockStatements(List blockStatements) {
            this.blockStatements = blockStatements;
        }

        public final List caseLabels = new ArrayList(); // Rvalue
        public boolean    hasDefaultLabel = false;
        public List       blockStatements; // BlockStatement
    }

    public final static class SynchronizedStatement extends Statement {
        public SynchronizedStatement(
            Location location,
            Scope    enclosingScope,
            Rvalue   expression
        ) {
            super(location, enclosingScope);
            this.expression = expression;
        }

        public void setBody(BlockStatement body) {
            this.body = body;
        }

        public final Rvalue   expression;
        public BlockStatement body         = null;

        // Compile time members:

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitSynchronizedStatement(this); }

        short monitorLvIndex = -1;
    }

    public final static class DoStatement extends ContinuableStatement {
        public DoStatement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        public void setBody(BlockStatement body) { this.body = body; }
        public void setCondition(Rvalue condition) { this.condition = condition; }

        public BlockStatement body      = null;
        public Rvalue         condition = null;

        // Compile time members:

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitDoStatement(this); }
    }

    public final static class LocalVariableDeclarationStatement extends Statement {

        /**
         * @param modifiers Only "final" allowed.
         */
        public LocalVariableDeclarationStatement(
            Location             location,
            Block                declaringBlock,
            short                modifiers,
            Type                 type,
            VariableDeclarator[] variableDeclarators
        ) {
            super(location, declaringBlock);
            this.declaringBlock      = declaringBlock;
            this.modifiers           = modifiers;
            this.type                = type;
            this.variableDeclarators = variableDeclarators;
        }

        final Block                       declaringBlock;
        public final short                modifiers;
        public final Type                 type;
        public final VariableDeclarator[] variableDeclarators;

        // Compile time members:

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitLocalVariableDeclarationStatement(this); }
    }

    public final static class ReturnStatement extends Statement {
        public ReturnStatement(
            Location location,
            Scope    enclosingScope,
            Rvalue   optionalReturnValue
        ) {
            super(location, enclosingScope);
            this.optionalReturnValue = optionalReturnValue;
        }

        public final Rvalue optionalReturnValue;

        // Compile time members:

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitReturnStatement(this); }
    }

    public final static class ThrowStatement extends Statement {
        public ThrowStatement(
            Location location,
            Scope    enclosingScope,
            Rvalue   expression
        ) {
            super(location, enclosingScope);
            this.expression = expression;
        }

        public final Rvalue expression;

        // Compile time members:

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitThrowStatement(this); }
    }

    /**
     * Representation of the Java<sup>TM</sup> "break" statement (JLS 14.14).
     */
    public final static class BreakStatement extends Statement {
        public BreakStatement(
            Location location,
            Scope    enclosingScope,
            String   optionalLabel
        ) {
            super(location, enclosingScope);
            this.optionalLabel = optionalLabel;
        }

        public final String optionalLabel;

        // Compile time members:

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitBreakStatement(this); }
    }

    /**
     * Representation of the Java<sup>TM</sup> "continue" statement (JLS
     * 14.15).
     */
    public final static class ContinueStatement extends Statement {
        public ContinueStatement(
            Location location,
            Scope    enclosingScope,
            String   optionalLabel
        ) {
            super(location, enclosingScope);
            this.optionalLabel = optionalLabel;
        }

        public final String optionalLabel;

        // Compile time members:

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitContinueStatement(this); }
    }

    /**
     * Represents the "empty statement", i.e. the blank semicolon.
     */
    public final static class EmptyStatement extends Statement {
        public EmptyStatement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        public final void accept(Visitor.BlockStatementVisitor visitor) { visitor.visitEmptyStatement(this); }
    }

    /**
     * Abstract base class for {@link Java.Type}, {@link Java.Rvalue} and
     * {@link Java.Lvalue}.
     */
    public static abstract class Atom extends Located {
        public Atom(Location location) {
            super(location);
        }

        public Type   toType()   { return null; }
        public Rvalue toRvalue() { return null; }
        public Lvalue toLvalue() { return null; }

        public abstract String toString();

        // Parse time members:

        public final Type toTypeOrPE() throws Parser.ParseException {
            Type result = this.toType();
            if (result == null) this.throwParseException("Expression \"" + this.toString() + "\" is not a type");
            return result;
        }
        public final Rvalue toRvalueOrPE() throws Parser.ParseException {
            Rvalue result = this.toRvalue();
            if (result == null) this.throwParseException("Expression \"" + this.toString() + "\" is not an rvalue");
            return result;
        }
        public final Lvalue toLvalueOrPE() throws Parser.ParseException {
            Lvalue result = this.toLvalue();
            if (result == null) this.throwParseException("Expression \"" + this.toString() + "\" is not an lvalue");
            return result;
        }


        public abstract void accept(Visitor.AtomVisitor visitor);
    }

    /**
     * Representation of a Java<sup>TM</sup> type.
     */
    public static abstract class Type extends Atom {
        protected Type(Location location) {
            super(location);
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

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitSimpleType(this); }
        public final void accept(Visitor.TypeVisitor visitor) { visitor.visitSimpleType(this); }
    };

    /**
     * Representation of a Java<sup>TM</sup> "basic type" (obviously
     * equaivalent to a "primitive type") (JLS 4.2).
     */
    public static final class BasicType extends Type {
        public /*final*/ int index;

        public BasicType(Location location, int index) {
            super(location);
            this.index = index;
        }

        public String toString() {
            switch (this.index) {
                case BasicType.VOID:    return "void";
                case BasicType.BYTE:    return "byte";
                case BasicType.SHORT:   return "short";
                case BasicType.CHAR:    return "char";
                case BasicType.INT:     return "int";
                case BasicType.LONG:    return "long";
                case BasicType.FLOAT:   return "float";
                case BasicType.DOUBLE:  return "double";
                case BasicType.BOOLEAN: return "boolean";
                default: throw new RuntimeException("Invalid index " + this.index);
            }
        }

        public final void accept(Visitor.TypeVisitor visitor) { visitor.visitBasicType(this); }
        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitBasicType(this); }

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
        public final Scope    scope;

        public ReferenceType(
            Location location,
            Scope    scope,
            String[] identifiers
        ) {
            super(location);
            this.scope       = scope;
            this.identifiers = identifiers;
        }

        public String toString() { return Java.join(this.identifiers, "."); }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitReferenceType(this); }
        public final void accept(Visitor.TypeVisitor visitor) { visitor.visitReferenceType(this); }
    }

    // Helper class for JLS 15.9.1
    public static final class RvalueMemberType extends Type {
        public final Rvalue rvalue;
        public final String identifier;

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

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitRvalueMemberType(this); }
        public final void accept(Visitor.TypeVisitor visitor) { visitor.visitRvalueMemberType(this); }
    }


    /**
     * Representation of a Java<sup>TM</sup> array type (JLS 10.1).
     */
    public static final class ArrayType extends Type {
        public final Type componentType;

        public ArrayType(Type componentType) {
            super(componentType.getLocation());
            this.componentType = componentType;
        }

        public String toString() {
            return this.componentType.toString() + "[]";
        }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitArrayType(this); }
        public final void accept(Visitor.TypeVisitor visitor) { visitor.visitArrayType(this); }
    }

    /**
     * Representation of an "rvalue", i.e. an expression that has a type and
     * a value, but cannot be assigned to: An expression that can be the
     * right-hand-side of an assignment.
     */
    public static abstract class Rvalue extends Atom {
        protected Rvalue(
            Location location
        ) {
            super(location);
        }

        public Rvalue toRvalue() { return this; }

        static final Object CONSTANT_VALUE_UNKNOWN = new Object();
        Object              constantValue = Java.Rvalue.CONSTANT_VALUE_UNKNOWN;
        public static final Object CONSTANT_VALUE_NULL = new Throwable();

        public abstract void accept(Visitor.RvalueVisitor rvv);

        public final static boolean JUMP_IF_TRUE  = true;
        public final static boolean JUMP_IF_FALSE = false;
    }

    /**
     * Base class for {@link Java.Rvalue}s that compile better as conditional
     * branches.
     */
    public static abstract class BooleanRvalue extends Rvalue {
        protected BooleanRvalue(Location location) {
            super(location);
        }
    }

    /**
     * Representation of an "lvalue", i.e. an expression that has a type and
     * a value, and can be assigned to: An expression that can be the
     * left-hand-side of an assignment.
     */
    public static abstract class Lvalue extends Rvalue {
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
        public AmbiguousName(
            Location location,
            Scope    scope,
            String[] identifiers
        ) {
            this(location, scope, identifiers, identifiers.length);
        }
        public AmbiguousName(
            Location location,
            Scope    scope,
            String[] identifiers,
            int      n
        ) {
            super(location);
            this.scope       = scope;
            this.identifiers = identifiers;
            this.n           = n;
        }

        // Override "Atom.toType()".
        private Type type = null;
        public Type toType() {
            if (this.type == null) {
                String[] sa;
                if (this.identifiers.length == this.n) {
                    sa = this.identifiers;
                } else
                {
                    sa = new String[this.n];
                    System.arraycopy(this.identifiers, 0, sa, 0, this.n);
                }
                this.type = new ReferenceType(this.getLocation(), this.scope, sa);
            }
            return this.type;
        }

        // Compile time members.

        public String toString() {
            return Java.join(this.identifiers, ".", 0, this.n);
        }

        Atom reclassified = null;

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitAmbiguousName(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitAmbiguousName(this); }
        public final void accept(Visitor.LvalueVisitor visitor) { visitor.visitAmbiguousName(this); }

        public final Scope    scope;
        public final String[] identifiers;
        final int             n;
    }

    // Helper class for 6.5.2.1.7, 6.5.2.2.1
    public static final class Package extends Atom {
        public final String name;

        public Package(Location location, String name) {
            super(location);
            this.name = name;
        }
        public String toString() { return this.name; }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitPackage(this); }
    }

    /**
     * Representation of a local variable access.
     */
    public static final class LocalVariableAccess extends Lvalue {
        public /*final*/ LocalVariable localVariable;

        public LocalVariableAccess(
            Location      location,
            LocalVariable localVariable
        ) {
            super(location);
            this.localVariable = localVariable;
        }

        // Compile time members.

        public String toString() { return this.localVariable.toString(); }

        public final void accept(Visitor.LvalueVisitor visitor) { visitor.visitLocalVariableAccess(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitLocalVariableAccess(this); }
        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitLocalVariableAccess(this); }
    }

    /**
     * Representation of an access to a field of a class or an interface. (Does not implement the
     * "array length" expression, e.g. "ia.length".)
     */
    public static final class FieldAccess extends Lvalue {
        public final BlockStatement enclosingBlockStatement;
        public final Atom           lhs;
        public final IClass.IField  field;

        public FieldAccess(
            Location       location,
            BlockStatement enclosingBlockStatement,
            Atom           lhs,
            IClass.IField  field
        ) {
            super(location);
            this.enclosingBlockStatement = enclosingBlockStatement;
            this.lhs                     = lhs;
            this.field                   = field;
        }

        // Compile time members.

        // Implement "Atom".
        public String toString() { return this.lhs.toString() + '.' + this.field.toString(); }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitFieldAccess(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitFieldAccess(this); }
        public final void accept(Visitor.LvalueVisitor visitor) { visitor.visitFieldAccess(this); }
    }

    public static final class ArrayLength extends Rvalue {
        public ArrayLength(
            Location location,
            Rvalue   lhs
        ) {
            super(location);
            this.lhs = lhs;
        }

        public final Rvalue lhs;

        // Compile time members.

        // Implement "Atom".
        public String toString() { return this.lhs.toString() + ".length"; }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitArrayLength(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitArrayLength(this); }
    }

    /**
     * Representation of an access to the innermost enclosing instance.
     */
    public static final class ThisReference extends Rvalue {

        /**
         * Access the declaring class.
         * @param location
         * @param scope
         */
        public ThisReference(Location location, Scope scope) {
            super(location);
            this.scope = scope;
        }

        final Scope scope;

        // Compile time members.

        IClass iClass = null;

        // Implement "Atom".
        public String toString() { return "this"; }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitThisReference(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitThisReference(this); }
    }

    /**
     * Representation of an access to the current object or an enclosing instance.
     */
    public static final class QualifiedThisReference extends Rvalue {
        public final Scope scope;
        public final Type  qualification;


        /**
         * Access the given enclosing instance of the declaring class.
         * @param location
         * @param scope
         * @param qualification
         */
        public QualifiedThisReference(
            Location location,
            Scope    scope,
            Type     qualification
        ) {
            super(location);

            if (scope         == null) throw new NullPointerException();
            if (qualification == null) throw new NullPointerException();

            this.scope         = scope;
            this.qualification = qualification;
        }

        // Compile time members.

        ClassDeclaration    declaringClass               = null;
        TypeBodyDeclaration declaringTypeBodyDeclaration = null;
        IClass              targetIClass                 = null;

        public QualifiedThisReference(
            Location            location,
            ClassDeclaration    declaringClass,
            TypeBodyDeclaration declaringTypeBodyDeclaration,
            IClass              targetIClass
        ) {
            super(location);
            if (declaringClass               == null) throw new NullPointerException();
            if (declaringTypeBodyDeclaration == null) throw new NullPointerException();
            if (targetIClass                 == null) throw new NullPointerException();

            this.scope                        = null;
            this.qualification                = null;
            this.declaringClass               = declaringClass;
            this.declaringTypeBodyDeclaration = declaringTypeBodyDeclaration;
            this.targetIClass                 = targetIClass;
        }

        // Implement "Atom".
        public String toString() {
            return this.qualification.toString() + ".this";
        }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitQualifiedThisReference(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitQualifiedThisReference(this); }
    }

    public static final class ClassLiteral extends Rvalue {
        final AbstractTypeDeclaration declaringType;
        final BlockStatement          enclosingBlockStatement;
        public final Type             type;

        public ClassLiteral(
            Location       location,
            BlockStatement enclosingBlockStatement,
            Type           type
        ) {
            super(location);
            this.enclosingBlockStatement = enclosingBlockStatement;
            Scope s;
            for (s = enclosingBlockStatement; !(s instanceof AbstractTypeDeclaration); s = s.getEnclosingScope());
            this.declaringType = (AbstractTypeDeclaration) s;
            this.type          = type;
        }

        // Compile time members.

        //Implement "Atom".
        public String toString() { return this.type.toString() + ".class"; }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitClassLiteral(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitClassLiteral(this); }
    }

    public static final class Assignment extends Rvalue {
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

        public final Lvalue lhs;
        public final String operator;
        public final Rvalue rhs;

        // Compile time members.

        // Implement "Atom".
        public String toString() { return this.lhs.toString() + ' ' + this.operator + ' ' + this.rhs.toString(); }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitAssignment(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitAssignment(this); }
    }

    public static final class ConditionalExpression extends Rvalue {
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
        public String toString() { return this.lhs.toString() + " ? " + this.mhs.toString() + " : " + this.rhs.toString(); }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitConditionalExpression(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitConditionalExpression(this); }

        public final Rvalue lhs, mhs, rhs;
    }

    /**
     * Objects of this class represent represent one pre- or post-increment
     * or decrement.
     */
    public static final class Crement extends Rvalue {
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

        public final boolean pre;
        public final String  operator;
        public final Lvalue  operand;

        // Compile time members.

        // Implement "Atom".
        public String toString() {
            return (
                this.pre ?
                this.operator + this.operand :
                this.operand + this.operator
            );
        }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitCrement(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitCrement(this); }
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

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitArrayAccessExpression(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitArrayAccessExpression(this); }
        public final void accept(Visitor.LvalueVisitor visitor) { visitor.visitArrayAccessExpression(this); }
    }

    /**
     * This class implements class or interface field access, and also the "array length"
     * expression "xy.length".
     */
    public static final class FieldAccessExpression extends Lvalue {
        public final BlockStatement enclosingBlockStatement;
        public final Atom           lhs;
        public final String         fieldName;

        public FieldAccessExpression(
            Location       location,
            BlockStatement enclosingBlockStatement,
            Atom           lhs,
            String         fieldName
        ) {
            super(location);
            this.enclosingBlockStatement = enclosingBlockStatement;
            this.lhs                     = lhs;
            this.fieldName               = fieldName;
        }

        // Compile time members:

        // Implement "Atom".
        public String toString() { return this.lhs.toString() + '.' + this.fieldName; }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitFieldAccessExpression(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitFieldAccessExpression(this); }
        public final void accept(Visitor.LvalueVisitor visitor) { visitor.visitFieldAccessExpression(this); }

        Rvalue value = null;
    }

    /**
     * This class implements the unary operators "+", "-", "~" and "!".
     */
    public static final class UnaryOperation extends BooleanRvalue {
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

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitUnaryOperation(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitUnaryOperation(this); }

        public final String operator;
        public final Rvalue operand;
    }

    public static final class Instanceof extends Rvalue {
        public Instanceof(
            Location location,
            Rvalue   lhs,
            Type     rhs // ReferenceType or ArrayType
        ) {
            super(location);
            this.lhs = lhs;
            this.rhs = rhs;
        }

        public final Rvalue lhs;
        public final Type   rhs;

        // Compile time members.

        // Implement "Atom".
        public String toString() { return this.lhs.toString() + " instanceof " + this.rhs.toString(); }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitInstanceof(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitInstanceof(this); }
    }

    /**
     * Representation of all non-operand-modifying Java<sup>TM</sup> binary
     * operations.
     * <p>
     * Operations with boolean result:<br>
     * <tt>|| && == != < > <= >=</tt>
     * <p>
     * Operations with non-boolean result:<br>
     * <tt>| ^ & * / % + - << >> >>></tt>
     */
    public static final class BinaryOperation extends BooleanRvalue {
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

        public final Rvalue lhs;
        public final String op;
        public final Rvalue rhs;

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
                if (
                    lhs instanceof BinaryOperation &&
                    ((BinaryOperation) lhs).op == this.op
                ) {
                    x = (BinaryOperation) lhs;
                } else {
                    operands.add(lhs);
                    break;
                }
            }
            return new ReverseListIterator(operands.listIterator(operands.size()));
        }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitBinaryOperation(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitBinaryOperation(this); }
    }

    public static final class Cast extends Rvalue {
        public Cast(
            Location location,
            Type     targetType,
            Rvalue   value
        ) {
            super(location);
            this.targetType = targetType;
            this.value      = value;
        }

        public final Type   targetType;
        public final Rvalue value;

        // Compile time members.

        // Implement "Atom".
        public String toString() { return '(' + this.targetType.toString() + ") " + this.value.toString(); }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitCast(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitCast(this); }
    }

    public final static class ParenthesizedExpression extends Lvalue {
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

    public static abstract class ConstructorInvocation extends Atom {
        protected final ClassDeclaration      declaringClass;
        protected final ConstructorDeclarator declaringConstructor;
        public final Rvalue[]                 arguments;

        protected ConstructorInvocation(
            Location              location,
            ClassDeclaration      declaringClass,
            ConstructorDeclarator declaringConstructor,
            Rvalue[]              arguments
        ) {
            super(location);
            this.declaringClass        = declaringClass;
            this.declaringConstructor  = declaringConstructor;
            this.arguments             = arguments;
        }

        public abstract void accept(Visitor.ConstructorInvocationVisitor visitor);
    }

    public final static class AlternateConstructorInvocation extends ConstructorInvocation {
        public AlternateConstructorInvocation(
            Location              location,
            ClassDeclaration      declaringClass,
            ConstructorDeclarator declaringConstructor,
            Rvalue[]              arguments
        ) {
            super(location, declaringClass, declaringConstructor, arguments);
        }

        // Implement Atom
        public String toString() { return "this()"; }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitAlternateConstructorInvocation(this); }
        public void accept(Visitor.ConstructorInvocationVisitor visitor) { visitor.visitAlternateConstructorInvocation(this); }
    }

    public final static class SuperConstructorInvocation extends ConstructorInvocation {
        public final Rvalue optionalQualification;

        public SuperConstructorInvocation(
            Location              location,
            ClassDeclaration      declaringClass,
            ConstructorDeclarator declaringConstructor,
            Rvalue                optionalQualification,
            Rvalue[]              arguments
        ) {
            super(location, declaringClass, declaringConstructor, arguments);
            this.optionalQualification = optionalQualification;
        }

        // Implement Atom
        public String toString() { return "super()"; }

        public void accept(Visitor.AtomVisitor visitor) { visitor.visitSuperConstructorInvocation(this); }
        public void accept(Visitor.ConstructorInvocationVisitor visitor) { visitor.visitSuperConstructorInvocation(this); }
    }

    public static final class MethodInvocation extends Invocation {
        public final Atom   optionalTarget; // null == simple method name.
        public final String methodName;

        public MethodInvocation(
            Location       location,
            BlockStatement enclosingBlockDeclaration,
            Atom           optionalTarget,
            String         methodName,
            Rvalue[]       arguments
        ) {
            super(location, enclosingBlockDeclaration, arguments);
            this.optionalTarget = optionalTarget;
            this.methodName     = methodName;
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

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitMethodInvocation(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitMethodInvocation(this); }
    }

    public static final class SuperclassMethodInvocation extends Invocation {
        public SuperclassMethodInvocation(
            Location       location,
            BlockStatement enclosingBlockStatement,
            String         methodName,
            Rvalue[]       arguments
        ) {
            super(location, enclosingBlockStatement, arguments);
            this.methodName = methodName;
        }

        // Implement "Atom".
        public String toString() { return "super." + this.methodName + "()"; }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitSuperclassMethodInvocation(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitSuperclassMethodInvocation(this); }

        public final String methodName;
    }

    public static abstract class Invocation extends Rvalue {
        protected Invocation(
            Location       location,
            BlockStatement enclosingBlockDeclaration,
            Rvalue[]       arguments
        ) {
            super(location);
            this.enclosingBlockStatement = enclosingBlockDeclaration;
            this.arguments                 = arguments;
        }

        protected final BlockStatement enclosingBlockStatement;
        public final Rvalue[]          arguments;
    }

    public static final class NewClassInstance extends Rvalue {
        public final Scope    scope;
        public final Rvalue   optionalQualification;
        public final Type     type;
        public final Rvalue[] arguments;

        public NewClassInstance(
            Location location,
            Scope    scope,
            Rvalue   optionalQualification,
            Type     type,
            Rvalue[] arguments
        ) {
            super(location);
            this.scope                 = scope;
            this.optionalQualification = optionalQualification;
            this.type                  = type;
            this.arguments             = arguments;
        }

        // Compile time members.

        protected IClass iClass = null;

        public NewClassInstance(
            Location location,
            Scope    scope,
            Rvalue   optionalQualification,
            IClass   iClass,
            Rvalue[] arguments
        ) {
            super(location);
            this.scope                 = scope;
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
            sb.append("()");
            return sb.toString();
        }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitNewClassInstance(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitNewClassInstance(this); }
    }

    public static final class NewAnonymousClassInstance extends Rvalue {
        final Scope                            scope;
        public final Rvalue                    optionalQualification;
        public final AnonymousClassDeclaration anonymousClassDeclaration;
        public final Rvalue[]                  arguments;

        public NewAnonymousClassInstance(
            Location                  location,
            Scope                     scope,
            Rvalue                    optionalQualification,
            AnonymousClassDeclaration anonymousClassDeclaration,
            Rvalue[]                  arguments
        ) {
            super(location);
            this.scope                     = scope;
            this.optionalQualification     = optionalQualification;
            this.anonymousClassDeclaration = anonymousClassDeclaration;
            this.arguments                 = arguments;
        }

        // Implement "Atom".
        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (this.optionalQualification != null) sb.append(this.optionalQualification.toString()).append('.');
            sb.append("new ").append(this.anonymousClassDeclaration.toString()).append("() { ... }");
            return sb.toString();
        }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitNewAnonymousClassInstance(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitNewAnonymousClassInstance(this); }
    }

    public static final class ParameterAccess extends Rvalue {
        public final FunctionDeclarator declaringFunction;
        public final String             name;

        public ParameterAccess(
            Location           location,
            FunctionDeclarator declaringFunction,
            String             name
        ) {
            super(location);
            this.declaringFunction = declaringFunction;
            this.name = name;
        }

        // Implement Atom
        public String toString() { return this.name; }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitParameterAccess(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitParameterAccess(this); }
    }

    public static final class NewArray extends Rvalue {
        public final Type     type;
        public final Rvalue[] dimExprs;
        public final int      dims;

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

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitNewArray(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitNewArray(this); }
    }

    /**
     * Represents a Java<sup>TM</sup> array initializer (JLS 10.6).
     * <p>
     * Allocates an array and initializes its members with (not necessarily
     * constant) values.
     */
    public static final class ArrayInitializer extends Rvalue {
        final ArrayType        arrayType;
        public final Rvalue[]  values;

        public ArrayInitializer(
            Location  location,
            ArrayType arrayType,
            Rvalue[]  values
        ) {
            super(location);
            this.arrayType = arrayType;
            this.values    = values;
        }

        // Implement "Atom".
        public String toString() { return "{ ... }"; }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitArrayInitializer(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitArrayInitializer(this); }
    }

    public static final class Literal extends Rvalue {
        public final Object value; // The "null" literal has "value == null".

        public Literal(Location location, Object value) {
            super(location);
            this.value = value;
        }

        // Implement "Atom".
        public String toString() { return Scanner.literalValueToString(this.value); }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitLiteral(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitLiteral(this); }
    }

    public static final class ConstantValue extends Rvalue {
        public final Object constantValue;

        public ConstantValue(Location location, Object constantValue) {
            super(location);
            this.constantValue = constantValue == null ? Rvalue.CONSTANT_VALUE_NULL : constantValue;
        }

        // Implement "Atom".
        public String toString() { return this.constantValue.toString(); }

        public final void accept(Visitor.AtomVisitor visitor) { visitor.visitConstantValue(this); }
        public final void accept(Visitor.RvalueVisitor visitor) { visitor.visitConstantValue(this); }
    }

    public static class LocalVariable {
        public LocalVariable(
            boolean            finaL,
            IClass             type,
            short              localVariableArrayIndex
        ) {
            this.finaL                   = finaL;
            this.type                    = type;
            this.localVariableArrayIndex = localVariableArrayIndex;
        }
        public final boolean finaL;
        public final IClass  type;
        public final short   localVariableArrayIndex;
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
