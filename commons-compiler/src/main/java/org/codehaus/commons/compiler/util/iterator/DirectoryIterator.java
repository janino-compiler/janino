
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

package org.codehaus.commons.compiler.util.iterator;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.commons.compiler.util.Producer;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * An {@link Iterator Iterator&lt;File>} that finds regular files who's names are {@link
 * FilenameFilter#accept(java.io.File, java.lang.String) accepted} by the <var>fileNameFilter</var> and
 * <ul>
 *   <li>
 *     exist in the given <var>rootDirectory</var>, or
 *   </li>
 *   <li>
 *     exist in any subdirectory of the <var>rootDirectory</var> that is {@link FilenameFilter#accept(java.io.File,
 *     java.lang.String) accepted} by the <var>directoryNameFilter</var>.
 *   </li>
 * </ul>
 * <p>
 *   The returned iterator will throw a {@link DirectoryNotListableException} when one of the relevant directories
 *   cannot be listed.
 * </p>
 */
public
class DirectoryIterator extends ProducerIterator<File> {

    public
    DirectoryIterator(
        final File           rootDirectory,
        final FilenameFilter directoryNameFilter,
        final FilenameFilter fileNameFilter
    ) {
        super(new Producer<File>() {
            private final List<State> stateStack = DirectoryIterator.<State>newArrayList(new State(rootDirectory));

            @Override @Nullable public File
            produce() {
                while (!this.stateStack.isEmpty()) {
                    State state = this.stateStack.get(this.stateStack.size() - 1);
                    if (state.directories.hasNext()) {
                        this.stateStack.add(new State((File) state.directories.next()));
                    } else
                    if (state.files.hasNext()) {
                        File file = (File) state.files.next();
                        return file;
                    } else
                    {
                        this.stateStack.remove(this.stateStack.size() - 1);
                    }
                }
                return null;
            }

            class State {
                State(File dir) {
                    File[] entries = dir.listFiles();
                    if (entries == null) {
                        throw new DirectoryNotListableException(dir.getPath());
                    }
                    List<File> directoryList = new ArrayList<File>();
                    List<File> fileList      = new ArrayList<File>();
                    for (File entry : entries) {
                        if (entry.isDirectory()) {
                            if (directoryNameFilter.accept(dir, entry.getName())) directoryList.add(entry);
                        } else
                        if (entry.isFile()) {
                            if (fileNameFilter.accept(dir, entry.getName())) fileList.add(entry);
                        }
                    }
                    this.directories = directoryList.iterator();
                    this.files       = fileList.iterator();
                }
                final Iterator<File> directories;
                final Iterator<File> files;
            }
        });
    }

    /**
     * Creates an {@link Iterator} that returns all matching {@link File}s locatable in a <em>set</em> of root
     * directories.
     *
     * @see #DirectoryIterator(File, FilenameFilter, FilenameFilter)
     */
    public static Iterator<File>
    traverseDirectories(
        File[]         rootDirectories,
        FilenameFilter directoryNameFilter,
        FilenameFilter fileNameFilter
    ) {
        List<Iterator<File>> result = new ArrayList<Iterator<File>>();
        for (File rootDirectory : rootDirectories) {
            result.add(new DirectoryIterator(rootDirectory, directoryNameFilter, fileNameFilter));
        }
        return new MultiDimensionalIterator<File>(result.iterator(), 2);
    }

    private static <T> ArrayList<T>
    newArrayList(T initialElement) {
        ArrayList<T> result = new ArrayList<T>();
        result.add(initialElement);
        return result;
    }

    /**
     * Indicates that {@link File#listFiles()} returned {@code null} for a particular directory.
     */
    public static
    class DirectoryNotListableException extends RuntimeException {
        public DirectoryNotListableException(String message) { super(message); }
    }
}
