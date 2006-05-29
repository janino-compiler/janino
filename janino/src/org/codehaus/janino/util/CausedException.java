
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

package org.codehaus.janino.util;

import java.io.*;
import java.lang.reflect.*;

/**
 * For compatibility with pre-1.4 JDKs, this class mimics 
 */
public class CausedException extends Exception {
    private Throwable     optionalCause = null;
    private static Method INIT_CAUSE; // Null for pre-1.4 JDKs.
    static {
        try {
            CausedException.INIT_CAUSE = Exception.class.getDeclaredMethod("initCause", new Class[] { Throwable.class });
        } catch (NoSuchMethodException e) {
            CausedException.INIT_CAUSE = null;
        }
    }

    public CausedException() {
    }

    public CausedException(String message) {
        super(message);
    }

    public CausedException(String message, Throwable optionalCause) {
        super(message);
        this.initCause(optionalCause);
    }

    public CausedException(Throwable optionalCause) {
        super(optionalCause == null ? null : optionalCause.getMessage());
        this.initCause(optionalCause);
    }

    public Throwable initCause(Throwable optionalCause) {
        if (CausedException.INIT_CAUSE == null) {
            this.optionalCause = optionalCause;
        } else
        {
            try {
                CausedException.INIT_CAUSE.invoke(this, new Object[] { optionalCause});
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Calling \"initCause()\"");
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Calling \"initCause()\"");
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Calling \"initCause()\"");
            }
        }
        return this;
    }

    public Throwable getCause() {
        return this.optionalCause;
    }

    public void printStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
        if (this.optionalCause == null) return;

        ps.print("Caused by: ");
        this.optionalCause.printStackTrace(ps);
    }
    public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
        if (this.optionalCause == null) return;

        pw.print("Caused by: ");
        this.optionalCause.printStackTrace(pw);
    }
}
