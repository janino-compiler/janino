
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
import java.util.zip.ZipFile;

import org.codehaus.janino.util.iterator.*;


/**
 * Finds resources in "*.jar" files that exist in a given set of directories.
 */
public class JarDirectoriesResourceFinder extends LazyMultiResourceFinder {

    /**
     * @param directories The set of directories to search for JAR files.
     */
    public JarDirectoriesResourceFinder(final File[] directories) {
        this(Arrays.asList(directories).iterator());
    }
    public JarDirectoriesResourceFinder(
        Iterator directoriesIterator  // File
    ) {
        super(JarDirectoriesResourceFinder.createIterator(directoriesIterator));
    }

    private static Iterator createIterator(
        Iterator directoriesIterator  // File
    ) {
        return new MultiDimensionalIterator(

            // Iterate over directories.
            new TransformingIterator(directoriesIterator) {
                protected Object transform(Object o) {

                    // Iterate over JAR files.
                    File[] jarFiles = ((File) o).listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) { return name.endsWith(".jar"); }
                    });
                    return new TransformingIterator(Arrays.asList(jarFiles).iterator()) {
                        protected Object transform(Object o) {
                            try {
                                return new ZipFileResourceFinder(new ZipFile((File) o));
                            } catch (IOException e) {
                                return MultiResourceFinder.EMPTY_RESOURCE_FINDER;
                            }
                        }
                    };
                }
            },
            2
        );
    }
}
