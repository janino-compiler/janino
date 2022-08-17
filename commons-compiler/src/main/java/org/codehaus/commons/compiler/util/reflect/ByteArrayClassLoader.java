
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.commons.compiler.util.reflect;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * This {@link ClassLoader} allows for the loading of a set of Java classes provided in class file format.
 */
public
class ByteArrayClassLoader extends ClassLoader {

    /**
     * The given {@link Map} of classes must not be modified afterwards.
     *
     * @param classes String className =&gt; byte[] data, or String classFileName =&gt; byte[] data
     */
    public
    ByteArrayClassLoader(Map<String /*className*/, byte[] /*data*/> classes) { this.classes = classes; }

    /**
     * @param classes {@code String className} =&gt; {@code byte[] data}, or {@code String classFileName} =&gt; {@code
     *                byte[] data}
     */
    public
    ByteArrayClassLoader(Map<String /*className*/, byte[] /*data*/> classes, ClassLoader parent) {
        super(parent);
        this.classes = classes;
    }

    public void
    setResources(Map<String /*resourceName*/, byte[] /*data*/> resources) {
        this.resources = resources;
    }

    /**
     * Implements {@link ClassLoader#findClass(String)}.
     * <p>
     *   Notice that, although nowhere documented, no more than one thread at a time calls this method, because {@link
     *   ClassLoader#loadClass(java.lang.String)} is {@code synchronized}.
     * </p>
     */
    @Override protected Class<?>
    findClass(@Nullable String name) throws ClassNotFoundException {
        assert name != null;

        byte[] data = (byte[]) this.classes.get(name);
        if (data == null) {
            data = (byte[]) this.classes.get(name.replace('.', '/') + ".class");
            if (data == null) throw new ClassNotFoundException(name);
        }

        // Notice: Not inheriting the protection domain will cause problems with Java Web Start /
        // JNLP. See
        //     http://jira.codehaus.org/browse/JANINO-104
        //     http://www.nabble.com/-Help-jel--java.security.AccessControlException-to13073723.html
        return super.defineClass(
            name,                                 // name
            data,                                 // b
            0,                                    // off
            data.length,                          // len
            this.getClass().getProtectionDomain() // protectionDomain
        );
    }

    @Override public InputStream
    getResourceAsStream(String name) {

        InputStream result = super.getResourceAsStream(name);
        if (result != null) return result;

        byte[] ba = (byte[]) this.resources.get(name);
        return ba == null ? null : new ByteArrayInputStream(ba);
    }


    private final Map<String /*className-or-classFileName*/, byte[] /*data*/> classes;
    private Map<String, byte[]>                                               resources = Collections.emptyMap();
}
