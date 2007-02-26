
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

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.*;

import junit.framework.*;
import junit.textui.TestRunner;

import org.codehaus.janino.*;

/**
 * @author Eugene Kuleshov
 */
public class AstGeneratorVisitorTest extends TestCase {
    private static final String GEN_NAME = "AstGen";
    
    private URL u;
    private String name;

    
    public static void main(String[] args) throws Exception {
        TestRunner.run(suite());
    }

    
    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite(AstGeneratorVisitor.class.getName());

        String testData = System.getProperty("janino.testData");

        File f = new File(testData);
        if (f.isDirectory()) {
            File[] fs = f.listFiles();
            for (int i = 0; i < fs.length; ++i) {
                String n = fs[i].getName();
                if (n.endsWith(".java")) {
                    n = n.substring(0, n.length() - 5).replace('/', '.');
                    suite.addTest(new AstGeneratorVisitorTest(n, fs[i].toURL()));
                }
            }

        } else {
            ZipFile zip = new ZipFile(f);
            int i = 0;
            for(Enumeration entries = zip.entries(); entries.hasMoreElements();) {
                ZipEntry e = (ZipEntry)entries.nextElement();
                String n = e.getName();
                if (n.endsWith(".java") && n.startsWith("src/java")) {
                    n = n.substring(0, n.length() - 5).replace('/', '.');
                    suite.addTest(new AstGeneratorVisitorTest(n, new URL("jar:file:"+zip.getName()+"!/"+e.getName())));
                    i++;
                }
            }

        }

        return suite;
    }


    public AstGeneratorVisitorTest(String name, URL u) {
        super("testParsing");
        this.name = name;
        this.u = u;
    }

    public void testParsing() throws Throwable {
        System.err.println(name);
        
        Java.CompilationUnit cu1 = null;
        InputStream is = null;
        long l1 = System.currentTimeMillis();
        try {
            is = u.openStream();
            cu1 = new Parser(new Scanner(name, is)).parseCompilationUnit();

        } finally {
             try {
                 is.close();
             } catch(Exception ex) {
             }
            long l2 = System.currentTimeMillis();
            System.err.println("  parse " + (l2 - l1) / 1000f);

        }
        
        l1 = System.currentTimeMillis();
        StringWriter sw1 = new StringWriter();
        UnparseVisitor uv = new UnparseVisitor(sw1);
        uv.unparseCompilationUnit(cu1);
        long l2 = System.currentTimeMillis();
        System.err.println("  unparse " + (l2 - l1) / 1000f);

        l1 = System.currentTimeMillis();
        StringWriter sw2 = new StringWriter();
        AstGeneratorVisitor gv = new AstGeneratorVisitor(sw2, GEN_NAME);
        gv.generateCompilationUnit(cu1);
        l2 = System.currentTimeMillis();
        System.err.println("  generate ast generator " + (l2 - l1) / 1000f);
        
        Scanner scanner = new Scanner(null, new StringReader(sw2.toString()));
        
        l1 = System.currentTimeMillis();
        AstCompilationUnitGenerator gen = null;
        try {
            SimpleCompiler comp = new SimpleCompiler(scanner, getClass().getClassLoader());
            gen = (AstCompilationUnitGenerator) comp.getClassLoader().loadClass("org.codehaus.janino."+GEN_NAME).newInstance();

        } catch(Throwable ex) {
            dumpSource(name+"Gen", sw2.toString());
            throw ex;

        } finally {
            l2 = System.currentTimeMillis();
            System.err.println("  compile generated " + (l2 - l1) / 1000f);
        }
        
        l1 = System.currentTimeMillis();
        Java.CompilationUnit cu2;
        try {
            cu2 = gen.generate();
        } catch(Throwable ex) {
            dumpSource(name+"Gen", sw2.toString());
            throw ex;

        } finally {
            l2 = System.currentTimeMillis();
            System.err.println("  generate ast2 " + (l2 - l1) / 1000f);
        }

        StringWriter sw3 = new StringWriter();
        UnparseVisitor uv2 = new UnparseVisitor(sw3);
        uv2.unparseCompilationUnit(cu2);

        assertEquals("Invalid AST "+name, sw1.toString(), sw3.toString());
    }

    private void dumpSource(String name, String source) throws IOException {
        FileWriter fw = new FileWriter(name);
        fw.write(source);
        fw.close();
    }

    public String getName() {
        return super.getName()+" : "+name;
    }

}

