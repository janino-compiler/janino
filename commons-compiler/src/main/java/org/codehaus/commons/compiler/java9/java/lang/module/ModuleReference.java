
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2018 Arno Unkrig. All rights reserved.
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

package org.codehaus.commons.compiler.java9.java.lang.module;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;

import org.codehaus.commons.compiler.java8.java.util.Optional;
import org.codehaus.commons.compiler.util.reflect.Classes;
import org.codehaus.commons.compiler.util.reflect.Methods;
import org.codehaus.commons.compiler.util.reflect.NoException;

/**
 * Pre-Java-9-compatible facade for Java 9's {@code java.lang.module.ModuleReference} interface.
 */
public
class ModuleReference {

    private static final Class<?> CLASS = Classes.load("java.lang.module.ModuleReference");

    // SUPPRESS CHECKSTYLE ConstantName:2
    private static final Method METHOD_location = Classes.getDeclaredMethod(ModuleReference.CLASS, "location");
    private static final Method METHOD_open     = Classes.getDeclaredMethod(ModuleReference.CLASS, "open");

    private final /*java.lang.module.ModuleReference*/ Object delegate;

    public
    ModuleReference(/*java.lang.module.ModuleReference*/ Object delegate) { this.delegate = delegate; }

    public Optional<URI>
    location() {
        return new Optional<URI>(Methods.<URI, NoException>invoke(ModuleReference.METHOD_location, this.delegate));
    }

    public ModuleReader
    open() throws IOException {
        return new ModuleReader(Methods.<Object, IOException>invoke(ModuleReference.METHOD_open, this.delegate));
    }
}
