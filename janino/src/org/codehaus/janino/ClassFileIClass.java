
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.janino.util.ClassFile;


/**
 * A wrapper object that turns a {@link ClassFile} object into a
 * {@link IClass}.
 */
public class ClassFileIClass extends IClass {
    private static final boolean DEBUG = false;

    private final ClassFile    classFile;
    private final IClassLoader iClassLoader;
    private final short        accessFlags;

    private final Map resolvedFields = new HashMap(); // FieldInfo => IField

    /**
     * @param classFile Source of data
     * @param iClassLoader {@link IClassLoader} through which to load other classes
     */
    public ClassFileIClass(
        ClassFile    classFile,
        IClassLoader iClassLoader
    ) {
        this.classFile    = classFile;
        this.iClassLoader = iClassLoader;

        // Determine class access flags.
        this.accessFlags = classFile.accessFlags;
    }

    // Implement IClass.

    protected IConstructor[] getDeclaredIConstructors2() {
        List iConstructors = new ArrayList();

        for (Iterator it = this.classFile.methodInfos.iterator(); it.hasNext();) {
            ClassFile.MethodInfo mi = (ClassFile.MethodInfo) it.next();
            IInvocable ii;
            try {
                ii = this.resolveMethod(mi);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex.getMessage());
            }
            if (ii instanceof IConstructor) iConstructors.add(ii);
        }

