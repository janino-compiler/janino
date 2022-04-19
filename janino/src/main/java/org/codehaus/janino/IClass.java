
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
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

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A simplified equivalent to "java.lang.reflect".
 * <p>
 *   'JLS7' means a reference to the <a href="http://docs.oracle.com/javase/specs/">Java Language Specification, Java
 *   SE 7 Edition</a>.
 * </p>
 */
public abstract
class IClass implements ITypeVariableOrIClass {

    private static final Logger LOGGER = Logger.getLogger(IClass.class.getName());

    /**
     * Special return value for {@link IField#getConstantValue()} indicating that the field does <em>not</em> have a
     * constant value.
     */
    public static final Object NOT_CONSTANT = new Object() {
        @Override public String toString() { return "NOT_CONSTANT"; }
    };

    /**
     * The {@link IClass} object for the type VOID.
     */
    public static final IClass VOID    = new PrimitiveIClass(Descriptor.VOID);

    /**
     * The {@link IClass} object for the primitive type BYTE.
     */
    public static final IClass BYTE    = new PrimitiveIClass(Descriptor.BYTE);

    /**
     * The {@link IClass} object for the primitive type CHAR.
     */
    public static final IClass CHAR    = new PrimitiveIClass(Descriptor.CHAR);

    /**
     * The {@link IClass} object for the primitive type DOUBLE.
     */
    public static final IClass DOUBLE  = new PrimitiveIClass(Descriptor.DOUBLE);

    /**
     * The {@link IClass} object for the primitive type FLOAT.
     */
    public static final IClass FLOAT   = new PrimitiveIClass(Descriptor.FLOAT);

    /**
     * The {@link IClass} object for the primitive type INT.
     */
    public static final IClass INT     = new PrimitiveIClass(Descriptor.INT);

    /**
     * The {@link IClass} object for the primitive type LONG.
     */
    public static final IClass LONG    = new PrimitiveIClass(Descriptor.LONG);

    /**
     * The {@link IClass} object for the primitive type SHORT.
     */
    public static final IClass SHORT   = new PrimitiveIClass(Descriptor.SHORT);

    /**
     * The {@link IClass} object for the primitive type BOOLEAN.
     */
    public static final IClass BOOLEAN = new PrimitiveIClass(Descriptor.BOOLEAN);

    private static
    class PrimitiveIClass extends IClass {
        private final String fieldDescriptor;

        PrimitiveIClass(String fieldDescriptor) { this.fieldDescriptor = fieldDescriptor; }

        @Override protected ITypeVariable[]  getITypeVariables2()        { return new ITypeVariable[0]; }
        @Override @Nullable protected IClass getComponentType2()         { return null;                 }
        @Override protected IClass[]         getDeclaredIClasses2()      { return new IClass[0];        }
        @Override protected IConstructor[]   getDeclaredIConstructors2() { return new IConstructor[0];  }
        @Override protected IField[]         getDeclaredIFields2()       { return new IField[0];        }
        @Override protected IMethod[]        getDeclaredIMethods2()      { return new IMethod[0];       }
        @Override @Nullable protected IClass getDeclaringIClass2()       { return null;                 }
        @Override protected String           getDescriptor2()            { return this.fieldDescriptor; }
        @Override protected IClass[]         getInterfaces2()            { return new IClass[0];        }
        @Override @Nullable protected IClass getOuterIClass2()           { return null;                 }
        @Override @Nullable protected IClass getSuperclass2()            { return null;                 }
        @Override public boolean             isAbstract()                { return false;                }
        @Override public boolean             isArray()                   { return false;                }
        @Override public boolean             isFinal()                   { return true;                 }
        @Override public boolean             isEnum()                    { return false;                }
        @Override public boolean             isInterface()               { return false;                }
        @Override public boolean             isPrimitive()               { return true;                 }
        @Override public Access              getAccess()                 { return Access.PUBLIC;        }

        @Override public boolean
        isPrimitiveNumeric() { return Descriptor.isPrimitiveNumeric(this.fieldDescriptor); }
    }

    /**
     * @return Zero-length array if this {@link IClass} declares no type variables
     */
    public final ITypeVariable[]
    getITypeVariables() throws CompileException {
        if (this.iTypeVariablesCache != null) return this.iTypeVariablesCache;
        return (this.iTypeVariablesCache = this.getITypeVariables2());
    }
    @Nullable private ITypeVariable[] iTypeVariablesCache;

    /**
     * The uncached version of {@link #getDeclaredIConstructors()} which must be implemented by derived classes.
     */
    protected abstract ITypeVariable[] getITypeVariables2() throws CompileException;

    /**
     * Returns all the constructors declared by the class represented by the type. If the class has a default
     * constructor, it is included.
     * <p>
     *   Returns an array with zero elements for an interface, array, primitive type or {@code void}.
     * </p>
     */
    public final IConstructor[]
    getDeclaredIConstructors() {
        if (this.declaredIConstructorsCache != null) return this.declaredIConstructorsCache;

        return (this.declaredIConstructorsCache = this.getDeclaredIConstructors2());
    }
    @Nullable private IConstructor[] declaredIConstructorsCache;

    /**
     * The uncached version of {@link #getDeclaredIConstructors()} which must be implemented by derived classes.
     */
    protected abstract IConstructor[] getDeclaredIConstructors2();

    /**
     * Returns the methods of the class or interface (but not inherited methods). For covariant methods, only the
     * method with the most derived return type is included.
     * <p>
     *   Returns an empty array for an array, primitive type or {@code void}.
     * </p>
     */
    public final IMethod[]
    getDeclaredIMethods() {
        if (this.declaredIMethodsCache != null) return this.declaredIMethodsCache;
        return (this.declaredIMethodsCache = this.getDeclaredIMethods2());
    }
    @Nullable private IMethod[] declaredIMethodsCache;

    /**
     * The uncached version of {@link #getDeclaredIMethods()} which must be implemented by derived classes.
     */
    protected abstract IMethod[] getDeclaredIMethods2();

