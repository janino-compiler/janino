
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2005, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino.util.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A {@link org.codehaus.janino.util.resource.ResourceFinder} that finds resources in a ZIP file.
 */
public class ZipFileResourceFinder implements ResourceFinder {
    private final ZipFile zipFile;

    public ZipFileResourceFinder(ZipFile zipFile) {
        this.zipFile = zipFile;
    }
    public String toString() { return "zip:" + this.zipFile.getName(); }

    // Implement ResourceFinder.

    public InputStream findResourceAsStream(String resourceName) {
        ZipEntry ze = this.zipFile.getEntry(resourceName);
        if (ze == null) return null;
        try {
            return this.zipFile.getInputStream(ze);
        } catch (IOException e) {
            return null;
        }
    }
    public URL findResource(String resourceName) {
        if (this.zipFile.getEntry(resourceName) == null) return null;
        try {
            return new URL("zip", null, this.zipFile.getName().replace(File.separatorChar, '/') + '!' + resourceName);
        } catch (MalformedURLException e) {
            throw new RuntimeException();
        }
    }
}
