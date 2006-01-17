
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

/**
 * Loads an {@link IClass} by type name.
 */
public abstract class IClassLoader {
    private static final boolean DEBUG = false;

    public IClass OBJECT;
    public IClass STRING;
    public IClass CLASS;
    public IClass THROWABLE;
    public IClass RUNTIME_EXCEPTION;
    public IClass ERROR;
    public IClass CLONEABLE;
    public IClass SERIALIZABLE;

    public IClassLoader(IClassLoader optionalParentIClassLoader) {
        this.optionalParentIClassLoader = optionalParentIClassLoader;
    }

    /**
     * This method must be called by the constructor of the directly derived
     * class. (The reason being is that this method invokes abstract
     * {@link #loadIClass(String)} which will not work until the implementing
     * class is constructed.)
     */
    protected final void postConstruct() {
        this.OBJECT            = this.loadIClass(Descriptor.OBJECT);
        this.STRING            = this.loadIClass(Descriptor.STRING);
        this.CLASS             = this.loadIClass(Descriptor.CLASS);
        this.THROWABLE         = this.loadIClass(Descriptor.THROWABLE);
        this.RUNTIME_EXCEPTION = this.loadIClass(Descriptor.RUNTIME_EXCEPTION);
        this.ERROR             = this.loadIClass(Descriptor.ERROR);
        this.CLONEABLE         = this.loadIClass(Descriptor.CLONEABLE);
        this.SERIALIZABLE      = this.loadIClass(Descriptor.SERIALIZABLE);
    }

    /**
     * Get an {@link IClass} by field descriptor.
     * @return <code>null</code> if an {@link IClass} could not be loaded
     */
    public final IClass loadIClass(String fieldDescriptor) {
        if (IClassLoader.DEBUG) System.out.println(this + ": Load type \"" + fieldDescriptor + "\"");

        if (Descriptor.isPrimitive(fieldDescriptor)) {
            return (
                fieldDescriptor.equals(Descriptor.VOID   ) ? IClass.VOID    :
                fieldDescriptor.equals(Descriptor.BYTE   ) ? IClass.BYTE    :
                fieldDescriptor.equals(Descriptor.CHAR   ) ? IClass.CHAR    :
                fieldDescriptor.equals(Descriptor.DOUBLE ) ? IClass.DOUBLE  :
                fieldDescriptor.equals(Descriptor.FLOAT  ) ? IClass.FLOAT   :
                fieldDescriptor.equals(Descriptor.INT    ) ? IClass.INT     :
                fieldDescriptor.equals(Descriptor.LONG   ) ? IClass.LONG    :
                fieldDescriptor.equals(Descriptor.SHORT  ) ? IClass.SHORT   :
                fieldDescriptor.equals(Descriptor.BOOLEAN) ? IClass.BOOLEAN :
                null
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

                // This may have defined the array type as a side effect.
                IClass arrayIClass = (IClass) this.loadedIClasses.get(fieldDescriptor);
                if (arrayIClass != null) return arrayIClass;

                // Now create and define the array type.
                arrayIClass = IClass.createArrayIClass(componentIClass, this.OBJECT);
                this.loadedIClasses.put(fieldDescriptor, arrayIClass);
                return arrayIClass;
            }

            // Load the class through the {@link #findIClass(String)} method implemented by the
            // derived class.
            if (IClassLoader.DEBUG) System.out.println("call IClassLoader.findIClass(\"" + fieldDescriptor + "\")");
            result = this.findIClass(fieldDescriptor);
            if (result == null) {
                this.unloadableIClasses.add(fieldDescriptor);
                return null;
            }
        }

        if (!result.getDescriptor().equalsIgnoreCase(fieldDescriptor)) throw new RuntimeException("\"findIClass()\" returned \"" + result.getDescriptor() + "\" instead of \"" + fieldDescriptor + "\"");

        if (IClassLoader.DEBUG) System.out.println(this + ": Loaded type \"" + fieldDescriptor + "\" as " + result);

        return result;
    }

    /**
     * Returns the type of an array of the class, interface, array or
     * primitive.<br>
     * Returns "null" for "void".
     */
    public final IClass loadArrayIClass(IClass componentType) {
        IClass result = this.loadIClass('[' + componentType.getDescriptor());
        if (result == null) throw new RuntimeException("S.N.O.: Cannot determine array type of \"" + componentType.toString() + "\"");
        return result;
    }

    /**
     * Find a new {@link IClass} by descriptor; return <code>null</code> if a class
     * for that <code>descriptor</code> could not be found.
     * <p>
     * Like <code>java.lang.ClassLoader.findClass(String)</code>, this method
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
     */
    protected abstract IClass findIClass(String descriptor);

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
    protected final void defineIClass(IClass iClass) {
        String descriptor = iClass.getDescriptor();

        // Already defined?
        IClass loadedIClass = (IClass) this.loadedIClasses.get(descriptor);
        if (loadedIClass != null) {
            if (loadedIClass == iClass) return;
            throw new RuntimeException("Non-identical definition of IClass \"" + descriptor + "\"");
        }

        // Define.
        this.loadedIClasses.put(descriptor, iClass);
        if (IClassLoader.DEBUG) System.out.println(this + ": Defined type \"" + descriptor + "\"");
    }

    private final IClassLoader optionalParentIClassLoader;
    private final Map          loadedIClasses = new HashMap(); // String descriptor => IClass
    private final Set          unloadableIClasses = new HashSet(); // String descriptor
}
