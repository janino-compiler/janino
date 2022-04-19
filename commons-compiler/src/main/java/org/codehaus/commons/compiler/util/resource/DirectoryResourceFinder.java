
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
    private final File                                     directory;
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

    @Nullable private Set<File>
    listFiles(@Nullable String subdirectoryName) {

        // Determine files existing in this subdirectory.
        Set<File> files = (Set<File>) this.subdirectoryNameToFiles.get(subdirectoryName);
        if (files == null && !this.subdirectoryNameToFiles.containsKey(subdirectoryName)) {
            File subDirectory = (
                subdirectoryName == null || subdirectoryName.isEmpty()
                ? this.directory
                : new File(this.directory, subdirectoryName)
            );
            File[] fa = subDirectory.listFiles();
            if (fa != null) {
                files = new HashSet<>();
                for (File file : fa) {
                    if (file.isFile()) files.add(file);
                }
            }
            this.subdirectoryNameToFiles.put(subdirectoryName, files);
        }

        return files;
    }

    @Override @Nullable public Iterable<Resource>
    list(String resourceNamePrefix, boolean recurse) {

        Set<File> files = this.listFiles(resourceNamePrefix.replace('/', File.separatorChar));
        if (files == null) return null;

        List<Resource> result = new ArrayList<>();
        for (File f : files) result.add(new FileResource(f));

        return result;
    }
}
