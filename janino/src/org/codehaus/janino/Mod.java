
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
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

    // Poorly documented JDK 1.5 modifiers:
    public final static short SYNTHETIC    = 0x1000;
    public final static short ANNOTATION   = 0x2000;
    public final static short ENUM         = 0x4000;

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

