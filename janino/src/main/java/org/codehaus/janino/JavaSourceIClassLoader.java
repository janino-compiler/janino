
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
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

package org.codehaus.janino;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.util.ClassFile;

/**
 * This {@link org.codehaus.janino.IClassLoader} finds, scans and parses compilation units.
 * <p>
 *   Notice that it does not compile them!
 * </p>
 */
public
class JavaSourceIClassLoader extends IClassLoader {

    private static final Logger LOGGER = Logger.getLogger(JavaSourceIClassLoader.class.getName());

    private ResourceFinder        sourceFinder;
    private Charset               sourceCharset;
    private EnumSet<JaninoOption> options = EnumSet.noneOf(JaninoOption.class);

    /**
     * Collection of parsed compilation units.
     */
    private final Set<UnitCompiler>  unitCompilers = new HashSet<UnitCompiler>();

    private int                      sourceVersion = -1;
    private int                      targetVersion = -1;
    @Nullable private ErrorHandler   compileErrorHandler;
    @Nullable private WarningHandler warningHandler;

    public
    JavaSourceIClassLoader(
        ResourceFinder         sourceFinder,
        @Nullable String       sourceCharsetName,
        @Nullable IClassLoader parentIClassLoader
    ) {
        super(parentIClassLoader);

        this.sourceFinder  = sourceFinder;
        this.sourceCharset = sourceCharsetName == null ? Charset.defaultCharset() : Charset.forName(sourceCharsetName);
        super.postConstruct();
    }

    public void
    setSourceVersion(int version) { this.sourceVersion = version; }

    public void
    setTargetVersion(int version) { this.targetVersion = version; }

    /**
     * Returns the set of {@link UnitCompiler}s that were created so far.
     */
    public Set<UnitCompiler>
    getUnitCompilers() { return this.unitCompilers; }

    /**
     * @param sourceFinder The source path
     */
    public void
    setSourceFinder(ResourceFinder sourceFinder) { this.sourceFinder = sourceFinder; }

    public ResourceFinder
    getSourceFinder() { return this.sourceFinder; }

    /**
     * @deprecated Use {@link #setSourceCharset(Charset)} instead
     */
    @Deprecated public void
    setCharacterEncoding(@Nullable String sourceCharsetName) {
        this.setSourceCharset(
            sourceCharsetName == null ? Charset.defaultCharset() : Charset.forName(sourceCharsetName)
        );
    }

    /**
     * @param sourceCharset The charset that is used to read source files
     */
    public void
    setSourceCharset(Charset sourceCharset) { this.sourceCharset = sourceCharset; }

    /**
     * @see UnitCompiler#setCompileErrorHandler(ErrorHandler)
     */
    public void
    setCompileErrorHandler(@Nullable ErrorHandler compileErrorHandler) {
        this.compileErrorHandler = compileErrorHandler;
    }

    /**
     * @see Parser#setWarningHandler(WarningHandler)
     * @see UnitCompiler#setCompileErrorHandler(ErrorHandler)
     */
    public void
    setWarningHandler(@Nullable WarningHandler warningHandler) {
        this.warningHandler = warningHandler;
    }

    /**
     * @return A reference to the currently effective compilation options; changes to it take
     *         effect immediately
     */
    public EnumSet<JaninoOption>
    options() { return this.options; }

    /**
     * Sets the options for all future compilations.
     */
    public JavaSourceIClassLoader
    options(EnumSet<JaninoOption> options) {
        this.options = options;
        return this;
    }

    /**
     * @param fieldDescriptor         Field descriptor of the {@link IClass} to load, e.g. "Lpkg1/pkg2/Outer$Inner;"
     * @throws ClassNotFoundException An exception was raised while loading the {@link IClass}
     */
    @Override @Nullable public IClass
    findIClass(final String fieldDescriptor) throws ClassNotFoundException {
        JavaSourceIClassLoader.LOGGER.entering(null, "findIClass", fieldDescriptor);

        // Class type.
        String className = Descriptor.toClassName(fieldDescriptor); // E.g. "pkg1.pkg2.Outer$Inner"
        JavaSourceIClassLoader.LOGGER.log(Level.FINE, "className={0}", className);

        // Do not attempt to load classes from package "java".
        if (className.startsWith("java.")) return null;

        // Determine the name of the top-level class.
        String topLevelClassName;
        {
            int idx = className.indexOf('$');
            topLevelClassName = idx == -1 ? className : className.substring(0, idx);
        }

        // Check the already-parsed compilation units.
        for (UnitCompiler uc : this.unitCompilers) {
            IClass res = uc.findClass(topLevelClassName);
            if (res != null) {
                if (!className.equals(topLevelClassName)) {
                    res = uc.findClass(className);
                    if (res == null) return null;
                }
                this.defineIClass(res);
                return res;
            }
        }

        try {
            Java.AbstractCompilationUnit acu = this.findCompilationUnit(className);
            if (acu == null) return null;

            UnitCompiler uc = new UnitCompiler(acu, this).options(this.options);
            uc.setTargetVersion(this.targetVersion);
            uc.setCompileErrorHandler(this.compileErrorHandler);
            uc.setWarningHandler(this.warningHandler);

            // Remember compilation unit for later compilation.
            this.unitCompilers.add(uc);

            // Find the class/interface declaration in the compiled unit.
            IClass res = uc.findClass(className);
            if (res == null) {
                if (className.equals(topLevelClassName)) {
                    throw new CompileException(
                        "Compilation unit '" + className + "' does not declare a class with the same name",
                        (Location) null
                    );
                }
                return null;
            }
            this.defineIClass(res);
            return res;
        } catch (IOException e) {
            throw new ClassNotFoundException("Parsing compilation unit '" + className + "'", e);
        } catch (CompileException e) {
            throw new ClassNotFoundException("Parsing compilation unit '" + className + "'", e);
        }
    }

    /**
     * Finds the Java source file for the named class through the configured 'source resource finder' and parses it.
     *
     * @return {@code null} iff the source file could not be found
     */
    @Nullable protected Java.AbstractCompilationUnit
    findCompilationUnit(String className) throws IOException, CompileException {

        // Find source file.
        Resource sourceResource = this.sourceFinder.findResource(ClassFile.getSourceResourceName(className));
        JavaSourceIClassLoader.LOGGER.log(Level.FINE, "sourceResource={0}", sourceResource);
        if (sourceResource == null) return null;

        // Scan and parse the source file.
        InputStream inputStream = sourceResource.open();
        try {

            Scanner scanner = new Scanner(
                sourceResource.getFileName(),
                new InputStreamReader(inputStream, this.sourceCharset)
            );

            Parser parser = new Parser(scanner);
            parser.setSourceVersion(this.sourceVersion);
            parser.setWarningHandler(this.warningHandler);

            return parser.parseAbstractCompilationUnit();
        } finally {
            try { inputStream.close(); } catch (IOException ex) {}
        }
    }
}
