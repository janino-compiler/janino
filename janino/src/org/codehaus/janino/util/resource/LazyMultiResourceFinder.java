
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

import java.util.*;

import org.codehaus.janino.util.iterator.IteratorCollection;


/**
 * A {@link org.codehaus.janino.util.resource.ResourceFinder} that examines a set
 * of {@link org.codehaus.janino.util.resource.ResourceFinder}s lazily as it
 * searches for resources.
 */
public class LazyMultiResourceFinder extends MultiResourceFinder {

    /**
     * @param resourceFinders delegate {@link ResourceFinder}s
     */
    public LazyMultiResourceFinder(Iterator resourceFinders) {
		super(new IteratorCollection(resourceFinders));
	}
}
