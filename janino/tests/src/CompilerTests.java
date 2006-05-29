
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

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.codehaus.janino.*;
import org.codehaus.janino.Compiler; // To resolve ambiguity with "java.lang.Compiler".
import org.codehaus.janino.util.*;
import org.codehaus.janino.util.enumerator.*;
import org.codehaus.janino.util.resource.*;

public class CompilerTests extends TestCase {
    public static Test suite() {
        TestSuite s = new TestSuite(Compiler.class.getName());
        s.addTest(new CompilerTests("testSelfCompile"));
        s.addTest(new CompilerTests("testCompileErrors"));
        return s;
    }

    public CompilerTests(String name) { super(name); }

    public void testSelfCompile() throws Exception {
        ClassLoader bootstrapClassLoader = new ClassLoader(null) {};
        File[] sourceFiles = new File[] {
            new File("src/org/codehaus/janino/Compiler.java"),
            new File("src/org/codehaus/janino/samples/ExpressionDemo.java"),
            new File("src/org/codehaus/janino/util/resource/MapResourceCreator.java"),
        };
        DirectoryResourceFinder sourceFinder = new DirectoryResourceFinder(new File("src"));
        boolean verbose = false;

        Benchmark b = new Benchmark(true);
        b.beginReporting("Compile Janino from scratch");
        MapResourceCreator classFileResources1 = new MapResourceCreator();
        new Compiler(
            sourceFinder,                                       // sourceFinder
            new ClassLoaderIClassLoader(bootstrapClassLoader),  // iClassLoader
            ResourceFinder.NO_RESOURCE_FINDER,                  // classFileFinder
            classFileResources1,                                // classFileCreator
            (String) null,                                      // optionalCharacterEncoding
            verbose,                                            // verbose
            DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION, // debuggingInformation
            (WarningHandler) null                               // optionalWarningHandler
        ).compile(sourceFiles);
        Map classFileMap1 = classFileResources1.getMap();
        b.endReporting("Generated " + classFileMap1.size() + " class files.");

        b.beginReporting("Compile Janino again, but with the class files created during the first compilation being available, i.e. only the explicitly given source files should be recompiled");
        MapResourceCreator classFileResources2 = new MapResourceCreator();
        MapResourceFinder classFileFinder = new MapResourceFinder(classFileMap1);
        classFileFinder.setLastModified(System.currentTimeMillis());
        new Compiler(
            sourceFinder,                                       // sourceFinder
            new ClassLoaderIClassLoader(bootstrapClassLoader),  // iClassLoader
            classFileFinder,                                    // classFileFinder
            classFileResources2,                                // classFileCreator
            (String) null,                                      // optionalCharacterEncoding
            verbose,                                            // verbose
            DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION, // debuggingInformation
            (WarningHandler) null                               // optionalWarningHandler
        ).compile(sourceFiles);
        b.endReporting("Generated " + classFileResources2.getMap().size() + " class files.");
        assertTrue("Only few class files re-generated", classFileResources2.getMap().size() < 30);

        b.beginReporting("Compile Janino again, but this time using the classes generated by the first compilation. A.k.a., \"compile Janino with itself\"");
        ClassLoader cl = new ResourceFinderClassLoader(new MapResourceFinder(classFileMap1), bootstrapClassLoader);
        Map classFileMap3 = new HashMap();
        Class cc = cl.loadClass(Compiler.class.getName());
        Object compiler = cc.getDeclaredConstructor(new Class[] {
            cl.loadClass(ResourceFinder.class.getName()),  // sourceFinder
            cl.loadClass(IClassLoader.class.getName()),    // iClassLoader
            cl.loadClass(ResourceFinder.class.getName()),  // classFileResourceFinder
            cl.loadClass(ResourceCreator.class.getName()), // classFileResourceCreator
            String.class,                                  // optionalCharacterEncoding
            boolean.class,                                 // verbose
            cl.loadClass(EnumeratorSet.class.getName()),   // debuggingInformation
            cl.loadClass(WarningHandler.class.getName()),  // optionalWarningHandler
        }).newInstance(new Object[] {
            (                          // sourceFinder
                cl
                .loadClass(DirectoryResourceFinder.class.getName())
                .getDeclaredConstructor(new Class[] { File.class })
                .newInstance(new Object[] { new File("src") })
            ),
            (                          // iClassLoader
                cl
                .loadClass(ClassLoaderIClassLoader.class.getName())
                .getDeclaredConstructor(new Class[] { ClassLoader.class })
                .newInstance(new Object[] { bootstrapClassLoader })
            ),
            (                          // classFileResourceFinder
                cl
                .loadClass(ResourceFinder.class.getName())
                .getDeclaredField("NO_RESOURCE_FINDER")
                .get(null)
            ),
            (                          // classFileResourceCreator
                cl
                .loadClass(MapResourceCreator.class.getName())
                .getDeclaredConstructor(new Class[] { Map.class })
                .newInstance(new Object[] { classFileMap3 })
            ),
            (String) null,             // optionalCharacterEncoding
            Boolean.valueOf(verbose),  // verbose
            (                          // debuggingInformation
                cl
                .loadClass(DebuggingInformation.class.getName())
                .getDeclaredField("DEFAULT_DEBUGGING_INFORMATION")
                .get(null)
            ),
            null,                      //  optionalWarningHandler
        });
        cc
        .getDeclaredMethod("compile", new Class[] { File[].class })
        .invoke(compiler, new Object[] { sourceFiles });
        b.endReporting("Generated " + classFileMap3.size() + " class files.");

        assertEquals(classFileMap1.size(), classFileMap3.size());
        for (Iterator it = classFileMap1.entrySet().iterator(); it.hasNext();) {
            Map.Entry me = (Map.Entry) it.next();
            assertTrue(Arrays.equals((byte[]) me.getValue(), (byte[]) classFileMap3.get(me.getKey())));
        }
    }

