
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

import java.io.*;

/**
 * Finds a resource by name.
 */
public abstract class ResourceFinder {

    /**
     * Find a resource by name and open it for reading
     * 
     * @param resourceName
     *            Slash-separated name that identifies the resource
     * @return <code>null</code> if the resource could not be found
     */
    public final InputStream findResourceAsStream(String resourceName) throws IOException {
        Resource resource = this.findResource(resourceName);
        if (resource == null) return null;
        return resource.open();
    }

    /**
     * Find a resource by name and return it as a {@link ResourceFinder.Resource} object.
     * 
     * @param resourceName Slash-separated name that identifies the resource
     * @return <code>null</code> if the resource could not be found
     */
    public abstract Resource findResource(String resourceName);

    /**
     * Representation of a resource than was found by a {@link ResourceFinder}.
     */
    public interface Resource {

        /**
         * Opens the resource. The caller is responsible for closing the
         * {@link java.io.InputStream}.
         */
        InputStream open() throws IOException;

        /**
         * Returns a decorative "file name" that can be used for reporting
         * errors and the like. It does not necessarily map to a file in the
         * local file system!
         */
        String getFileName();
    }

    /**
     * Representation of a resource that is a {@link java.io.File}.
     */
    public static class FileResource implements Resource {
        public FileResource(File file) { this.file = file; }
        public String getFileName() { return this.file.toString(); }
        public InputStream open() throws IOException { return new FileInputStream(this.file); }

        public File getFile() { return this.file; }

        private final File file;
    }
}