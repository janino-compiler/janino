
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved.
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

package org.codehaus.commons.compiler.jdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A {@link ClassLoader} that loads classes through a {@link JavaFileManager}.
 */
public
class JavaFileManagerClassLoader extends ClassLoader {

    private final JavaFileManager javaFileManager;

    public
    JavaFileManagerClassLoader(JavaFileManager javaFileManager) { this.javaFileManager = javaFileManager; }

    public
    JavaFileManagerClassLoader(JavaFileManager javaFileManager, ClassLoader parentClassLoader) {
        super(parentClassLoader);
        this.javaFileManager = javaFileManager;
    }

    @Override protected Class<?>
    findClass(@Nullable String className) throws ClassNotFoundException {
        byte[] ba;
        try {
            JavaFileObject classFile = this.javaFileManager.getJavaFileForInput(
                StandardLocation.CLASS_OUTPUT,
                className,
                Kind.CLASS
            );
            if (classFile == null) throw new ClassNotFoundException(className);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            {
                InputStream is = classFile.openInputStream();
                try {
                    byte[] buffer = new byte[8192];
                    for (;;) {
                        int count = is.read(buffer);
                        if (count == -1) break;
                        baos.write(buffer, 0, count);
                    }
                } finally {
                    try { is.close(); } catch (Exception e) {}
                }
            }
            ba = baos.toByteArray();

            if (Logger.getLogger("org.codehaus.janino.UnitCompiler").isLoggable(Level.FINEST)) {
                JavaFileManagerClassLoader.disassembleToStdout(ba);
            }

        } catch (IOException ioe) {
            throw new ClassNotFoundException(className, ioe);
        }
        return this.defineClass(className, ba, 0, ba.length);
    }

    /**
     * Loads a "{@code de.unkrig.jdisasm.Disassembler}" through reflection (to avoid a compile-time dependency) and
     * uses it to disassemble the given bytes to {@code System.out}.
     */
    public static void
    disassembleToStdout(byte[] contents) {
        try {
            Class<?> disassemblerClass = Class.forName("de.unkrig.jdisasm.Disassembler");
            disassemblerClass.getMethod("disasm", InputStream.class).invoke(
                disassemblerClass.getConstructor().newInstance(),
                new ByteArrayInputStream(contents)
            );
        } catch (Exception e) {
            System.err.println((
                "Notice: Could not disassemble class file for logging because "
                + "\"de.unkrig.jdisasm.Disassembler\" is not on the classpath. "
                + "If you are interested in disassemblies of class files generated by JANINO, "
                + "get de.unkrig.jdisasm and put it on the classpath."
            ));
        }
    }
}
