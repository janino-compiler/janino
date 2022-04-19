
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

import org.codehaus.commons.compiler.java8.java.util.stream.Stream;
import org.codehaus.commons.compiler.util.reflect.Classes;
import org.codehaus.commons.compiler.util.reflect.Methods;

/**
 * Pre-Java-9-compatible facade for Java 9's {@code java.lang.module.ModuleReader} class.
 */
public
class ModuleReader {

    private static final Class<?> CLASS = Classes.load("java.lang.module.ModuleReader");

    // SUPPRESS CHECKSTYLE ConstantName:1
    private static final Method METHOD_list = Classes.getDeclaredMethod(ModuleReader.CLASS, "list");

    private final /*java.lang.module.ModuleReader*/ Object delegate;

    public
    ModuleReader(/*java.lang.module.ModuleReader*/ Object delegate) {
        this.delegate = delegate;
    }

    public Stream<String> // SUPPRESS CHECKSTYLE Javadoc
    list() throws IOException {
        return new Stream<>(Methods.<Object, IOException>invoke(ModuleReader.METHOD_list, this.delegate));
    }
}
