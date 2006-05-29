
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

package org.codehaus.janino.tools;

import java.util.*;
import java.io.*;

import org.codehaus.janino.*;
import org.codehaus.janino.util.*;
import org.codehaus.janino.util.enumerator.*;
import org.codehaus.janino.util.iterator.*;
import org.codehaus.janino.util.resource.*;


/**
 * Reads a set of compilation units from the file system and searches it for specific
 * Java<sup>TM</sup> constructs, e.g. invocations of a particular method.
 *
 * Usage:
 * <pre>
 * java org.codehaus.janino.JGrep \
 *           [ -dirs <i>directory-name-patterns</i> ] \
 *           [ -files <i>file-name-patterns</i> ] \
 *           { <i>directory-path</i> } \
 *           -method-invocation <i>class.method(arg-types)</i>
 * java org.codehaus.janino.JGrep -help
 * </pre>
 * 
 * If "-dirs" is not given, then all <i>directory-path</i>es are scanned for files.
 * The <i>directory-name-patterns</i> work as described in
 * {@link org.codehaus.janino.util.StringPattern#parseCombinedPattern(String)}.
 * <p>
 * If "-files" is not given, then all files ending in ".java" are read. The
 * <i>file-name-patterns</i> work as described in
 * {@link org.codehaus.janino.util.StringPattern#parseCombinedPattern(String)}.
 */
public class JGrep {
    private static final boolean DEBUG = false;
    private List         parsedCompilationUnits = new ArrayList(); // UnitCompiler

    /**
     * Command line interface.
     */
    public static void main(String[] args) {
        int idx = 0;

        StringPattern[] directoryNamePatterns = StringPattern.PATTERNS_ALL;
        StringPattern[] fileNamePatterns = new StringPattern[] { new StringPattern("*.java") };
        File[]          classPath = new File[] { new File(".") };
        File[]          optionalExtDirs = null;
        File[]          optionalBootClassPath = null;
        String          optionalCharacterEncoding = null;
        boolean         verbose = false;

        for (; idx < args.length; ++idx) {
            String arg = args[idx];
            if (arg.charAt(0) != '-') break;
            if (arg.equals("-dirs")) {
                directoryNamePatterns = StringPattern.parseCombinedPattern(args[++idx]);
            } else
            if (arg.equals("-files")) {
                fileNamePatterns = StringPattern.parseCombinedPattern(args[++idx]);
            } else
            if (arg.equals("-classpath")) {
                classPath = PathResourceFinder.parsePath(args[++idx]);
            } else
            if (arg.equals("-extdirs")) {
                optionalExtDirs = PathResourceFinder.parsePath(args[++idx]);
            } else
            if (arg.equals("-bootclasspath")) {
                optionalBootClassPath = PathResourceFinder.parsePath(args[++idx]);
            } else
            if (arg.equals("-encoding")) {
                optionalCharacterEncoding = args[++idx];
            } else
            if (arg.equals("-verbose")) {
                verbose = true;
            } else
            if (arg.equals("-help")) {
                for (int j = 0; j < JGrep.USAGE.length; ++j) System.out.println(JGrep.USAGE[j]);
                System.exit(1);
            } else
            {
                System.err.println("Unexpected command-line argument \"" + arg + "\", try \"-help\".");
                System.exit(1);
                /* NEVER REACHED */ return;
            }
        }

        // { directory-path }
        File[] rootDirectories;
        {
            int first = idx;
            for (; idx < args.length && args[idx].charAt(0) != '-'; ++idx);
            if (idx == first) {
                System.err.println("No <directory-path>es given, try \"-help\".");
                System.exit(1);
                /* NEVER REACHED */ return;
            }
            rootDirectories = new File[idx - first];
            for (int i = first; i < idx; ++i) rootDirectories[i - first] = new File(args[i]);
        }

        // Create the JGrep object.
        final JGrep jGrep = new JGrep(
            classPath,
            optionalExtDirs,
            optionalBootClassPath,
            optionalCharacterEncoding,
            verbose
        );

        List mits = new ArrayList();
        for (; idx < args.length; ++idx) {
            String arg = args[idx];
            if (arg.equals("-method-invocation")) {
                MethodInvocationTarget mit;
                try {
                    mit = JGrep.parseMethodInvocationPattern(args[++idx]);
                } catch (Exception ex) {
                    System.err.println("Parsing method invocation pattern \"" + args[idx] + "\": " + ex.getMessage());
                    System.exit(1);
                    /* NEVER REACHED */ return;
                }
                while (idx < args.length - 1) {
                    arg = args[idx + 1];
                    if (arg.startsWith("predicate:")) {
                        String predicateExpression = arg.substring(10);
                        try {
                            mit.predicates.add(ExpressionEvaluator.createFastExpressionEvaluator(
                                new Scanner(null, new StringReader(predicateExpression)), // expression
                                JGrep.class.getName() + "PE",                             // className
                                null,                                                     // optionalExtendedType
                                MethodInvocationPredicate.class,                          // interfaceToImplement
                                new String[] { "uc", "invocation", "method" },            // parameterNames
                                null                                                      // optionalClassLoader
                            ));
                        } catch (Exception ex) {
                            System.err.println("Compiling predicate expression \"" + predicateExpression + "\": " + ex.getMessage());
                            System.exit(1);
                            /* NEVER REACHED */ return;
                        }
                    } else
                    if (arg.startsWith("action:")) {
                        String action = arg.substring(7);
                        try {
                            mit.actions.add(Action.getMethodInvocationAction(action));
                        } catch (Exception ex) {
                            System.err.println("Compiling method invocation action \"" + action + "\": " + ex.getMessage());
                            System.exit(1);
                            /* NEVER REACHED */ return;
                        }
                    } else
                    {
                        break;
                    }
                    ++idx;
                }
                mits.add(mit);
            } else
            {
                System.err.println("Unexpected command-line argument \"" + arg + "\", try \"-help\".");
                System.exit(1);
                /* NEVER REACHED */ return;
            }
        }

        // JGrep the root directories.
        try {
            jGrep.jGrep(
                rootDirectories,
                directoryNamePatterns,
                fileNamePatterns,
                mits                   // methodInvocationTargets
            );
        } catch (Exception e) {
            System.err.println(e.toString());
            System.exit(1);
        }
    }

