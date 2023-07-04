
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.ZipFile;

import org.codehaus.commons.compiler.util.StringUtil;
import org.codehaus.commons.compiler.util.iterator.TransformingIterator;

/**
 * A {@link org.codehaus.commons.compiler.util.resource.ResourceFinder} that finds its resources along a "path"
 * consisting of JAR file names, ZIP file names, and directory names.
 *
 * @see org.codehaus.commons.compiler.util.resource.ZipFileResourceFinder
 * @see org.codehaus.commons.compiler.util.resource.DirectoryResourceFinder
 */
public
class PathResourceFinder extends LazyMultiResourceFinder {

    /**
     * @param entries The entries of the "path"
     */
    public
    PathResourceFinder(final File[] entries) {
        super(PathResourceFinder.createIterator(Arrays.asList(entries).iterator()));
    }

    /**
     * @param entries The entries of the "path"
     */
    public
    PathResourceFinder(Iterator<ResourceFinder> entries) { super(entries); }

    /**
     * @param path A java-like path, i.e. a "path separator"-separated list of entries
     */
    public
    PathResourceFinder(String path) { this(StringUtil.parsePath(path)); }

    private static Iterator<ResourceFinder>
    createIterator(final Iterator<File> entries) {

        return new TransformingIterator<Object, ResourceFinder>(entries) {

            @Override protected ResourceFinder
            transform(Object o) { return PathResourceFinder.createResourceFinder((File) o); }
        };
    }

    /**
     * A factory method that creates a Java classpath-style ResourceFinder as
     * follows:
     * <table>
     *   <tr><th>{@code entry}</th><th>Returned {@link ResourceFinder}</th></tr>
     *   <tr><td>"*.jar" file</td><td>{@link ZipFileResourceFinder}</td></tr>
     *   <tr><td>"*.zip" file</td><td>{@link ZipFileResourceFinder}</td></tr>
     *   <tr><td>directory</td><td>{@link DirectoryResourceFinder}</td></tr>
     *   <tr><td>any other</td><td>A {@link ResourceFinder} that never finds a resource</td></tr>
     * </table>
     * @return a valid {@link ResourceFinder}
     */
    private static ResourceFinder
    createResourceFinder(final File entry) {

        // ZIP file or JAR file.
        // Notice: In old JDKs, the ZIP and JAR files had to be named "*.zip" and ".jar", but no longer. Issue #205
        if (
//            (entry.getName().endsWith(".jar") || entry.getName().endsWith(".zip")) &&
            entry.isFile()
        ) {
            try {
                return new ZipFileResourceFinder(new ZipFile(entry));
            } catch (IOException e) {
                return ResourceFinder.EMPTY_RESOURCE_FINDER;
            }
        }

        // Directory.
        if (entry.isDirectory()) {
            return new DirectoryResourceFinder(entry);
        }

        // Invalid entry.
        return ResourceFinder.EMPTY_RESOURCE_FINDER;
    }
}
