
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

package org.codehaus.janino;

import java.io.*;

import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.resource.ResourceFinder;


/**
 * This {@link org.codehaus.janino.IClassLoader} loads IClasses through a
 * a {@link org.codehaus.janino.util.resource.ResourceFinder} that designates
 * {@link org.codehaus.janino.util.ClassFile}s.
 */
public class ResourceFinderIClassLoader extends IClassLoader {
    private final ResourceFinder resourceFinder;

    public ResourceFinderIClassLoader(
        ResourceFinder resourceFinder,
        IClassLoader   optionalParentIClassLoader
    ) {
        super(optionalParentIClassLoader);
        this.resourceFinder = resourceFinder;
        this.postConstruct();
    }

    protected IClass findIClass(String descriptor) {
        String className = Descriptor.toClassName(descriptor);
        InputStream is = this.resourceFinder.findResourceAsStream(className.replace('.', '/') + ".class");
        if (is == null) return null;
        IClass iClass;
        try {
            iClass = new ClassFileIClass(new ClassFile(is), this);
        } catch (IOException e) {
            throw new ClassFormatError(className);
        } finally {
            try { is.close(); } catch (IOException e) {}
        }
        this.defineIClass(iClass);
        return iClass;
    }

}
