
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