    public void testCompileErrors() throws Exception {
        Map sources = new HashMap();
        sources.put("pkg/A.java", ( // Class A uses class B, C, D.
            "package pkg;\n" +
            "public class A {\n" +
            "    void meth() {\n" +
            "        new B();\n" +
            "        new C();\n" +
            "        new D();\n" +
            "    }\n" +
            "}\n"
        ).getBytes());
        sources.put("pkg/B.java", (
            "package pkg;\n" +
            "public class B {\n" +
            "}\n"
        ).getBytes());
        sources.put("pkg/C.java", ( // Class C contains a compile error.
            "package pkg;\n" +
            "public class C extends E {\n" + // Compile error, because a class "E" is not defined.
            "}\n"
        ).getBytes());
        sources.put("pkg/D.java", (
            "package pkg;\n" +
            "public class D {\n" +
            "}\n"
        ).getBytes());
        ResourceFinder sourceFinder = new MapResourceFinder(sources);

        Map classes = new HashMap();
        Compiler compiler = new Compiler(
            sourceFinder,                                                  // sourceFinder
            new ClassLoaderIClassLoader(this.getClass().getClassLoader()), // iClassLoader
            ResourceFinder.NO_RESOURCE_FINDER,                             // classFileFinder
            new MapResourceCreator(classes),                               // classFileCreator
            (String) null,                                                 // optionalCharacterEncoding
            false,                                                         // verbose
            DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION,            // debuggingInformation
            (WarningHandler) null                                          // optionalWarningHandler
        );
        COMPILE: {
            try {
                compiler.compile(new Resource[] { sourceFinder.findResource("pkg/A.java") });
            } catch (CompileException ex) {
                break COMPILE;
            }
            fail("CompileException expected");
        }

        assertEquals(makeSet(new Object[] { "pkg/A.class", "pkg/B.class", }), classes.keySet());
    }

    private Set makeSet(Object[] elements) {
        Set s = new HashSet();
        for (int i = 0; i < elements.length; ++i) s.add(elements[i]);
        return s;
    }
}
