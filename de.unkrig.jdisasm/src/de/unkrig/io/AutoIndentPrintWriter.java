
/*
 * JDISASM - A Java[TM] class file disassembler
 *
 * Copyright (c) 2001-2011, Arno Unkrig
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

package de.unkrig.io;

import java.io.PrintWriter;
import java.io.Writer;

public class AutoIndentPrintWriter extends PrintWriter {

    private String prefix = "";
    private String eolIndenters = "{";
    private String bolUnindenters = "}";
    private String levelIndent = "    ";

    private boolean atBol = true;
    private int     level;
    private boolean eolIndenterPending;

    public AutoIndentPrintWriter(Writer out) {
        super(out);
    }

    public String getPrefix() { return this.prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public String getEolIndenters() { return this.prefix; }
    public void setEolIndenters(String eolIndenters) { this.eolIndenters = eolIndenters; }
    
    public String getBolUnindenters() { return this.bolUnindenters; }
    public void setBolUnindenters(String bolUnindenters) { this.bolUnindenters = bolUnindenters; }
    
    public String getLevelIndent() { return this.levelIndent; }
    public void setLevelIndent(String levelIndent) { this.levelIndent = levelIndent; }
    
    @Override
    public void write(int c) {
        preWrite((char) c);
        super.write(c);
        postWrite((char) c);
    }

    private void preWrite(char c) {
        if (this.atBol) {
            if (this.bolUnindenters.indexOf(c) != -1) this.level--;
            super.write(this.prefix);
            for (int i = 0; i < this.level; ++i) super.write(this.levelIndent);
            this.atBol = false;
        }
    }

    private void postWrite(char c) {
        this.eolIndenterPending = this.eolIndenters.indexOf(c) != -1;
    }

    @Override
    public void write(char[] buf, int off, int len) {
        if (len == 0) return;
        preWrite(buf[off]);
        super.write(buf, off, len);
        postWrite(buf[off + len - 1]);
    }

    @Override
    public void write(String s, int off, int len) {
        if (len == 0) return;
        preWrite(s.charAt(0));
        super.write(s, off, len);
        postWrite(s.charAt(off + len - 1));
    }

    @Override
    public void println() {
        if (this.eolIndenterPending) {
            this.level++;
            this.eolIndenterPending = false;
        }
        super.println();
    }
}
