
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2005, Arno Unkrig
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A simplified equivalent to "java.lang.reflect".
 */
public abstract class IClass {
    private static final boolean DEBUG = false;

    public static final IClass VOID    = new PrimitiveIClass(Descriptor.VOID);
    public static final IClass BYTE    = new PrimitiveIClass(Descriptor.BYTE);
    public static final IClass CHAR    = new PrimitiveIClass(Descriptor.CHAR);
    public static final IClass DOUBLE  = new PrimitiveIClass(Descriptor.DOUBLE);
    public static final IClass FLOAT   = new PrimitiveIClass(Descriptor.FLOAT);
    public static final IClass INT     = new PrimitiveIClass(Descriptor.INT);
    public static final IClass LONG    = new PrimitiveIClass(Descriptor.LONG);
    public static final IClass SHORT   = new PrimitiveIClass(Descriptor.SHORT);
    public static final IClass BOOLEAN = new PrimitiveIClass(Descriptor.BOOLEAN);

    private static class PrimitiveIClass extends IClass {
        private final String fieldDescriptor;

        public PrimitiveIClass(String fieldDescriptor) {
            this.fieldDescriptor = fieldDescriptor;
        }
        protected IClass         getComponentType2()         { return null; }
        protected IClass[]       getDeclaredIClasses2()      { return new IClass[0]; }
        protected IConstructor[] getDeclaredIConstructors2() { return new IConstructor[0]; }
        protected IField[]       getDeclaredIFields2()       { return new IField[0]; }
        protected IMethod[]      getDeclaredIMethods2()      { return new IMethod[0]; }
        protected IClass         getDeclaringIClass2()       { return null; }
        protected String         getDescriptor2()            { return this.fieldDescriptor; }
        protected IClass[]       getInterfaces2()            { return new IClass[0]; }
        protected IClass         getOuterIClass2()           { return null; }
        protected IClass         getSuperclass2()            { return null; }
        public boolean           isAbstract()                { return false; }
        public boolean           isArray()                   { return false; }
        public boolean           isFinal()                   { return true; }
        public boolean           isInterface()               { return false; }
        public boolean           isPrimitive()               { return true; }
        public boolean           isPrimitiveNumeric()        { return Descriptor.isPrimitiveNumeric(this.fieldDescriptor); }
        public boolean           isPublic()                  { return true; }
}

    /**
     * Returns all the constructors declared by the class represented by the
     * type. If the class has a default constructor, it is included.
     * <p>
     * Returns an array with zero elements for an interface, array, primitive type or
     * "void".
     */
    public final IConstructor[] getDeclaredIConstructors() {
        if (this.declaredIConstructors == null) {
            this.declaredIConstructors = this.getDeclaredIConstructors2();
        }
        return this.declaredIConstructors;
    }
    private IConstructor[] declaredIConstructors = null;
    protected abstract IConstructor[] getDeclaredIConstructors2();

    /**
     * Returns the methods of the class or interface (but not inherited
     * methods).<br>
     * Returns an empty array for an array, primitive type or "void".
     */
    public final IMethod[] getDeclaredIMethods() {
        if (this.declaredIMethods == null) {
            this.declaredIMethods = this.getDeclaredIMethods2();
        }
        return this.declaredIMethods;
    }
    protected IMethod[] declaredIMethods = null;
    protected abstract IMethod[] getDeclaredIMethods2();

    /**
     * Returns the fields of a class or interface (but not inherited
     * fields).<br>
     * Returns an empty array for an array, primitive type or "void".
     */
    public final IField[] getDeclaredIFields() {
        if (this.declaredIFields == null) {
            this.declaredIFields = this.getDeclaredIFields2();
        }
        return this.declaredIFields;
    }
    private IField[] declaredIFields = null;
    protected abstract IField[] getDeclaredIFields2();

