
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

import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * A {@link org.codehaus.janino.util.resource.ResourceFinder} that finds its resources through a collection of
 * other {@link org.codehaus.janino.util.resource.ResourceFinder}s.
 */
public class MultiResourceFinder implements ResourceFinder {
    private final Collection resourceFinders; // One for each entry

    /**
     * @param resourceFinders The entries of the "path"
     */
    public MultiResourceFinder(Collection resourceFinders) {
        this.resourceFinders = resourceFinders;
    }

    // Implement ResourceFinder.

    public InputStream findResourceAsStream(String resourceName) {
        for (Iterator it = this.resourceFinders.iterator(); it.hasNext();) {
            ResourceFinder rf = (ResourceFinder) it.next();
            InputStream is = rf.findResourceAsStream(resourceName);
//System.err.println("*** " + resourceName + " in " + rf + "? => " + is);
            if (is != null) return is;
        }
        return null;
    }
    public URL findResource(String resourceName) {
        for (Iterator it = this.resourceFinders.iterator(); it.hasNext();) {
            ResourceFinder rf = (ResourceFinder) it.next();
            URL url = rf.findResource(resourceName);
//System.err.println("*** " + resourceName + " in " + rf + "? => " + url);
            if (url != null) return url;
        }
        return null;
    }

    /**
     * This one's useful when a resource finder is required, but cannot be created
     * for some reason.
     */
    public static final ResourceFinder EMPTY_RESOURCE_FINDER = new ResourceFinder() {
        public InputStream findResourceAsStream(String resourceName) { return null; }
        public URL findResource(String resourceName) { return null; }
        public String toString() { return "invalid entry"; }
    };
}