    /**
     * Returns all methods with the given name declared in the class or interface (but not inherited methods).
     * <p>
     *   Returns an empty array if no methods with that name are declared.
     * </p>
     *
     * @return an array of {@link IMethod}s that must not be modified
     */
    public final IMethod[]
    getDeclaredIMethods(String methodName) {
        Map<String, Object> dimc = this.declaredIMethodCache;
        if (dimc == null) {
            IMethod[] dims = this.getDeclaredIMethods();

            // Fill the map with "IMethod"s and "List<IMethod>"s.
            dimc = new HashMap<>();
            for (IMethod dim : dims) {
                String  mn  = dim.getName();
                Object  o   = dimc.get(mn);
                if (o == null) {
                    dimc.put(mn, dim);
                } else
                if (o instanceof IMethod) {
                    List<IMethod> l = new ArrayList<>();
                    l.add((IMethod) o);
                    l.add(dim);
                    dimc.put(mn, l);
                } else {
                    @SuppressWarnings("unchecked") List<IMethod> tmp = (List<IMethod>) o;
                    tmp.add(dim);
                }
            }

            // Convert "IMethod"s and "List"s to "IMethod[]"s.
            for (Map.Entry<String, Object/*IMethod-or-List<IMethod>*/> me : dimc.entrySet()) {
                Object v = me.getValue();
                if (v instanceof IMethod) {
                    me.setValue(new IMethod[] { (IMethod) v });
                } else {
                    @SuppressWarnings("unchecked") List<IMethod> l = (List<IMethod>) v;
                    me.setValue(l.toArray(new IMethod[l.size()]));
                }
            }
            this.declaredIMethodCache = dimc;
        }

        IMethod[] methods = (IMethod[]) dimc.get(methodName);
        return methods == null ? IClass.NO_IMETHODS : methods;
    }
    @Nullable private Map<String /*methodName*/, Object /*IMethod-or-List<IMethod>*/> declaredIMethodCache;

    /**
     * Returns all methods declared in the class or interface, its superclasses and its superinterfaces.
     *
     * @return an array of {@link IMethod}s that must not be modified
     */
    public final IMethod[]
    getIMethods() throws CompileException {

        if (this.iMethodCache != null) return this.iMethodCache;

        List<IMethod> iMethods = new ArrayList<>();
        this.getIMethods(iMethods);
        return (this.iMethodCache = (IMethod[]) iMethods.toArray(new IMethod[iMethods.size()]));
    }
    @Nullable private IMethod[] iMethodCache;

    private void
    getIMethods(List<IMethod> result) throws CompileException {
        IMethod[] ms = this.getDeclaredIMethods();

        SCAN_DECLARED_METHODS:
        for (IMethod candidate : ms) {
            MethodDescriptor candidateDescriptor = candidate.getDescriptor();
            String           candidateName       = candidate.getName();

            // Check if a method with the same name and descriptor has been added before.
            for (IMethod oldMethod : result) {
                if (
                    candidateName.equals(oldMethod.getName())
                    && candidateDescriptor.equals(oldMethod.getDescriptor())
                ) continue SCAN_DECLARED_METHODS;
            }
            result.add(candidate);
        }
        IClass sc = this.getSuperclass();
        if (sc != null) sc.getIMethods(result);

        for (IClass ii : this.getInterfaces()) ii.getIMethods(result);
    }

    private static final IMethod[] NO_IMETHODS = new IMethod[0];

    /**
     * @return Whether this {@link IClass} (or its superclass or the interfaces it implements) has an {@link IMethod}
     *         with the given name and parameter types
     */
    public final boolean
    hasIMethod(String methodName, IClass[] parameterTypes) throws CompileException {
        return this.findIMethod(methodName, parameterTypes) != null;
    }

    /**
     * @return The {@link IMethod} declared in this {@link IClass} (or its superclass or the interfaces it implements)
     *         with the given name and parameter types, or {@code null} if an applicable method could not be found
     */
    @Nullable public final IMethod
    findIMethod(String methodName, IClass[] parameterTypes) throws CompileException {
        {
            IMethod result = null;
            for (IMethod im : this.getDeclaredIMethods(methodName)) {
                if (
                    Arrays.equals(im.getParameterTypes(), parameterTypes)
                    && (result == null || result.getReturnType().isAssignableFrom(im.getReturnType()))
                ) result = im;
            }
            if (result != null) return result;
        }

        {
            IClass superclass = this.getSuperclass();
            if (superclass != null) {
                IMethod result = superclass.findIMethod(methodName, parameterTypes);
                if (result != null) return result;
            }
        }

        {
            IClass[] interfaces = this.getInterfaces();
            for (IClass interfacE : interfaces) {
                IMethod result = interfacE.findIMethod(methodName, parameterTypes);
                if (result != null) return result;
            }
        }

        return null;
    }

    /**
     * @return The {@link IConstructor} declared in this {@link IClass} with the given parameter types, or {@code null}
     *         if an applicable constructor could not be found
     */
    @Nullable public final IConstructor
    findIConstructor(IClass[] parameterTypes) throws CompileException {
        IConstructor[] ics = this.getDeclaredIConstructors();
        for (IConstructor ic : ics) {
            if (Arrays.equals(ic.getParameterTypes(), parameterTypes)) return ic;
        }

        return null;
    }

    /**
     * Returns the {@link IField}s declared in this {@link IClass} (but not inherited fields).
     *
     * @return An empty array for an array, primitive type or {@code void}
     */
    public final IField[]
    getDeclaredIFields() {
        Collection<IField> allFields = this.getDeclaredIFieldsCache().values();
        return (IField[]) allFields.toArray(new IField[allFields.size()]);
    }

    /**
     * @return {@code String fieldName => IField}
     */
    private Map<String /*fieldName*/, IField>
    getDeclaredIFieldsCache() {
        if (this.declaredIFieldsCache != null) return this.declaredIFieldsCache;

        IField[] fields = this.getDeclaredIFields2();

        Map<String /*fieldName*/, IField> m = new LinkedHashMap<>();
        for (IField f : fields) m.put(f.getName(), f);
        return (this.declaredIFieldsCache = m);
    }

