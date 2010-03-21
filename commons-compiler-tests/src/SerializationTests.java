
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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.codehaus.commons.compiler.*;

public class SerializationTests extends TestCase {

    public static TestSuite suite(ICompilerFactory compilerFactory) {
        TestSuite s = new TestSuite("SerializationTests");
        s.addTest(new SerializationTests("testExceptionSerializable", compilerFactory));
        return s;
    }

    private final ICompilerFactory compilerFactory;

    public SerializationTests(String name, ICompilerFactory compilerFactory) {
        super(name);
        this.compilerFactory = compilerFactory;
    }

    public void testExceptionSerializable() throws Exception {
        ISimpleCompiler compiler = compilerFactory.newSimpleCompiler();
        try {
            compiler.cook("this is not valid Java");
            fail("Cook should have thrown an exception");
        } catch (LocatedException e) {
            ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
            oos.writeObject(e);
            oos.close();
        }
    }
}