
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright 2004 Arno Unkrig
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.janino.samples;

import java.lang.reflect.*;

import org.codehaus.janino.*;

/**
 * A test program that allows you to play around with the
 * {@link org.codehaus.janino.ClassBodyEvaluator ClassBodyEvaluator} class.
 */

public class ClassBodyDemo extends DemoBase {
    public static void main(String[] args) throws Exception {
        String script = (
            "import java.util.*;\n" +
            "\n" +
            "public static int add(int a, int b) {\n" +
            "    return a + b;\n" +
            "}\n" +
            "public String toString() {\n" +
            "    return \"HELLO\";\n" +
            "}\n" +
            ""
        );

        Class c = new ClassBodyEvaluator(script).evaluate();
        Method m = c.getMethod("add", new Class[] { Integer.TYPE, Integer.TYPE });
        Integer res = (Integer) m.invoke(null, new Object[] {
            new Integer(7),
            new Integer(11),
        });
        System.out.println("res = " + res);

        Object o = c.newInstance();
        String s = o.toString();
        System.out.println("o.toString()=" + s);
    }

    private ClassBodyDemo() {}
}