        return (IConstructor[]) iConstructors.toArray(new IConstructor[iConstructors.size()]);
    }

    protected IMethod[] getDeclaredIMethods2() {
        List iMethods = new ArrayList();

        for (Iterator it = this.classFile.methodInfos.iterator(); it.hasNext();) {
            ClassFile.MethodInfo mi = (ClassFile.MethodInfo) it.next();
            IInvocable ii;
            try {
                ii = this.resolveMethod(mi);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex.getMessage());
            }
            if (ii instanceof IMethod) iMethods.add(ii);
        }

        return (IMethod[]) iMethods.toArray(new IMethod[iMethods.size()]);
    }

    protected IField[] getDeclaredIFields2() {
        IField[] ifs = new IClass.IField[this.classFile.fieldInfos.size()];
        for (int i = 0; i < this.classFile.fieldInfos.size(); ++i) {
            try {
                ifs[i] = this.resolveField((ClassFile.FieldInfo) this.classFile.fieldInfos.get(i));
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex.getMessage());
            }
        }
        return ifs;
    }

    protected IClass[] getDeclaredIClasses2() throws Java.CompileException {
        ClassFile.InnerClassesAttribute ica = this.classFile.getInnerClassesAttribute();
        if (ica == null) return new IClass[0];

        List ices = ica.getEntries(); // ClassFile.InnerClassAttribute.Entry
        List res = new ArrayList(); // IClass
        for (Iterator it = ices.iterator(); it.hasNext();) {
            ClassFile.InnerClassesAttribute.Entry e = (ClassFile.InnerClassesAttribute.Entry) it.next();
            if (e.outerClassInfoIndex == this.classFile.thisClass) {
                try {
                    res.add(this.resolveClass(e.innerClassInfoIndex));
                } catch (ClassNotFoundException ex) {
                    Java.compileError(ex.getMessage());
                    return new IClass[0];
                }
            } 
        }
        return (IClass[]) res.toArray(new IClass[res.size()]);
    }

    protected IClass getDeclaringIClass2() throws Java.CompileException {
        ClassFile.InnerClassesAttribute ica = this.classFile.getInnerClassesAttribute();
        if (ica == null) return null;

        List ices = ica.getEntries(); // ClassFile.InnerClassAttribute.Entry
        for (Iterator it = ices.iterator(); it.hasNext();) {
            ClassFile.InnerClassesAttribute.Entry e = (ClassFile.InnerClassesAttribute.Entry) it.next();
            if (e.innerClassInfoIndex == this.classFile.thisClass) {
                // Is this an anonymous class?
                if (e.outerClassInfoIndex == 0) return null;
                try {
                    return this.resolveClass(e.outerClassInfoIndex);
                } catch (ClassNotFoundException ex) {
                    Java.compileError(ex.getMessage());
                    return null;
                }
            } 
        }
        return null;
    }

    protected IClass getOuterIClass2() throws Java.CompileException {
        ClassFile.InnerClassesAttribute ica = this.classFile.getInnerClassesAttribute();
        if (ica == null) return null;

        List ices = ica.getEntries(); // ClassFile.InnerClassAttribute.Entry
        for (Iterator it = ices.iterator(); it.hasNext();) {
            ClassFile.InnerClassesAttribute.Entry e = (ClassFile.InnerClassesAttribute.Entry) it.next();
            if (e.innerClassInfoIndex == this.classFile.thisClass) {
                if (e.outerClassInfoIndex == 0) {

                    // Anonymous class or local class.
                    // TODO: Determine enclosing instance of anonymous class or local class
                    return null;
                } else  {

                    // Member type.
                    if ((e.innerClassAccessFlags & Mod.STATIC) != 0) return null;
                    try {
                        return this.resolveClass(e.outerClassInfoIndex);
                    } catch (ClassNotFoundException ex) {
                        Java.compileError(ex.getMessage());
                        return null;
                    }
                }
            } 
        }
        return null;
    }

    protected IClass getSuperclass2() throws Java.CompileException {
        if (this.classFile.superclass == 0) return null;
        try {
            return this.resolveClass(this.classFile.superclass);
        } catch (ClassNotFoundException e) {
            Java.compileError(e.getMessage());
            return Java.getIClassLoader().OBJECT;
        }
    }

    public boolean isPublic() {
        return (this.accessFlags & Mod.PUBLIC) != 0;
    }

    public boolean isFinal() {
        return (this.accessFlags & Mod.FINAL) != 0;
    }

    protected IClass[] getInterfaces2() throws Java.CompileException {
        return this.resolveClasses(this.classFile.interfaces);
    }

    public boolean isAbstract() {
        return (this.accessFlags & Mod.ABSTRACT) != 0;
    }

    protected String getDescriptor2() {
        return Descriptor.fromClassName(this.classFile.getThisClassName());
    }

    public boolean isInterface() {
        return (this.accessFlags & Mod.INTERFACE) != 0;
    }

    public boolean isArray() {
        return false;
    }

    public boolean isPrimitive() {
        return false;
    }

    public boolean isPrimitiveNumeric() {
        return false;
    }

    protected IClass getComponentType2() {
        return null;
    }

    public void resolveHalf() throws ClassNotFoundException {

        // Resolve superclass.
        this.resolveClass(this.classFile.superclass);

        // Resolve interfaces.
        for (int i = 0; i < this.classFile.interfaces.length; ++i) {
            this.resolveClass(this.classFile.interfaces[i]);
        }

        // Resolve constructors and methods.
        for (int i = 0; i < this.classFile.methodInfos.size(); ++i) {
            this.resolveMethod((ClassFile.MethodInfo) this.classFile.methodInfos.get(i));
        }

        // Process fields.
        for (int i = 0; i < this.classFile.fieldInfos.size(); ++i) {
            this.resolveField((ClassFile.FieldInfo) this.classFile.fieldInfos.get(i));
        }
    }

    public void resolveAllClasses() throws ClassNotFoundException {
        for (short i = 0; i < this.classFile.constantPool.size(); ++i) {
            ClassFile.ConstantPoolInfo cpi = this.classFile.getConstantPoolInfo(i);
            if (cpi instanceof ClassFile.ConstantClassInfo) {
                this.resolveClass(i);
            } else
            if (cpi instanceof ClassFile.ConstantNameAndTypeInfo) {
                short descriptorIndex = ((ClassFile.ConstantNameAndTypeInfo) cpi).getDescriptorIndex();
                String descriptor = this.classFile.getConstantUtf8(descriptorIndex);
                if (descriptor.charAt(0) == '(') {
                    MethodDescriptor md = new MethodDescriptor(descriptor);
                    this.resolveClass(md.returnFD);
                    for (int j = 0; j < md.parameterFDs.length; ++j) this.resolveClass(md.parameterFDs[j]);
                } else {
                    this.resolveClass(descriptor);
                }
            }
        }
    }

    /**
     * 
     * @param index Index of the CONSTANT_Class_info to resolve (JVMS 4.4.1)
     */
    private IClass resolveClass(short index) throws ClassNotFoundException {
        if (ClassFileIClass.DEBUG) System.out.println("index=" + index);
        return this.resolveClass(Descriptor.fromInternalForm(this.classFile.getConstantClassName(index)));
    }
    
    private IClass resolveClass(String descriptor) throws ClassNotFoundException {
        if (ClassFileIClass.DEBUG) System.out.println("descriptor=" + descriptor);

        IClass result = (IClass) this.resolvedClasses.get(descriptor);
        if (result != null) return result;
        
        result = this.iClassLoader.loadIClass(descriptor);
        if (result == null) throw new ClassNotFoundException(descriptor);

        this.resolvedClasses.put(descriptor, result);
        return result;
    }
    private final Map resolvedClasses = new HashMap(); // String descriptor => IClass

    private IClass[] resolveClasses(short[] ifs) throws Java.CompileException {
        IClass[] result = new IClass[ifs.length];
        for (int i = 0; i < result.length; ++i) {
            try {
                result[i] = this.resolveClass(ifs[i]);
            } catch (ClassNotFoundException e) {
                Java.compileError(e.getMessage());
                return new IClass[0];
            }
        }
        return result;
    }

    /**
     * Turn a {@link MethodInfo} into an {@link IInvocable}. This includes the checking and the
     * removal of the magic first parameter of an inner class constructor.
     * @param methodInfo
     * @return
     * @throws ClassNotFoundException
     */
    private IInvocable resolveMethod(final ClassFile.MethodInfo methodInfo) throws ClassNotFoundException {
        IInvocable result = (IInvocable) this.resolvedMethods.get(methodInfo);
        if (result != null) return result;

        // Determine method name.
        final String name = this.classFile.getConstantUtf8(methodInfo.getNameIndex());

        // Determine return type.
        MethodDescriptor md = new MethodDescriptor(this.classFile.getConstantUtf8(methodInfo.getDescriptorIndex()));
        final IClass returnType = this.resolveClass(md.returnFD);

        // Determine parameter types.
        final IClass[] parameterTypes = new IClass[md.parameterFDs.length];
        for (int i = 0; i < parameterTypes.length; ++i) parameterTypes[i] = this.resolveClass(md.parameterFDs[i]);

        // Determine thrown exceptions.
        IClass tes[] = null;
        ClassFile.AttributeInfo[] ais = methodInfo.getAttributes();
        for (int i = 0; i < ais.length; ++i) {
            ClassFile.AttributeInfo ai = ais[i];
            if (ai instanceof ClassFile.ExceptionsAttribute) {
                short[] teis = ((ClassFile.ExceptionsAttribute) ai).getExceptionIndexes();
                tes = new IClass[teis.length];
                for (int j = 0; j < teis.length; ++j) tes[j] = this.resolveClass(teis[j]);
            }
        }
        final IClass thrownExceptions[] = tes == null ? new IClass[0] : tes;

        // Determine access.
        short af = methodInfo.getAccessFlags();
        final int access = (
            (af & Mod.PUBLIC   ) != 0 ? IClass.PUBLIC    :
            (af & Mod.PROTECTED) != 0 ? IClass.PROTECTED :
            (af & Mod.PRIVATE  ) != 0 ? IClass.PRIVATE   :
            IClass.PACKAGE
        );

        if (name.equals("<init>")) {
            result = new IClass.IConstructor() {
                public IClass[] getParameterTypes() throws Java.CompileException {

                    // Process magic first parameter of inner class constructor.
                    IClass outerIClass = ClassFileIClass.this.getOuterIClass();
                    if (outerIClass != null) {
                        if (parameterTypes.length < 1) throw new RuntimeException("Inner class constructor lacks magic first parameter");
                        if (parameterTypes[0] != outerIClass) throw new RuntimeException("Magic first parameter of inner class constructor has type \"" + parameterTypes[0].toString() + "\" instead of that of its enclosing instance (\"" + outerIClass.toString() + "\")");
                        IClass[] tmp = new IClass[parameterTypes.length - 1];
                        System.arraycopy(parameterTypes, 1, tmp, 0, tmp.length);
                        return tmp;
                    }

                    return parameterTypes;
                }
                public IClass[] getThrownExceptions() throws Java.CompileException { return thrownExceptions; }
                public int getAccess() { return access; }
            };
        } else {
            result = new IClass.IMethod() {
                public String getName() { return name; }
                public IClass getReturnType() throws Java.CompileException { return returnType; }
                public boolean isStatic() { return (methodInfo.getAccessFlags() & Mod.STATIC) != 0; }
                public boolean isAbstract() { return (methodInfo.getAccessFlags() & Mod.ABSTRACT) != 0; }
                public IClass[] getParameterTypes() throws Java.CompileException { return parameterTypes; }
                public IClass[] getThrownExceptions() throws Java.CompileException { return thrownExceptions; }
                public int getAccess() { return access; }
            };
        }
        this.resolvedMethods.put(methodInfo, result);
        return result;
    }
    private final Map resolvedMethods = new HashMap(); // MethodInfo => IInvocable

    private IField resolveField(final ClassFile.FieldInfo fieldInfo) throws ClassNotFoundException {
        IField result = (IField) this.resolvedFields.get(fieldInfo);
        if (result != null) return result;

        // Determine field name.
        final String name = this.classFile.getConstantUtf8(fieldInfo.getNameIndex());

        // Determine field type.
        final String descriptor = this.classFile.getConstantUtf8(fieldInfo.getDescriptorIndex());
        final IClass type = this.resolveClass(descriptor);

        // Determine optional constant value.
        ClassFile.ConstantValueAttribute cva = null;
        ClassFile.AttributeInfo[] ais = fieldInfo.getAttributes();
        for (int i = 0; i < ais.length; ++i) {
            ClassFile.AttributeInfo ai = ais[i];
            if (ai instanceof ClassFile.ConstantValueAttribute) {
                cva = (ClassFile.ConstantValueAttribute) ai;
                break;
            }
        }
        Object ocv = null;
        if (cva != null) {
            ClassFile.ConstantPoolInfo cpi = this.classFile.getConstantPoolInfo(cva.getConstantValueIndex());
            if (cpi instanceof ClassFile.ConstantValuePoolInfo) {
                ocv = ((ClassFile.ConstantValuePoolInfo) cpi).getValue(this.classFile);
            } else
            {
                throw new RuntimeException("Unexpected constant pool info type \"" + cpi.getClass().getName() + "\"");
            }
        }
        final Object optionalConstantValue = ocv;

        // Determine access.
        short af = fieldInfo.getAccessFlags();
        final int access = (
            (af & Mod.PUBLIC   ) != 0 ? IClass.PUBLIC    :
            (af & Mod.PROTECTED) != 0 ? IClass.PROTECTED :
            (af & Mod.PRIVATE  ) != 0 ? IClass.PRIVATE   :
            IClass.PACKAGE
        );

        result = new IField() {
            public Object getConstantValue() throws Java.CompileException { return optionalConstantValue; }
            public String getName() { return name; }
            public IClass getType() throws Java.CompileException { return type; }
            public boolean isStatic() { return (fieldInfo.getAccessFlags() & Mod.STATIC) != 0; }
            public int getAccess() { return access; }
        };
        this.resolvedFields.put(fieldInfo, result);
        return result;
    }
}
