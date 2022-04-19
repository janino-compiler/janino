
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.ClassFile.Annotation;
import org.codehaus.janino.util.ClassFile.ArrayElementValue;
import org.codehaus.janino.util.ClassFile.BooleanElementValue;
import org.codehaus.janino.util.ClassFile.ByteElementValue;
import org.codehaus.janino.util.ClassFile.CharElementValue;
import org.codehaus.janino.util.ClassFile.ClassElementValue;
import org.codehaus.janino.util.ClassFile.ConstantClassInfo;
import org.codehaus.janino.util.ClassFile.DoubleElementValue;
import org.codehaus.janino.util.ClassFile.EnumConstValue;
import org.codehaus.janino.util.ClassFile.FloatElementValue;
import org.codehaus.janino.util.ClassFile.IntElementValue;
import org.codehaus.janino.util.ClassFile.LongElementValue;
import org.codehaus.janino.util.ClassFile.ShortElementValue;
import org.codehaus.janino.util.ClassFile.SignatureAttribute;
import org.codehaus.janino.util.ClassFile.StringElementValue;
import org.codehaus.janino.util.signature.SignatureParser;
import org.codehaus.janino.util.signature.SignatureParser.ArrayTypeSignature;
import org.codehaus.janino.util.signature.SignatureParser.ClassSignature;
import org.codehaus.janino.util.signature.SignatureParser.ClassTypeSignature;
import org.codehaus.janino.util.signature.SignatureParser.FieldTypeSignature;
import org.codehaus.janino.util.signature.SignatureParser.FieldTypeSignatureVisitor;
import org.codehaus.janino.util.signature.SignatureParser.FormalTypeParameter;
import org.codehaus.janino.util.signature.SignatureParser.SignatureException;
import org.codehaus.janino.util.signature.SignatureParser.TypeVariableSignature;

/**
 * A wrapper object that turns a {@link ClassFile} object into an {@link IClass}.
 */
public
class ClassFileIClass extends IClass {

    private static final Logger LOGGER = Logger.getLogger(ClassFileIClass.class.getName());

    private final ClassFile                classFile;
    private final IClassLoader             iClassLoader;
    private final short                    accessFlags;
    @Nullable final private ClassSignature classSignature;

    private final Map<ClassFile.FieldInfo, IField> resolvedFields = new HashMap<>();

    /**
     * @param classFile Source of data
     * @param iClassLoader {@link IClassLoader} through which to load other classes
     */
    public
    ClassFileIClass(ClassFile classFile, IClassLoader iClassLoader) {
        this.classFile    = classFile;
        this.iClassLoader = iClassLoader;

        // Determine class access flags.
        this.accessFlags = classFile.accessFlags;

        SignatureAttribute sa = classFile.getSignatureAttribute();
        try {
            this.classSignature = sa == null ? null : new SignatureParser().decodeClassSignature(sa.getSignature(classFile));
        } catch (SignatureException e) {
            throw new InternalCompilerException("Decoding signature of \"" + this + "\"", e);
        }

        // Example 1:
        //   interface Map<K, V>
        // has signature
        //   <K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/lang/Object;
        //   [this-class]<K extends Object, V extends Object> extends Object

        // Example 2:
        //   class HashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, Serializable
        // has signature
        //   <K:Ljava/lang/Object;V:Ljava/lang/Object;>\
        //     Ljava/util/AbstractMap<TK;TV;>;\
        //     Ljava/util/Map<TK;TV;>;\
        //     Ljava/lang/Cloneable;\
        //     Ljava/io/Serializable;
        //   [this-class]<K extends Object, V extends Object> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, java.io.Serializable
    }

    // Implement IClass.

