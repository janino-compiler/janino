
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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.janino.util.resource.JarDirectoriesResourceFinder;
import org.codehaus.janino.util.resource.PathResourceFinder;
import org.codehaus.janino.util.resource.ResourceFinder;

/** Loads an {@link IClass} by type name. */
@SuppressWarnings({ "rawtypes", "unchecked" }) public abstract
class IClassLoader {
    private static final boolean DEBUG = false;

    // The following are constants, but cannot be declared FINAL, because they are only initialized by the
    // "postConstruct()".

    // CHECKSTYLE MemberName:OFF
    // CHECKSTYLE AbbreviationAsWordInName:OFF
    // CHECKSTYLE JavadocVariable:OFF
    public IClass JAVA_LANG_OBJECT;
    public IClass JAVA_LANG_STRING;
    public IClass JAVA_LANG_CLASS;
    public IClass JAVA_LANG_THROWABLE;
    public IClass JAVA_LANG_RUNTIMEEXCEPTION;
    public IClass JAVA_LANG_EXCEPTION;
    public IClass JAVA_LANG_ERROR;
    public IClass JAVA_LANG_CLONEABLE;
    public IClass JAVA_LANG_ASSERTIONERROR;
    public IClass JAVA_LANG_OVERRIDE;
    public IClass JAVA_IO_SERIALIZABLE;

    public IClass JAVA_LANG_BOOLEAN;
    public IClass JAVA_LANG_BYTE;
    public IClass JAVA_LANG_CHARACTER;
    public IClass JAVA_LANG_SHORT;
    public IClass JAVA_LANG_INTEGER;
    public IClass JAVA_LANG_LONG;
    public IClass JAVA_LANG_FLOAT;
    public IClass JAVA_LANG_DOUBLE;
    // CHECKSTYLE JavadocVariable:ON
    // CHECKSTYLE AbbreviationAsWordInName:ON
    // CHECKSTYLE MemberName:ON

    public
    IClassLoader(IClassLoader optionalParentIClassLoader) {
        this.optionalParentIClassLoader = optionalParentIClassLoader;
    }

    /**
     * This method must be called by the constructor of the directly derived
     * class. (The reason being is that this method invokes abstract
     * {@link #loadIClass(String)} which will not work until the implementing
     * class is constructed.)
     */
    protected final void
    postConstruct() {
        try {
            this.JAVA_LANG_OBJECT           = this.loadIClass(Descriptor.JAVA_LANG_OBJECT);
            this.JAVA_LANG_STRING           = this.loadIClass(Descriptor.JAVA_LANG_STRING);
            this.JAVA_LANG_CLASS            = this.loadIClass(Descriptor.JAVA_LANG_CLASS);
            this.JAVA_LANG_THROWABLE        = this.loadIClass(Descriptor.JAVA_LANG_THROWABLE);
            this.JAVA_LANG_RUNTIMEEXCEPTION = this.loadIClass(Descriptor.JAVA_LANG_RUNTIMEEXCEPTION);
            this.JAVA_LANG_EXCEPTION        = this.loadIClass(Descriptor.JAVA_LANG_EXCEPTION);
            this.JAVA_LANG_ERROR            = this.loadIClass(Descriptor.JAVA_LANG_ERROR);
            this.JAVA_LANG_CLONEABLE        = this.loadIClass(Descriptor.JAVA_LANG_CLONEABLE);
            this.JAVA_LANG_ASSERTIONERROR   = this.loadIClass(Descriptor.JAVA_LANG_ASSERTIONERROR);
            this.JAVA_LANG_OVERRIDE         = this.loadIClass(Descriptor.JAVA_LANG_OVERRIDE);
            this.JAVA_IO_SERIALIZABLE       = this.loadIClass(Descriptor.JAVA_IO_SERIALIZABLE);

            this.JAVA_LANG_BOOLEAN   = this.loadIClass(Descriptor.JAVA_LANG_BOOLEAN);
            this.JAVA_LANG_BYTE      = this.loadIClass(Descriptor.JAVA_LANG_BYTE);
            this.JAVA_LANG_CHARACTER = this.loadIClass(Descriptor.JAVA_LANG_CHARACTER);
            this.JAVA_LANG_SHORT     = this.loadIClass(Descriptor.JAVA_LANG_SHORT);
            this.JAVA_LANG_INTEGER   = this.loadIClass(Descriptor.JAVA_LANG_INTEGER);
            this.JAVA_LANG_LONG      = this.loadIClass(Descriptor.JAVA_LANG_LONG);
            this.JAVA_LANG_FLOAT     = this.loadIClass(Descriptor.JAVA_LANG_FLOAT);
            this.JAVA_LANG_DOUBLE    = this.loadIClass(Descriptor.JAVA_LANG_DOUBLE);
        } catch (ClassNotFoundException e) {
            throw new JaninoRuntimeException("Cannot load simple types", e);
        }
    }

