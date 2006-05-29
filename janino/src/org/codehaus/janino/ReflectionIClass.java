
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
import java.lang.reflect.*;

/**
 * Wraps a {@link java.lang.Class} in an {@link org.codehaus.janino.IClass}.
 */
class ReflectionIClass extends IClass {
    private /*final*/ Class        clazz;
    private /*final*/ IClassLoader iClassLoader;

    /**
     * @param iClassLoader required to load other {@link IClass}es on <code>get...()</code>.
     */
    public ReflectionIClass(Class clazz, IClassLoader iClassLoader) {
        this.clazz = clazz;
        this.iClassLoader = iClassLoader;
    }

    protected IConstructor[] getDeclaredIConstructors2() {
        Constructor[] constructors = this.clazz.getDeclaredConstructors();
        IConstructor[] result = new IConstructor[constructors.length];
        for (int i = 0; i < constructors.length; ++i) {
            result[i] = new ReflectionIConstructor(constructors[i]);
        }
        return result;
    }

    protected IMethod[] getDeclaredIMethods2() {
        Method[] methods = this.clazz.getDeclaredMethods();
        List iMethods = new ArrayList();
        for (int i = 0; i < methods.length; ++i) {
            Method m = methods[i];

            // Skip JDK 1.5 synthetic methods (e.g. those generated for
            // covariant return values).
            if ((m.getModifiers() & Mod.SYNTHETIC) != 0) continue;

            // Wrap java.reflection.Method in an IMethod.
            iMethods.add(new ReflectionIMethod(m));
        }
        return (IMethod[]) iMethods.toArray(new IMethod[iMethods.size()]);
    }

    protected IField[] getDeclaredIFields2() {
        Field[] fields = this.clazz.getDeclaredFields();
        IField[] result = new IField[fields.length];
        for (int i = 0; i < fields.length; ++i) {
            result[i] = new ReflectionIField(fields[i]);
        }
        return result;
    }

    protected IClass[] getDeclaredIClasses2() {
        return this.classesToIClasses(this.clazz.getDeclaredClasses());
    }

    protected IClass getDeclaringIClass2() {
        Class declaringClass = this.clazz.getDeclaringClass();
        if (declaringClass == null) return null;
        return this.classToIClass(declaringClass);
    }
    protected IClass getOuterIClass2() throws CompileException {
        if (Modifier.isStatic(this.clazz.getModifiers())) return null;
        return this.getDeclaringIClass();
    }
    protected IClass getSuperclass2() {
        Class superclass = this.clazz.getSuperclass();
        return superclass == null ? null : this.classToIClass(superclass);
    }
    protected IClass[] getInterfaces2() {
        return this.classesToIClasses(this.clazz.getInterfaces());
    }
    protected String getDescriptor2() {
        return Descriptor.fromClassName(this.clazz.getName());
    }
    public Access  getAccess()   { return ReflectionIClass.modifiers2Access(this.clazz.getModifiers()); }
    public boolean isFinal()     { return Modifier.isFinal(this.clazz.getModifiers()); }
    public boolean isInterface() { return this.clazz.isInterface(); }
    public boolean isAbstract()  { return Modifier.isAbstract(this.clazz.getModifiers()); }
    public boolean isArray()     { return this.clazz.isArray(); }
    protected IClass getComponentType2() {
        Class componentType = this.clazz.getComponentType();
        return componentType == null ? null : this.classToIClass(componentType);
    }
    public boolean isPrimitive() {
        return this.clazz.isPrimitive();
    }
    public boolean isPrimitiveNumeric() {
        return (
            this.clazz == byte.class   ||
            this.clazz == short.class  ||
            this.clazz == int.class    ||
            this.clazz == long.class   ||
            this.clazz == char.class   ||
            this.clazz == float.class  ||
            this.clazz == double.class
        );
    }

    /**
     * @return E.g. "int", "int[][]", "pkg1.pkg2.Outer$Inner[]"
     */
    public String toString() {
        int brackets = 0;
        Class c = this.clazz;
        while (c.isArray()) {
            ++brackets;
            c = c.getComponentType();
        }
        String s = c.getName();
        while (brackets-- > 0) s += "[]";
        return s;
    }

    private class ReflectionIConstructor extends IConstructor {
        public ReflectionIConstructor(Constructor constructor) { this.constructor = constructor; }

        // Implement IMember.
        public Access getAccess() {
            int mod = this.constructor.getModifiers();
            return ReflectionIClass.modifiers2Access(mod);
        }

