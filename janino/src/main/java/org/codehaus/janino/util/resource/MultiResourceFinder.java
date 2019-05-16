
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

import java.util.Collection;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A {@link org.codehaus.janino.util.resource.ResourceFinder} that finds its resources through a collection of
 * other {@link org.codehaus.janino.util.resource.ResourceFinder}s.
 */
public
class MultiResourceFinder extends ResourceFinder {

    private final Collection<? extends ResourceFinder> resourceFinders; // One for each entry

    /**
     * @param resourceFinders The entries of the "path"
     */
    public
    MultiResourceFinder(Collection<? extends ResourceFinder> resourceFinders) {
        this.resourceFinders = resourceFinders;
    }

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
}