    private static final class Action extends Enumerator {
        public static final Action PRINT_LOCATION_AND_MATCH = new Action("print-location-and-match");
        public static final Action PRINT_LOCATION           = new Action("print-location");
   
        private Action(String name) { super(name); }
        public static Action fromString(String name) throws EnumeratorFormatException {
            return (Action) Enumerator.fromString(name, Action.class);
        }

        static MethodInvocationAction getMethodInvocationAction(String action) throws CompileException, Parser.ParseException, Scanner.ScanException {
            if ("print-location-and-match".equals(action)) {
                return new MethodInvocationAction() { public void execute(UnitCompiler uc, Java.Invocation invocation, IClass.IMethod method) { System.out.println(invocation.getLocation() + ": " + method); } };
            } else
            if ("print-location".equals(action)) {
                return new MethodInvocationAction() { public void execute(UnitCompiler uc, Java.Invocation invocation, IClass.IMethod method) { System.out.println(invocation.getLocation()); } };
            } else
            {
                return (MethodInvocationAction) ScriptEvaluator.createFastScriptEvaluator(
                    action,                                       // script
                    MethodInvocationAction.class,                 // interfaceToImplement
                    new String[] { "uc", "invocation", "method" } // parameterNames
                );
            }
        }
    }

    private static MethodInvocationTarget parseMethodInvocationPattern(String mip) throws Scanner.ScanException, IOException, Parser.ParseException {
        MethodInvocationTarget mit = new MethodInvocationTarget();
        Scanner scanner = new Scanner(null, new StringReader(mip));
        Parser parser = new Parser(scanner);

        for (;;) {
            String s = JGrep.readIdentifierPattern(parser);
            if (parser.peekOperator("(")) {
                mit.methodNamePattern = s;
                parser.eatToken();
                List l = new ArrayList();
                if (!parser.peekOperator(")")) {
                    for (;;) {
                        l.add(JGrep.readIdentifierPattern(parser));
                        if (parser.peekOperator(")")) break;
                        parser.readOperator(",");
                    }
                }
                mit.optionalArgumentTypeNamePatterns = (String[]) l.toArray(new String[l.size()]);
                return mit;
            } else
            if (parser.peekOperator(".")) {
                if (mit.optionalClassNamePattern == null) {
                    mit.optionalClassNamePattern = s;
                } else
                {
                    mit.optionalClassNamePattern += '.' + s;
                }
                parser.eatToken();
            } else
            if (scanner.peek().isEOF()) {
                mit.methodNamePattern = s;
                return mit;
            }
        }
    }

