
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2019 Arno Unkrig. All rights reserved.
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

import java.lang.reflect.Method;

/**
 * Utility methods related to {@link Class}.
 */
public final
class Classes {

    private Classes() {}

    /**
     * Shorthand for {@link #load(ClassLoader, String) load}{@code (ClassLoader.getSystemClassLoader(),}
     * <var>className</var>{@code )}.
     */
    public static Class<?>
    load(String className) { return Classes.load(ClassLoader.getSystemClassLoader(), className); }

    /**
     * A wrapper for {@link ClassLoader#loadClass(String) <var>classLoader</var>.loadClass(<var>className</var>)} that
     * catches any exception, wraps it in an {@link AssertionError}, and throws that.
     */
    public static Class<?>
    load(ClassLoader classLoader, String className) {
        try {
            return classLoader.loadClass(className);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /**
     * A wrapper for {@link Class#getDeclaredMethod(String, Class...)
     * <var>declaringClass</var>.getDeclaredMethod(<var>methodName</var>, <var>parameterTypes</var>)} that catches any
     * exception, wraps it in an {@link AssertionError}, and throws that.
     */
    public static Method
    getDeclaredMethod(Class<?> declaringClass, String methodName, Class<?>... parameterTypes) {
        try {
            return declaringClass.getDeclaredMethod(methodName, parameterTypes);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
