
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
 * A {@link org.codehaus.janino.util.resource.ResourceFinder} that finds its resources along a "path"
 * consisting of JAR file names, ZIP file names, and directory names.
 * @see org.codehaus.janino.util.resource.ZipFileResourceFinder
 * @see org.codehaus.janino.util.resource.DirectoryResourceFinder
 */
public class PathResourceFinder extends LazyMultiResourceFinder {

    /**
     * @param entries The entries of the "path"
     */
    public PathResourceFinder(final File[] entries) {
        super(PathResourceFinder.createIterator(Arrays.asList(entries).iterator()));
    }

    /**
     * @param entries The entries of the "path" (type must be {@link File})
     */
    public PathResourceFinder(Iterator entries) {
        super(entries);
    }

    /**
     * @param path A java-like path, i.e. a "path separator"-separated list of entries.
     */
    public PathResourceFinder(String path) {
        this(new EnumerationIterator(new StringTokenizer(path, File.pathSeparator)));
    }

    private static Iterator createIterator(final Iterator entries) {
        return new TransformingIterator(entries) {
            protected Object transform(Object o) {
                return PathResourceFinder.createResourceFinder((File) o);
            }
        };
    }

    /**
     * Break a given string up by a "separator" string. Empty components are
     * ignored.
     * <p>
     * Examples:
     * <dl>
     *   <dt>A*B*C          <dd>A, B, C
     *   <dt>**B*           <dd>B
     *   <dt>*A             <dd>A
     *   <dt>(Empty string) <dd>(Zero components)
     * </dl>
     */
    public static File[] parsePath(String s) {
        int from = 0;
        List l = new ArrayList(); // File
        for (;;) {
            int to = s.indexOf(File.pathSeparatorChar, from);
            if (to == -1) {
                if (from != s.length()) l.add(new File(s.substring(from)));
                break;
            }
            if (to != from) l.add(new File(s.substring(from, to)));
            from = to + 1;
        }
        return (File[]) l.toArray(new File[l.size()]);
    }

    /**
     * A factory method that creates a Java classpath-style ResourceFinder as
     * follows:
     * <table>
     *   <tr><th><code>entry</code></th><th>Returned {@link ResourceFinder}</th></tr>
     *   <tr><td>"*.jar" file</td><td>{@link ZipFileResourceFinder}</td></tr>
     *   <tr><td>"*.zip" file</td><td>{@link ZipFileResourceFinder}</td></tr>
     *   <tr><td>directory</td><td>{@link DirectoryResourceFinder}</td></tr>
     *   <tr><td>any other</td><td>A {@link ResourceFinder} that never finds a resource</td></tr>
     * </table>
     * @return a valid {@link ResourceFinder}
     */
    private static ResourceFinder createResourceFinder(final File entry) {

        // ZIP file or JAR file.
        if (
            (entry.getName().endsWith(".jar") || entry.getName().endsWith(".zip")) &&
            entry.isFile()
        ) {
            try {
                return new ZipFileResourceFinder(new ZipFile(entry));
            } catch (IOException e) {
                return MultiResourceFinder.EMPTY_RESOURCE_FINDER;
            }
        }

        // Directory.
        if (entry.isDirectory()) {
            return new DirectoryResourceFinder(entry);
        }

        // Invalid entry.
        return MultiResourceFinder.EMPTY_RESOURCE_FINDER;
    }
}