    private static String readIdentifierPattern(Parser p) throws Parser.ParseException, Scanner.ScanException, IOException {
        StringBuffer sb = new StringBuffer();
        if (p.peekOperator("*")) {
            sb.append('*');
            p.eatToken();
        } else
        {
            sb.append(p.readIdentifier());
        }
        for (;;) {
            if (p.peekOperator("*")) {
                sb.append('*');
                p.eatToken();
            } else
            if (p.peekIdentifier()) {
                sb.append(p.readIdentifier());
            } else
            {
                return sb.toString();
            }
        }
    }
    private static class MethodInvocationTarget {
        String   optionalClassNamePattern = null;
        String   methodNamePattern = null;
        String[] optionalArgumentTypeNamePatterns = null;
        List     predicates = new ArrayList(); // MethodInvocationPredicate
        List     actions = new ArrayList(); // MethodInvocationAction

        void apply(UnitCompiler uc, Java.Invocation invocation, IClass.IMethod method) throws CompileException {
            if (this.optionalClassNamePattern != null) {
                if (!typeMatches(this.optionalClassNamePattern, Descriptor.toClassName(method.getDeclaringIClass().getDescriptor()))) return;
            }
            if (!new StringPattern(this.methodNamePattern).matches(method.getName())) return;
            IClass[] fpts = method.getParameterTypes();
            if (this.optionalArgumentTypeNamePatterns != null) {
                String[] atnps = this.optionalArgumentTypeNamePatterns;
                if (atnps.length != fpts.length) return;
                for (int i = 0; i < atnps.length; ++i) {
                    if (!new StringPattern(atnps[i]).matches(Descriptor.toClassName(fpts[i].getDescriptor())));
                }
            }
            for (Iterator it = this.predicates.iterator(); it.hasNext();) {
                MethodInvocationPredicate mip = (MethodInvocationPredicate) it.next();
                try {
                    if (!mip.evaluate(uc, invocation, method)) return;
                } catch (Exception ex) {
                    return; // Treat exception as a "false" predicate.
                }
            }
            for (Iterator it = this.actions.iterator(); it.hasNext();) {
                MethodInvocationAction mia = (MethodInvocationAction) it.next();
                try {
                    mia.execute(uc, invocation, method);
                } catch (Exception ex) {
                    ; // Ignore action throwing an exception.
                }
            }
        }
    }
    public interface MethodInvocationPredicate { boolean evaluate(UnitCompiler uc, Java.Invocation invocation, IClass.IMethod method) throws Exception; }
    public interface MethodInvocationAction { void execute(UnitCompiler uc, Java.Invocation invocation, IClass.IMethod method) throws Exception; }

    static boolean typeMatches(String pattern, String typeName) {
        return new StringPattern(pattern).matches(
            pattern.indexOf('.') == -1
            ? typeName.substring(typeName.lastIndexOf('.') + 1)
            : typeName
        );
    }
    private static final String[] USAGE = {
        "To be written.", // TODO
/*
        "Usage:",
        "",
        "  java org.codehaus.janino.Compiler [ <option> ] ... <source-file> ...",
        "",
        "Supported <option>s are:",
        "  -d <output-dir>           Where to save class files",
        "  -sourcepath <dirlist>     Where to look for other source files",
        "  -classpath <dirlist>      Where to look for other class files",
        "  -extdirs <dirlist>        Where to look for other class files",
        "  -bootclasspath <dirlist>  Where to look for other class files",
        "  -encoding <encoding>      Encoding of source files, e.g. \"UTF-8\" or \"ISO-8859-1\"",
        "  -verbose",
        "  -g                        Generate all debugging info",
        "  -g:none                   Generate no debugging info",
        "  -g:{lines,vars,source}    Generate only some debugging info",
        "  -warn:<pattern-list>      Issue certain warnings; examples:",
        "    -warn:*                 Enables all warnings",
        "    -warn:IASF              Only warn against implicit access to static fields",
        "    -warn:*-IASF            Enables all warnings, except those against implicit",
        "                            access to static fields",
        "    -warn:*-IA*+IASF        Enables all warnings, except those against implicit",
        "                            accesses, but do warn against implicit access to",
        "                            static fields",
        "  -rebuild                  Compile all source files, even if the class files",
        "                            seems up-to-date",
        "  -help",
        "",
        "The default encoding in this environment is \"" + new InputStreamReader(new ByteArrayInputStream(new byte[0])).getEncoding() + "\".",
*/
    };