    /**
     * Returns the named {@link IField} declared in this {@link IClass} (does not work for inherited fields).
     *
     * @return {@code null} iff this {@link IClass} does not declare an {@link IField} with that name
     */
    @Nullable public final IField
    getDeclaredIField(String name) { return (IField) this.getDeclaredIFieldsCache().get(name); }

    /**
     * Clears the cache of declared fields which this class maintains in order to minimize the invocations of {@link
     * #getDeclaredIFields2()}.
     */
    protected void
    clearIFieldCaches() { this.declaredIFieldsCache = null; }

    @Nullable private Map<String /*fieldName*/, IField> declaredIFieldsCache;

    /**
     * Uncached version of {@link #getDeclaredIFields()}.
     */
    protected abstract IField[] getDeclaredIFields2();

    /**
     * @return The synthetic fields of an anonymous or local class, in the order in which they are passed to all
     *         constructors
     */
    public IField[]
    getSyntheticIFields() { return new IField[0]; }

    /**
     * Returns the classes and interfaces declared as members of the class (but not inherited classes and interfaces).
     * <p>
     *   Returns an empty array for an array, primitive type or {@code void}.
     * </p>
     */
    public final IClass[]
    getDeclaredIClasses() throws CompileException {
        if (this.declaredIClassesCache != null) return this.declaredIClassesCache;
        return (this.declaredIClassesCache = this.getDeclaredIClasses2());
    }
    @Nullable private IClass[] declaredIClassesCache;

    /**
     * @return The member types of this type
     */
    protected abstract IClass[] getDeclaredIClasses2() throws CompileException;

    /**
     * @return If this class is a member class, the declaring class, otherwise {@code null}
     */
    @Nullable public final IClass
    getDeclaringIClass() throws CompileException {
        if (!this.declaringIClassIsCached) {
            this.declaringIClassCache    = this.getDeclaringIClass2();
            this.declaringIClassIsCached = true;
        }
        return this.declaringIClassCache;
    }
    private boolean          declaringIClassIsCached;
    @Nullable private IClass declaringIClassCache;

    /**
     * @return If this class is a member class, the declaring class, otherwise {@code null}
     */
    @Nullable protected abstract IClass
    getDeclaringIClass2() throws CompileException;

    /**
     * The following types have an "outer class":
     * <ul>
     *   <li>Anonymous classes declared in a non-static method of a class
     *   <li>Local classes declared in a non-static method of a class
     *   <li>Non-static member classes
     * </ul>
     *
     * @return The outer class of this type, or {@code null}
     */
    @Nullable public final IClass
    getOuterIClass() throws CompileException {
        if (this.outerIClassIsCached) return this.outerIClassCache;

        this.outerIClassIsCached = true;
        return (this.outerIClassCache = this.getOuterIClass2());
    }
    private boolean          outerIClassIsCached;
    @Nullable private IClass outerIClassCache;

    /**
     * @see #getOuterIClass()
     */
    @Nullable protected abstract IClass
    getOuterIClass2() throws CompileException;

    /**
     * Returns the superclass of the class.
     * <p>
     *   Returns {@code null} for class {@link Object}, interfaces, arrays, primitive types and {@code void}.
     * </p>
     */
    @Nullable public final IClass
    getSuperclass() throws CompileException {
        if (this.superclassIsCached) return this.superclassCache;

        IClass sc = this.getSuperclass2();
        if (sc != null && IClass.rawTypeOf(sc).isSubclassOf(this)) {
            throw new CompileException(
                "Class circularity detected for \"" + Descriptor.toClassName(this.getDescriptor()) + "\"",
                null
            );
        }
        this.superclassIsCached = true;
        return (this.superclassCache = sc);
    }
    private boolean          superclassIsCached;
    @Nullable private IClass superclassCache;

    /**
     * @see #getSuperclass()
     */
    @Nullable protected abstract IClass
    getSuperclass2() throws CompileException;

    /**
     * @return The accessibility of this type
     */
    public abstract Access getAccess();

    /**
     * Whether subclassing is allowed (JVMS 4.1 access_flags)
     *
     * @return {@code true} if subclassing is prohibited
     */
    public abstract boolean isFinal();

    /**
     * Returns the interfaces implemented by the class, respectively the superinterfaces of the interface, respectively
     * <code>{</code> {@link Cloneable}{@code ,} {@link Serializable} <code>}</code> for arrays.
     * <p>
     *   Returns an empty array for primitive types and {@code void}.
     * </p>
     */
    public final IClass[]
    getInterfaces() throws CompileException {
        if (this.interfacesCache != null) return this.interfacesCache;

        IClass[] is = this.getInterfaces2();
        for (IClass ii : is) {
            if (ii.implementsInterface(this)) {
                throw new CompileException(
                    "Interface circularity detected for \"" + Descriptor.toClassName(this.getDescriptor()) + "\"",
                    null
                );
            }
        }
        return (this.interfacesCache = is);
    }
    @Nullable private IClass[] interfacesCache;

    /**
     * @see #getInterfaces()
     */
    protected abstract IClass[] getInterfaces2() throws CompileException;

    /**
     * Whether the class may be instantiated (JVMS 4.1 access_flags).
     *
     * @return {@code true} if instantiation is prohibited
     */
    public abstract boolean isAbstract();

    /**
     * Returns the field descriptor for the type as defined by JVMS 4.3.2. This method is fast.
     */
    public final String
    getDescriptor() {
        if (this.descriptorCache != null) return this.descriptorCache;
        return (this.descriptorCache = this.getDescriptor2());
    }
    @Nullable private String descriptorCache;

    /**
     * @return The field descriptor for the type as defined by JVMS 4.3.2.
     */
    protected abstract String getDescriptor2();

    /**
     * Convenience method that determines the field descriptors of an array of {@link IClass}es.
     *
     * @see #getDescriptor()
     */
    public static String[]
    getDescriptors(IClass[] iClasses) {
        String[] descriptors = new String[iClasses.length];
        for (int i = 0; i < iClasses.length; ++i) descriptors[i] = iClasses[i].getDescriptor();
        return descriptors;
    }

    /**
     * @return Whether this type represents an enum
     */
    public abstract boolean isEnum();

    /**
     * @return Whether this type represents an interface
     */
    public abstract boolean isInterface();