    @Override protected ITypeVariable[]
    getITypeVariables2() throws CompileException {

        ClassSignature cs = this.classSignature;
        if (cs == null) return new ITypeVariable[0];

        ITypeVariable[] result = new ITypeVariable[cs.formalTypeParameters.size()];
        for (int i = 0; i < result.length; i++) {
            final FormalTypeParameter ftp = (FormalTypeParameter) cs.formalTypeParameters.get(i);
            final ITypeVariableOrIClass[] bounds = ClassFileIClass.this.getBounds(ftp);
            result[i] = new ITypeVariable() {

                @Override public String
                getName() { return ftp.identifier; }

                @Override public ITypeVariableOrIClass[]
                getBounds() { return bounds; }

                @Override public String
                toString() {
                    ITypeVariableOrIClass[] bs = this.getBounds();
                    String s = this.getName() + " extends " + bs[0];
                    for (int i = 1; i < bs.length; i++) s += " & " + bs[i];
                    return s;
                }
            };
        }

        return result;
    }

    private ITypeVariableOrIClass[]
    getBounds(SignatureParser.FormalTypeParameter ftp) throws CompileException {
        List<ITypeVariableOrIClass> result = new ArrayList<ITypeVariableOrIClass>();
        if (ftp.classBound != null) {
            result.add(this.fieldTypeSignatureToITypeVariableOrIClass(ftp.classBound));
        }
        return (ITypeVariableOrIClass[]) result.toArray(new ITypeVariableOrIClass[result.size()]);
    }

    private ITypeVariableOrIClass
    fieldTypeSignatureToITypeVariableOrIClass(FieldTypeSignature fts) throws CompileException {

        return (ITypeVariableOrIClass) fts.accept(new FieldTypeSignatureVisitor<ITypeVariableOrIClass, CompileException>() {

            @Override public ITypeVariableOrIClass
            visitArrayTypeSignature(ArrayTypeSignature ats) { throw new AssertionError(ats); }

            @Override public ITypeVariableOrIClass
            visitClassTypeSignature(ClassTypeSignature cts) throws CompileException {
                String fd = Descriptor.fromClassName(cts.packageSpecifier + cts.simpleClassName);
                IClass result;
                try {
                    result = ClassFileIClass.this.iClassLoader.loadIClass(fd);
                } catch (ClassNotFoundException cnfe) {
                    throw new CompileException("Loading \"" + Descriptor.toClassName(fd) + "\"", null, cnfe);
                }
                if (result == null) throw new CompileException("Cannot load \"" + Descriptor.toClassName(fd) + "\"", null);
                return result;
            }

            @Override public ITypeVariableOrIClass
            visitTypeVariableSignature(final TypeVariableSignature tvs) {
                return new ITypeVariable() {

                    @Override public String
                    getName() { return tvs.identifier; }

                    @Override public ITypeVariableOrIClass[]
                    getBounds() { throw new AssertionError(this); }
                };
            }
        });
    }

    @Override protected IConstructor[]
    getDeclaredIConstructors2() {
        List<IInvocable> iConstructors = new ArrayList<>();

        for (ClassFile.MethodInfo mi : this.classFile.methodInfos) {
            IInvocable ii;
            try {
                ii = this.resolveMethod(mi);
            } catch (ClassNotFoundException ex) {
                throw new InternalCompilerException(ex.getMessage(), ex);
            }
            if (ii instanceof IConstructor) iConstructors.add(ii);
        }

        return (IConstructor[]) iConstructors.toArray(new IConstructor[iConstructors.size()]);
    }

    @Override protected IMethod[]
    getDeclaredIMethods2() {
        List<IMethod> iMethods = new ArrayList<>();

        for (ClassFile.MethodInfo mi : this.classFile.methodInfos) {

            // Skip JDK 1.5 synthetic methods (e.g. those generated for
            // covariant return values).
//            if (Mod.isSynthetic(mi.getAccessFlags())) continue;

            IInvocable ii;
            try {
                ii = this.resolveMethod(mi);
            } catch (ClassNotFoundException ex) {
                throw new InternalCompilerException(ex.getMessage(), ex);
            }
            if (ii instanceof IMethod) iMethods.add((IMethod) ii);
        }

        return (IMethod[]) iMethods.toArray(new IMethod[iMethods.size()]);
    }