    /**
     * Returns the classes and interfaces declared as members of the class
     * (but not inherited classes and interfaces).<br>
     * Returns an empty array for an array, primitive type or "void".
     */
    public final IClass[] getDeclaredIClasses() throws Java.CompileException {
        if (this.declaredIClasses == null) {
            this.declaredIClasses = this.getDeclaredIClasses2();
        }
        return this.declaredIClasses;
    }
    private IClass[] declaredIClasses = null;
    protected abstract IClass[] getDeclaredIClasses2() throws Java.CompileException;

    /**
     * If this class is a member class, return the declaring class, otherwise return
     * <code>null</code>.
     */
    public final IClass getDeclaringIClass() throws Java.CompileException {
        if (!this.declaringIClassIsCached) {
            this.declaringIClass = this.getDeclaringIClass2();
            this.declaringIClassIsCached = true;
        }
        return this.declaringIClass;
    }
    private boolean declaringIClassIsCached = false;
    private IClass declaringIClass = null;
    protected abstract IClass getDeclaringIClass2() throws Java.CompileException;

    /**
     * The following types have an "outer class":
     * <ul>
     *   <li>Anonymous classes declared in a non-static method of a class
     *   <li>Local classes declared in a non-static method of a class
     *   <li>Non-static member classes
     * </ul>
     */
    public final IClass getOuterIClass() throws Java.CompileException {
        if (!this.outerIClassIsCached) {
            this.outerIClass = this.getOuterIClass2();
            this.outerIClassIsCached = true;
        }
        return this.outerIClass;
    }
    private boolean outerIClassIsCached = false;
    private IClass outerIClass = null;
    protected abstract IClass getOuterIClass2() throws Java.CompileException;

    /**
     * Returns the superclass of the class.<br>
     * Returns "null" for class "Object", interfaces, arrays, primitive types
     * and "void".
     */
    public final IClass getSuperclass() throws Java.CompileException {
        if (!this.superclassIsCached) {
            this.superclass = this.getSuperclass2();
            this.superclassIsCached = true;
            if (this.superclass != null && this.superclass.isSubclassOf(this)) throw new Java.CompileException("Class circularity detected for \"" + Descriptor.toClassName(this.getDescriptor()) + "\"", null);
        }
        return this.superclass;
    }
    private boolean superclassIsCached = false;
    private IClass superclass = null;
    protected abstract IClass getSuperclass2() throws Java.CompileException;

    /**
     * Whether the class may be accessed from outside its package (JVMS 4.1 access_flags)
     */
    public abstract boolean isPublic();

    /**
     * Whether subclassing is allowed (JVMS 4.1 access_flags)
     * @return <code>true</code> if subclassing is prohibited
     */
    public abstract boolean isFinal();

    /**
     * Returns the interfaces implemented by the class.<br>
     * Returns the superinterfaces of the interface.<br>
     * Returns "Cloneable" and "Serializable" for arrays.<br>
     * Returns an empty array for primitive types and "void".
     */
    public final IClass[] getInterfaces() throws Java.CompileException {
        if (this.interfaces == null) {
            this.interfaces = this.getInterfaces2();
            for (int i = 0; i < this.interfaces.length; ++i) {
                if (this.interfaces[i].implementsInterface(this)) throw new Java.CompileException("Interface circularity detected for \"" + Descriptor.toClassName(this.getDescriptor()) + "\"", null);
            }
        }
        return this.interfaces;
    }
    private IClass[] interfaces = null;
    protected abstract IClass[] getInterfaces2() throws Java.CompileException;

    /**
     * Whether the class may be instantiated (JVMS 4.1 access_flags)
     * @return <code>true</code> if instantiation is prohibited
     */
    public abstract boolean isAbstract();

    /**
     * Returns the field descriptor for the type as defined by JVMS 4.3.2.
     */
    public final String getDescriptor() {
        if (this.descriptor == null) {
            this.descriptor = this.getDescriptor2();
        }
        return this.descriptor;
    }
    private String descriptor = null;
    protected abstract String getDescriptor2();

