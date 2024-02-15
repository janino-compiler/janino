
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

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Extends the {@link ResourceFinder} class with a {@link #list(String, boolean)} method.
 */
public abstract
class ListableResourceFinder extends ResourceFinder {

    /**
     * Returns all resources who's names start with a given string. Only resources with a content are included, no
     * "special entries" of any kind, e.g. directories.
     * <p>
     *   If the prefix string ends with "/", you will get a proper directory listing (<var>recurse</var>{@code =false})
     *   or directory tree (<var>recurse</var>{@code =true}). Example:
     * </p>
     * <pre>
     *   resourceFinder.list("dir/", true) =&gt;
     *     dir/
     *     dir/afile
     *     dir/bfile
     *     dir/adir/
     *     dir/adir/file
     *     dir/bdir/
     *     dir/bdir/file
     * </pre>
     * <p>
     *   Otherwise, you will get a strange subset of a directory listing, resp. directory tree, as follows:
     * </p>
     * <pre>
     *   resourceFinder.list("dir/a", true) =&gt;
     *     dir/afile
     *     dir/adir/
     *     dir/adir/file
     * </pre>
     *
     * @param resourceNamePrefix E.g. {@code ""} or {@code "java/lang/"}
     * @return                   All resources who's name starts with the given prefix; {@code null} iff
     *                           a location designated by the <var>resourceNamePrefix</var> does not exist
     */
    @Nullable public abstract Iterable<Resource>
    list(String resourceNamePrefix, boolean recurse) throws IOException;
}