    /**
     * Get an {@link IClass} by field descriptor.
     *
     * @param fieldDescriptor         E.g. 'Lpkg1/pkg2/Outer$Inner;'
     * @return                        {@code null} if an {@link IClass} could not be loaded
     * @throws ClassNotFoundException An exception was raised while loading the {@link IClass}
     */
    public final IClass
    loadIClass(String fieldDescriptor) throws ClassNotFoundException {
        if (IClassLoader.DEBUG) System.out.println(this + ": Load type \"" + fieldDescriptor + "\"");

        if (Descriptor.isPrimitive(fieldDescriptor)) {
            return (
                fieldDescriptor.equals(Descriptor.VOID) ? IClass.VOID
                : fieldDescriptor.equals(Descriptor.BYTE) ? IClass.BYTE
                : fieldDescriptor.equals(Descriptor.CHAR) ? IClass.CHAR
                : fieldDescriptor.equals(Descriptor.DOUBLE) ? IClass.DOUBLE
                : fieldDescriptor.equals(Descriptor.FLOAT) ? IClass.FLOAT
                : fieldDescriptor.equals(Descriptor.INT) ? IClass.INT
                : fieldDescriptor.equals(Descriptor.LONG) ? IClass.LONG
                : fieldDescriptor.equals(Descriptor.SHORT) ? IClass.SHORT
                : fieldDescriptor.equals(Descriptor.BOOLEAN) ? IClass.BOOLEAN
                : null
            );
        }

        // Ask parent IClassLoader first.
        if (this.optionalParentIClassLoader != null) {
            IClass res = this.optionalParentIClassLoader.loadIClass(fieldDescriptor);
            if (res != null) return res;
        }

        // We need to synchronize here because "unloadableIClasses" and
        // "loadedIClasses" are unsynchronized containers.
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
                IClass arrayIClass = componentIClass.getArrayIClass(this.JAVA_LANG_OBJECT);
                this.loadedIClasses.put(fieldDescriptor, arrayIClass);
                return arrayIClass;
            }

            if (IClassLoader.DEBUG) System.out.println("call IClassLoader.findIClass(\"" + fieldDescriptor + "\")");

            // Load the class through the {@link #findIClass(String)} method implemented by the
            // derived class.
            result = this.findIClass(fieldDescriptor);
            if (result == null) {
                this.unloadableIClasses.add(fieldDescriptor);
                return null;
            }
        }

        if (!result.getDescriptor().equalsIgnoreCase(fieldDescriptor)) {
            throw new JaninoRuntimeException(
                "\"findIClass()\" returned \""
                + result.getDescriptor()
                + "\" instead of \""
                + fieldDescriptor
                + "\""
            );
        }

        if (IClassLoader.DEBUG) System.out.println(this + ": Loaded type \"" + fieldDescriptor + "\" as " + result);

        return result;
    }

    /**
     * Find a new {@link IClass} by descriptor; return <code>null</code> if a class
     * for that <code>descriptor</code> could not be found.
     * <p>
     * Similar {@link java.lang.ClassLoader#findClass(java.lang.String)}, this method
     * must
     * <ul>
     *   <li>Get an {@link IClass} object from somewhere for the given type
     *   <li>Call {@link #defineIClass(IClass)} with that {@link IClass} object as
     *       the argument
     *   <li>Return the {@link IClass} object
     * </ul>
     * <p>
     * The format of a <code>descriptor</code> is defined in JVMS 4.3.2. Typical
     * descriptors are:
     * <ul>
     *   <li><code>I</code> (Integer)
     *   <li><code>Lpkg1/pkg2/Cls;</code> (Class declared in package)
     *   <li><code>Lpkg1/pkg2/Outer$Inner;</code> Member class
     * </ul>
     * Notice that this method is never called for array types.
     * <p>
     * Notice that this method is never called from more than one thread at a time.
     * In other words, implementations of this method need not be synchronized.
     *
     * @return <code>null</code> if a class with that descriptor could not be found
     * @throws ClassNotFoundException if an exception was raised while loading the class
     */
    protected abstract IClass findIClass(String descriptor) throws ClassNotFoundException;

    /**
     * Define an {@link IClass} in the context of this {@link IClassLoader}.
     * If an {@link IClass} with that descriptor already exists, a
     * {@link RuntimeException} is thrown.
     * <p>
     * This method should only be called from an implementation of
     * {@link #findIClass(String)}.
     *
     * @throws RuntimeException A different {@link IClass} object is already defined for this type
     */
    protected final void
    defineIClass(IClass iClass) {
        String descriptor = iClass.getDescriptor();

        // Already defined?
        IClass loadedIClass = (IClass) this.loadedIClasses.get(descriptor);
        if (loadedIClass != null) {
            if (loadedIClass == iClass) return;
            throw new JaninoRuntimeException("Non-identical definition of IClass \"" + descriptor + "\"");
        }

        // Define.
        this.loadedIClasses.put(descriptor, iClass);
        if (IClassLoader.DEBUG) System.out.println(this + ": Defined type \"" + descriptor + "\"");
    }

    /**
     * Create an {@link IClassLoader} that looks for classes in the given "boot class
     * path", then in the given "extension directories", and then in the given
     * "class path".
     * <p>
     * The default for the <code>optionalBootClassPath</code> is the path defined in
     * the system property "sun.boot.class.path", and the default for the
     * <code>optionalExtensionDirs</code> is the path defined in the "java.ext.dirs"
     * system property.
     */
    public static IClassLoader
    createJavacLikePathIClassLoader(
        final File[] optionalBootClassPath,
        final File[] optionalExtDirs,
        final File[] classPath
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
        ResourceFinder classPathResourceFinder = new PathResourceFinder(classPath);

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

    private final IClassLoader                       optionalParentIClassLoader;
    private final Map/*<String descriptor, IClass>*/ loadedIClasses     = new HashMap();
    private final Set/*<String descriptor>*/         unloadableIClasses = new HashSet();
}
