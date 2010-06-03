
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

package org.codehaus.commons.compiler;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Utility class that finds implementations of {@link ICompilerFactory}s.
 */
public final class CompilerFactoryFactory {
    private CompilerFactoryFactory() {}

    private static ICompilerFactory defaultCompilerFactory = null;

    /**
     * Finds the first implementation of <code>org.codehaus.commons.compiler</code> on the class path, then loads and
     * instantiates its {@link ICompilerFactory}.
     *
     * @return           The {@link ICompilerFactory} of the first implementation on the class path
     * @throws Exception Many things can go wrong while finding and initializing the default compiler factory
     */
    public static ICompilerFactory getDefaultCompilerFactory() throws Exception {
        if (defaultCompilerFactory == null) {
            Properties properties = new Properties();
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "org.codehaus.commons.compiler.properties"
            );
            if (is == null) {
                throw new ClassNotFoundException(
                    "No implementation of org.codehaus.commons.compiler is on the class path"
                );
            }
            try {
                properties.load(is);
            } finally {
                is.close();
            }
            String compilerFactoryClassName = properties.getProperty("compilerFactory");
            defaultCompilerFactory = getCompilerFactory(compilerFactoryClassName);
        }
        return defaultCompilerFactory;
    }

    /**
     * Finds all implementation of <code>org.codehaus.commons.compiler</code> on the class path, then loads and
     * instantiates their {@link ICompilerFactory}s.
     *
     * @return           The {@link ICompilerFactory}s of all implementations on the class path
     * @throws Exception Many things can go wrong while finding and initializing compiler factories
     */
    public static ICompilerFactory[] getAllCompilerFactories() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        List/*<IConpilerFactory>*/ factories = new ArrayList();
        for (Enumeration en = cl.getResources("org.codehaus.commons.compiler.properties"); en.hasMoreElements();) {
            URL url = (URL) en.nextElement();

            Properties properties;
            {
                properties = new Properties();
                InputStream is = url.openStream();
                try {
                    properties.load(is);
                } finally {
                    is.close();
                }
            }

            String compilerFactoryClassName = properties.getProperty("compilerFactory");
            if (compilerFactoryClassName == null) {
                throw new IllegalStateException(url.toString() + " does not specify the 'compilerFactory' property");
            }

            factories.add(getCompilerFactory(compilerFactoryClassName));
        }
        return (ICompilerFactory[]) factories.toArray(new ICompilerFactory[factories.size()]);
    }

    /**
     * Loads an {@link ICompilerFactory} by class name.
     *
     * @param compilerFactoryClassName Name of a class that implements {@link ICompilerFactory}
     * @throws Exception               Many things can go wrong while loading and initializing the default compiler
     *                                 factory
     */
    public static ICompilerFactory getCompilerFactory(String compilerFactoryClassName) throws Exception {
        return (ICompilerFactory) Thread.currentThread().getContextClassLoader().loadClass(
            compilerFactoryClassName
        ).newInstance();
    }

    /**
     * @return The version of the commons-compiler specification, or <code>null</code>
     */
    public static String getSpecificationVersion() {
        return CompilerFactoryFactory.class.getPackage().getSpecificationVersion();
    }
}