    /**
     * @return Whether  this type represents an array
     */
    public abstract boolean isArray();

    /**
     * @return Whether this type represents a primitive type or {@code void}
     */
    public abstract boolean isPrimitive();

    /**
     * @return Whether this type represents {@code byte}, {@code short}, {@code int}, {@code long}, {@code char},
     *         {@code float} or {@code double}
     */
    public abstract boolean isPrimitiveNumeric();

    /**
     * @return The component type of the array, or {@code null} for classes, interfaces, primitive types and {@code
     *         void}
     */
    @Nullable public final IClass
    getComponentType() {
        if (this.componentTypeIsCached) return this.componentTypeCache;

        this.componentTypeCache    = this.getComponentType2();
        this.componentTypeIsCached = true;
        return this.componentTypeCache;
    }
    private boolean          componentTypeIsCached;
    @Nullable private IClass componentTypeCache;

    /**
     * @see #getComponentType()
     */
    @Nullable protected abstract IClass
    getComponentType2();

    @Override public String
    toString() {
        String className = Descriptor.toClassName(this.getDescriptor());
        if (className.startsWith("java.lang.") && className.indexOf('.', 10) == -1) className = className.substring(10);
        return className;
    }

    /**
     * Determines if {@code this} is assignable from <var>that</var>. This is true if {@code this} is identical with
     * <var>that</var> (JLS7 5.1.1), or if <var>that</var> is widening-primitive-convertible to {@code this} (JLS7
     * 5.1.2), or if <var>that</var> is widening-reference-convertible to {@code this} (JLS7 5.1.5).
     */
    public boolean
    isAssignableFrom(IClass that) throws CompileException {

        // Identity conversion, JLS7 5.1.1
        if (this == that) return true;

        // Widening primitive conversion, JLS7 5.1.2
        {
            String ds = that.getDescriptor() + this.getDescriptor();
            if (ds.length() == 2 && IClass.PRIMITIVE_WIDENING_CONVERSIONS.contains(ds)) return true;
        }

        // Widening reference conversion, JLS7 5.1.5
        {

            // JLS7 5.1.4.1: Target type is superclass of source class type.
            if (that.isSubclassOf(this)) return true;

            // JLS7 5.1.4.2: Source class type implements target interface type.
            // JLS7 5.1.4.4: Source interface type implements target interface type.
            if (that.implementsInterface(this)) return true;

            // JLS7 5.1.4.3 Convert "null" literal to any reference type.
            if (that == IClass.VOID && !this.isPrimitive()) return true;

            // JLS7 5.1.4.5: From any interface to type "Object".
            if (that.isInterface() && this.getDescriptor().equals(Descriptor.JAVA_LANG_OBJECT)) return true;

            if (that.isArray()) {

                // JLS7 5.1.4.6: From any array type to type "Object".
                if (this.getDescriptor().equals(Descriptor.JAVA_LANG_OBJECT)) return true;

                // JLS7 5.1.4.7: From any array type to type "Cloneable".
                if (this.getDescriptor().equals(Descriptor.JAVA_LANG_CLONEABLE)) return true;

                // JLS7 5.1.4.8: From any array type to type "java.io.Serializable".
                if (this.getDescriptor().equals(Descriptor.JAVA_IO_SERIALIZABLE)) return true;

                // JLS7 5.1.4.9: From SC[] to TC[] while SC if widening reference convertible to TC.
                if (this.isArray()) {
                    IClass thisCt = this.getComponentType();
                    IClass thatCt = that.getComponentType();

                    assert thisCt != null;
                    assert thatCt != null;

                    if (!thisCt.isPrimitive() && thisCt.isAssignableFrom(thatCt)) return true;
                }
            }
        }
        return false;
    }

    private static final Set<String> PRIMITIVE_WIDENING_CONVERSIONS = new HashSet<>();
    static {
        String[] pwcs = {
            Descriptor.BYTE  + Descriptor.SHORT,

            Descriptor.BYTE  + Descriptor.INT,
            Descriptor.SHORT + Descriptor.INT,
            Descriptor.CHAR  + Descriptor.INT,

            Descriptor.BYTE  + Descriptor.LONG,
            Descriptor.SHORT + Descriptor.LONG,
            Descriptor.CHAR  + Descriptor.LONG,
            Descriptor.INT   + Descriptor.LONG,

            Descriptor.BYTE  + Descriptor.FLOAT,
            Descriptor.SHORT + Descriptor.FLOAT,
            Descriptor.CHAR  + Descriptor.FLOAT,
            Descriptor.INT   + Descriptor.FLOAT,

            Descriptor.LONG  + Descriptor.FLOAT,

            Descriptor.BYTE  + Descriptor.DOUBLE,
            Descriptor.SHORT + Descriptor.DOUBLE,
            Descriptor.CHAR  + Descriptor.DOUBLE,
            Descriptor.INT   + Descriptor.DOUBLE,

            Descriptor.LONG  + Descriptor.DOUBLE,

            Descriptor.FLOAT + Descriptor.DOUBLE,
        };
        for (String pwc : pwcs) IClass.PRIMITIVE_WIDENING_CONVERSIONS.add(pwc);
    }

    /**
     * Returns {@code true} if this class is an immediate or non-immediate subclass of {@code that} class.
     */
    public boolean
    isSubclassOf(IClass that) throws CompileException {
        for (IClass sc = this.getSuperclass(); sc != null; sc = sc.getSuperclass()) {
            if (sc == that) return true;
        }
        return false;
    }

    /**
     * If {@code this} represents a class: Return {@code true} if this class directly or indirectly implements {@code
     * that} interface.
     * <p>
     *   If {@code this} represents an interface: Return {@code true} if this interface directly or indirectly extends
     *   {@code that} interface.
     * </p>
     */
    public boolean
    implementsInterface(IClass that) throws CompileException {
        for (IClass c = this; c != null; c = c.getSuperclass()) {
            IClass[] tis = c.getInterfaces();
            for (IClass ti : tis) {
                if (ti == that || ti.implementsInterface(that)) return true;
            }
        }
        return false;
    }

