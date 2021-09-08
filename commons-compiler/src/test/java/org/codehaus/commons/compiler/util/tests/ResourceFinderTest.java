
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2019 Arno Unkrig. All rights reserved.
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

package org.codehaus.commons.compiler.util.tests;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import org.codehaus.commons.compiler.lang.ClassLoaders;
import org.codehaus.commons.compiler.util.resource.LocatableResource;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceFinders;
import org.codehaus.commons.compiler.util.resource.ZipFileResourceFinder;
import org.junit.Assert;
import org.junit.Test;

// SUPPRESS CHECKSTYLE Javadoc:9999

public
class ResourceFinderTest {

    @SuppressWarnings("static-method") @Test public void
    testJarResource() throws Exception {

        URL zipFileUrl = new File("target/test-classes/foobar.zip").toURI().toURL();
        ResourceFinderTest.assertMatches("file:.*/target/test-classes/foobar\\.zip", zipFileUrl.toString());

        URLClassLoader cl = new URLClassLoader(new URL[] { zipFileUrl });
        Assert.assertNotNull(cl.getResource("foo.txt"));

        Resource r = ResourceFinders.fromClassLoader(cl).findResource("foo.txt");
        Assert.assertNotNull(r);

        Assert.assertTrue(r instanceof LocatableResource);
        URL url = ((LocatableResource) r).getLocation();
        Assert.assertEquals("jar:" + zipFileUrl + "!/foo.txt", url.toString());
    }

    @SuppressWarnings("static-method") @Test public void
    testClassLoaders() throws Exception {
        ClassLoader cl = ClassLoaders.getsResourceAsStream(
            new ZipFileResourceFinder(new ZipFile("target/test-classes/foobar.zip")),
            null // parent
        );

        Assert.assertNotNull(cl.getResourceAsStream("foo.txt"));

        URL r = cl.getResource("foo.txt");
        Assert.assertNotNull(r);

        Assert.assertEquals(
            "jar:file:target" + File.separator + "test-classes" + File.separator + "foobar.zip!foo.txt",
            r.toString()
        );

    }

    private static void
    assertMatches(String regex, String actual) {
        Assert.assertTrue("\"" + actual + "\" does not match regex \"" + regex + "\"", Pattern.matches(regex, actual));
    }
}