    /**
     * Convenience method that determines the field descriptors of an array of {@link IClass}es.
     * @see #getDescriptor()
     */
    public static String[] getDescriptors(IClass[] iClasses) {
        String[] descriptors = new String[iClasses.length];
        for (int i = 0; i < iClasses.length; ++i) descriptors[i] = iClasses[i].getDescriptor();
        return descriptors;
    }

    /**
     * Returns "true" if this type represents an interface.
     */
    public abstract boolean isInterface();

    /**
     * Returns "true" if this type represents an array.
     */
    public abstract boolean isArray();

    /**
     * Returns "true" if this type represents a primitive type or "void".
     */
    public abstract boolean isPrimitive();

    /**
     * Returns "true" if this type represents "byte", "short", "int", "long",
     * "char", "float" or "double".
     */
    public abstract boolean isPrimitiveNumeric();

    /**
     * Returns the component type of the array.<br>
     * Returns "null" for classes, interfaces, primitive types and "void".
     */
    public final IClass getComponentType() {
        if (!this.componentTypeIsCached) {
            this.componentType = this.getComponentType2();
            this.componentTypeIsCached = true;
        }
        return this.componentType;
    }
    private boolean componentTypeIsCached = false;
    private IClass componentType = null;
    protected abstract IClass getComponentType2();

    /**
     * Returns a string representation for this object.
     */
    public String toString() { return Descriptor.toClassName(this.getDescriptor()); }

    /**
     * Determine if "this" is assignable from "that". This is true if "this"
     * is identical with "that", or if "this" is assignable from "that"'s
     * superclass (if any), or if "this" is assignable from any of the
     * interfaces which "that" implements.
     */
    public boolean isAssignableFrom(IClass that) throws Java.CompileException {

        // Identity conversion, JLS 5.1.1
        if (this == that) return true;

        // Widening primitive conversion, JLS 5.1.2
        if (Java.isWideningPrimitiveConvertible(that, this)) return true;

        // Widening reference conversion, JLS 5.1.4
        if (Java.isWideningReferenceConvertible(that, this)) return true;

        return false;
    }

    /**
     * Returns <code>true</code> if this class is an immediate or non-immediate
     * subclass of <code>that</code> class.
     */
    public boolean isSubclassOf(IClass that) throws Java.CompileException {
        for (IClass sc = this.getSuperclass(); sc != null; sc = sc.getSuperclass()) {
            if (sc == that) return true;
        }
        return false;
    }

    /**
     * If <code>this</code> represents a class: Return <code>true</code> if this class
     * directly or indirectly implements <code>that</code> interface.
     * <p>
     * If <code>this</code> represents an interface: Return <code>true</code> if this
     * interface directly or indirectly extends <code>that</code> interface.
     */
    public boolean implementsInterface(IClass that) throws Java.CompileException {
        for (IClass c = this; c != null; c = c.getSuperclass()) {
            IClass[] tis = c.getInterfaces();
            for (int i = 0; i < tis.length; ++i) {
                IClass ti = tis[i];
                if (ti == that || ti.implementsInterface(that)) return true;
            }
        }
        return false;
    }

    /**
     * Create an {@link IClass} that represents an array of the given component type.
     * @param componentType
     * @param objectType Required because the superclass of an array class is {@link Object} by definition
     */
    public static IClass createArrayIClass(final IClass componentType, final IClass objectType) {
        return new IClass() {
            public IClass.IConstructor[] getDeclaredIConstructors2() { return new IClass.IConstructor[0]; }
            public IClass.IMethod[]      getDeclaredIMethods2() { return new IClass.IMethod[0]; }
            public IClass.IField[]       getDeclaredIFields2() { return new IClass.IField[0]; }
            public IClass[]              getDeclaredIClasses2() { return new IClass[0]; }
            public IClass                getDeclaringIClass2() { return null; }
            public IClass                getOuterIClass2() { return null; }
            public IClass                getSuperclass2() { return objectType; }
            public IClass[]              getInterfaces2() { return new IClass[0]; }
            public String                getDescriptor2() { return '[' + componentType.getDescriptor(); }
            public boolean               isPublic() { return componentType.isPublic(); }
            public boolean               isFinal() { return true; }
            public boolean               isInterface() { return false; }
            public boolean               isAbstract() { return false; }
            public boolean               isArray() { return true; }
            public boolean               isPrimitive() { return false; }
            public boolean               isPrimitiveNumeric() { return false; }
            public IClass                getComponentType2() { return componentType; }

            public String toString() { return componentType.toString() + "[]"; }
        };
    }

