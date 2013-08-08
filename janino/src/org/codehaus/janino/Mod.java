
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino;

/**
 * This class defines constants and convenience methods for the handling of modifiers as defined by the JVM.
 * <p>
 * Notice: This class should be named <code>IClass.IModifier</code>, but changing the name would break existing client
 * code. Thus it won't be renamed until there's a really good reason to do it (maybe with a major design change).
 */
public final
class Mod {
    private Mod() {} // Don't instantiate me!

    public static final short NONE         = 0x0000;

    public static final short PUBLIC       = 0x0001;
    public static final short PRIVATE      = 0x0002;
    public static final short PROTECTED    = 0x0004;
    public static final short PACKAGE      = 0x0000;
    public static final short PPP          = 0x0007;

    public static boolean isPublicAccess(short sh)    { return (sh & Mod.PPP) == Mod.PUBLIC; }
    public static boolean isPrivateAccess(short sh)   { return (sh & Mod.PPP) == Mod.PRIVATE; }
    public static boolean isProtectedAccess(short sh) { return (sh & Mod.PPP) == Mod.PROTECTED; }
    public static boolean isPackageAccess(short sh)   { return (sh & Mod.PPP) == Mod.PACKAGE; }

    public static short
    changeAccess(short modifiers, short newAccess) { return (short) ((modifiers & ~Mod.PPP) | newAccess); }

    public static final short STATIC       = 0x0008;
    public static final short FINAL        = 0x0010;
    public static final short SUPER        = 0x0020;
    public static final short SYNCHRONIZED = 0x0020;
    public static final short VOLATILE     = 0x0040;
    public static final short TRANSIENT    = 0x0080;
    public static final short NATIVE       = 0x0100;
    public static final short INTERFACE    = 0x0200;
    public static final short ABSTRACT     = 0x0400;
    public static final short STRICTFP     = 0x0800;

    // Poorly documented JDK 1.5 modifiers:
    public static final short SYNTHETIC    = 0x1000;
    public static final short ANNOTATION   = 0x2000;
    public static final short ENUM         = 0x4000;

    public static String
    shortToString(short sh) {
        if (sh == 0) return "";
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < Mod.MAPPINGS.length; i += 2) {
            if ((sh & ((Short) Mod.MAPPINGS[i + 1]).shortValue()) == 0) continue;
            if (res.length() > 0) res.append(' ');
            res.append((String) Mod.MAPPINGS[i]);
        }
        return res.toString();
    }

    private static final Object[] MAPPINGS = {
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

