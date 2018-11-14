
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

package org.codehaus.commons.compiler.jdk.java9.java.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.codehaus.commons.compiler.jdk.java9.java.util.function.Consumer;
import org.codehaus.commons.nullanalysis.NotNullByDefault;

/**
 * Pre-Java-9-compatible facade for Java 9's {@code java.util.Stream} class.
 */
public
class Stream<T> {

    // SUPPRESS CHECKSTYLE ConstantName|LineLength:2
    private static final Class<?> java_util_stream_Stream     = Stream.loadClass("java.util.stream.Stream");
    private static final Class<?> java_util_function_Consumer = Stream.loadClass("java.util.function.Consumer");

    // SUPPRESS CHECKSTYLE ConstantName|LineLength:1
    private static final Method java_util_stream_Stream_forEach = Stream.getDeclaredMethod(Stream.java_util_stream_Stream, "forEach", Stream.java_util_function_Consumer);

    private final /*java.util.stream.Stream<T>*/ Object delegate;

    public
    Stream(/*java.util.stream.Stream<T>*/ Object delegate) {
        this.delegate = delegate;
    }

    public void
    forEach(final Consumer<? super T> action) {

        try {

//          this.delegate.forEach(new java.util.function.Consumer<T>() {
//              @Override public void accept(T o) { action.accept​(o); }
//          });
            Object pi = Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class<?>[] { Stream.java_util_function_Consumer },
                new InvocationHandler() {

                    @Override @SuppressWarnings("unchecked") @NotNullByDefault(false) public Object
                    invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        assert method.getName().equals("accept");
                        assert args.length == 1;

                        action.accept((T) args[0]);
                        return null;
                    }
                }
            );
            Stream.java_util_stream_Stream_forEach.invoke(this.delegate, pi);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
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
