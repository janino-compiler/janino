
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

package org.codehaus.janino.util;

import java.io.*;
import java.net.URL;

import org.codehaus.janino.util.resource.ResourceFinder;


/**
 * A {@link ClassLoader} that uses a {@link org.codehaus.janino.util.resource.ResourceFinder}
 * to find ".class" files.
 */
public class ResourceFinderClassLoader extends ClassLoader {
    private final ResourceFinder resourceFinder;

    public ResourceFinderClassLoader(ResourceFinder resourceFinder, ClassLoader parent) {
        super(parent);
        this.resourceFinder = resourceFinder;
    }

    public ResourceFinder getResourceFinder() {
        return this.resourceFinder;
    }

    /**
     * @throws ClassNotFoundException
     * @throws TunnelException Tunnels a {@link IOException}
     */
    protected Class findClass(String className) throws ClassNotFoundException {

        // Find the resource containing the class bytecode.
        InputStream is = this.resourceFinder.findResourceAsStream(className.replace('.', '/') + ".class");
        if (is == null) throw new ClassNotFoundException(className);

        // Read bytecode from the resource into a byte array.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[4096];
            for (;;) {
                int bytesRead = is.read(buffer);
                if (bytesRead == -1) break;
                baos.write(buffer, 0, bytesRead);
            }
        } catch (IOException ex) {
            throw new TunnelException(ex);
        } finally {
            try { is.close(); } catch (IOException ex) {}
        }
        byte[] ba = baos.toByteArray();

        // Define the class in this ClassLoader.
        Class clazz = super.defineClass(null, ba, 0, ba.length);

        if (!clazz.getName().equals(className)) {

            // This is a really complicated case: We may find a class file on
            // the class path that seemingly defines the class we are looking
            // for, but doesn't. This is possible if the underlying file system
            // has case-insensitive file names and/or file names that are
            // limited in length (e.g. DOS 8.3).
            throw new ClassNotFoundException(className);
        }

        return clazz;
    }

    public URL findResource(String resourceName) {
        return this.resourceFinder.findResource(resourceName);
    }
}
