
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

/**
 * An {@link IClassLoader} that loads {@link IClass}es through a reflection
 * {@link ClassLoader}.
 */
public class ClassLoaderIClassLoader extends IClassLoader {
    private static final boolean DEBUG = false;

    public ClassLoaderIClassLoader(ClassLoader classLoader) {
        super(
            null   // optionalParentIClassLoader
        );

        if (classLoader == null) throw new RuntimeException();

        this.classLoader = classLoader;
        super.postConstruct();
    }

    /**
     * Equivalent to
     * <pre>
     *   ClassLoaderIClassLoader(Thread.currentThread().getContextClassLoader())
     * </pre>
     */
    public ClassLoaderIClassLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * Find a new {@link IClass} by descriptor.
     */
    protected IClass findIClass(String descriptor) {

        //
        // See also [ 931385 ] Janino 2.0 throwing exception on arrays of java.io.File:
        //
        // "ClassLoader.loadClass()" and "Class.forName()" should be identical,
        // but "ClassLoader.loadClass("[Ljava.lang.Object;")" throws a
        // ClassNotFoundException under JDK 1.5.0 beta.
        // Unclear whether this a beta version bug and SUN will fix this in the final
        // release, but "Class.forName()" seems to work fine in all cases, so we
        // use that.
        //

        Class clazz;
        try {
//            clazz = this.classLoader.loadClass(className);
            clazz = Class.forName(Descriptor.toClassName(descriptor), false, this.classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
        if (ClassLoaderIClassLoader.DEBUG) System.out.println("clazz = " + clazz);

        IClass result = new ReflectionIClass(clazz, this);
        this.defineIClass(result);
        return result;
    }

    private /*final*/ ClassLoader classLoader;
}
