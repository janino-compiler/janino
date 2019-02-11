
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved.
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

package org.codehaus.janino.util.resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A {@link org.codehaus.janino.util.resource.ResourceFinder} that provides access to resource stored as byte arrays in
 * a {@link java.util.Map}.
 */
public
class MapResourceFinder extends ResourceFinder {

    private final Map<String, Resource> map = new HashMap<String, Resource>();
    private long                        lastModified;

    public
    MapResourceFinder() {}

    public
    MapResourceFinder(Map<String /*fileName*/, byte[] /*data*/> map) {
        for (Entry<String, byte[]> me : map.entrySet()) {
            Resource prev = this.addResource(me.getKey(), me.getValue());
            assert prev == null;
        }
    }

    /**
     * @return The resource that was previously associated with the <var>fileName</var>, or {@code null}
     */
    @Nullable public Resource
    addResource(final String fileName, final byte[] data) {
        return this.map.put(fileName, new Resource() {
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

    public void
    addResource(final Resource resource) {
        this.map.put(resource.getFileName(), resource);
    }

    public Collection<Resource>
    resources() { return this.map.values(); }

    /**
     * @param lastModified The return value of {@link Resource#lastModified()} for the next resources added
     */
    public final void
    setLastModified(long lastModified) { this.lastModified = lastModified; }

    @Override @Nullable public final Resource
    findResource(final String resourceName) {
        return this.map.get(resourceName);
    }
}