    private /*final*/ IClassLoader iClassLoader;
    private /*final*/ String       optionalCharacterEncoding;
    private /*final*/ Benchmark    benchmark;

    public JGrep(
        File[]  classPath,
        File[]  optionalExtDirs,
        File[]  optionalBootClassPath,
        String  optionalCharacterEncoding,
        boolean verbose
    ) {
        this(
            JGrep.createJavacLikePathIClassLoader( // iClassLoader
                optionalBootClassPath,
                optionalExtDirs,
                classPath
            ),
            optionalCharacterEncoding,             // optionalCharacterEncoding
            verbose                                // verbose
        );

        this.benchmark.report("*** JGrep - search Java(TM) source files for specific language constructs");
        this.benchmark.report("*** For more information visit http://janino.codehaus.org");
        this.benchmark.report("Class path",         classPath                );
        this.benchmark.report("Ext dirs",           optionalExtDirs          );
        this.benchmark.report("Boot class path",    optionalBootClassPath    );
        this.benchmark.report("Character encoding", optionalCharacterEncoding);
    }

    public JGrep(
        IClassLoader iClassLoader,
        final String optionalCharacterEncoding,
        boolean      verbose
    ) {
        this.iClassLoader              = new JGrepIClassLoader(iClassLoader);
        this.optionalCharacterEncoding = optionalCharacterEncoding;
        this.benchmark                 = new Benchmark(verbose);
    }

    /**
     * Create an {@link IClassLoader} that looks for classes in the given "boot class
     * path", then in the given "extension directories", and then in the given
     * "class path".
     * <p>
     * The default for the <code>optionalBootClassPath</code> is the path defined in
     * the system property "sun.boot.class.path", and the default for the
     * <code>optionalExtensionDirs</code> is the path defined in the "java.ext.dirs"
     * system property.
     */
    private static IClassLoader createJavacLikePathIClassLoader(
        final File[] optionalBootClassPath,
        final File[] optionalExtDirs,
        final File[] classPath
    ) {
        ResourceFinder bootClassPathResourceFinder = new PathResourceFinder(
            optionalBootClassPath == null ?
            PathResourceFinder.parsePath(System.getProperty("sun.boot.class.path")):
            optionalBootClassPath
        );
        ResourceFinder extensionDirectoriesResourceFinder = new JarDirectoriesResourceFinder(
            optionalExtDirs == null ?
            PathResourceFinder.parsePath(System.getProperty("java.ext.dirs")):
            optionalExtDirs
        );
        ResourceFinder classPathResourceFinder = new PathResourceFinder(classPath);

        // We can load classes through "ResourceFinderIClassLoader"s, which means
        // they are read into "ClassFile" objects, or we can load classes through
        // "ClassLoaderIClassLoader"s, which means they are loaded into the JVM.
        //
        // In my environment, the latter is slightly faster. No figures about
        // resource usage yet.
        //
        // In applications where the generated classes are not loaded into the
        // same JVM instance, we should avoid to use the
        // ClassLoaderIClassLoader, because that assumes that final fields have
        // a constant value, even if not compile-time-constant but only
        // initialization-time constant. The classical example is
        // "File.separator", which is non-blank final, but not compile-time-
        // constant.
        if (true) {
            IClassLoader icl;
            icl = new ResourceFinderIClassLoader(bootClassPathResourceFinder, null);
            icl = new ResourceFinderIClassLoader(extensionDirectoriesResourceFinder, icl);
            icl = new ResourceFinderIClassLoader(classPathResourceFinder, icl);
            return icl;
        } else {
            ClassLoader cl;

            // This is the only way to instantiate a "bootstrap" class loader, i.e. a class loader
            // that finds the bootstrap classes like "Object", "String" and "Throwable", but not
            // the classes on the JVM's class path.
            cl = new ClassLoader(null) {};
            cl = new ResourceFinderClassLoader(bootClassPathResourceFinder, cl);
            cl = new ResourceFinderClassLoader(extensionDirectoriesResourceFinder, cl);
            cl = new ResourceFinderClassLoader(classPathResourceFinder, cl);

            return new ClassLoaderIClassLoader(cl);
        }
    }

