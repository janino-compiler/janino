
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.commons.nullanalysis.NotNullByDefault;

/**
 * Simple logging of method invocations.
 */
public final
class ApiLog  {

    private ApiLog() {}

    /**
     * Creates and returns an object that implements <em>all</em> interfaces that the <var>delegate</var> implements.
     * All method invocations are forwarded to the <var>delegate</var>, and, after the <var>delegate</var>'s method
     * returns, logged to {@code System.err}.
     */
    public static Object
    logMethodInvocations(final Object delegate) {

        if (!Boolean.getBoolean(ApiLog.class.getName() + ".enable")) return delegate;

        final Class<? extends Object> c = delegate.getClass();

        return Proxy.newProxyInstance(
            c.getClassLoader(),
            ApiLog.getAllInterfaces(c),
            new InvocationHandler() {

                @Override @NotNullByDefault(false) public Object
                invoke(Object proxy, Method method, Object[] arguments) throws Throwable {

                    final Object returnValue;
                    try {
                        returnValue = method.invoke(delegate, arguments);
                    } catch (InvocationTargetException ite) {
                        final Throwable targetException = ite.getTargetException();

                        System.err.printf(
                            "%s.%s(%s) throws %s%n",
                            delegate.getClass().getSimpleName(),
                            method.getName(),
                            ApiLog.truncate(Arrays.deepToString(arguments)),
                            targetException
                        );

                        throw targetException;
                    }

                    System.err.printf(
                        "%s.%s(%s) => %s%n",
                        delegate.getClass().getSimpleName(),
                        method.getName(),
                        ApiLog.truncate(Arrays.deepToString(arguments)),
                        ApiLog.truncate(String.valueOf(returnValue))
                    );

                    return returnValue;
                }
            }
        );
    }

    protected static String
    truncate(String s) { return s.length() < 200 ? s : s.substring(0, 100) + "..."; }

    private static Class<?>[]
    getAllInterfaces(final Class<?> c) {
        Set<Class<?>> result = new HashSet<>();
        ApiLog.getAllInterfaces(c, result);
        return result.toArray(new Class<?>[result.size()]);
    }

    private static void
    getAllInterfaces(Class<?> c, Set<Class<?>> result) {

        if (c.isInterface()) result.add(c);

        for (Class<?> i : c.getInterfaces()) ApiLog.getAllInterfaces(i, result);

        Class<?> sc = c.getSuperclass();
        if (sc != null) ApiLog.getAllInterfaces(sc, result);
    }
}
