
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.TunnelException;
import org.codehaus.janino.util.resource.ResourceFinder;


/**
 * This {@link org.codehaus.janino.IClassLoader} finds, scans and parses compilation units.
 * <p>
 * Notice that it does not compile them!
 */
final class JavaSourceIClassLoader extends IClassLoader {
    private static final boolean DEBUG = false;

    private final ResourceFinder sourceFinder;
    private final String         optionalCharacterEncoding;
    private final Set            uncompiledCompilationUnits; // Java.CompilationUnit

    /**
     * Notice that the <code>uncompiledCompilationUnits</code> set is both read and written
     * by the {@link JavaSourceIClassLoader}: As it searches for {@link IClass}es, it looks
     * into <code>uncompiledCompilationUnits</code> for class declarations, and as it opens,
     * scans and parses compilation units on-the-fly, it adds them to
     * <code>uncompiledCompilationUnits</code>.
     */
    public JavaSourceIClassLoader(
        ResourceFinder sourceFinder,
        String         optionalCharacterEncoding,
        Set            uncompiledCompilationUnits,
        IClassLoader   optionalParentIClassLoader
    ) {
        super(optionalParentIClassLoader);

        this.sourceFinder               = sourceFinder;
        this.optionalCharacterEncoding  = optionalCharacterEncoding;
        this.uncompiledCompilationUnits = uncompiledCompilationUnits;
        super.postConstruct();
    }

    /**
     * @param type field descriptor of the {@IClass} to load, e.g. "Lpkg1/pkg2/Outer$Inner;"
     * @throws TunnelException wraps a {@link Scanner.ScanException}
     * @throws TunnelException wraps a {@link Parser.ParseException}
     * @throws TunnelException wraps a {@link IOException}
     */
    public IClass findIClass(final String type) {
        if (JavaSourceIClassLoader.DEBUG) System.out.println("type = " + type);
    
        // Class type.
        String className = Descriptor.toClassName(type); // E.g. "pkg1.pkg2.Outer$Inner"
        if (JavaSourceIClassLoader.DEBUG) System.out.println("2 className = \"" + className + "\"");
    
        // Do not attempt to load classes from package "java".
        if (className.startsWith("java.")) return null;
    
        // Check the already-parsed compilation units.
        for (Iterator it = this.uncompiledCompilationUnits.iterator(); it.hasNext();) {
            Java.CompilationUnit cu = (Java.CompilationUnit) it.next();
            IClass res = cu.findClass(className);
            if (res != null) {
                this.defineIClass(res);
                return res;
            } 
        }

        try {

            // Find source file.
            URL sourceURL = this.sourceFinder.findResource(ClassFile.getSourceResourceName(className));
            if (sourceURL == null) return null;
            if (JavaSourceIClassLoader.DEBUG) System.out.println("sourceURL=" + sourceURL);

            // Scan and parse the source file.
            Java.CompilationUnit cu;
            InputStream inputStream = sourceURL.openStream();
            try {
                Scanner scanner = new Scanner(sourceURL.getFile(), inputStream, this.optionalCharacterEncoding);
                Parser parser = new Parser(scanner);
                cu = parser.parseCompilationUnit();
            } finally {
                try { inputStream.close(); } catch (IOException ex) {}
            }

            // Remember compilation unit for later compilation.
            this.uncompiledCompilationUnits.add(cu);

            // Find the class/interface declaration in the com
            IClass res = cu.findClass(className);
            if (res == null) throw new Parser.ParseException("Source file \"" + sourceURL.getFile() + "\" does not declare class \"" + className + "\"", (Scanner.Location) null);
            this.defineIClass(res);
            return res;
        } catch (Scanner.ScanException e) {
            throw new TunnelException(e);
        } catch (Parser.ParseException e) {
            throw new TunnelException(e);
        } catch (IOException e) {
            throw new TunnelException(e);
        }
    }
}