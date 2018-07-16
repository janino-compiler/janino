
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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.IClass.IConstructor;
import org.codehaus.janino.IClass.IMethod;
import org.codehaus.janino.util.resource.JarDirectoriesResourceFinder;
import org.codehaus.janino.util.resource.PathResourceFinder;
import org.codehaus.janino.util.resource.ResourceFinder;

/**
 * Loads an {@link IClass} by type name.
 */
public abstract
class IClassLoader {

    private static final Logger LOGGER = Logger.getLogger(IClassLoader.class.getName());

    // The following are constants, but cannot be declared FINAL, because they are only initialized by
    // "postConstruct()".

    // SUPPRESS CHECKSTYLE MemberName|AbbreviationAsWordInName|JavadocVariable:57

    // Representations of commonly used classes.
    public IClass TYPE_java_lang_annotation_Retention;
    public IClass TYPE_java_lang_AssertionError;
    public IClass TYPE_java_lang_Boolean;
    public IClass TYPE_java_lang_Byte;
    public IClass TYPE_java_lang_Character;
    public IClass TYPE_java_lang_Class;
    public IClass TYPE_java_lang_Cloneable;
    public IClass TYPE_java_lang_Double;
    public IClass TYPE_java_lang_Enum;
    public IClass TYPE_java_lang_Error;
    public IClass TYPE_java_lang_Exception;
    public IClass TYPE_java_lang_Float;
    public IClass TYPE_java_lang_Integer;
    public IClass TYPE_java_lang_Iterable;
    public IClass TYPE_java_lang_Long;
    public IClass TYPE_java_lang_Object;
    public IClass TYPE_java_lang_Override;
    public IClass TYPE_java_lang_RuntimeException;
    public IClass TYPE_java_lang_Short;
    public IClass TYPE_java_lang_String;
    public IClass TYPE_java_lang_StringBuilder;
    public IClass TYPE_java_lang_System;
    public IClass TYPE_java_lang_Throwable;
    public IClass TYPE_java_io_Serializable;
    public IClass TYPE_java_util_Iterator;

    // Representations of commonly used methods.
    public IMethod           METH_java_lang_Enum__ordinal;
    public IMethod           METH_java_lang_Iterable__iterator;
    public IMethod           METH_java_lang_String__concat__java_lang_String;
    public IMethod           METH_java_lang_String__equals__java_lang_Object;
    public IMethod           METH_java_lang_String__hashCode;
    public IMethod           METH_java_lang_String__valueOf__int;
    public IMethod           METH_java_lang_String__valueOf__long;
    public IMethod           METH_java_lang_String__valueOf__float;
    public IMethod           METH_java_lang_String__valueOf__double;
    public IMethod           METH_java_lang_String__valueOf__char;
    public IMethod           METH_java_lang_String__valueOf__boolean;
    public IMethod           METH_java_lang_String__valueOf__java_lang_Object;
    public IMethod           METH_java_lang_StringBuilder__append__int;
    public IMethod           METH_java_lang_StringBuilder__append__long;
    public IMethod           METH_java_lang_StringBuilder__append__float;
    public IMethod           METH_java_lang_StringBuilder__append__double;
    public IMethod           METH_java_lang_StringBuilder__append__char;
    public IMethod           METH_java_lang_StringBuilder__append__boolean;
    public IMethod           METH_java_lang_StringBuilder__append__java_lang_Object;
    public IMethod           METH_java_lang_StringBuilder__append__java_lang_String;
    public IMethod           METH_java_lang_StringBuilder__toString;
    @Nullable public IMethod METH_java_lang_Throwable__addSuppressed;
    public IMethod           METH_java_util_Iterator__hasNext;
    public IMethod           METH_java_util_Iterator__next;

    // Representations of commonly used constructors.
    public IConstructor CTOR_java_lang_StringBuilder__java_lang_String;

    /**
     * @param parentIClassLoader {@code null} iff this {@link IClassLoader} has no parent
     */
    @SuppressWarnings("null") public
    IClassLoader(@Nullable IClassLoader parentIClassLoader) {
        this.parentIClassLoader = parentIClassLoader;
    }

