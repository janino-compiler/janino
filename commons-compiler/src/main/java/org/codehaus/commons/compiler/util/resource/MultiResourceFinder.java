
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A {@link org.codehaus.commons.compiler.util.resource.ResourceFinder} that finds its resources through a collection of
 * other {@link org.codehaus.commons.compiler.util.resource.ResourceFinder}s.
 */
public
class MultiResourceFinder extends ListableResourceFinder {

    private final Iterable<? extends ResourceFinder> resourceFinders; // One for each entry

    /**
     * @param resourceFinders The entries of the "path"
     */
    public
    MultiResourceFinder(Iterable<? extends ResourceFinder> resourceFinders) {
        this.resourceFinders = resourceFinders;
    }

    /**
     * @param resourceFinders The entries of the "path"
     */
    public
    MultiResourceFinder(ResourceFinder... resourceFinders) { this(Arrays.asList(resourceFinders)); }

    // Implement ResourceFinder.

    @Override @Nullable public final Resource
    findResource(String resourceName) {
        for (ResourceFinder rf : this.resourceFinders) {
            Resource resource = rf.findResource(resourceName);
//System.err.println("*** " + resourceName + " in " + rf + "? => " + url);
            if (resource != null) return resource;
        }
        return null;
    }

    @Override @Nullable public Iterable<Resource>
    list(String resourceNamePrefix, boolean recurse) {
        List<Resource> result = new ArrayList<Resource>();
        for (ResourceFinder rf : this.resourceFinders) {
            Iterable<Resource> resources = ((ListableResourceFinder) rf).list(resourceNamePrefix, recurse);
            if (resources != null) {
                for (Resource r : resources) result.add(r);
            }
        }

        return result;
    }
}
