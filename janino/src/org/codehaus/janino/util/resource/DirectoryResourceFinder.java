
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright 2004 Arno Unkrig
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.janino.util.resource;

import java.io.*;
import java.util.*;

/**
 * A {@link org.codehaus.janino.util.resource.FileResourceFinder} that finds file resources in
 * a directory. The name of the file is constructed by concatenating a dirctory name
 * with the resource name such that slashes in the resource name map to file
 * separators.
 */
public class DirectoryResourceFinder extends FileResourceFinder {
    private final File directory;
    private final Map  subdirectoryNameToFiles = new HashMap(); // String directoryName => Set => File

    /**
     * @param directory the directory to use as the search base
     */
    public DirectoryResourceFinder(File directory) {
        this.directory = directory;
    }

    public String toString() { return "dir:" + this.directory; }

    // Implement FileResourceFinder.
    protected File findResourceAsFile(String resourceName) {
    
        // Determine the subdirectory name (null for no subdirectory).
        int idx = resourceName.lastIndexOf('/');
        String subdirectoryName = (
            idx == -1 ? null :
            resourceName.substring(0, idx).replace('/', File.separatorChar)
        );
    
        // Determine files existing in this subdirectory.
        Set files = (Set) this.subdirectoryNameToFiles.get(subdirectoryName); // String directoryName => Set => File
        if (files == null) {
            File subDirectory = (subdirectoryName == null) ? this.directory : new File(this.directory, subdirectoryName);
            File[] fa = subDirectory.listFiles();
            files = (fa == null) ? Collections.EMPTY_SET : new HashSet(Arrays.asList(fa));
            this.subdirectoryNameToFiles.put(subdirectoryName, files);
        }
    
        // Notice that "File.equals()" performs all the file-system dependent
        // magic like case conversion.
        File file = new File(this.directory, resourceName.replace('/', File.separatorChar));
        if (!files.contains(file)) return null;
    
        return file;
    }
}