    /**
     * If <code>optionalName</code> is <code>null</code>, find all {@link IClass}es visible in the
     * scope of the current class.
     * <p>
     * If <code>optionalName</code> is not <code>null</code>, find the member {@link IClass}es
     * that has the given name. If the name is ambiguous (i.e. if more than one superclass,
     * interface of enclosing type declares a type with that name), then the size of the
     * returned array is greater than one.
     * <p>
     * Examines superclasses, interfaces and enclosing type declarations.
     * @return an array of {@link IClass}es inunspecified order, possibly of length zero
     */
    IClass[] findMemberType(String optionalName) throws Java.CompileException {
        IClass[] res = (IClass[]) this.memberTypeCache.get(optionalName);
        if (res == null) {

            // Notice: A type may be added multiply to the result set because we are in its scope
            // multiply. E.g. the type is a member of a superclass AND a member of an enclosing type.
            Set s = new HashSet(); // IClass
            this.findMemberType(optionalName, s);
            res = s.isEmpty() ? IClass.ZERO_ICLASSES : (IClass[]) s.toArray(new IClass[s.size()]);
    
            this.memberTypeCache.put(optionalName, res);
        }

        return res;
    }
    private final Map memberTypeCache = new HashMap(); // String name => IClass[]
    private static final IClass[] ZERO_ICLASSES = new IClass[0];
    private void findMemberType(String optionalName, Collection result) throws Java.CompileException {

        // Search for a type with the given name in the current class.
        IClass[] memberTypes = this.getDeclaredIClasses();
        if (optionalName == null) {
            result.addAll(Arrays.asList(memberTypes));
        } else {
            String memberDescriptor = Descriptor.fromClassName(Descriptor.toClassName(this.getDescriptor()) + '$' + optionalName);
            for (int i = 0; i < memberTypes.length; ++i) {
                final IClass mt = memberTypes[i];
                if (mt.getDescriptor().equals(memberDescriptor)) {
                    result.add(mt);
                    return;
                } 
            }
        }
        
        // Examine superclass.
        {
            IClass superclass = this.getSuperclass();
            if (superclass != null) superclass.findMemberType(optionalName, result); 
        }
        
        // Examine interfaces.
        {
            IClass[] ifs = this.getInterfaces();
            for (int i = 0; i < ifs.length; ++i) ifs[i].findMemberType(optionalName, result);
        }

        // Examine enclosing type declarations.
        {
            IClass declaringIClass = this.getDeclaringIClass();
            IClass outerIClass = this.getOuterIClass();
            if (declaringIClass != null) {
                declaringIClass.findMemberType(optionalName, result);
            }
            if (outerIClass != null && outerIClass != declaringIClass) {
                outerIClass.findMemberType(optionalName, result);
            }
        }
    }

    public interface IMember {

        /**
         * @return One of {@link #PRIVATE}, {@link #PROTECTED},
         * {@link #PACKAGE} and {@link #PUBLIC}.
         */
        int getAccess();

        /**
         * Returns the {@link IClass} that declares this {@link IClass.IMember}.
         */
        IClass getDeclaringIClass();
    }