    /**
     * This method must be called by the constructor of the <em>derived</em> class. (The reason being is that this
     * method invokes abstract {@link #loadIClass(String)} which will not work until the derived class is constructed.)
     */
    protected final void
    postConstruct() {
        try {
            this.TYPE_java_lang_annotation_Retention = this.requireType(Descriptor.JAVA_LANG_ANNOTATION_RETENTION);
            this.TYPE_java_lang_AssertionError       = this.requireType(Descriptor.JAVA_LANG_ASSERTIONERROR);
            this.TYPE_java_lang_Boolean              = this.requireType(Descriptor.JAVA_LANG_BOOLEAN);
            this.TYPE_java_lang_Byte                 = this.requireType(Descriptor.JAVA_LANG_BYTE);
            this.TYPE_java_lang_Character            = this.requireType(Descriptor.JAVA_LANG_CHARACTER);
            this.TYPE_java_lang_Class                = this.requireType(Descriptor.JAVA_LANG_CLASS);
            this.TYPE_java_lang_Cloneable            = this.requireType(Descriptor.JAVA_LANG_CLONEABLE);
            this.TYPE_java_lang_Double               = this.requireType(Descriptor.JAVA_LANG_DOUBLE);
            this.TYPE_java_lang_Enum                 = this.requireType(Descriptor.JAVA_LANG_ENUM);
            this.TYPE_java_lang_Error                = this.requireType(Descriptor.JAVA_LANG_ERROR);
            this.TYPE_java_lang_Exception            = this.requireType(Descriptor.JAVA_LANG_EXCEPTION);
            this.TYPE_java_lang_Float                = this.requireType(Descriptor.JAVA_LANG_FLOAT);
            this.TYPE_java_lang_Integer              = this.requireType(Descriptor.JAVA_LANG_INTEGER);
            this.TYPE_java_lang_Iterable             = this.requireType(Descriptor.JAVA_LANG_ITERABLE);
            this.TYPE_java_lang_Long                 = this.requireType(Descriptor.JAVA_LANG_LONG);
            this.TYPE_java_lang_Object               = this.requireType(Descriptor.JAVA_LANG_OBJECT);
            this.TYPE_java_lang_Override             = this.requireType(Descriptor.JAVA_LANG_OVERRIDE);
            this.TYPE_java_lang_RuntimeException     = this.requireType(Descriptor.JAVA_LANG_RUNTIMEEXCEPTION);
            this.TYPE_java_lang_Short                = this.requireType(Descriptor.JAVA_LANG_SHORT);
            this.TYPE_java_lang_String               = this.requireType(Descriptor.JAVA_LANG_STRING);
            this.TYPE_java_lang_StringBuilder        = this.requireType(Descriptor.JAVA_LANG_STRINGBUILDER);
            this.TYPE_java_lang_System               = this.requireType(Descriptor.JAVA_LANG_SYSTEM);
            this.TYPE_java_lang_Throwable            = this.requireType(Descriptor.JAVA_LANG_THROWABLE);
            this.TYPE_java_io_Serializable           = this.requireType(Descriptor.JAVA_IO_SERIALIZABLE);
            this.TYPE_java_util_Iterator             = this.requireType(Descriptor.JAVA_UTIL_ITERATOR);

            // SUPPRESS CHECKSTYLE LineLength:24
            this.METH_java_lang_Enum__ordinal                           = IClassLoader.requireMethod(this.TYPE_java_lang_Enum,          "ordinal");
            this.METH_java_lang_Iterable__iterator                      = IClassLoader.requireMethod(this.TYPE_java_lang_Iterable,      "iterator");
            this.METH_java_lang_String__concat__java_lang_String        = IClassLoader.requireMethod(this.TYPE_java_lang_String,        "concat",   this.TYPE_java_lang_String);
            this.METH_java_lang_String__equals__java_lang_Object        = IClassLoader.requireMethod(this.TYPE_java_lang_String,        "equals",   this.TYPE_java_lang_Object);
            this.METH_java_lang_String__hashCode                        = IClassLoader.requireMethod(this.TYPE_java_lang_String,        "hashCode");
            this.METH_java_lang_String__valueOf__int                    = IClassLoader.requireMethod(this.TYPE_java_lang_String,        "valueOf",  IClass.INT);
            this.METH_java_lang_String__valueOf__long                   = IClassLoader.requireMethod(this.TYPE_java_lang_String,        "valueOf",  IClass.LONG);
            this.METH_java_lang_String__valueOf__float                  = IClassLoader.requireMethod(this.TYPE_java_lang_String,        "valueOf",  IClass.FLOAT);
            this.METH_java_lang_String__valueOf__double                 = IClassLoader.requireMethod(this.TYPE_java_lang_String,        "valueOf",  IClass.DOUBLE);
            this.METH_java_lang_String__valueOf__char                   = IClassLoader.requireMethod(this.TYPE_java_lang_String,        "valueOf",  IClass.CHAR);
            this.METH_java_lang_String__valueOf__boolean                = IClassLoader.requireMethod(this.TYPE_java_lang_String,        "valueOf",  IClass.BOOLEAN);
            this.METH_java_lang_String__valueOf__java_lang_Object       = IClassLoader.requireMethod(this.TYPE_java_lang_String,        "valueOf",  this.TYPE_java_lang_Object);
            this.METH_java_lang_StringBuilder__append__int              = IClassLoader.requireMethod(this.TYPE_java_lang_StringBuilder, "append",   IClass.INT);
            this.METH_java_lang_StringBuilder__append__long             = IClassLoader.requireMethod(this.TYPE_java_lang_StringBuilder, "append",   IClass.LONG);
            this.METH_java_lang_StringBuilder__append__float            = IClassLoader.requireMethod(this.TYPE_java_lang_StringBuilder, "append",   IClass.FLOAT);
            this.METH_java_lang_StringBuilder__append__double           = IClassLoader.requireMethod(this.TYPE_java_lang_StringBuilder, "append",   IClass.DOUBLE);
            this.METH_java_lang_StringBuilder__append__char             = IClassLoader.requireMethod(this.TYPE_java_lang_StringBuilder, "append",   IClass.CHAR);
            this.METH_java_lang_StringBuilder__append__boolean          = IClassLoader.requireMethod(this.TYPE_java_lang_StringBuilder, "append",   IClass.BOOLEAN);
            this.METH_java_lang_StringBuilder__append__java_lang_Object = IClassLoader.requireMethod(this.TYPE_java_lang_StringBuilder, "append",   this.TYPE_java_lang_Object);
            this.METH_java_lang_StringBuilder__append__java_lang_String = IClassLoader.requireMethod(this.TYPE_java_lang_StringBuilder, "append",   this.TYPE_java_lang_String);
            this.METH_java_lang_StringBuilder__toString                 = IClassLoader.requireMethod(this.TYPE_java_lang_StringBuilder, "toString");
            this.METH_java_lang_Throwable__addSuppressed                = IClassLoader.getMethod(this.TYPE_java_lang_Throwable, "addSuppressed", this.TYPE_java_lang_Throwable);
            this.METH_java_util_Iterator__hasNext                       = IClassLoader.requireMethod(this.TYPE_java_util_Iterator,      "hasNext");
            this.METH_java_util_Iterator__next                          = IClassLoader.requireMethod(this.TYPE_java_util_Iterator,      "next");

            // SUPPRESS CHECKSTYLE LineLength:1
            this.CTOR_java_lang_StringBuilder__java_lang_String = IClassLoader.requireConstructor(this.TYPE_java_lang_StringBuilder, this.TYPE_java_lang_String);

        } catch (Exception e) {
            throw new InternalCompilerException("Cannot load simple types", e);
        }
    }

