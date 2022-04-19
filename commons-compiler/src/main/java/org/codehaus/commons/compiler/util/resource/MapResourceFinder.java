
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

package org.codehaus.commons.compiler.util.resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.commons.compiler.util.Predicate;
import org.codehaus.commons.compiler.util.iterator.Iterables;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A {@link org.codehaus.commons.compiler.util.resource.ResourceFinder} that provides access to resource stored as
 * byte arrays in a {@link java.util.Map}.
 */
public
class MapResourceFinder extends ListableResourceFinder {

    private final Map<String, Resource> map = new HashMap<>();
    private long                        lastModified;

    public
    MapResourceFinder() {}

    public
    MapResourceFinder(Map<String /*fileName*/, byte[] /*data*/> map) {
        for (Entry<String, byte[]> me : map.entrySet()) {
            Resource prev = this.addResource((String) me.getKey(), (byte[]) me.getValue());
            assert prev == null;
        }
    }

    /**
     * Adds another {@link Resource}, so that it can later be found with {@link #findResource(String)}, {@link
     * #findResourceAsStream(String)} and {@link #resources()}.
     *
     * @return The resource that was previously associated with the <var>fileName</var>, or {@code null}
     */
    @Nullable public Resource
    addResource(final String fileName, final byte[] data) {
        return (Resource) this.map.put(fileName, new Resource() {
            @Override public InputStream open()         { return new ByteArrayInputStream(data);      }
            @Override public String      getFileName()  { return fileName;                            }
            @Override public long        lastModified() { return MapResourceFinder.this.lastModified; }
        });
    }

    /**
     * @param data The text to store (in platform default encoding)
     * @return The resource that was previously associated with the <var>fileName</var>, or {@code null}
     */
    @Nullable public Resource
    addResource(String fileName, String data) { return this.addResource(fileName, data.getBytes()); }

    /**
     * Adds another {@link Resource}, so that it can later be found with {@link #findResource(String)}, {@link
     * #findResourceAsStream(String)} and {@link #resources()}.
     *
     * @return The resource that was previously associated with the <var>fileName</var>, or {@code null}
     */
    public Resource
    addResource(final Resource resource) { return (Resource) this.map.put(resource.getFileName(), resource); }

    /**
     * @return All resources that were previously added with {@link #addResource(Resource)}
     */
    public Collection<Resource>
    resources() { return Collections.unmodifiableCollection(this.map.values()); }

    /**
     * @param lastModified The return value of {@link Resource#lastModified()} for the next resources added
     */
    public final void
    setLastModified(long lastModified) { this.lastModified = lastModified; }

    @Override @Nullable public final Resource
    findResource(final String resourceName) { return (Resource) this.map.get(resourceName); }

    @Override @Nullable public Iterable<Resource>
    list(final String resourceNamePrefix, final boolean recurse) {

        return Iterables.filter(this.map.values(), new Predicate<Object>() {

            @Override public boolean
            evaluate(@Nullable Object o) {
                Resource r = (Resource) o;

                assert r != null;

                String    resourceName = r.getFileName();             // E.g. "org/codehaus/janino/util/ClassFile.class"
                final int rnpl         = resourceNamePrefix.length(); // E.g. "org/codehaus/janino"
                return (
                    resourceName.startsWith(resourceNamePrefix)
                    && (recurse || (
                        resourceName.charAt(rnpl) == '/'
                        && resourceName.indexOf('/', rnpl + 1) == -1
                    ))
                );
            }
        });
    }
}
