
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright 2004 Arno Unkrig
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.janino;

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
        IMethod[] result = new IMethod[methods.length];
        for (int i = 0; i < methods.length; ++i) {
            result[i] = new ReflectionIMethod(methods[i]);
        }
        return result;
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
    protected IClass getOuterIClass2() throws Java.CompileException {
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
    public boolean isPublic()    { return Modifier.isPublic(this.clazz.getModifiers()); }
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
            this.clazz == Byte.TYPE      ||
            this.clazz == Short.TYPE     ||
            this.clazz == Integer.TYPE   ||
            this.clazz == Long.TYPE      ||
            this.clazz == Character.TYPE ||
            this.clazz == Float.TYPE     ||
            this.clazz == Double.TYPE
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
        public int getAccess() {
            int mod = this.constructor.getModifiers();
            return (
                Modifier.isPrivate(mod)   ? IClass.PRIVATE   :
                Modifier.isProtected(mod) ? IClass.PROTECTED :
                Modifier.isPublic(mod)    ? IClass.PUBLIC    :
                IClass.PACKAGE
            );
        }

        // Implement "IConstructor".
        public IClass[] getParameterTypes() throws Java.CompileException {
            IClass[] parameterTypes = ReflectionIClass.this.classesToIClasses(this.constructor.getParameterTypes());

            // The JAVADOC of java.lang.reflect.Constructor does not document it, but
            // "getParameterTypes()" includes the synthetic "enclosing instance" parameter.
            IClass outerClass = ReflectionIClass.this.getOuterIClass();
            if (outerClass != null) {
                if (parameterTypes.length < 1) throw new Java.CompileException("Constructor \"" + this.constructor + "\" lacks synthetic enclosing instance parameter", null);
                if (parameterTypes[0] != outerClass) throw new Java.CompileException("Enclosing instance parameter of constructor \"" + this.constructor + "\" has wrong type -- \"" + parameterTypes[0] + "\" vs. \"" + outerClass + "\"", null);
                IClass[] tmp = new IClass[parameterTypes.length - 1];
                System.arraycopy(parameterTypes, 1, tmp, 0, tmp.length);
                parameterTypes = tmp;
            }

            return parameterTypes;
        }
        public IClass[] getThrownExceptions() {
            return ReflectionIClass.this.classesToIClasses(this.constructor.getExceptionTypes());
        }

        final Constructor constructor;
    };
    private class ReflectionIMethod extends IMethod {
        public ReflectionIMethod(Method method) { this.method = method; }

        // Implement IMember.
        public int getAccess() {
            int mod = this.method.getModifiers();
            return (
                Modifier.isPrivate(mod)   ? IClass.PRIVATE   :
                Modifier.isProtected(mod) ? IClass.PROTECTED :
                Modifier.isPublic(mod)    ? IClass.PUBLIC    :
                IClass.PACKAGE
            );
        }

        // Implemnt "IMethod".
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
        public int getAccess() {
            int mod = this.field.getModifiers();
            return (
                Modifier.isPrivate(mod)   ? IClass.PRIVATE   :
                Modifier.isProtected(mod) ? IClass.PROTECTED :
                Modifier.isPublic(mod)    ? IClass.PUBLIC    :
                IClass.PACKAGE
            );
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
                getName()
            );
        }
        public Object getConstantValue() throws Java.CompileException {
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
                    throw new Java.CompileException("Field \"" + this.field.getName() + "\" is not accessible", (Scanner.Location) null);
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
    /*private*/ IClass classToIClass(Class c) {
        return this.iClassLoader.loadIClass(Descriptor.fromClassName(c.getName()));
    }

    /**
     * @see #classToIClass(Class)
     */
    /*private*/ IClass[] classesToIClasses(Class[] cs) {
        IClass[] result = new IClass[cs.length];
        for (int i = 0; i < cs.length; ++i) result[i] = this.classToIClass(cs[i]);
        return result;
    }
}