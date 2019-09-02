
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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.commons.compiler.util.reflect.Classes;
import org.codehaus.commons.compiler.util.reflect.Methods;
import org.codehaus.commons.compiler.util.reflect.NoException;

/**
 * Pre-Java-9-compatible facade for Java 9's {@code java.lang.module.ModuleFinder} class.
 */
public final
class ModuleFinder {

    private static final Class<?> CLASS = Classes.load("java.lang.module.ModuleFinder");

    // SUPPRESS CHECKSTYLE ConstantName:2
    private static final Method METHOD_ofSystem = Classes.getDeclaredMethod(ModuleFinder.CLASS, "ofSystem");
    private static final Method METHOD_findAll  = Classes.getDeclaredMethod(ModuleFinder.CLASS, "findAll");

    private final /*java.lang.module.ModuleFinder*/ Object delegate;

    private
    ModuleFinder(/*java.lang.module.ModuleFinder*/ Object delegate) { this.delegate = delegate; }

    public static ModuleFinder
    ofSystem() { return new ModuleFinder(Methods.<Object, NoException>invoke(ModuleFinder.METHOD_ofSystem, null)); }

    public Set<ModuleReference> // SUPPRESS CHECKSTYLE Javadoc
    findAll() {

        return (Set<ModuleReference>) ModuleFinder.wrapModuleReferences(
            (Set<?>) Methods.<Set<?>, NoException>invoke(ModuleFinder.METHOD_findAll, this.delegate),
            new HashSet<ModuleReference>()
        );
    }

    /**
     * Wraps each {@code java.lang.module.ModuleReference} in a {@link ModuleReference} and adds these to the
     * <var>result</var>.
     *
     * @return The <var>result</var>
     */
    private static <C extends Collection<ModuleReference>> C
    wrapModuleReferences(Collection</*java.lang.module.ModuleReference*/ ?> moduleReferences, C result) {
        for (/*java.lang.module.ModuleReference*/ Object mref : moduleReferences) {
            result.add(new ModuleReference(mref));
        }
        return result;
    }
}
