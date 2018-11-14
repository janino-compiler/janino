
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

package org.codehaus.commons.compiler.jdk.java9.java.lang.module;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.commons.compiler.jdk.java9.java.util.Optional;

/**
 * Pre-Java-9-compatible facade for Java 9's {@code java.lang.module.ModuleFinder} class.
 */
public
class ModuleFinder {

    // SUPPRESS CHECKSTYLE ConstantName|LineLength:3
    private static final Class<?> java_lang_module_ModuleFinder    = ModuleFinder.loadClass("java.lang.module.ModuleFinder");
    private static final Class<?> java_lang_module_ModuleReference = ModuleFinder.loadClass("java.lang.module.ModuleReference");
    private static final Class<?> java_util_Optional               = ModuleFinder.loadClass("java.util.Optional");

    // SUPPRESS CHECKSTYLE ConstantName|LineLength:5
    private static final Method java_lang_module_ModuleFinder_ofSystem    = ModuleFinder.getDeclaredMethod(ModuleFinder.java_lang_module_ModuleFinder,    "ofSystem");
    private static final Method java_lang_module_ModuleFinder_findAll     = ModuleFinder.getDeclaredMethod(ModuleFinder.java_lang_module_ModuleFinder,    "findAll");
    private static final Method java_lang_module_ModuleReference_location = ModuleFinder.getDeclaredMethod(ModuleFinder.java_lang_module_ModuleReference, "location");
    private static final Method java_util_Optional_get                    = ModuleFinder.getDeclaredMethod(ModuleFinder.java_util_Optional,               "get");
    private static final Method java_lang_module_ModuleReference_open     = ModuleFinder.getDeclaredMethod(ModuleFinder.java_lang_module_ModuleReference, "open");

    private static final ModuleFinder INSTANCE = new ModuleFinder();

    public static ModuleFinder
    ofSystem() { return ModuleFinder.INSTANCE; }

    public Set<ModuleReference>
    findAll() {
        Set<ModuleReference> result = new HashSet<ModuleReference>();

        try {


//          for (final java.lang.module.ModuleReference mref : java.lang.module.ModuleFinder.ofSystem().findAll()) {
            /*java.lang.module.ModuleFinder*/ Object
            moduleFinder = ModuleFinder.java_lang_module_ModuleFinder_ofSystem.invoke(null);
            for (
                final /*java.lang.module.ModuleReference*/ Object mref
                : (Set<?>) ModuleFinder.java_lang_module_ModuleFinder_findAll.invoke(moduleFinder)
            ) {

                result.add(new ModuleReference() {

                    @Override public Optional<URI>
                    location() {

                        try {

//                          return new Optional<URI>(mref.location().get());
                            // SUPPRESS CHECKSTYLE LineLength:2
                            Object /*java.util.Optional<URI>*/ optional = ModuleFinder.java_lang_module_ModuleReference_location.invoke(mref);
                            URI                                uri      = (URI) ModuleFinder.java_util_Optional_get.invoke(optional);
                            return new Optional<URI>(uri);
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    }

                    @Override public ModuleReader
                    open() throws IOException {

                        try {

//                          return new ModuleReader(mref.open());
                            return new ModuleReader(ModuleFinder.java_lang_module_ModuleReference_open.invoke(mref));
                        } catch (InvocationTargetException ite) {
                            Throwable te = ite.getTargetException();
                            if (te instanceof IOException) throw (IOException) te;
                            throw new AssertionError(ite);
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    }
                });
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }


        return result;
    }

    private static Class<?>
    loadClass(String className) {
        try {
            return ClassLoader.getSystemClassLoader().loadClass(className);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Method
    getDeclaredMethod(Class<?> declaringClass, String methodName, Class<?>... parameterTypes) {
        try {
            return declaringClass.getDeclaredMethod(methodName, parameterTypes);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
