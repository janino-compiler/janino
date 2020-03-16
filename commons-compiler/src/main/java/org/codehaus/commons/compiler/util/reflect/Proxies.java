
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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.codehaus.commons.nullanalysis.NotNullByDefault;

public final
class Proxies {

    private Proxies() {}

    /**
     * @param methodsAndDelegateMethod Pairs of methods declared in {@code T} and methods declared in {@code
     *                                 delegate.getClass()}
     * @param <T>                      The interface that declares the <var>method</var>
     * @return                         An object that implements the interface that declares the <var>method</var>s,
     *                                 and delegates all invocations of the <var>method</var>s to the
     *                                 <var>delegate</var> and the <var>delegateMethod</var>s
     */
    public static <T> T
    newInstance(final Object delegate, final Method... methodsAndDelegateMethod) {

        Class<?> declaringInterface = methodsAndDelegateMethod[0].getDeclaringClass();
        assert declaringInterface.isInterface();

        for (int i = 0; i < methodsAndDelegateMethod.length; i += 2) {
            Method method         = methodsAndDelegateMethod[i];
            Method delegateMethod = methodsAndDelegateMethod[i + 1];

            // Verify that *all* methods aredeclared by the same interface.
            assert method.getDeclaringClass().equals(declaringInterface);

            delegateMethod.setAccessible(true);

            assert method.getReturnType().equals(delegateMethod.getReturnType());
            assert Arrays.equals(method.getParameterTypes(), delegateMethod.getParameterTypes());
        }

        @SuppressWarnings("unchecked") T result = (T) Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            new Class<?>[] { declaringInterface },
            new InvocationHandler() {

                @Override @NotNullByDefault(false) public Object
                invoke(Object proxy, Method actualMethod, Object[] args) throws Throwable {

                    for (int i = 0; i < methodsAndDelegateMethod.length; i += 2) {
                        Method method         = methodsAndDelegateMethod[i];
                        Method delegateMethod = methodsAndDelegateMethod[i + 1];

                        if (actualMethod.equals(method)) return delegateMethod.invoke(delegate, args);
                    }

                    String message = "Expected invocation of [";
                    for (int i = 2; i < methodsAndDelegateMethod.length; i += 2) {
                        Method method = methodsAndDelegateMethod[i];
                        
                        if (i > 0) message += ", ";
                        message += method;
                    }
                    message += "], but was " + actualMethod;

                    throw new AssertionError(message);
                }
            }
        );

        return result;
    }
}