    public void jGrep(
        File[]                rootDirectories,
        final StringPattern[] directoryNamePatterns,
        final StringPattern[] fileNamePatterns,
        List                  methodInvocationTargets // MethodInvocationTarget
    ) throws Scanner.ScanException, Parser.ParseException, CompileException, IOException {
        this.benchmark.report("Root dirs",               rootDirectories                );
        this.benchmark.report("Directory name patterns", directoryNamePatterns          );
        this.benchmark.report("File name patterns",      fileNamePatterns);

        this.jGrep(DirectoryIterator.traverseDirectories(
            rootDirectories,              // rootDirectories
            new FilenameFilter() {        // directoryNameFilter
                public boolean accept(File dir, String name) {
                    return StringPattern.matches(directoryNamePatterns, name);
                }
            },
            new FilenameFilter() {        // fileNameFilter
                public boolean accept(File dir, String name) {
                    return StringPattern.matches(fileNamePatterns, name);
                }
            }
        ), methodInvocationTargets);
    }

    public void jGrep(Iterator sourceFilesIterator, final List methodInvocationTargets)
    throws Scanner.ScanException, Parser.ParseException, CompileException, IOException {

        // Parse the given source files.
        this.benchmark.beginReporting();
        int sourceFileCount = 0;
        try {

            // Parse all source files.
            while (sourceFilesIterator.hasNext()) {
                File sourceFile = (File) sourceFilesIterator.next();
                UnitCompiler uc = new UnitCompiler(this.parseCompilationUnit(
                    sourceFile,                    // sourceFile
                    this.optionalCharacterEncoding // optionalCharacterEncoding
                ), this.iClassLoader);
                this.parsedCompilationUnits.add(uc);
                ++sourceFileCount;
            }
        } finally {
            this.benchmark.endReporting("Parsed " + sourceFileCount + " source file(s)");
        }

        // Traverse the parsed compilation units.
        this.benchmark.beginReporting();
        try {
            for (Iterator it = this.parsedCompilationUnits.iterator(); it.hasNext();) {
                final UnitCompiler uc = (UnitCompiler) it.next();
                this.benchmark.beginReporting("Grepping \"" + uc.compilationUnit.optionalFileName + "\"");
                class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
                try {
                    new Traverser() {
    
                        // "method(...)", "x.method(...)"
                        public void traverseMethodInvocation(Java.MethodInvocation mi) {
                            try {
                                this.match(mi, uc.findIMethod(mi));
                            } catch (CompileException ex) {
                                throw new UCE(ex);
                            }
                            super.traverseMethodInvocation(mi);
                        }
    
                        // "super.method(...)"
                        public void traverseSuperclassMethodInvocation(Java.SuperclassMethodInvocation scmi) {
                            try {
                                this.match(scmi, uc.findIMethod(scmi));
                            } catch (CompileException ex) {
                                throw new UCE(ex);
                            }
                            super.traverseSuperclassMethodInvocation(scmi);
                        }
                            
                        // new Xyz(...)
                        public void traverseNewClassInstance(Java.NewClassInstance nci) {
    //                        System.out.println(nci.getLocation() + ": " + nci);
                            super.traverseNewClassInstance(nci);
                        }
    
                        // new Xyz(...) {}
                        public void traverseNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci) {
    //                        System.out.println(naci.getLocation() + ": " + naci);
                            super.traverseNewAnonymousClassInstance(naci);
                        }
    
                        // Explicit constructor invocation ("this(...)", "super(...)").
                        public void traverseConstructorInvocation(Java.ConstructorInvocation ci) {
    //                        System.out.println(ci.getLocation() + ": " + ci);
                            super.traverseConstructorInvocation(ci);
                        }

                        private void match(Java.Invocation invocation, IClass.IMethod method) throws CompileException {
                            for (Iterator it2 = methodInvocationTargets.iterator(); it2.hasNext();) {
                                MethodInvocationTarget mit = (MethodInvocationTarget) it2.next();
                                mit.apply(uc, invocation, method);
                            }
                        }
                    }.traverseCompilationUnit(uc.compilationUnit);
                } catch (UCE uce) {
                    throw uce.ce;
                } finally {
                    this.benchmark.endReporting();
                }
            }
        } finally {
            this.benchmark.endReporting("Traversed " + sourceFileCount + " compilation units");
        }
    }

    /**
     * Read one compilation unit from a file and parse it.
     * <p>
     * The <code>inputStream</code> is closed before the method returns.
     * @return the parsed compilation unit
     */
    private Java.CompilationUnit parseCompilationUnit(
        File   sourceFile,
        String optionalCharacterEncoding
    ) throws Scanner.ScanException, Parser.ParseException, IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(sourceFile));
        try {
            Parser parser = new Parser(new Scanner(sourceFile.getPath(), is, optionalCharacterEncoding));

            this.benchmark.beginReporting("Parsing \"" + sourceFile + "\"");
            try {
                return parser.parseCompilationUnit();
            } finally {
                this.benchmark.endReporting();
            }
        } finally {
            try { is.close(); } catch (IOException ex) {}
        }
    }

    /**
     * Construct the name of a file that could store the byte code of the class with the given
     * name.
     * <p>
     * If <code>optionalDestinationDirectory</code> is non-null, the returned path is the
     * <code>optionalDestinationDirectory</code> plus the package of the class (with dots replaced
     * with file separators) plus the class name plus ".class". Example:
     * "destdir/pkg1/pkg2/Outer$Inner.class"
     * <p>
     * If <code>optionalDestinationDirectory</code> is null, the returned path is the
     * directory of the <code>sourceFile</code> plus the class name plus ".class". Example:
     * "srcdir/Outer$Inner.class"
     * @param className E.g. "pkg1.pkg2.Outer$Inner"
     * @param sourceFile E.g. "srcdir/Outer.java"
     * @param optionalDestinationDirectory E.g. "destdir"
     */
    public static File getClassFile(String className, File sourceFile, File optionalDestinationDirectory) {
        if (optionalDestinationDirectory != null) {
            return new File(optionalDestinationDirectory, ClassFile.getClassFileResourceName(className));
        } else {
            int idx = className.lastIndexOf('.');
            return new File(sourceFile.getParentFile(), ClassFile.getClassFileResourceName(className.substring(idx + 1)));
        }
    }

    /**
     * A specialized {@link IClassLoader} that loads {@link IClass}es from the following
     * sources:
     * <ol>
     *   <li>An already-parsed compilation unit
     *   <li>A class file in the output directory (if existant and younger than source file)
     *   <li>A source file in any of the source path directories
     *   <li>The parent class loader
     * </ol>
     * Notice that the {@link JGrepIClassLoader} is an inner class of {@link JGrep} and
     * heavily uses {@link JGrep}'s members.
     */
    private class JGrepIClassLoader extends IClassLoader {

        /**
         * @param optionalParentIClassLoader {@link IClassLoader} through which {@link IClass}es are to be loaded
         */
        public JGrepIClassLoader(IClassLoader optionalParentIClassLoader) {
            super(optionalParentIClassLoader);
            super.postConstruct();
        }

        /**
         * @param type field descriptor of the {@IClass} to load, e.g. "Lpkg1/pkg2/Outer$Inner;"
         */
        protected IClass findIClass(final String type) {
            if (JGrep.DEBUG) System.out.println("type = " + type);

            // Class type.
            String className = Descriptor.toClassName(type); // E.g. "pkg1.pkg2.Outer$Inner"
            if (JGrep.DEBUG) System.out.println("2 className = \"" + className + "\"");

            // Do not attempt to load classes from package "java".
            if (className.startsWith("java.")) return null;

            // Check the already-parsed compilation units.
            for (int i = 0; i < JGrep.this.parsedCompilationUnits.size(); ++i) {
                UnitCompiler uc = (UnitCompiler) JGrep.this.parsedCompilationUnits.get(i);
                IClass res = uc.findClass(className);
                if (res != null) {
                    this.defineIClass(res);
                    return res;
                } 
            }
            return null;
        }
    }
}


