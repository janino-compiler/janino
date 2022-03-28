
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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * An {@link IClassLoader} that loads {@link IClass}es through a reflection {@link ClassLoader}.
 */
public
class ClassLoaderIClassLoader extends IClassLoader {

    private static final Logger LOGGER = Logger.getLogger(ClassLoaderIClassLoader.class.getName());

    /**
     * @param classLoader The delegate that loads the classes
     */
    public
    ClassLoaderIClassLoader(ClassLoader classLoader) {
        super(
            null   // parentIClassLoader
        );
        this.classLoader = classLoader;

        super.postConstruct();
    }

    /**
     * Equivalent to
     * <pre>
     *   ClassLoaderIClassLoader(Thread.currentThread().getContextClassLoader())
     * </pre>
     */
    public
    ClassLoaderIClassLoader() { this(Thread.currentThread().getContextClassLoader()); }

    /**
     * @return The delegate {@link ClassLoader}
     */
    public ClassLoader
    getClassLoader() { return this.classLoader; }

    @Override @Nullable protected IClass
    findIClass(String descriptor) throws ClassNotFoundException {
        ClassLoaderIClassLoader.LOGGER.entering(null, "findIClass", descriptor);

        Class<?> clazz;
        try {
            clazz = this.classLoader.loadClass(Descriptor.toClassName(descriptor));
        } catch (NoClassDefFoundError ncdfe) {

            // Handle a very special case here -- see issue #165:
            // Iff a class and a subpackage with the same name (less capitalization) exist in the same
            // package, then class may be loaded accidentially; and "ClassLoader.defaineClass()" throws, e.g.
            //   java.lang.NoClassDefFoundError: com/company/User (wrong name: com/company/user)
            // This case must be handled like "class not found".
            // Tested with
            //    adopt_openjdk-8.0.292.10-hotspot
            //    adopt_openjdk-11.0.11.9-hotspot
            //    jdk-17.0.1+12
            if (ncdfe.getMessage().contains("wrong name")) return null;

            throw ncdfe;
        } catch (ClassNotFoundException e) {

            // Determine whether the class DOES NOT EXIST, or whether there were problems loading it. That's easier
            // said than done... the following seems to work:
            // (See also https://github.com/janino-compiler/janino/issues/104).
            {
                Throwable t = e.getCause();
                while (t instanceof ClassNotFoundException) t = t.getCause();
                if (t == null) return null;
            }

            throw e;
        }

        ClassLoaderIClassLoader.LOGGER.log(Level.FINE, "clazz={0}", clazz);

        IClass result = new ReflectionIClass(clazz, this);
        this.defineIClass(result);
        return result;
    }

    private final ClassLoader classLoader;
}