    /** Return value for {@link IClass.IMember#getAccess}. */
    public static final int PRIVATE   = 0;
    /** Return value for {@link IMember#getAccess}. */
    public static final int PROTECTED = 1;
    /** Return value for {@link IMember#getAccess}. */
    public static final int PACKAGE   = 2;
    /** Return value for {@link IMember#getAccess}. */
    public static final int PUBLIC    = 3;

    public abstract class IInvocable implements IMember {

        // Implement IMember.
        public abstract int getAccess();
        public IClass       getDeclaringIClass() { return IClass.this; }
        public abstract IClass[] getParameterTypes() throws Java.CompileException;
        public abstract String   getDescriptor() throws Java.CompileException;
        public abstract IClass[] getThrownExceptions() throws Java.CompileException;

        public boolean isMoreSpecificThan(IInvocable that) throws Java.CompileException {
            if (IClass.DEBUG) System.out.print("\"" + this + "\".isMoreSpecificThan(\"" + that + "\") => ");
            if (!that.getDeclaringIClass().isAssignableFrom(this.getDeclaringIClass())) {
                if (IClass.DEBUG) System.out.println("falsE");
                return false;
            }
            IClass[] thisParameterTypes = this.getParameterTypes();
            IClass[] thatParameterTypes = that.getParameterTypes();
            int i;
            for (i = 0; i < thisParameterTypes.length; ++i) {
                if (!thatParameterTypes[i].isAssignableFrom(thisParameterTypes[i])) {
                    if (IClass.DEBUG) System.out.println("false");
                    return false;
                }
            }
            if (IClass.DEBUG) System.out.println("true");
            return true;
        }
        public boolean isLessSpecificThan(IInvocable that) throws Java.CompileException {
            return that.isMoreSpecificThan(this);
        }
        public abstract String toString();
    }
    public abstract class IConstructor extends IInvocable {

        /**
         * Opposed to {@link java.lang.reflect.Constructor#getParameterTypes()}, the
         * return value of this method does not include the optionally leading "synthetic
         * parameters".
         */
        public abstract IClass[] getParameterTypes() throws Java.CompileException;

        /**
         * Opposed to {@link #getParameterTypes()}, the method descriptor returned by this
         * method does include the optionally leading synthetic parameters.
         */
        public String getDescriptor() throws Java.CompileException {
            return new MethodDescriptor(IClass.getDescriptors(this.getParameterTypes()), Descriptor.VOID).toString();
        }
        public String toString() {
            StringBuffer sb = new StringBuffer(this.getDeclaringIClass().toString());
            sb.append('(');
            try {
                IClass[] parameterTypes = this.getParameterTypes();
                for (int i = 0; i < parameterTypes.length; ++i) {
                    if (i > 0) sb.append(", ");
                    sb.append(parameterTypes[i].toString());
                }
            } catch (Java.CompileException ex) {
                sb.append("<invalid type>");
            }
            sb.append(')');
            return sb.toString();
        }
    }
    public abstract class IMethod extends IInvocable {
        public abstract boolean isStatic();
        public abstract boolean isAbstract();
        public abstract IClass  getReturnType() throws Java.CompileException;
        public abstract String  getName();
        public String getDescriptor() throws Java.CompileException {
            return new MethodDescriptor(
                IClass.getDescriptors(this.getParameterTypes()),
                getReturnType().getDescriptor()
            ).toString();
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            try {
                sb.append(this.getReturnType().toString());
            } catch (Java.CompileException ex) {
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
            } catch (Java.CompileException ex) {
                sb.append("<invalid type>");
            }
            sb.append(')');
            return sb.toString();
        }
    }
    public abstract class IField implements IMember {

        // Implement IMember.
        public abstract int     getAccess();
        public IClass           getDeclaringIClass() { return IClass.this; }

        public abstract boolean isStatic();
        public abstract IClass  getType() throws Java.CompileException;
        public abstract String  getName();
        public String getDescriptor() throws Java.CompileException {
            return getType().getDescriptor();
        }
        public abstract Object  getConstantValue() throws Java.CompileException;
        public String toString() { return this.getName(); }
    }
}