    private IClass
    requireType(String descriptor) {

        IClass result;
        try {
            result = this.loadIClass(descriptor);
        } catch (ClassNotFoundException cnfe) {
            throw new AssertionError(cnfe);
        }

        if (result != null) return result;

        throw new AssertionError("Required type \"" + descriptor + "\" not found");
    }

    /**
     * @return {@code null} iff the <var>declaringType</var> does not declare a method with that name and parameter
     *         types
     */
    @Nullable private static IMethod
    getMethod(IClass declaringType, String name, IClass... parameterTypes) {

        try {
            return declaringType.findIMethod(name, parameterTypes);
        } catch (CompileException ce) {
            throw new AssertionError(ce);
        }
    }

    /**
     * @throws AssertionError The <var>declaringType</var> does not declare a method with that name and parameter
     *         types
     */
    private static IMethod
    requireMethod(IClass declaringType, String name, IClass... parameterTypes) {

        IMethod result = IClassLoader.getMethod(declaringType, name, parameterTypes);

        if (result == null) {
            throw new AssertionError("Required method \"" + name + "\" not found in \"" + declaringType + "\"");
        }

        return result;
    }

    private static IConstructor
    requireConstructor(IClass declaringType, IClass... parameterTypes) {

        IConstructor result;
        try {
            result = declaringType.findIConstructor(parameterTypes);
        } catch (CompileException ce) {
            throw new AssertionError(ce);
        }

        if (result != null) return result;

        throw new AssertionError("Required constructor not found in \"" + declaringType + "\"");
    }

