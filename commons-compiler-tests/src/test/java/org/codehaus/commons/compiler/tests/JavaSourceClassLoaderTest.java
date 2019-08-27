
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2019 Arno Unkrig. All rights reserved.
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

// SUPPRESS CHECKSTYLE JavadocMethod:9999

package org.codehaus.commons.compiler.tests;

import java.io.File;
import java.util.Collection;
import java.util.ResourceBundle;

import org.codehaus.commons.compiler.AbstractJavaSourceClassLoader;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.lang.ClassLoaders;
import org.codehaus.commons.compiler.util.resource.DirectoryResourceFinder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.TestUtil;

/**
 * Tests for the {@link IJavaSourceClassLoader}.
 */
@RunWith(Parameterized.class) public
class JavaSourceClassLoaderTest {

    private final ICompilerFactory compilerFactory;

    @Parameters(name = "CompilerFactory={0}") public static Collection<Object[]>
    compilerFactories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }

    public
    JavaSourceClassLoaderTest(ICompilerFactory compilerFactory) {
        this.compilerFactory = compilerFactory;
    }

    @Test public void
    testJavaSourceClassLoader() throws Exception {

        ClassLoader extensionsClassLoader = JavaSourceClassLoaderTest.getExtensionsClassLoader();

        AbstractJavaSourceClassLoader jscl = this.compilerFactory.newJavaSourceClassLoader(extensionsClassLoader);
        jscl.setSourcePath(new File[] {
            new File("../janino/src/main/java"),
            new File("../commons-compiler/src/main/java"),
        });

        // Load the "Compiler" class.
        jscl.loadClass("org.codehaus.janino.Compiler");

        // Run "ee = new ExpressionEvaluator(); ee.cook("7"); ee.evaluate(null);".
        Object ee = jscl.loadClass("org.codehaus.janino.ExpressionEvaluator").getConstructor().newInstance();
        ee.getClass().getMethod("cook", String.class).invoke(ee, "7");
        Object result = ee.getClass().getMethod("evaluate", Object[].class).invoke(ee, new Object[] { null });
        Assert.assertEquals(7, result);
    }

    @Test public void
    testCircularSingleTypeImports() throws Exception {
        AbstractJavaSourceClassLoader jscl = this.compilerFactory.newJavaSourceClassLoader(
            ClassLoader.getSystemClassLoader().getParent()
        );
        jscl.setSourcePath(new File[] { new File("src/test/resources/testCircularSingleTypeImports/") });
        jscl.loadClass("test.Func1");
    }

    @Test public void
    testCircularStaticImports() throws Exception {
        AbstractJavaSourceClassLoader jscl = this.compilerFactory.newJavaSourceClassLoader(
            ClassLoader.getSystemClassLoader().getParent()
        );
        jscl.setSourcePath(new File[] { new File("src/test/resources/testCircularStaticImports/") });
        jscl.loadClass("test.Func1");
    }

    @Test public void
    testOverloadedStaticImports() throws Exception {
        AbstractJavaSourceClassLoader jscl = this.compilerFactory.newJavaSourceClassLoader(
            ClassLoader.getSystemClassLoader().getParent()
        );
        jscl.setSourcePath(new File[] { new File("src/test/resources/testOverloadedStaticImports/") });
        jscl.loadClass("test.StaticImports");
    }

    @Test public void
    testOverloadedSingleStaticImport() throws Exception {
        AbstractJavaSourceClassLoader jscl = this.compilerFactory.newJavaSourceClassLoader(
            ClassLoader.getSystemClassLoader().getParent()
        );
        jscl.setSourcePath(new File[] { new File("src/test/resources/testOverloadedStaticImports/") });
        jscl.loadClass("test.SingleStaticImport");
    }

    @Test @SuppressWarnings("static-method") public void
    testBundles1() throws Exception {

        ClassLoader cl = ClassLoaders.getsResourceAsStream(
            new DirectoryResourceFinder(new File("src/test/resources/testBundles/")),
            ClassLoader.getSystemClassLoader().getParent()
        );

        Assert.assertNotNull(cl.getResourceAsStream("path/to/some_resource.txt"));
    }

    @Test public void
    testBundles2() throws Exception {

        ClassLoader jscl = this.compilerFactory.newJavaSourceClassLoader(
            ClassLoaders.getsResourceAsStream(
                new DirectoryResourceFinder(new File("src/test/resources/testBundles/")),
                ClassLoader.getSystemClassLoader().getParent()
            )
        );

        Assert.assertNotNull(jscl.getResourceAsStream("path/to/some_resource.txt"));
    }

    @Test public void
    testBundles3() throws Exception {
        AbstractJavaSourceClassLoader jscl = this.compilerFactory.newJavaSourceClassLoader(
            ClassLoaders.getsResourceAsStream(
                new DirectoryResourceFinder(new File("src/test/resources/testBundles/")),
                ClassLoader.getSystemClassLoader().getParent()
            )
        );
        jscl.setSourceFinder(new DirectoryResourceFinder(new File("src/test/resources/testBundles/")));

        ResourceBundle rb = (ResourceBundle) jscl.loadClass("test.GetBundle").getDeclaredMethod("main").invoke(null);
        Assert.assertNotNull(rb);
        Assert.assertEquals("b", rb.getString("a"));
    }

    private static ClassLoader
    getExtensionsClassLoader() throws ClassNotFoundException {

        ClassLoader result = ClassLoader.getSystemClassLoader().getParent();

        // Verify that a class on the CLASSPATH cannot be loaded through that class loader.
        try {
            result.loadClass(JavaSourceClassLoaderTest.class.getName());
            Assert.fail("extensionsClassLoader should be separate from the current classloader");
        } catch (ClassNotFoundException cnfe) {
            // as we had intended
        }

        // Verify that a class on the BOOTSTRAPCLASSPATH can be loaded through the class loader.
        result.loadClass("java.lang.String");

        return result;
    }
}