    /**
     * Gets an {@link IClass} that represents an n-dimensional array of this type.
     *
     * @param n dimension count
     * @param objectType Required because the superclass of an array class is {@link Object} by definition
     */
    public IClass
    getArrayIClass(int n, IClass objectType) {
        IClass result = this;
        for (int i = 0; i < n; ++i) result = result.getArrayIClass(objectType);
        return result;
    }

    /**
     * Gets an {@link IClass} that represents an array of this type.
     *
     * @param objectType Required because the superclass of an array class is {@link Object} by definition
     */
    public synchronized IClass
    getArrayIClass(IClass objectType) {
        if (this.arrayIClass != null) return this.arrayIClass;
        return (this.arrayIClass = this.getArrayIClass2(objectType));
    }
    @Nullable private IClass arrayIClass;

    /**
     * @param objectType Must pass {@link IClassLoader#TYPE_java_lang_Object} here
     */
    private IClass
    getArrayIClass2(final IClass objectType) {

        final IClass componentType = this;

        return new IClass() {

            @Override @Nullable protected ITypeVariable[] getITypeVariables2()        { return new ITypeVariable[0];       }
            @Override public IClass.IConstructor[]        getDeclaredIConstructors2() { return new IClass.IConstructor[0]; }

            // Special trickery #17: Arrays override "Object.clone()", but without "throws
            // CloneNotSupportedException"!
            @Override public IClass.IMethod[]
            getDeclaredIMethods2() {
                return new IClass.IMethod[] {
                    new IMethod() {
                        @Override public IAnnotation[] getAnnotations()       { return new IAnnotation[0]; }
                        @Override public Access        getAccess()            { return Access.PUBLIC;      }
                        @Override public boolean       isStatic()             { return false;              }
                        @Override public boolean       isAbstract()           { return false;              }
                        @Override public IClass        getReturnType()        { return objectType;         }
                        @Override public String        getName()              { return "clone";            }
                        @Override public IClass[]      getParameterTypes2()   { return new IClass[0];      }
                        @Override public boolean       isVarargs()            { return false;              }
                        @Override public IClass[]      getThrownExceptions2() { return new IClass[0];      }
                    }
                };
            }

            @Override public IClass.IField[]  getDeclaredIFields2()  { return new IClass.IField[0];                }
            @Override public IClass[]         getDeclaredIClasses2() { return new IClass[0];                       }
            @Override @Nullable public IClass getDeclaringIClass2()  { return null;                                }
            @Override @Nullable public IClass getOuterIClass2()      { return null;                                }
            @Override public IClass           getSuperclass2()       { return objectType;                          }
            @Override public IClass[]         getInterfaces2()       { return new IClass[0];                       }
            @Override public String           getDescriptor2()       { return '[' + componentType.getDescriptor(); }
            @Override public Access           getAccess()            { return componentType.getAccess();           }
            @Override public boolean          isFinal()              { return true;                                }
            @Override public boolean          isEnum()               { return false;                               }
            @Override public boolean          isInterface()          { return false;                               }
            @Override public boolean          isAbstract()           { return false;                               }
            @Override public boolean          isArray()              { return true;                                }
            @Override public boolean          isPrimitive()          { return false;                               }
            @Override public boolean          isPrimitiveNumeric()   { return false;                               }
            @Override public IClass           getComponentType2()    { return componentType;                       }

            @Override public String toString() { return componentType.toString() + "[]"; }
        };
    }

    /**
     * If <var>name</var> is {@code null}, finds all {@link IClass}es visible in the scope of the current
     * class.
     * <p>
     *   If <var>name</var> is not {@code null}, finds the member {@link IClass}es that has the given name. If
     *   the name is ambiguous (i.e. if more than one superclass, interface of enclosing type declares a type with that
     *   name), then the size of the returned array is greater than one.
     * </p>
     * <p>
     *   Examines superclasses, interfaces and enclosing type declarations.
     * </p>
     *
     * @return an array of {@link IClass}es in unspecified order, possibly of length zero
     */
    IClass[]
    findMemberType(@Nullable String name) throws CompileException {
        IClass[] res = (IClass[]) this.memberTypeCache.get(name);
        if (res == null) {

            // Notice: A type may be added multiply to the result set because we are in its scope
            // multiply. E.g. the type is a member of a superclass AND a member of an enclosing type.
            Set<IClass> s = new HashSet<>();
            this.findMemberType(name, s);
            res = s.isEmpty() ? IClass.ZERO_ICLASSES : (IClass[]) s.toArray(new IClass[s.size()]);

            this.memberTypeCache.put(name, res);
        }

        return res;
    }
    private final Map<String /*name*/, IClass[]> memberTypeCache = new HashMap<>();
    private static final IClass[]                ZERO_ICLASSES   = new IClass[0];
    private void
    findMemberType(@Nullable String name, Collection<IClass> result) throws CompileException {

        // Search for a type with the given name in the current class.
        IClass[] memberTypes = this.getDeclaredIClasses();
        if (name == null) {
            result.addAll(Arrays.asList(memberTypes));
        } else {
            String memberDescriptor = Descriptor.fromClassName(
                Descriptor.toClassName(this.getDescriptor())
                + '$'
                + name
            );
            for (final IClass mt : memberTypes) {
                if (mt.getDescriptor().equals(memberDescriptor)) {
                    result.add(mt);
                    return;
                }
            }
        }

        // Examine superclass.
        {
            IClass superclass = this.getSuperclass();
            if (superclass != null) superclass.findMemberType(name, result);
        }

        // Examine interfaces.
        for (IClass i : this.getInterfaces()) i.findMemberType(name, result);

        // Examine enclosing type declarations.
        {
            IClass declaringIClass = this.getDeclaringIClass();
            IClass outerIClass     = this.getOuterIClass();
            if (declaringIClass != null) {
                declaringIClass.findMemberType(name, result);
            }
            if (outerIClass != null && outerIClass != declaringIClass) {
                outerIClass.findMemberType(name, result);
            }
        }
    }