    /**
     * Gets an {@link IClass} by field descriptor.
     *
     * @param fieldDescriptor         E.g. 'Lpkg1/pkg2/Outer$Inner;'
     * @return                        {@code null} if an {@link IClass} could not be loaded
     * @throws ClassNotFoundException An exception was raised while loading the {@link IClass}
     */
    @Nullable public final IClass
    loadIClass(String fieldDescriptor) throws ClassNotFoundException {
        IClassLoader.LOGGER.entering(null, "loadIClass", fieldDescriptor);

        if (Descriptor.isPrimitive(fieldDescriptor)) {
            return (
                fieldDescriptor.equals(Descriptor.VOID)    ? IClass.VOID :
                fieldDescriptor.equals(Descriptor.BYTE)    ? IClass.BYTE :
                fieldDescriptor.equals(Descriptor.CHAR)    ? IClass.CHAR :
                fieldDescriptor.equals(Descriptor.DOUBLE)  ? IClass.DOUBLE :
                fieldDescriptor.equals(Descriptor.FLOAT)   ? IClass.FLOAT :
                fieldDescriptor.equals(Descriptor.INT)     ? IClass.INT :
                fieldDescriptor.equals(Descriptor.LONG)    ? IClass.LONG :
                fieldDescriptor.equals(Descriptor.SHORT)   ? IClass.SHORT :
                fieldDescriptor.equals(Descriptor.BOOLEAN) ? IClass.BOOLEAN :
                null
            );
        }

        // Ask parent IClassLoader first.
        if (this.parentIClassLoader != null) {
            IClass res = this.parentIClassLoader.loadIClass(fieldDescriptor);
            if (res != null) return res;
        }

        // We need to synchronize here because "unloadableIClasses" and "loadedIClasses" are unsynchronized
        // containers. Also, "findIClass()" is not thread safe.
        IClass result;

        synchronized (this) {

            // Class could not be loaded before?
            if (this.unloadableIClasses.contains(fieldDescriptor)) return null;

            // Class already loaded?
            result = (IClass) this.loadedIClasses.get(fieldDescriptor);
            if (result != null) return result;

            // Special handling for array types.
            if (Descriptor.isArrayReference(fieldDescriptor)) {

                // Load the component type.
                IClass componentIClass = this.loadIClass(
                    Descriptor.getComponentDescriptor(fieldDescriptor)
                );
                if (componentIClass == null) return null;

                // Now get and define the array type.
                IClass arrayIClass = componentIClass.getArrayIClass(this.TYPE_java_lang_Object);
                this.loadedIClasses.put(fieldDescriptor, arrayIClass);
                return arrayIClass;
            }

            // Load the class through the {@link #findIClass(String)} method implemented by the derived class.
            // By contract, {@link findIClass(String)} <em>must</em> invoke {@link #defineIClass(IClass)}!
            IClassLoader.LOGGER.log(Level.FINE, "About to call \"findIClass({0})\"", fieldDescriptor);
            result = this.findIClass(fieldDescriptor);
            if (result == null) {
                if (this.loadedIClasses.containsKey(fieldDescriptor)) {
                    throw new InternalCompilerException((
                        "\"findIClass(\""
                        + fieldDescriptor
                        + "\")\" called \"defineIClass()\", but returned null!?"
                    ));
                }
                this.unloadableIClasses.add(fieldDescriptor);
                return null;
            }
            if (!this.loadedIClasses.containsKey(fieldDescriptor)) {
                throw new InternalCompilerException((
                    "\"findIClass(\""
                    + fieldDescriptor
                    + "\")\" did not call \"defineIClass()\"!?"
                ));
            }
        }

        if (!result.getDescriptor().equalsIgnoreCase(fieldDescriptor)) {
            throw new InternalCompilerException(
                "\"findIClass()\" returned \""
                + result.getDescriptor()
                + "\" instead of \""
                + fieldDescriptor
                + "\""
            );
        }

        IClassLoader.LOGGER.exiting(null, "loadIClass", result);
        return result;
    }

