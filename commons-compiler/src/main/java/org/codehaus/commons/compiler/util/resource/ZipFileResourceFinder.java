
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A {@link org.codehaus.commons.compiler.util.resource.ResourceFinder} that finds resources in a ZIP file.
 */
public
class ZipFileResourceFinder extends ResourceFinder {
    private final ZipFile zipFile;

    public
    ZipFileResourceFinder(ZipFile zipFile) {
        this.zipFile = zipFile;
    }

    @Override public final String toString() { return "zip:" + this.zipFile.getName(); }

    // Implement ResourceFinder.

    @Override @Nullable public final Resource
    findResource(final String resourceName) {
        final ZipEntry ze = this.zipFile.getEntry(resourceName);
        if (ze == null) return null;
        return new LocatableResource() {

            @Override public URL
            getLocation() throws IOException {
                return new URL(
                    "jar",                                                                      // protocol
                    null,                                                                       // host
                    "file:" + ZipFileResourceFinder.this.zipFile.getName() + "!" + resourceName // file
                );
            }

            @Override public InputStream
            open() throws IOException {
                return ZipFileResourceFinder.this.zipFile.getInputStream(ze);
            }

            @Override public String
            getFileName() {
                return ZipFileResourceFinder.this.zipFile.getName() + ':' + resourceName;
            }

            @Override public long
            lastModified() { long l = ze.getTime(); return l == -1L ? 0L : l; }

            @Override public String
            toString() { return this.getFileName(); }
        };
    }
}