    @Override protected IField[]
    getDeclaredIFields2() {
        IField[] ifs = new IClass.IField[this.classFile.fieldInfos.size()];
        for (int i = 0; i < this.classFile.fieldInfos.size(); ++i) {
            try {
                ifs[i] = this.resolveField((ClassFile.FieldInfo) this.classFile.fieldInfos.get(i));
            } catch (ClassNotFoundException ex) {
                throw new InternalCompilerException(ex.getMessage(), ex);
            }
        }
        return ifs;
    }

    @Override protected IClass[]
    getDeclaredIClasses2() throws CompileException {
        ClassFile.InnerClassesAttribute ica = this.classFile.getInnerClassesAttribute();
        if (ica == null) return new IClass[0];

        List<IClass> res = new ArrayList<>();
        for (ClassFile.InnerClassesAttribute.Entry e : ica.getEntries()) {
            if (e.outerClassInfoIndex == this.classFile.thisClass) {
                try {
                    res.add(this.resolveClass(e.innerClassInfoIndex));
                } catch (ClassNotFoundException ex) {
                    throw new CompileException(ex.getMessage(), null); // SUPPRESS CHECKSTYLE AvoidHidingCause
                }
            }
        }
        return (IClass[]) res.toArray(new IClass[res.size()]);
    }

    @Override @Nullable protected IClass
    getDeclaringIClass2() throws CompileException {

        ClassFile.InnerClassesAttribute ica = this.classFile.getInnerClassesAttribute();
        if (ica == null) return null;

        for (ClassFile.InnerClassesAttribute.Entry e : ica.getEntries()) {
            if (e.innerClassInfoIndex == this.classFile.thisClass) {
                // Is this an anonymous class?
                if (e.outerClassInfoIndex == 0) return null;
                try {
                    return this.resolveClass(e.outerClassInfoIndex);
                } catch (ClassNotFoundException ex) {
                    throw new CompileException(ex.getMessage(), null); // SUPPRESS CHECKSTYLE AvoidHidingCause
                }
            }
        }
        return null;
    }

    @Override @Nullable protected IClass
    getOuterIClass2() throws CompileException {
        ClassFile.InnerClassesAttribute ica = this.classFile.getInnerClassesAttribute();
        if (ica == null) return null;

        for (ClassFile.InnerClassesAttribute.Entry e : ica.getEntries()) {
            if (e.innerClassInfoIndex == this.classFile.thisClass) {
                if (e.outerClassInfoIndex == 0) {

                    // Anonymous class or local class.
                    // TODO: Determine enclosing instance of anonymous class or local class
                    return null;
                } else  {

                    // Member type.
                    if (Mod.isStatic(e.innerClassAccessFlags)) return null;
                    try {
                        return this.resolveClass(e.outerClassInfoIndex);
                    } catch (ClassNotFoundException ex) {
                        throw new CompileException(ex.getMessage(), null); // SUPPRESS CHECKSTYLE AvoidHidingCause
                    }
                }
            }
        }
        return null;
    }

