
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A {@link FileResourceFinder} that finds file resources in a directory. The name of
 * the file is constructed by concatenating a dirctory name with the resource name such that slashes in the resource
 * name map to file separators.
 */
public
class DirectoryResourceFinder extends FileResourceFinder {

    private final File directory;

    /**
     * Keys don't have trailing file separators (like "dir\"). The "root directory" is designated by key {@code null}.
     * A {@code null} value indicates that the directory does not exist.
     */
    private final Map<String /*directoryName*/, Set<File>> subdirectoryNameToFiles = new HashMap<>();

    /**
     * @param directory the directory to use as the search base
     */
    public
    DirectoryResourceFinder(File directory) { this.directory = directory; }

    @Override public final String toString() { return "dir:" + this.directory; }

    // Implement FileResourceFinder.
    @Override @Nullable protected final File
    findResourceAsFile(String resourceName) {

        // Determine the subdirectory name (null for no subdirectory).
        int    idx              = resourceName.lastIndexOf('/');
        String subdirectoryName = (
            idx == -1 ? null :
            resourceName.substring(0, idx).replace('/', File.separatorChar)
        );

        Set<File> files = this.listFiles(subdirectoryName);
        if (files == null) return null;

        // Notice that "File.equals()" performs all the file-system dependent
        // magic like case conversion.
        File file = new File(this.directory, resourceName.replace('/', File.separatorChar));
        if (!files.contains(file)) return null;

        return file;
    }

    /**
     * @param subdirectoryName E.g {@code "java/lang"}, or {@code "java\lang"}, or {@code null}
     * @return {@code null}    iff that subdirectory does not exist
     */
    @Nullable private Set<File>
    listFiles(@Nullable String subdirectoryName) {

        // Unify file separators.
        if (subdirectoryName != null) {
            subdirectoryName = subdirectoryName.replace('/', File.separatorChar);
        }

        // Check the cache.
        {
            Set<File> files = (Set<File>) this.subdirectoryNameToFiles.get(subdirectoryName);
            if (files != null || this.subdirectoryNameToFiles.containsKey(subdirectoryName)) {

                // Cache hit!
                return files;
            }
        }

        // Determine files existing in this subdirectory.
        File subDirectory = subdirectoryName == null ? this.directory : new File(this.directory, subdirectoryName);

        File[] members = subDirectory.listFiles();
        if (members == null) {

            // Directory does not exist; put a "null" value in the cache.
            this.subdirectoryNameToFiles.put(subdirectoryName, null);
            return null;
        }

        // Reduce to "normal" files.
        Set<File> normalFiles = new HashSet<>();
        for (File file : members) {
            if (file.isFile()) normalFiles.add(file);
        }
        this.subdirectoryNameToFiles.put(subdirectoryName, normalFiles);

        return normalFiles;
    }

    @Override @Nullable public Iterable<Resource>
    list(String resourceNamePrefix, boolean recurse) {

        assert !recurse : "This implementation does not support recursive directory listings";

        int    idx              = resourceNamePrefix.lastIndexOf('/');
        String directoryName    = idx == -1 ? null : resourceNamePrefix.substring(0, idx); // No trailing slashes
        String relativeFileName = resourceNamePrefix.substring(idx + 1);                   // Contains no slashes. "" means "all".

        // List all files in the directory.
        Set<File> files = this.listFiles(directoryName);
        if (files == null) return null;

        // Select the members with the given prefix, and wrap them as FileResources.
        List<Resource> result = new ArrayList<>();
        for (File file : files) {
            if (file.getName().startsWith(relativeFileName)) {
                result.add(new FileResource(file));
            }
        }

        return result;
    }
}
