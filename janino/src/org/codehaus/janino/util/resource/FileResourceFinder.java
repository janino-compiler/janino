
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
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class specializes the {@link org.codehaus.janino.util.resource.ResourceFinder}
 * for finding resources in {@link java.io.File}s.
 */
public abstract class FileResourceFinder implements ResourceFinder {
    public final InputStream findResourceAsStream(String resourceName) {
        File file = this.findResourceAsFile(resourceName);
        if (file == null) return null;
        try {
            return new FileInputStream(file);
        } catch (IOException e) {
            return null;
        }
    }
    public final URL findResource(String resourceName) {
        File file = this.findResourceAsFile(resourceName);
        if (file == null) return null;
        try {
            return new URL("file", null, file.getPath());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Converts a given resource resource name into a {@link File}.
     */
    protected abstract File findResourceAsFile(String resourceName);
}
