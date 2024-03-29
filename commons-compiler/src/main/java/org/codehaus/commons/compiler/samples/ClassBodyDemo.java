
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

package org.codehaus.commons.compiler.samples;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.IClassBodyEvaluator;

/**
 * A test program that allows you to play with the {@link IClassBodyEvaluator} API.
 */
public final
class ClassBodyDemo {

    /***/
    public static void
    main(String[] args) throws Exception {

        if (args.length > 0 && "-help".equals(args[0])) {
            System.out.println("Usage:");
            System.out.println("  ClassBodyDemo <class-body> { <argument> }");
            System.out.println("  ClassBodyDemo -help");
            System.out.println("If <class-body> starts with a '@', then the class body is read");
            System.out.println("from the named file.");
            System.out.println("The <class-body> must declare a method \"public static void main(String[])\"");
            System.out.println("to which the <argument>s are passed. If the return type of that method is");
            System.out.println("not VOID, then the returned value is printed to STDOUT.");
            System.exit(0);
        }

        int i = 0;

        // Get class body.
        if (i >= args.length) {
            System.err.println("Class body missing; try \"-help\".");
        }
        String classBody = args[i++];
        if (classBody.startsWith("@")) classBody = ClassBodyDemo.readFileToString(classBody.substring(1));

        // Get arguments.
        String[] arguments = new String[args.length - i];
        System.arraycopy(args, i, arguments, 0, arguments.length);

        // Compile the class body.
        IClassBodyEvaluator cbe = (
            CompilerFactoryFactory
            .getDefaultCompilerFactory(ClassBodyDemo.class.getClassLoader())
            .newClassBodyEvaluator()
        );
        cbe.cook(classBody);
        Class<?> c = cbe.getClazz();

        // Invoke the "public static main(String[])" method.
        Method m           = c.getMethod("main", String[].class);
        Object returnValue = m.invoke(null, (Object) arguments);

        // If non-VOID, print the return value.
        if (m.getReturnType() != void.class) {
            System.out.println(
                returnValue instanceof Object[]
                ? Arrays.toString((Object[]) returnValue)
                : String.valueOf(returnValue)
            );
        }
    }

    private ClassBodyDemo() {}

    private static String
    readFileToString(String fileName) throws IOException {

        Reader r = new FileReader(fileName);
        try {
            StringBuilder sb = new StringBuilder();
            char[]        ca = new char[1024];
            for (;;) {
                int cnt = r.read(ca, 0, ca.length);
                if (cnt == -1) break;
                sb.append(ca, 0, cnt);
            }
            return sb.toString();
        } finally {
            r.close();
        }
    }
}