    /**
     * @return The annotations of this type (possibly the empty array)
     */
    public final IAnnotation[]
    getIAnnotations() throws CompileException {
        if (this.iAnnotationsCache != null) return this.iAnnotationsCache;
        return (this.iAnnotationsCache = this.getIAnnotations2());
    }
    @Nullable private IAnnotation[] iAnnotationsCache;

    /**
     * @throws CompileException
     */
    protected IAnnotation[]
    getIAnnotations2() throws CompileException { return IClass.NO_ANNOTATIONS; }

    /**
     * Array of zero {@link IAnnotation}s.
     */
    public static final IAnnotation[] NO_ANNOTATIONS = new IAnnotation[0];

    /**
     * Base for the members of an {@link IClass}. {@link IMember} are expected to be immutable, i.e. all getter methods
     * return constant values.
     */
    public
    interface IMember {

        /**
         * @return One of {@link Access#PRIVATE}, {@link Access#PROTECTED}, {@link Access#DEFAULT} and {@link
         *         Access#PUBLIC}.
         */
        Access getAccess();

        /**
         * @return Modifiers and/or annotations of this member
         */
        IAnnotation[] getAnnotations();

        /**
         * @return The {@link IClass} that declares this {@link IClass.IMember}
         */
        IClass getDeclaringIClass();
    }

    /**
     * Base class for {@link IConstructor} and {@link IMethod}.
     */
    public abstract
    class IInvocable implements IMember {

        private boolean argsNeedAdjust;

        /**
         * TODO
         */
        public void
        setArgsNeedAdjust(boolean newVal) { this.argsNeedAdjust = newVal; }

        /**
         * TODO
         */
        public boolean
        argsNeedAdjust() { return this.argsNeedAdjust; }

        /**
         * @return Whether this invocable is 'variable arity', i.e. its last parameter has an ellipsis ('...') after
         *         the type
         */
        public abstract boolean isVarargs();

        // Implement IMember.

        @Override public IClass getDeclaringIClass() { return IClass.this; }

        /**
         * Returns the types of the parameters of this constructor or method. This method is fast.
         */
        public final IClass[]
        getParameterTypes() throws CompileException {
            if (this.parameterTypesCache != null) return this.parameterTypesCache;
            return (this.parameterTypesCache = this.getParameterTypes2());
        }
        @Nullable private IClass[] parameterTypesCache;

        /**
         * Opposed to the {@link Constructor}, there is no magic "{@code this$0}" parameter.
         * <p>
         *   Opposed to the {@link Constructor}, {@code enum}s have no magic parameters "{@code String name}" and
         *   "{@code int ordinal}".
         * </p>
         * <p>
         *   However, the "synthetic parameters" ("{@code val$}<var>locvar</var>") <em>are</em> included.
         * </p>
         */
        public abstract IClass[]
        getParameterTypes2() throws CompileException;

        /**
         * Returns the method descriptor of this constructor or method. This method is fast.
         */
        public final MethodDescriptor
        getDescriptor() throws CompileException {
            if (this.descriptorCache != null) return this.descriptorCache;
            return (this.descriptorCache = this.getDescriptor2());
        }
        @Nullable private MethodDescriptor descriptorCache;

        /**
         * Uncached implementation of {@link #getDescriptor()}.
         */
        public abstract MethodDescriptor
        getDescriptor2() throws CompileException;

        /**
         * Returns the types thrown by this constructor or method. This method is fast.
         */
        public final IClass[]
        getThrownExceptions() throws CompileException {
            if (this.thrownExceptionsCache != null) return this.thrownExceptionsCache;
            return (this.thrownExceptionsCache = this.getThrownExceptions2());
        }
        @Nullable private IClass[] thrownExceptionsCache;

        /**
         * @return The types thrown by this constructor or method
         */
        public abstract IClass[]
        getThrownExceptions2() throws CompileException;

        /**
         * @return Whether this {@link IInvocable} is more specific then <var>that</var> (in the sense of JLS7
         *         15.12.2.5)
         */
        public boolean
        isMoreSpecificThan(IInvocable that) throws CompileException {
            IClass.LOGGER.entering(null, "isMoreSpecificThan", that);

            // a variable-length argument is always less specific than a fixed arity.
            final boolean thatIsVarargs;

            if ((thatIsVarargs = that.isVarargs()) != this.isVarargs()) {

                // Only one of the two is varargs.
                return thatIsVarargs;
            } else
            if (thatIsVarargs) {

                // Both are varargs.
                final IClass[] thisParameterTypes = this.getParameterTypes();
                final IClass[] thatParameterTypes = that.getParameterTypes();

                IClass[] t, u;
                int      n, k;

                if (thisParameterTypes.length >= thatParameterTypes.length) {
                    t = thisParameterTypes;
                    u = thatParameterTypes;
                    n = t.length;
                    k = u.length;
                    IClass[] s = u;
                    // this = T | T_n
                    // that = U | U_k
                    // n >= k
                    //              ignore generics, for now

                    // T0, T1, ..., Tn-1, Tn[]
                    // U0, U1, .., Uk[]
                    final int kMinus1 = k - 1;
                    for (int j = 0; j < kMinus1; ++j) {
                        // expect T[j] <: S[j]
                        if (!s[j].isAssignableFrom(t[j])) {
                            return false;
                        }
                    }

                    final IClass sk1 = s[kMinus1].getComponentType();
                    assert sk1 != null;

                    final int nMinus1 = n - 1;
                    for (int j = kMinus1; j < nMinus1; ++j) {
                        // expect T[j] <: S[k -1]
                        if (!sk1.isAssignableFrom(t[j])) {
                            return false;
                        }
                    }
                    if (!sk1.isAssignableFrom(t[nMinus1])) {
                        return false;
                    }
                } else {
                    u = thisParameterTypes;
                    t = thatParameterTypes;
                    n = t.length;
                    k = u.length;
                    IClass[] s = t;
                    // n >= k
                    final int kMinus1 = k - 1;
                    for (int j = 0; j < kMinus1; ++j) {
                        // expect U[j] <: S[j]
                        if (!s[j].isAssignableFrom(u[j])) {
                            return false;
                        }
                    }

                    final IClass uk1 = u[kMinus1].getComponentType();
                    assert uk1 != null;

                    final int nMinus1 = n - 1;
                    for (int j = kMinus1; j < nMinus1; ++j) {
                        // expect U[k -1] <: S[j]
                        if (!s[j].isAssignableFrom(uk1)) {
                            return false;
                        }
                    }
                    IClass snm1ct = s[nMinus1].getComponentType();
                    assert snm1ct != null;
                    if (!snm1ct.isAssignableFrom(uk1)) {
                        return false;
                    }
                }

                return true;
            }

            // both are fixed arity

            // The following case is tricky: JLS7 says that the invocation is AMBIGUOUS, but only JAVAC 1.2 issues an
            // error; JAVAC 1.4.1, 1.5.0 and 1.6.0 obviously ignore the declaring type and invoke "A.meth(String)".
            // JLS7 is not clear about this. For compatibility with JAVA 1.4.1, 1.5.0 and 1.6.0, JANINO also ignores
            // the declaring type.
            //
            // See also JANINO-79 and JlsTests / 15.12.2.2
            // if (false) {
            //     if (!that.getDeclaringIClass().isAssignableFrom(this.getDeclaringIClass())) {
            //         if (IClass.DEBUG) System.out.println("falsE");
            //         return false;
            //     }
            // }

            IClass[] thisParameterTypes = this.getParameterTypes();
            IClass[] thatParameterTypes = that.getParameterTypes();
            for (int i = 0; i < thisParameterTypes.length; ++i) {
                if (!thatParameterTypes[i].isAssignableFrom(thisParameterTypes[i])) {
                    IClass.LOGGER.exiting(null, "isMoreSpecificThan", false);
                    return false;
                }
            }

            boolean result = !Arrays.equals(thisParameterTypes, thatParameterTypes);
            IClass.LOGGER.exiting(null, "isMoreSpecificThan", result);
            return result;
        }