    @Override @Nullable protected IClass
    getSuperclass2() throws CompileException {
        if (this.classFile.superclass == 0 || (this.classFile.accessFlags & Mod.INTERFACE) != 0) return null;
        try {
            return this.resolveClass(this.classFile.superclass);
        } catch (ClassNotFoundException e) {
            throw new CompileException(e.getMessage(), null); // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }

    @Override public Access
    getAccess() { return ClassFileIClass.accessFlags2Access(this.accessFlags); }

    @Override public boolean
    isFinal() { return Mod.isFinal(this.accessFlags); }

    @Override protected IClass[]
    getInterfaces2() throws CompileException { return this.resolveClasses(this.classFile.interfaces); }

    @Override public boolean
    isAbstract() { return Mod.isAbstract(this.accessFlags); }

    @Override protected String
    getDescriptor2() { return Descriptor.fromClassName(this.classFile.getThisClassName()); }

    @Override public boolean
    isEnum() { return Mod.isEnum(this.accessFlags); }

    @Override public boolean
    isInterface() { return Mod.isInterface(this.accessFlags); }

    @Override public boolean
    isArray() { return false; }

    @Override public boolean
    isPrimitive() { return false; }

    @Override public boolean
    isPrimitiveNumeric() { return false; }

    @Override @Nullable protected IClass
    getComponentType2() { return null; }

    @Override protected IAnnotation[]
    getIAnnotations2() throws CompileException { return this.toIAnnotations(this.classFile.getAnnotations(true)); }

    private IAnnotation[]
    toIAnnotations(ClassFile.Annotation[] annotations) throws CompileException {

        int count = annotations.length;
        if (count == 0) return new IAnnotation[0];

        IAnnotation[] result = new IAnnotation[count];
        for (int i = 0; i < count; i++) result[i] = this.toIAnnotation(annotations[i]);
        return result;
    }

    private IAnnotation
    toIAnnotation(final ClassFile.Annotation annotation) throws CompileException {

        final Map<String, Object> evps2 = new HashMap<>();
        for (Entry<Short, ClassFile.ElementValue> e : annotation.elementValuePairs.entrySet()) {
            Short                  elementNameIndex = (Short) e.getKey();
            ClassFile.ElementValue elementValue     = (ClassFile.ElementValue) e.getValue();

            Object ev = elementValue.accept(
                new ClassFile.ElementValue.Visitor<Object, CompileException>() {

                    final ClassFile cf = ClassFileIClass.this.classFile;

                    // SUPPRESS CHECKSTYLE LineLength:13
                    @Override public Object visitBooleanElementValue(BooleanElementValue subject) { return this.getConstantValue(subject.constantValueIndex); }
                    @Override public Object visitByteElementValue(ByteElementValue subject)       { return this.getConstantValue(subject.constantValueIndex); }
                    @Override public Object visitCharElementValue(CharElementValue subject)       { return this.getConstantValue(subject.constantValueIndex); }
                    @Override public Object visitClassElementValue(ClassElementValue subject)     { return this.getConstantValue(subject.constantValueIndex); }
                    @Override public Object visitDoubleElementValue(DoubleElementValue subject)   { return this.getConstantValue(subject.constantValueIndex); }
                    @Override public Object visitFloatElementValue(FloatElementValue subject)     { return this.getConstantValue(subject.constantValueIndex); }
                    @Override public Object visitIntElementValue(IntElementValue subject)         { return this.getConstantValue(subject.constantValueIndex); }
                    @Override public Object visitLongElementValue(LongElementValue subject)       { return this.getConstantValue(subject.constantValueIndex); }
                    @Override public Object visitShortElementValue(ShortElementValue subject)     { return this.getConstantValue(subject.constantValueIndex); }
                    @Override public Object visitStringElementValue(StringElementValue subject)   { return this.getConstantValue(subject.constantValueIndex); }

                    @Override public Object
                    visitAnnotation(Annotation subject) {
                        throw new AssertionError("NYI");
                    }
                    @Override public Object
                    visitArrayElementValue(ArrayElementValue subject) throws CompileException {
                        Object[] result = new Object[subject.values.length];

                        for (int i = 0; i < result.length; i++) {
                            result[i] = subject.values[i].accept(this);
                        }

                        return result;
                    }
                    @Override public Object
                    visitEnumConstValue(EnumConstValue subject) throws CompileException {
                        IClass enumIClass;
                        try {
                            enumIClass = ClassFileIClass.this.resolveClass(
                                this.cf.getConstantUtf8(subject.typeNameIndex)
                            );
                        } catch (ClassNotFoundException cnfe) {
                            throw new CompileException("Resolving enum element value: " + cnfe.getMessage(), null);
                        }

                        String enumConstantName = this.cf.getConstantUtf8(subject.constNameIndex);

                        IField enumConstField = enumIClass.getDeclaredIField(enumConstantName);
                        if (enumConstField == null) {
                            throw new CompileException((
                                "Enum \""
                                + enumIClass
                                + "\" has no constant \""
                                + enumConstantName
                                + ""
                            ), null);
                        }

                        return enumConstField;
                    }

                    private Object
                    getConstantValue(short index) { return this.cf.getConstantValuePoolInfo(index).getValue(this.cf); }
                }
            );

            evps2.put(ClassFileIClass.this.classFile.getConstantUtf8(elementNameIndex), ev);
        }

        return new IAnnotation() {

            @Override public Object
            getElementValue(String name) { return evps2.get(name); }

            @Override public IClass
            getAnnotationType() throws CompileException {
                try {
//                    return ClassFileIClass.this.resolveClass(annotation.typeIndex);
                    return ClassFileIClass.this.resolveClass(
                        ClassFileIClass.this.classFile.getConstantUtf8(annotation.typeIndex)
                    );
                } catch (ClassNotFoundException cnfe) {
                    throw new CompileException("Resolving annotation type: " + cnfe.getMessage(), null);
                }
            }

            @Override public String
            toString() {
                String result = (
                    "@"
                    + Descriptor.toClassName(ClassFileIClass.this.classFile.getConstantUtf8(annotation.typeIndex))
                );

                StringBuilder args = null;
                for (Entry<String, Object> e : evps2.entrySet()) {
                    if (args == null) {
                        args = new StringBuilder("(");
                    } else {
                        args.append(", ");
                    }
                    args.append(e.getKey()).append(" = ").append(e.getValue());
                }

                return args == null ? result : result + args.toString() + ")";
            }
        };
    }

    /**
     * Resolves all classes referenced by this class file.
     */
    public void
    resolveAllClasses() throws ClassNotFoundException {
        for (short i = 1; i < this.classFile.getConstantPoolSize(); ++i) {
            ClassFile.ConstantPoolInfo cpi = this.classFile.getConstantPoolInfo(i);
            if (cpi instanceof ClassFile.ConstantClassInfo) {
                this.resolveClass(i);
            } else
            if (cpi instanceof ClassFile.ConstantNameAndTypeInfo) {
                String descriptor = ((ClassFile.ConstantNameAndTypeInfo) cpi).getDescriptor(this.classFile);
                if (descriptor.charAt(0) == '(') {
                    MethodDescriptor md = new MethodDescriptor(descriptor);
                    this.resolveClass(md.returnFd);
                    for (String parameterFd : md.parameterFds) this.resolveClass(parameterFd);
                } else {
                    this.resolveClass(descriptor);
                }
            }

            if (cpi.isWide()) i++;
        }
    }

    /**
     * @param index Index of the CONSTANT_Class_info to resolve (JVMS 4.4.1)
     */
    private IClass
    resolveClass(short index) throws ClassNotFoundException {
        ClassFileIClass.LOGGER.entering(null, "resolveClass", index);

        final String cnif = this.classFile.getConstantClassInfo(index).getName(this.classFile);
        try {
            return this.resolveClass(Descriptor.fromInternalForm(cnif));
        } catch (RuntimeException re) {
            throw new RuntimeException("Resolving class \"" + cnif + "\": " + re.getMessage(), re);
        }
    }

    private IClass
    resolveClass(String descriptor) throws ClassNotFoundException {
        ClassFileIClass.LOGGER.entering(null, "resolveIClass", descriptor);

        IClass result = (IClass) this.resolvedClasses.get(descriptor);
        if (result != null) return result;

        result = this.iClassLoader.loadIClass(descriptor);
        if (result == null) throw new ClassNotFoundException(descriptor);

        this.resolvedClasses.put(descriptor, result);
        return result;
    }
    private final Map<String /*descriptor*/, IClass> resolvedClasses = new HashMap<>();

    private IClass[]
    resolveClasses(short[] ifs) throws CompileException {
        IClass[] result = new IClass[ifs.length];
        for (int i = 0; i < result.length; ++i) {
            try {
                result[i] = this.resolveClass(ifs[i]);
            } catch (ClassNotFoundException e) {
                throw new CompileException(e.getMessage(), null); // SUPPRESS CHECKSTYLE AvoidHidingCause
            }
        }
        return result;
    }

    /**
     * Turns a {@link ClassFile.MethodInfo} into an {@link IInvocable}. This includes the checking and the removal of
     * the magic first parameter of an inner class constructor.
     *
     * @param methodInfo
     * @throws ClassNotFoundException
     */
    private IInvocable
    resolveMethod(final ClassFile.MethodInfo methodInfo) throws ClassNotFoundException {
        IInvocable result = (IInvocable) this.resolvedMethods.get(methodInfo);
        if (result != null) return result;

        // Determine method name.
        final String name = methodInfo.getName();

        // Determine return type.
        MethodDescriptor md = new MethodDescriptor(methodInfo.getDescriptor());

        final IClass returnType = this.resolveClass(md.returnFd);

        // Determine parameter types.
        final IClass[] parameterTypes = new IClass[md.parameterFds.length];
        for (int i = 0; i < parameterTypes.length; ++i) parameterTypes[i] = this.resolveClass(md.parameterFds[i]);

        // Determine thrown exceptions.
        IClass[]                  tes = null;
        ClassFile.AttributeInfo[] ais = methodInfo.getAttributes();
        for (ClassFile.AttributeInfo ai : ais) {
            if (ai instanceof ClassFile.ExceptionsAttribute) {
                ConstantClassInfo[] ccis = ((ClassFile.ExceptionsAttribute) ai).getExceptions(this.classFile);
                tes = new IClass[ccis.length];
                for (int i = 0; i < tes.length; ++i) {
                    tes[i] = this.resolveClass(Descriptor.fromInternalForm(ccis[i].getName(this.classFile)));
                }
            }
        }
        final IClass[] thrownExceptions = tes == null ? new IClass[0] : tes;

        // Determine access.
        final Access access = ClassFileIClass.accessFlags2Access(methodInfo.getAccessFlags());

        final IAnnotation[] iAnnotations;
        try {
            iAnnotations = ClassFileIClass.this.toIAnnotations(methodInfo.getAnnotations(true));
        } catch (CompileException ce) {
            throw new InternalCompilerException(ce.getMessage(), ce);
        }

        if ("<init>".equals(name)) {
            result = new IClass.IConstructor() {

                @Override public boolean
                isVarargs() { return Mod.isVarargs(methodInfo.getAccessFlags()); }

                @Override public IClass[]
                getParameterTypes2() throws CompileException {

                    // Process magic first parameter of inner class constructor.
                    IClass outerIClass = ClassFileIClass.this.getOuterIClass();
                    if (outerIClass != null) {
                        if (parameterTypes.length < 1) {
                            throw new InternalCompilerException("Inner class constructor lacks magic first parameter");
                        }
                        if (parameterTypes[0] != outerIClass) {
                            throw new InternalCompilerException(
                                "Magic first parameter of inner class constructor has type \""
                                + parameterTypes[0].toString()
                                + "\" instead of that of its enclosing instance (\""
                                + outerIClass.toString()
                                + "\")"
                            );
                        }
                        IClass[] tmp = new IClass[parameterTypes.length - 1];
                        System.arraycopy(parameterTypes, 1, tmp, 0, tmp.length);
                        return tmp;
                    }

                    return parameterTypes;
                }

                @Override public IClass[]      getThrownExceptions2() { return thrownExceptions; }
                @Override public Access        getAccess()            { return access;           }
                @Override public IAnnotation[] getAnnotations()       { return iAnnotations;     }
            };
        } else {

            result = new IClass.IMethod() {
                @Override public IAnnotation[] getAnnotations()       { return iAnnotations;                                } // SUPPRESS CHECKSTYLE LineLength:9
                @Override public Access        getAccess()            { return access;                                      }
                @Override public boolean       isStatic()             { return Mod.isStatic(methodInfo.getAccessFlags());   }
                @Override public boolean       isAbstract()           { return Mod.isAbstract(methodInfo.getAccessFlags()); }
                @Override public IClass        getReturnType()        { return returnType;                                  }
                @Override public String        getName()              { return name;                                        }
                @Override public IClass[]      getParameterTypes2()   { return parameterTypes;                              }
                @Override public boolean       isVarargs()            { return Mod.isVarargs(methodInfo.getAccessFlags());  }
                @Override public IClass[]      getThrownExceptions2() { return thrownExceptions;                            }
            };
        }
        this.resolvedMethods.put(methodInfo, result);
        return result;
    }

    private final Map<ClassFile.MethodInfo, IInvocable>
    resolvedMethods = new HashMap<>();

    private IField
    resolveField(final ClassFile.FieldInfo fieldInfo) throws ClassNotFoundException {
        IField result = (IField) this.resolvedFields.get(fieldInfo);
        if (result != null) return result;

        // Determine field name.
        final String name = fieldInfo.getName(this.classFile);

        // Determine field type.
        final String descriptor = fieldInfo.getDescriptor(this.classFile);
        final IClass type       = this.resolveClass(descriptor);

        // Determine optional "constant value" of the field (JLS7 15.28, bullet 14). If a field has a "ConstantValue"
        // attribute, we assume that it has a constant value. Notice that this assumption is not always correct,
        // because typical Java compilers do not generate a "ConstantValue" attribute for fields like
        // "int RED = 0", because "0" is the default value for an integer field.
        ClassFile.ConstantValueAttribute cva = null;
        for (ClassFile.AttributeInfo ai : fieldInfo.getAttributes()) {
            if (ai instanceof ClassFile.ConstantValueAttribute) {
                cva = (ClassFile.ConstantValueAttribute) ai;
                break;
            }
        }

        final Object constantValue = (
            cva == null
            ? IClass.NOT_CONSTANT
            : cva.getConstantValue(this.classFile).getValue(this.classFile)
        );

        final Access access = ClassFileIClass.accessFlags2Access(fieldInfo.getAccessFlags());

        final IAnnotation[] iAnnotations;
        try {
            iAnnotations = ClassFileIClass.this.toIAnnotations(fieldInfo.getAnnotations(true));
        } catch (CompileException ce) {
            throw new InternalCompilerException(ce.getMessage(), ce);
        }

        result = new IField() {
            @Override public Object        getConstantValue() { return constantValue;                            }
            @Override public String        getName()          { return name;                                     }
            @Override public IClass        getType()          { return type;                                     }
            @Override public boolean       isStatic()         { return Mod.isStatic(fieldInfo.getAccessFlags()); }
            @Override public Access        getAccess()        { return access;                                   }
            @Override public IAnnotation[] getAnnotations()   { return iAnnotations;                             }
        };
        this.resolvedFields.put(fieldInfo, result);
        return result;
    }

    private static Access
    accessFlags2Access(short accessFlags) {
        return (
            Mod.isPublicAccess(accessFlags)      ? Access.PUBLIC
            : Mod.isProtectedAccess(accessFlags) ? Access.PROTECTED
            : Mod.isPrivateAccess(accessFlags)   ? Access.PRIVATE
            : Access.DEFAULT
        );
    }
}
