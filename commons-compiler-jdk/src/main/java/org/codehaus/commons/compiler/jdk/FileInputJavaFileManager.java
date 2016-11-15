
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

package org.codehaus.commons.compiler.jdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.nullanalysis.NotNullByDefault;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A {@link ForwardingJavaFileManager} that maps accesses to a particular {@link Location} and {@link Kind} to a
 * path-based search in the file system.
 */
@NotNullByDefault(false) final
class FileInputJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    private final Location         location;
    private final Kind             kind;
    private final File[]           path;
    @Nullable private final String optionalCharacterEncoding;

    /**
     * @param path                      List of directories to look through
     * @param optionalCharacterEncoding Encoding of the files being read
     */
    FileInputJavaFileManager(
        JavaFileManager  delegate,
        Location         location,
        Kind             kind,
        File[]           path,
        @Nullable String optionalCharacterEncoding
    ) {
        super(delegate);
        this.location                  = location;
        this.kind                      = kind;
        this.path                      = path;
        this.optionalCharacterEncoding = optionalCharacterEncoding;
    }

    @Override public Iterable<JavaFileObject>
    list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {

        if (location == this.location && kinds.contains(this.kind)) {
            Collection<JavaFileObject> result = new ArrayList<JavaFileObject>();

            String rel = packageName.replace('.', File.separatorChar);
            for (File directory : this.path) {
                File packageDirectory = new File(directory, rel);
                result.addAll(this.list(
                    packageDirectory,
                    packageName.isEmpty() ? "" : packageName + ".",
                    this.kind,
                    recurse
                ));
            }
            return result;
        }

        return super.list(location, packageName, kinds, recurse);
    }

    /**
     * @param qualification E.g. "", or "pkg1.pkg2."
     * @return              All {@link JavaFileObject}s of the given <var>kind</var> in the given <var>directory</var>
     */
    private Collection<JavaFileObject>
    list(File directory, String qualification, Kind kind, boolean recurse) throws IOException {
        if (!directory.isDirectory()) return Collections.emptyList();

        Collection<JavaFileObject> result = new ArrayList<JavaFileObject>();
        for (String name : directory.list()) {
            File file = new File(directory, name);
            if (name.endsWith(kind.extension)) {
                result.add(new InputFileJavaFileObject(
                    file,
                    qualification + name.substring(0, name.length() - kind.extension.length())
                ));
            } else if (recurse && file.isDirectory()) {
                result.addAll(this.list(file, qualification + name + ".", kind, true));
            }
        }
        return result;
    }

    @Override public String
    inferBinaryName(Location location, JavaFileObject file) {
        if (location == this.location) return ((InputFileJavaFileObject) file).getBinaryName();

        return super.inferBinaryName(location, file);
    }

    @Override public boolean
    hasLocation(Location location) { return location == this.location || super.hasLocation(location); }

    @Override public JavaFileObject
    getJavaFileForInput(Location location, String className, Kind kind)
    throws IOException {
        assert location != null;
        assert className != null;
        assert kind != null;

        if (location == this.location && kind == this.kind) {

            // Find the source file through the source path.
            final File sourceFile;
            FIND_SOURCE: {
                String rel = className.replace('.', File.separatorChar) + kind.extension;
                for (File sourceDirectory : this.path) {
                    File f = new File(sourceDirectory, rel);
                    if (f.exists()) {
                        sourceFile = f.getCanonicalFile();
                        break FIND_SOURCE;
                    }
                }
                return null;
            }

            // Create and return a JavaFileObject.
            return new InputFileJavaFileObject(sourceFile, className);
        }

        return super.getJavaFileForInput(location, className, kind);
    }

    /**
     * A {@link JavaFileObject} that reads from a {@link File}.
     */
    private
    class InputFileJavaFileObject extends SimpleJavaFileObject {
        private final File   file;
        private final String binaryName;

        InputFileJavaFileObject(File file, String binaryName) {
            super(file.toURI(), FileInputJavaFileManager.this.kind);
            this.file       = file;
            this.binaryName = binaryName;
        }

        @Override public Reader
        openReader(boolean ignoreEncodingErrors) throws IOException {
            return (
                FileInputJavaFileManager.this.optionalCharacterEncoding == null
                ? new FileReader(this.file)
                : new InputStreamReader(
                    new FileInputStream(this.file),
                    FileInputJavaFileManager.this.optionalCharacterEncoding
                )
            );
        }

        @Override public CharSequence
        getCharContent(boolean ignoreEncodingErrors) throws IOException {
            Reader r = this.openReader(true);
            try {
                return Cookable.readString(r);
            } finally {
                r.close();
            }
        }

        String
        getBinaryName() { return this.binaryName; }
    }
}