        /**
         * @return Whether this {@link IInvocable} is less specific then <var>that</var> (in the sense of JLS7
         *         15.12.2.5)
         */
        public boolean
        isLessSpecificThan(IInvocable that) throws CompileException { return that.isMoreSpecificThan(this); }

        @Override public abstract String
        toString();
    }

    /**
     * Representation of a constructor of an {@link IClass}.
     */
    public abstract
    class IConstructor extends IInvocable {

        @Override public MethodDescriptor
        getDescriptor2() throws CompileException {

            IClass[] parameterTypes = this.getParameterTypes();

            // Iff this is an inner class, prepend the magic "this$0" constructor parameter.
            {
                IClass outerIClass = IClass.this.getOuterIClass();
                if (outerIClass != null) {
                    IClass[] tmp = new IClass[parameterTypes.length + 1];
                    tmp[0] = outerIClass;
                    System.arraycopy(parameterTypes, 0, tmp, 1, parameterTypes.length);
                    parameterTypes = tmp;
                }
            }

            String[] parameterFds = IClass.getDescriptors(parameterTypes);

            // Iff this is an enum, prepend the magic "String name" and "int ordinal" constructor parameters.
            if (this.getDeclaringIClass().isEnum()) {
                String[] tmp = new String[parameterFds.length + 2];
                tmp[0] = Descriptor.JAVA_LANG_STRING;
                tmp[1] = Descriptor.INT;
                System.arraycopy(parameterFds, 0, tmp, 2, parameterFds.length);
                parameterFds = tmp;
            }

            return new MethodDescriptor(Descriptor.VOID, parameterFds);
        }

        @Override public String
        toString() {
            StringBuilder sb = new StringBuilder(this.getDeclaringIClass().toString());
            sb.append('(');
            try {
                IClass[] parameterTypes = this.getParameterTypes();
                for (int i = 0; i < parameterTypes.length; ++i) {
                    if (i > 0) sb.append(", ");
                    sb.append(parameterTypes[i].toString());
                }
            } catch (CompileException ex) {
                sb.append("<invalid type>");
            }
            sb.append(')');
            return sb.toString();
        }
    }

    /**
     * Representation of a method in an {@link IClass}.
     */
    public abstract
    class IMethod extends IInvocable {

        /**
         * @return Whether this method is STATIC
         */
        public abstract boolean isStatic();

        /**
         * @return Whether this method is ABSTRACT
         */
        public abstract boolean isAbstract();

        /**
         * @return The return type of this method
         */
        public abstract IClass getReturnType() throws CompileException;

        /**
         * @return The name of this method
         */
        public abstract String getName();

        @Override public MethodDescriptor
        getDescriptor2() throws CompileException {
            return new MethodDescriptor(
                this.getReturnType().getDescriptor(),
                IClass.getDescriptors(this.getParameterTypes())
            );
        }

