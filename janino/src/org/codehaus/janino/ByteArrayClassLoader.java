
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * This {@link ClassLoader} allows for the loading of a set of Java<sup>TM</sup> classes
 * provided in class file format.
 */
public class ByteArrayClassLoader extends ClassLoader {

    /**
     * The given {@link Map} of classes must not be modified afterwards.
     * 
     * @param classes String className => byte[] data
     */
    public ByteArrayClassLoader(Map classes) {
        this.classes = classes;
    }

    /**
     * @see #ByteArrayClassLoader(Map)
     */
    public ByteArrayClassLoader(Map classes, ClassLoader parent) {
        super(parent);
        this.classes = classes;
    }

    /**
     * Implements {@link ClassLoader#findClass(String)}.
     * <p>
     * Notice that, although nowhere documented, no more than one thread at a time calls this
     * method, because {@link ClassLoader#loadClass(java.lang.String)} is
     * <code>synchronized</code>.
     */
    protected Class findClass(String name) throws ClassNotFoundException {
        byte[] data = (byte[]) this.classes.get(name);
        if (data == null) throw new ClassNotFoundException(name); 

        return super.defineClass(
            name,                // name
            data, 0, data.length // b, off, len
        );
    }

    /**
     * An object is regarded equal to <code>this</code> iff
     * <ul>
     *   <li>It is an instance of {@link ByteArrayClassLoader}
     *   <li>{@link #equals(ByteArrayClassLoader)} returns <code>true</code>
     * </ul>
     * @see #equals(ByteArrayClassLoader)
     */
    public boolean equals(Object that) {
        return that instanceof ByteArrayClassLoader && this.equals((ByteArrayClassLoader) that);
    }

    /**
     * Two {@link ByteArrayClassLoader}s are regarded equal iff
     * <ul>
     *   <li>Both have the same parent {@link ClassLoader}
     *   <li>Exactly the same classes (name, bytecode) were added to both
     * </ul>
     * Roughly speaking, equal {@link ByteArrayClassLoader}s will return functionally identical
     * {@link Class}es on {@link ClassLoader#loadClass(java.lang.String)}.
     */
    public boolean equals(ByteArrayClassLoader that) {
        if (this == that) return true;

        if (this.getParent() != that.getParent()) return false;

        if (this.classes.size() != that.classes.size()) return false;
        for (Iterator it = this.classes.entrySet().iterator(); it.hasNext();) {
            Map.Entry me = (Map.Entry) it.next();
            byte[] ba = (byte[]) that.classes.get(me.getKey());
            if (ba == null) return false;
            if (!Arrays.equals((byte[]) me.getValue(), ba)) return false;
        }
        return true;
    }

    private final Map classes; // String className => byte[] data
}
