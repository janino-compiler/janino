
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

/**
 * Finds a resource by name.
 */
public interface ResourceFinder {

    /**
     * Find a resource by name and open it for reading
     * @param resourceName Slash-separated name that identifies the resource
     * @return <code>null</code> if the resource could not be found
     */
    InputStream findResourceAsStream(String resourceName);

    /**
     * Find a resource by name and return a URL that refers to it.
     * @param resourceName Slash-separated name that identifies the resource
     * @return <code>null</code> if the resource could not be found
     */
    URL findResource(String resourceName);
}