        @Override public String
        toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getAccess().toString()).append(' ');
            if (this.isStatic()) sb.append("static ");
            if (this.isAbstract()) sb.append("abstract ");
            try {
                sb.append(this.getReturnType().toString());
            } catch (CompileException ex) {
                sb.append("<invalid type>");
            }
            sb.append(' ');
            sb.append(this.getDeclaringIClass().toString());
            sb.append('.');
            sb.append(this.getName());
            sb.append('(');
            try {
                IClass[] parameterTypes = this.getParameterTypes();
                for (int i = 0; i < parameterTypes.length; ++i) {
                    if (i > 0) sb.append(", ");
                    sb.append(parameterTypes[i].toString());
                }
            } catch (CompileException ex) {
                sb.append("<invalid type>");
            }
            sb.append(')');
            try {
                IClass[] tes = this.getThrownExceptions();
                if (tes.length > 0) {
                    sb.append(" throws ").append(tes[0]);
                    for (int i = 1; i < tes.length; ++i) sb.append(", ").append(tes[i]);
                }
            } catch (CompileException ex) {
                sb.append("<invalid thrown exception type>");
            }
            return sb.toString();
        }
    }

    /**
     * Representation of a field of this {@link IClass}.
     */
    public abstract
    class IField implements IMember {

        // Implement IMember.
        @Override public abstract Access getAccess();
        @Override public IClass          getDeclaringIClass() { return IClass.this; }

        /**
         * @return Whether this field is STATIC
         */
        public abstract boolean isStatic();

        /**
         * @return The type of this field
         */
        public abstract IClass getType() throws CompileException;

        /**
         * @return The name this field
         */
        public abstract String getName();

        /**
         * @return The descriptor of this field
         */
        public String getDescriptor() throws CompileException {
            return IClass.rawTypeOf(this.getType()).getDescriptor();
        }

        /**
         * Returns the value of the field if it is a compile-time constant value, i.e. the field is FINAL and its
         * initializer is a constant expression (JLS7 15.28, bullet 12).
         */
        @Nullable public abstract Object getConstantValue() throws CompileException;

        @Override public String
        toString() { return this.getDeclaringIClass().toString() + "." + this.getName(); }
    }

    /**
     * Representation of a Java "annotation".
     */
    public
    interface IAnnotation {

        /**
         * @return The type of the annotation
         */
        IType getAnnotationType() throws CompileException;

        /**
         * Returns the value of the <var>name</var>d element:
         * <dl>
         *   <dt>{@link Boolean}</dt>
         *   <dt>{@link Byte}</dt>
         *   <dt>{@link Character}</dt>
         *   <dt>{@link Double}</dt>
         *   <dt>{@link Float}</dt>
         *   <dt>{@link Integer}</dt>
         *   <dt>{@link Long}</dt>
         *   <dt>{@link Short}</dt>
         *   <dd>
         *     A primitive value
         *   </dd>
         *   <dt>{@link String}</dt>
         *   <dd>
         *     A string value
         *   </dd>
         *   <dt>{@link IField}</dt>
         *   <dd>
         *     An enum constant
         *   </dd>
         *   <dt>{@link IClass}</dt>
         *   <dd>
         *     A class literal
         *   </dd>
         *   <dt>{@link IAnnotation}</dt>
         *   <dd>
         *     An annotation
         *   </dd>
         *   <dt>{@link Object}{@code []}</dt>
         *   <dd>
         *     An array value
         *   </dd>
         * </dl>
         * <p>
         *   Notice that {@code null} is <em>not</em> a valid return value.
         * </p>
         */
        Object getElementValue(String name) throws CompileException;
    }

    /**
     * This class caches the declared methods in order to minimize the invocations of {@link #getDeclaredIMethods2()}.
     */
    public void
    invalidateMethodCaches() {
        this.declaredIMethodsCache = null;
        this.declaredIMethodCache  = null;
    }

    public static IClass
    rawTypeOf(IType type) {
        while (type instanceof IParameterizedType) type = ((IParameterizedType) type).getRawType();
        assert type instanceof IClass;
        return (IClass) type;
    }

//    /**
//     * @param typeArguments Zero-length array if this {@link IClass} is not parameterized
//     */
//    public IType
//    parameterize(final IType[] typeArguments) throws CompileException {
//
//        ITypeVariable[] iTypeVariables = this.getITypeVariables();
//
//        if (typeArguments.length == 0 && iTypeVariables.length == 0) return this;
//
//        if (iTypeVariables.length == 0) {
//            throw new CompileException("Class \"" + this.toString() + "\" cannot be parameterized", null);
//        }
//
//        if (typeArguments.length == 0) {
//            // throw new CompileException("Class \"" + this.toString() + "\" must be parameterized", null);
//            return this;
//        }
//
//        List<IType> key = Arrays.asList(typeArguments);
//
//        Map<List<IType> /*typeArguments*/, IParameterizedType> m = this.parameterizations;
//        if (m == null) this.parameterizations = (m = new HashMap<List<IType>, IParameterizedType>());
//
//        {
//            IType result = m.get(key);
//            if (result != null) return result;
//        }
//
//        if (iTypeVariables.length != typeArguments.length) {
//            throw new CompileException((
//                "Number of type arguments ("
//                + typeArguments.length
//                + ") does not match number of type variables ("
//                + iTypeVariables.length
//                + ") of class \""
//                + this.toString()
//                + "\""
//            ), null);
//        }
//
//        for (int i = 0; i < iTypeVariables.length; i++) {
//            ITypeVariable tv = iTypeVariables[i];
//            IType         ta = typeArguments[i];
//
//            for (ITypeVariableOrIClass b : tv.getBounds()) {
//                assert b instanceof IClass;
//
//                if (ta instanceof IClass) {
//                    IClass taic = (IClass) ta;
//                    if (!(ta == b || taic.isInterface() || taic.isSubclassOf((IClass) b))) {
//                        throw new CompileException("Type argument #" + (1 + i) + " (" + ta + ") is not a subclass of bound " + b, null);
//                    }
//                } else
//                if (ta instanceof IWildcardType) {
//                    IWildcardType tawt = (IWildcardType) ta;
//                    IClass ub = (IClass) tawt.getUpperBound();
//                    if (!(ub == b || ub.isSubclassOf((IClass) b))) {
//                        throw new CompileException("Type argument #" + (1 + i) + " (" + ta + ") is not a subclass of " + b, null);
//                    }
//                    IClass lb = (IClass) tawt.getLowerBound();
//                    if (lb != null && !"Ljava/lang/Object;".equals(((IClass) b).getDescriptor())) {
//                        throw new CompileException("Type argument #" + (1 + i) + " (" + ta + ") is not a subclass of " + b, null);
//                    }
//                } else
//                if (ta instanceof ITypeVariable) {
//                    throw new AssertionError(ta.getClass() + ": " + ta);
//                } else
//                if (ta instanceof IParameterizedType) {
//                    throw new AssertionError(ta.getClass() + ": " + ta);
//                } else
//                {
//                    throw new AssertionError(ta.getClass() + ": " + ta);
//                }
//            }
//        }
//
//        IParameterizedType result = new IParameterizedType() {
//
//            @Override public IType[] getActualTypeArguments() { return typeArguments; }
//            @Override public IType getRawType() { return IClass.this; }
//
//            @Override public String
//            toString() {
//                return IClass.this + "<" + Arrays.toString(typeArguments) + ">";
//            }
//        };
//
//        m.put(key, result);
//
//        return result;
//    }
//    @Nullable Map<List<IType> /*typeArguments*/, IParameterizedType> parameterizations;
}
