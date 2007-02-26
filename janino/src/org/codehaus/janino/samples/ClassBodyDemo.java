
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2007, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino.samples;

import java.io.*;
import java.lang.reflect.*;

import org.codehaus.janino.*;

public class ClassBodyDemo {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("-help")) {
            System.out.println("Usage:  ClassBodyDemo <class-body> { <argument> }");
            System.out.println("        ClassBodyDemo -help");
            System.out.println("If <class-body> starts with a '@', then the class body is read");
            System.out.println("from the named file.");
            System.out.println("The <class-body> must declare a method \"public static main(String[])\"");
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
        Class c = new ClassBodyEvaluator(classBody).getClazz();

        // Invoke the "public static main(String[])" method.
        Method m = c.getMethod("main", new Class[] { String[].class });
        Integer returnValue = (Integer) m.invoke(null, new Object[] { arguments });

        // If non-VOID, print the return value.
        if (m.getReturnType() != Void.TYPE) System.out.println(DemoBase.toString(returnValue));
    }

    private ClassBodyDemo() {}

    private static String readFileToString(String fileName) throws IOException {
        Reader r = new FileReader(fileName);
        try {
            StringBuffer sb = new StringBuffer();
            char[] ca = new char[1024];
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