        // Implement "IConstructor".
        public IClass[] getParameterTypes() throws CompileException {
            IClass[] parameterTypes = ReflectionIClass.this.classesToIClasses(this.constructor.getParameterTypes());

            // The JAVADOC of java.lang.reflect.Constructor does not document it, but
            // "getParameterTypes()" includes the synthetic "enclosing instance" parameter.
            IClass outerClass = ReflectionIClass.this.getOuterIClass();
            if (outerClass != null) {
                if (parameterTypes.length < 1) throw new CompileException("Constructor \"" + this.constructor + "\" lacks synthetic enclosing instance parameter", null);
                if (parameterTypes[0] != outerClass) throw new CompileException("Enclosing instance parameter of constructor \"" + this.constructor + "\" has wrong type -- \"" + parameterTypes[0] + "\" vs. \"" + outerClass + "\"", null);
                IClass[] tmp = new IClass[parameterTypes.length - 1];
                System.arraycopy(parameterTypes, 1, tmp, 0, tmp.length);
                parameterTypes = tmp;
            }

            return parameterTypes;
        }
        public String getDescriptor() {
        	Class[] parameterTypes = this.constructor.getParameterTypes();
        	String[] parameterDescriptors = new String[parameterTypes.length];
        	for (int i = 0; i < parameterDescriptors.length; ++i) {
        		parameterDescriptors[i] = Descriptor.fromClassName(parameterTypes[i].getName());
        	}
            return new MethodDescriptor(parameterDescriptors, Descriptor.VOID).toString();
        }
        public IClass[] getThrownExceptions() {
            return ReflectionIClass.this.classesToIClasses(this.constructor.getExceptionTypes());
        }

        final Constructor constructor;
    };
    private class ReflectionIMethod extends IMethod {
        public ReflectionIMethod(Method method) { this.method = method; }

        // Implement IMember.
        public Access getAccess() {
            return ReflectionIClass.modifiers2Access(this.method.getModifiers());
        }

        // Implement "IMethod".
        public String getName() { return this.method.getName(); }
        public IClass[] getParameterTypes() {
            return ReflectionIClass.this.classesToIClasses(this.method.getParameterTypes());
        }
        public boolean isStatic() { return Modifier.isStatic(this.method.getModifiers()); }
        public boolean isAbstract() { return Modifier.isAbstract(this.method.getModifiers()); }
        public IClass getReturnType() {
            return ReflectionIClass.this.classToIClass(this.method.getReturnType());
        }
        public IClass[] getThrownExceptions() {
            return ReflectionIClass.this.classesToIClasses(this.method.getExceptionTypes());
        }

        final Method method;
    };
    private class ReflectionIField extends IField {
        public ReflectionIField(Field field) { this.field = field; }

        // Implement IMember.
        public Access getAccess() {
            return ReflectionIClass.modifiers2Access(this.field.getModifiers());
        }

        // Implement "IField".
        public String getName() { return this.field.getName(); }
        public boolean isStatic() {
            return Modifier.isStatic(this.field.getModifiers());
        }
        public IClass getType() {
            return ReflectionIClass.this.classToIClass(this.field.getType());
        }
        public String toString() {
            return (
                Descriptor.toString(this.getDeclaringIClass().getDescriptor()) +
                "." +
                this.getName()
            );
        }
        /**
         * This implementation of {@link IClass.IField#getConstantValue()} is
         * not completely correct:
         * <ul>
         *   <li>
         *   It treats non-static fields as non-constant
         *   <li>
         *   Even fields with a <i>non-constant</i> initializer are identified
         *   as constant. (The value of that field may be different in a
         *   different JVM instance -- the classical example is
         *   {@link java.io.File#separator}.)
         * </ul>
         */
        public Object getConstantValue() throws CompileException {
            int mod = this.field.getModifiers();
            Class clazz = this.field.getType();
            if (
                Modifier.isStatic(mod) &&
                Modifier.isFinal(mod) &&
                (clazz.isPrimitive() || clazz == String.class)
            ) {
                try {
                    return this.field.get(null);
                } catch (IllegalAccessException ex) {
                    throw new CompileException("Field \"" + this.field.getName() + "\" is not accessible", (Location) null);
                }
            }
            return null;
        }

        final Field field;
    }

    /**
     * Load {@link Class} through {@link IClassLoader} to
     * ensure unique {@link IClass}es.
     */
    private IClass classToIClass(Class c) {
        IClass iClass;
        try {
            iClass = this.iClassLoader.loadIClass(Descriptor.fromClassName(c.getName()));
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Loading IClass \"" + c.getName() + "\": " + ex);
        }
        if (iClass == null) throw new RuntimeException("Cannot load class \"" + c.getName() + "\" through the given ClassLoader");
        return iClass;
    }

    /**
     * @see #classToIClass(Class)
     */
    private IClass[] classesToIClasses(Class[] cs) {
        IClass[] result = new IClass[cs.length];
        for (int i = 0; i < cs.length; ++i) result[i] = this.classToIClass(cs[i]);
        return result;
    }

    private static Access modifiers2Access(int modifiers) {
        return (
            Modifier.isPrivate(modifiers)   ? Access.PRIVATE   :
            Modifier.isProtected(modifiers) ? Access.PROTECTED :
            Modifier.isPublic(modifiers)    ? Access.PUBLIC    :
            Access.DEFAULT
        );
    }
}