    /**
     * Finds a new {@link IClass} by descriptor and calls {@link #defineIClass(IClass)}.
     * <p>
     *   Similar {@link java.lang.ClassLoader#findClass(java.lang.String)}, this method must
     * </p>
     * <ul>
     *   <li>Get an {@link IClass} object from somewhere for the given type</li>
     *   <li>Call {@link #defineIClass(IClass)} with that {@link IClass} object as the argument</li>
     *   <li>Return the {@link IClass} object</li>
     * </ul>
     * <p>
     *   The format of a {@code descriptor} is defined in JVMS 4.3.2. Typical descriptors are:
     * </p>
     * <ul>
     *   <li>{@code I} (Integer)</li>
     *   <li>{@code Lpkg1/pkg2/Clazz;} (Class declared in package)</li>
     *   <li>{@code Lpkg1/pkg2/Outer$Inner;} Member class</li>
     * </ul>
     * <p>
     *   Notice that this method is never called for array types.
     * </p>
     * <p>
     *   Notice that this method is never called from more than one thread at a time. In other words, implementations
     *   of this method need not be thread-safe.
     * </p>
     *
     * @return                        {@code null} if a class with that descriptor could not be found
     * @throws ClassNotFoundException An exception was raised while loading the class
     */
    @Nullable protected abstract IClass
    findIClass(String descriptor) throws ClassNotFoundException;

    /**
     * Defines an {@link IClass} in the context of this {@link IClassLoader}.
     * <p>
     *   This method should only be called from an implementation of {@link #findIClass(String)}.
     * </p>
     *
     * @throws InternalCompilerException A different {@link IClass} object is already defined for this type
     */
    protected final void
    defineIClass(IClass iClass) {
        String descriptor = iClass.getDescriptor();
        IClassLoader.LOGGER.log(Level.FINE, "{0}: Defined type \"{0}\"", descriptor);

        // Define.
        IClass prev = (IClass) this.loadedIClasses.put(descriptor, iClass);

        // Previously defined?
        if (prev != null) {
            throw new InternalCompilerException("Non-identical definition of IClass \"" + descriptor + "\"");
        }
    }

    /**
     * Creates an {@link IClassLoader} that looks for classes in the given "boot class path", then in the given
     * "extension directories", and then in the given "class path".
     * <p>
     *   The default for the {@code optionalBootClassPath} is the path defined in the system property
     *   "sun.boot.class.path", and the default for the {@code optionalExtensionDirs} is the path defined in the
     *   "java.ext.dirs" system property.
     * </p>
     */
    public static IClassLoader
    createJavacLikePathIClassLoader(
        @Nullable final File[] optionalBootClassPath,
        @Nullable final File[] optionalExtDirs,
        final File[]           classPath
    ) {
        ResourceFinder bootClassPathResourceFinder = new PathResourceFinder(
            optionalBootClassPath == null
            ? PathResourceFinder.parsePath(System.getProperty("sun.boot.class.path"))
            : optionalBootClassPath
        );
        ResourceFinder extensionDirectoriesResourceFinder = new JarDirectoriesResourceFinder(
            optionalExtDirs == null
            ? PathResourceFinder.parsePath(System.getProperty("java.ext.dirs"))
            : optionalExtDirs
        );
        final ResourceFinder classPathResourceFinder = new PathResourceFinder(classPath);

        // We can load classes through "ResourceFinderIClassLoader"s, which means
        // they are read into "ClassFile" objects, or we can load classes through
        // "ClassLoaderIClassLoader"s, which means they are loaded into the JVM.
        //
        // In my environment, the latter is slightly faster. No figures about
        // resource usage yet.
        //
        // In applications where the generated classes are not loaded into the
        // same JVM instance, we should avoid to use the
        // ClassLoaderIClassLoader, because that assumes that final fields have
        // a constant value, even if not compile-time-constant but only
        // initialization-time constant. The classical example is
        // "File.separator", which is non-blank final, but not compile-time-
        // constant.
        IClassLoader icl;
        icl = new ResourceFinderIClassLoader(bootClassPathResourceFinder, null);
        icl = new ResourceFinderIClassLoader(extensionDirectoriesResourceFinder, icl);
        icl = new ResourceFinderIClassLoader(classPathResourceFinder, icl);
        return icl;
    }

    @Nullable private final IClassLoader             parentIClassLoader;
    private final Map<String /*descriptor*/, IClass> loadedIClasses     = new HashMap<String, IClass>();
    private final Set<String /*descriptor*/>         unloadableIClasses = new HashSet<String>();
}
