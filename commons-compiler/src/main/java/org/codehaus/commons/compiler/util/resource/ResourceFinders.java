
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

package org.codehaus.commons.compiler.util.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.commons.compiler.lang.ClassLoaders;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Utility methods around the {@link ResourceFinder}.
 */
public final
class ResourceFinders {

    private ResourceFinders() {}

    /**
     * @return Finds resources via <var>classLoader</var>{@code .getResource()}; all found resources are {@link
     *         LocatableResource}s
     */
    public static ListableResourceFinder
    fromClassLoader(final ClassLoader classLoader) {

        return new ListableResourceFinder() {

            @Override @Nullable public Resource
            findResource(String resourceName) {

                // "ClassLoader.getResource()" doesn't like leading slashes - not clear why.
                if (resourceName.startsWith("/")) resourceName = resourceName.substring(1);

                final URL url = classLoader.getResource(resourceName);
                if (url == null) return null;

                final String finalResourceName = resourceName;
                return ResourceFinders.resourceFromUrl(url, finalResourceName);
            }

            @Override @Nullable public Iterable<Resource>
            list(String resourceNamePrefix, boolean recurse) throws IOException {

                Map<String, URL> allSubresources = ClassLoaders.getSubresources(
                    classLoader,
                    resourceNamePrefix,
                    false, // includeDirectories
                    recurse
                );

                Collection<Resource> result = new ArrayList<>(allSubresources.size());
                for (Entry<String, URL> e : allSubresources.entrySet()) {
                    final String name = e.getKey();
                    final URL    url  = e.getValue();

                    if (!name.endsWith(".class")) continue;

                    result.add(ResourceFinders.resourceFromUrl(url, name));
                }

                return result;
            }
        };
    }

    private static LocatableResource
    resourceFromUrl(final URL url, final String resourceName) {
        return new LocatableResource() {

            @Override public URL
            getLocation() throws IOException { return url; }

            @Override public InputStream
            open() throws IOException { return url.openStream(); }

            @Override public String
            getFileName() { return resourceName; }

            @Override public long
            lastModified() { return 0; }
        };
    }
}
