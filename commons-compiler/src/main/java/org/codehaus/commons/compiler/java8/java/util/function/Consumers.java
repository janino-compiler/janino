
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

package org.codehaus.commons.compiler.java8.java.util.function;

import org.codehaus.commons.compiler.util.reflect.Classes;
import org.codehaus.commons.compiler.util.reflect.Methods;
import org.codehaus.commons.compiler.util.reflect.NoException;
import org.codehaus.commons.compiler.util.reflect.Proxies;

/**
 * Helper methods for the {@link Consumer} facade.
 */
public final
class Consumers {

    private Consumers() {}

    /**
     * @param delegate Must be a {@code java.util.Consumer}
     * @return         A {@link Consumer} that forwards all invocations of {@link Consumer#accept(Object)} to the
     *                 <var>delegate</var>
     */
    public static <T> Consumer<T>
    from(/*java.util.function.Consumer*/ final Object delegate) {

        return new Consumer<T>() {

            @Override public void
            accept(T t) { Methods.<Void, NoException>invoke(Consumer.METHOD_accept__T, delegate, t); }
        };
    }

    /**
     * @return A {@code java.util.Consumer} that forwards all invocations of {@code accept(T)} to the
     *         <var>delegate</var>.
     */
    public static <T> /*java.util.Consumer*/ Object
    from(Consumer<T> delegate) {
        return Proxies.newInstance(
            delegate,
            Consumer.METHOD_accept__T,
            Classes.getDeclaredMethod(delegate.getClass(), "accept", Object.class)
        );
    }
}
