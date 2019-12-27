
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

package org.codehaus.commons.compiler.lang;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.codehaus.commons.compiler.util.resource.LocatableResource;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.NotNullByDefault;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Utility methods around the {@link java.lang.ClassLoader}.
 */
public final
class ClassLoaders {

    /**
     * The {@link ClassLoader} that loads the classes on the currently executing JVM's "class path", i.e. the JARs in
     * the JRE's "lib" and "lib/ext" directories, and the JARs and class directories specified through the class path.
     */
    public static final ClassLoader CLASSPATH_CLASS_LOADER = ClassLoader.getSystemClassLoader();

    /**
     * The {@link ClassLoader} that loads the classes on the currently executing JVM's "boot class path", i.e. the JARs
     * in the JRE's "lib" and "lib/ext" directories, but not the JARs and class directories specified through the
     * {@code --classpath} command line option.
     */
    public static final ClassLoader BOOTCLASSPATH_CLASS_LOADER = ClassLoader.getSystemClassLoader().getParent();

    private ClassLoaders() {}

    /**
     * Creates and returns a {@link ClassLoader} that implements {@link ClassLoader#getResourceAsStream(String)} via a
     * {@link ResourceFinder}.
     * <p>
     *   {@link ClassLoader#getResource(String)} returns a non-{@code null} value iff then resoure finder finds a
     *   {@link LocatableResource}.
     * </p>
     * <p>
     *   Notice that {@link ClassLoader#getResources(String)} is <em>not</em> overridden.
     * </p>
     */
    public static ClassLoader
    getsResourceAsStream(final ResourceFinder finder, @Nullable ClassLoader parent) {

        return new ClassLoader(parent) {

            @Override @NotNullByDefault(false) public URL
            getResource(String resourceName) {

                {
                    URL result = super.getResource(resourceName);
                    if (result != null) return result;
                }

                Resource r = finder.findResource(resourceName);
                if (r == null) return null;

                if (r instanceof LocatableResource) {
                    try {
                        return ((LocatableResource) r).getLocation();
                    } catch (IOException ioe) {
                        return null;
                    }
                }

                return null;
            }

            @Override @NotNullByDefault(false) public InputStream
            getResourceAsStream(String resourceName) {

                {
                    InputStream result = super.getResourceAsStream(resourceName);
                    if (result != null) return result;
                }

                try {
                    return finder.findResourceAsStream(resourceName);
                } catch (IOException ioe) {
                    return null;
                }
            }
        };
    }

    /**
     * @return Finds resources via <var>classLoader</var>{@code .getResource()}; all found resources are {@link
     *         LocatableResource}s
     */
    public static ResourceFinder
    getsResourceAsStream(final ClassLoader classLoader) {

        return new ResourceFinder() {

            @Override @Nullable public Resource
            findResource(String resourceName) {

                // "ClassLoader.getResource()" doesn't like leading slashes - not clear why.
                if (resourceName.startsWith("/")) resourceName = resourceName.substring(1);

                final URL url = classLoader.getResource(resourceName);
                if (url == null) return null;

                final String finalResourceName = resourceName;
                return new LocatableResource() {

                    @Override public URL
                    getLocation() throws IOException { return url; }

                    @Override public InputStream
                    open() throws IOException { return url.openStream(); }

                    @Override public String
                    getFileName() { return finalResourceName; }

                    @Override public long
                    lastModified() { return 0; }
                };
            }
        };
    }
}
