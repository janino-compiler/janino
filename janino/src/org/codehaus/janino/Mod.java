
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

package org.codehaus.janino;

/**
 * This class defines constants and convenience methods for the handling of
 * modifiers as defined by the JVM.
 */
public class Mod {
    private Mod() {} // Don't instantiate me!

    public final static short NONE         = 0x0000;

    public final static short PUBLIC       = 0x0001;
    public final static short PRIVATE      = 0x0002;
    public final static short PROTECTED    = 0x0004;
    public final static short PACKAGE      = 0x0000;
    public final static short PPP          = 0x0007;

    public final static short STATIC       = 0x0008;
    public final static short FINAL        = 0x0010;
    public final static short SUPER        = 0x0020;
    public final static short SYNCHRONIZED = 0x0020;
    public final static short VOLATILE     = 0x0040;
    public final static short TRANSIENT    = 0x0080;
    public final static short NATIVE       = 0x0100;
    public final static short INTERFACE    = 0x0200;
    public final static short ABSTRACT     = 0x0400;
    public final static short STRICTFP     = 0x0800;

    public static String shortToString(short sh) {
        String res = "";
        for (int i = 0; i < Mod.mappings.length; i += 2) {
            if ((sh & ((Short) Mod.mappings[i + 1]).shortValue()) == 0) continue;
            if (res.length() > 0) res += ' ';
            res += (String) Mod.mappings[i];
        }
        return res;
    }

    private final static Object[] mappings = {
        "public",       new Short(Mod.PUBLIC),
        "private",      new Short(Mod.PRIVATE),
        "protected",    new Short(Mod.PROTECTED),
        "static",       new Short(Mod.STATIC),
        "final",        new Short(Mod.FINAL),
//      "super",        new Short(Mod.SUPER),
        "synchronized", new Short(Mod.SYNCHRONIZED),
        "volatile",     new Short(Mod.VOLATILE),
        "transient",    new Short(Mod.TRANSIENT),
        "native",       new Short(Mod.NATIVE),
        "interface",    new Short(Mod.INTERFACE),
        "abstract",     new Short(Mod.ABSTRACT),
        "strictfp",     new Short(Mod.STRICTFP),
    };
}

