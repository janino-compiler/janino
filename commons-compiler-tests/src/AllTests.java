
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

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.ICompilerFactory;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * This suite executes all portable tests for all implementations on the class path, and all (implementation-)specific
 * test packages on the class path.
 */
public class AllTests extends TestSuite {

    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite("AllTests");

        // Get all implementations.
        ICompilerFactory[] allCompilerFactories = CompilerFactoryFactory.getAllCompilerFactories();
        if (allCompilerFactories.length == 0) {
            throw new Exception("No implementations of 'org.codehaus.commons.compiler' are on the class path");
        }

        // Apply the portable tests to all implementations.
        for (ICompilerFactory cf : allCompilerFactories) {
            TestSuite implementationTestSuite = new TestSuite(cf.getId());
            implementationTestSuite.addTest(new PortableTests(cf));
            suite.addTest(implementationTestSuite);
        }

        // Get all implementation-specific tests.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (
            Enumeration<URL> en = cl.getResources("org.codehaus.commons.compiler.tests.properties");
            en.hasMoreElements();
        ) {
            URL url = en.nextElement();

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

            String testClassName = properties.getProperty("test");
            if (testClassName == null) {
                throw new IllegalStateException(url.toString() + " does not specify the 'test' property");
            }

            suite.addTest((Test) Thread.currentThread().getContextClassLoader().loadClass(testClassName).newInstance());
        }

        return suite;
    }
}
