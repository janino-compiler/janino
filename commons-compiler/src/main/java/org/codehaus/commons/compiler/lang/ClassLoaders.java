
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

import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.NotNullByDefault;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A {@link ClassLoader} that, unlike usual {@link ClassLoader}s, does not load byte code, but reads Java source
 * code and then scans, parses, compiles and loads it into the virtual machine.
 * <p>
 *   As with any {@link ClassLoader}, it is not possible to "update" classes after they've been loaded. The way to
 *   achieve this is to give up on the {@link ClassLoaders} and create a new one.
 * </p>
 */
public final
class ClassLoaders {

    private ClassLoaders() {}

    /**
     * Creates and returns a {@link ClassLoader} that implements {@link ClassLoader#getResourceAsStream(String)} via a
     * {@link ResourceFinder}.
     * <p>
     *   Notice that {@link ClassLoader#getResource(String)} and {@link ClassLoader#getResources(String)} are
     *   <em>not</em> overridden, because a {@link ResourceFinder} cannot produce {@link URL}s.
     * </p>
     */
    public static ClassLoader
    getsResourceAsStream(final ResourceFinder finder, ClassLoader parent) {

        return new ClassLoader(parent) {

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
     * @return Finds resources via <var>classLoader</var>{@code .getResource()}
     */
    public static ResourceFinder
    getsResourceAsStream(final ClassLoader classLoader) {

        return new ResourceFinder() {

            @Override @Nullable public Resource
            findResource(final String resourceName) {

                final URL resource = classLoader.getResource(resourceName);
                if (resource == null) return null;

                return new Resource() {

                    @Override public InputStream
                    open() throws IOException { return resource.openStream(); }

                    @Override public String
                    getFileName() { return resourceName; }

                    @Override public long
                    lastModified() { return 0; }
                };
            }
        };
    }
}
